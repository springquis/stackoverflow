package com.nxq.perform.stackoverflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nxq.perform.stackoverflow.dto.PostSummaryDto;
import com.nxq.perform.stackoverflow.entity.es.PostEs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostSearchService {

    // Inject ElasticsearchOperations thay vì Repository cho các query phức tạp
    private final ElasticsearchOperations elasticsearchOperations;
    private final RedisTemplate<String, byte[]> byteRedisTemplate;
    private final ObjectMapper objectMapper;

    // L1 Cache: Lưu thẳng byte[] trên Heap
    private final Cache<String, byte[]> localCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofSeconds(60))
            .build();

    private static final String CACHE_PREFIX = "s:kw:";
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public byte[] search(String keyword) {
        if (keyword == null || keyword.isBlank()) return new byte[0];

        String key = CACHE_PREFIX + DigestUtils.sha256Hex(keyword.trim().toLowerCase());

        // 1. L1 Cache
        byte[] l1 = localCache.getIfPresent(key);
        if (l1 != null) return l1;

        // 2. Redis Cache
        byte[] l2 = byteRedisTemplate.opsForValue().get(key);
        if (l2 != null) {
            localCache.put(key, l2);
            return l2;
        }

        // 3. Cache Miss -> Query ES Direct
        return fetchFromElasticAndCache(keyword, key);
    }

    private byte[] fetchFromElasticAndCache(String keyword, String key) {
        try {
            // --- FIX: Dùng class cụ thể 'NativeQuery' thay vì interface 'Query' chung chung ---
            NativeQuery nativeQuery = NativeQuery.builder()
                    .withSourceFilter(new FetchSourceFilter(new String[]{"id", "title"}, null))
                    .withQuery(q -> q.bool(b -> b
                            .should(s -> s.multiMatch(m -> m
                                    .query(keyword)
                                    .fields("title^3", "body_text")
                                    .fuzziness("AUTO")
                            ))
                            .should(s -> s.term(t -> t
                                    .field("tags")
                                    .value(keyword)
                                    .caseInsensitive(true)
                            ))
                            .should(s -> s.nested(n -> n
                                    .path("comments")
                                    .query(nq -> nq.match(m -> m
                                            .field("comments.text")
                                            .query(keyword)
                                    ))
                                    .scoreMode(ChildScoreMode.Max)
                            ))
                    ))
                    .withPageable(PageRequest.of(0, 5))
                    .build();

            // Lúc này nativeQuery đã đúng kiểu mà search() cần
            SearchHits<PostEs> searchHits = elasticsearchOperations.search(nativeQuery, PostEs.class);

            if (!searchHits.hasSearchHits()) {
                return "[]".getBytes();
            }

            List<PostSummaryDto> dtos = searchHits.stream()
                    .map(hit -> {
                        PostEs e = hit.getContent();
                        String title = e.getTitle() != null ? e.getTitle() : "";
                        String slug = title.toLowerCase().trim()
                                .replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
                        return new PostSummaryDto(e.getId(), title, slug);
                    })
                    .collect(Collectors.toList());

            byte[] jsonBytes = objectMapper.writeValueAsBytes(dtos);

            virtualExecutor.submit(() -> {
                try {
                    localCache.put(key, jsonBytes);
                    byteRedisTemplate.opsForValue().set(key, jsonBytes, Duration.ofMinutes(30));
                } catch (Exception ex) {
                    log.error("Cache update failed", ex);
                }
            });

            return jsonBytes;

        } catch (Exception e) {
            log.error("ES Error: {}", e.getMessage());
            return new byte[0];
        }
    }
}