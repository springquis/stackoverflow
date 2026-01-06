package com.nxq.perform.stackoverflow.service;

import com.nxq.perform.stackoverflow.entity.es.PostEs;
import com.nxq.perform.stackoverflow.entity.sql.CommentSql;
import com.nxq.perform.stackoverflow.entity.sql.PostSql;
import com.nxq.perform.stackoverflow.repository.CommentSqlRepository;
import com.nxq.perform.stackoverflow.repository.PostEsRepository;
import com.nxq.perform.stackoverflow.repository.PostSqlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationService {

    private final PostSqlRepository postSqlRepository;
    private final CommentSqlRepository commentSqlRepository;
    private final PostEsRepository postEsRepository;

    private final Pattern TAG_PATTERN = Pattern.compile("<([^>]+)>");

    // CONFIG TỐI ƯU HIỆU NĂNG
    // Tăng số lượng bản ghi mỗi lần query (Query to hơn, ít lần query hơn)
    private static final int PAGE_SIZE = 2000;

    // Số luồng chạy song song (Số trang xử lý cùng lúc)
    // Virtual thread rất nhẹ, nhưng ta giới hạn để không làm sập Database Connection Pool
    private static final int CONCURRENCY_LIMIT = 20;

    @Transactional(readOnly = true)
    public void migratePosts() {
        long startTime = System.currentTimeMillis();

        // 1. Tính toán tổng số trang cần chạy
        long totalPosts = postSqlRepository.count();
        int totalPages = (int) Math.ceil((double) totalPosts / PAGE_SIZE);

        log.info("Bắt đầu migrate. Tổng records: {}. Page Size: {}. Tổng trang: {}", totalPosts, PAGE_SIZE, totalPages);

        // 2. Khởi tạo Executor sử dụng Virtual Threads (Java 21+)
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (int i = 0; i < totalPages; i++) {
                int pageNumber = i;

                // Submit task xử lý trang thứ 'i' vào Virtual Thread
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    processPage(pageNumber);
                }, executor);

                futures.add(future);

                // 3. Kiểm soát áp lực (Backpressure):
                // Nếu danh sách task đang chờ >= giới hạn (ví dụ 20), thì dừng lại đợi chúng xong mới submit tiếp.
                // Điều này giúp Connection Pool không bị quá tải và RAM không bị tràn.
                if (futures.size() >= CONCURRENCY_LIMIT) {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    futures.clear();
                    log.info("Đã hoàn thành batch {} trang tiếp theo...", CONCURRENCY_LIMIT);
                }
            }

            // Chạy nốt những task còn sót lại ở cuối vòng lặp
            if (!futures.isEmpty()) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("MIGRATION HOÀN TẤT trong {} ms", duration);
    }

    /**
     * Logic xử lý trọn vẹn 1 trang dữ liệu (Fetch -> Transform -> Push)
     * Hàm này sẽ chạy trên 1 Virtual Thread riêng biệt.
     */
    private void processPage(int pageNumber) {
        try {
            log.debug("Thread-{}: Đang xử lý trang {}", Thread.currentThread().threadId(), pageNumber);

            // 1. Lấy dữ liệu Post
            Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
            Page<PostSql> pagePosts = postSqlRepository.findAll(pageable);
            List<PostSql> posts = pagePosts.getContent();

            if (posts.isEmpty()) return;

            // 2. Lấy ID để query Comments
            List<Long> postIds = posts.stream().map(PostSql::getId).toList();

            // 3. Query Comments (Bulk query)
            List<CommentSql> comments = commentSqlRepository.findByPostIdIn(postIds);

            // 4. Group Comments in Memory
            Map<Long, List<CommentSql>> commentsByPostId = comments.stream()
                    .collect(Collectors.groupingBy(CommentSql::getPostId));

            // 5. Transform sang ES Document
            List<PostEs> esDocuments = new ArrayList<>(posts.size());
            for (PostSql sqlPost : posts) {
                List<CommentSql> relatedComments = commentsByPostId.getOrDefault(sqlPost.getId(), Collections.emptyList());

                List<PostEs.CommentEs> esComments = relatedComments.stream()
                        .map(c -> PostEs.CommentEs.builder()
                                .text(c.getText())
                                .user_id(c.getUserId())
                                .build())
                        .collect(Collectors.toList());

                List<String> cleanTags = parseTags(sqlPost.getTags());

                PostEs esPost = PostEs.builder()
                        .id(sqlPost.getId())
                        .title(sqlPost.getTitle())
                        .body(sqlPost.getBody())
                        .tags(cleanTags)
                        .owner_user_id(sqlPost.getOwnerUserId())
                        .score(sqlPost.getScore())
                        .creation_date(sqlPost.getCreationDate())
                        .comments(esComments)
                        .build();

                esDocuments.add(esPost);
            }

            // 6. Bulk Insert vào Elasticsearch
            if (!esDocuments.isEmpty()) {
                postEsRepository.saveAll(esDocuments);
            }

        } catch (Exception e) {
            log.error("Lỗi khi xử lý trang {}: {}", pageNumber, e.getMessage());
            // Có thể throw tiếp nếu muốn dừng chương trình, hoặc log để bỏ qua trang lỗi
        }
    }

    private List<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isEmpty()) return Collections.emptyList();
        List<String> tags = new ArrayList<>();
        Matcher matcher = TAG_PATTERN.matcher(rawTags);
        while (matcher.find()) {
            tags.add(matcher.group(1));
        }
        return tags;
    }
}