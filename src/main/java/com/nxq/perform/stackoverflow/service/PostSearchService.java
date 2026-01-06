package com.nxq.perform.stackoverflow.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.nxq.perform.stackoverflow.entity.es.PostEs;
import com.nxq.perform.stackoverflow.repository.PostEsRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostSearchService {

    private final PostEsRepository postEsRepository;
    private final ElasticsearchOperations elasticsearchOperations; // Dùng cho Query phức tạp (MLT)
    private final RedisTemplate<String, Object> redisTemplate;

    @Qualifier("localSearchCache")
    private final Cache<String, Object> localCache;

    // Executor ảo cho tác vụ lưu cache ngầm
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private static final String KEYWORD_PREFIX = "search:kw:";
    private static final String MLT_PREFIX = "search:mlt:";
    private static final String TRENDING_KEY = "search:trending";


    // CHIẾN LƯỢC 1: FULL-TEXT SEARCH (TÌM THEO TỪ KHÓA)
    @CircuitBreaker(name = "esSearch", fallbackMethod = "fallbackSearchByKeyword")
    @SuppressWarnings("unchecked")
    public List<PostEs> searchByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();

        // 1. Check Cache (L1 -> L2)
        String cacheKey = KEYWORD_PREFIX + DigestUtils.sha256Hex(keyword.trim().toLowerCase());
        List<PostEs> cached = getFromCache(cacheKey);
        if (cached != null) return cached;

        log.info("[Strategy 1] Searching ES for keyword: {}", keyword);

        // 2. Query Elasticsearch (Thông qua Repository bạn đã viết)
        List<PostEs> results = postEsRepository.searchByKeyword(keyword);

        // 3. Update Cache Async
        updateCacheAsync(cacheKey, results);

        return results;
    }

    // CHIẾN LƯỢC 2: MORE LIKE THIS (TÌM BÀI VIẾT TƯƠNG TỰ)
    @CircuitBreaker(name = "esSearch", fallbackMethod = "fallbackMoreLikeThis")
    @SuppressWarnings("unchecked")
    public List<PostEs> findMoreLikeThis(Long postId) {
        if (postId == null) return Collections.emptyList();

        // 1. Check Cache
        String cacheKey = MLT_PREFIX + postId;
        List<PostEs> cached = getFromCache(cacheKey);
        if (cached != null) return cached;

        log.info("[Strategy 2] Finding similar posts for ID: {}", postId);

        // 2. Build Query More Like This (Spring Boot 3 / ES 8 client)
        // Tìm các bài viết có nội dung (title, body) giống với bài postId
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .moreLikeThis(mlt -> mlt
                                .like(l -> l.document(d -> d.index("post_es").id(String.valueOf(postId))))
                                .fields("title", "body") // So sánh dựa trên tiêu đề và nội dung
                                .minTermFreq(1)          // Tần suất từ xuất hiện tối thiểu
                                .minDocFreq(1)           // Tần suất document tối thiểu
                                .maxQueryTerms(12)       // Giới hạn số từ khóa dùng để so sánh (để đỡ nặng)
                        )
                )
                .withPageable(PageRequest.of(0, 10)) // Lấy 10 bài tương tự nhất
                .build();

        SearchHits<PostEs> searchHits = elasticsearchOperations.search(query, PostEs.class);
        List<PostEs> results = searchHits.stream()
                .map(org.springframework.data.elasticsearch.core.SearchHit::getContent)
                .collect(Collectors.toList());

        // 3. Update Cache
        updateCacheAsync(cacheKey, results);

        return results;
    }

    @SuppressWarnings("unchecked")
    private List<PostEs> getFromCache(String key) {
        // Check L1
        List<PostEs> l1 = (List<PostEs>) localCache.getIfPresent(key);
        if (l1 != null) return l1;

        // Check L2
        try {
            List<PostEs> l2 = (List<PostEs>) redisTemplate.opsForValue().get(key);
            if (l2 != null) {
                localCache.put(key, l2); // Promote to L1
                return l2;
            }
        } catch (Exception e) {
            log.warn("Redis error (L2 missed): {}", e.getMessage());
        }
        return null;
    }

    private void updateCacheAsync(String key, List<PostEs> data) {
        if (data == null || data.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                localCache.put(key, data);
                redisTemplate.opsForValue().set(key, data, Duration.ofHours(1));
            } catch (Exception e) {
                log.error("Cache update failed", e);
            }
        }, virtualExecutor);
    }

    // --- FALLBACK METHODS
    @Scheduled(fixedRate = 600000)
    public void refreshTrendingPosts() {
        log.info("Refreshing Trending Posts Cache...");
        try {
            // Query lấy top 20 bài có score cao nhất (Sort DESC)
            Query query = NativeQuery.builder()
                    .withQuery(q -> q.matchAll(m -> m))
                    .withSort(Sort.by(Sort.Direction.DESC, "score"))
                    .withPageable(PageRequest.of(0, 20))
                    .build();

            SearchHits<PostEs> hits = elasticsearchOperations.search(query, PostEs.class);
            List<PostEs> trendingPosts = hits.stream()
                    .map(org.springframework.data.elasticsearch.core.SearchHit::getContent)
                    .collect(Collectors.toList());

            if (!trendingPosts.isEmpty()) {
                // 1. Lưu vào Redis (TTL dài: 1 ngày để an toàn)
                redisTemplate.opsForValue().set(TRENDING_KEY, trendingPosts, Duration.ofDays(1));

                // 2. Lưu vào Local Cache ngay lập tức
                localCache.put(TRENDING_KEY, trendingPosts);

                log.info("Trending Posts updated successfully: {} items", trendingPosts.size());
            }
        } catch (Exception e) {
            log.error("Failed to refresh Trending Posts", e);
        }
    }

    // FALLBACK METHODS (TRẢ VỀ HOT SEARCH TỪ CACHE)

    public List<PostEs> fallbackSearchByKeyword(String keyword, Throwable t) {
        log.error("⚠️ Circuit Breaker OPEN for Keyword: '{}'. Reason: {}. Returning TRENDING POSTS.", keyword, t.getMessage());
        return getTrendingPosts();
    }

    public List<PostEs> fallbackMoreLikeThis(Long postId, Throwable t) {
        log.error("⚠️ Circuit Breaker OPEN for MLT ID: '{}'. Reason: {}. Returning TRENDING POSTS.", postId, t.getMessage());
        return getTrendingPosts();
    }

    @SuppressWarnings("unchecked")
    private List<PostEs> getTrendingPosts() {
        // 1. Thử lấy từ Local Cache (Cực nhanh)
        List<PostEs> localTrending = (List<PostEs>) localCache.getIfPresent(TRENDING_KEY);
        if (localTrending != null && !localTrending.isEmpty()) {
            return localTrending;
        }

        // 2. Nếu Local không có (ví dụ mới restart app), lấy từ Redis
        try {
            List<PostEs> redisTrending = (List<PostEs>) redisTemplate.opsForValue().get(TRENDING_KEY);
            if (redisTrending != null && !redisTrending.isEmpty()) {
                // Đẩy ngược lại vào Local Cache để lần sau nhanh hơn
                localCache.put(TRENDING_KEY, redisTrending);
                return redisTrending;
            }
        } catch (Exception e) {
            log.error("Failed to fetch trending from Redis", e);
        }

        // 3. Nếu cả 2 đều không có -> Chấp nhận trả về rỗng
        return Collections.emptyList();
    }


}