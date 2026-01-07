package com.nxq.perform.stackoverflow.controller;

import com.nxq.perform.stackoverflow.entity.es.PostEs;
import com.nxq.perform.stackoverflow.service.PostSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostSearchController {

    private final PostSearchService postSearchService;

    public PostSearchController(PostSearchService postSearchService) {
        this.postSearchService = postSearchService;
    }

    // API 1: Tìm kiếm thông thường (Fuzzy, Multi-match)
    // VD: GET /api/posts/search?query=java spring
    @GetMapping("/search")
    public ResponseEntity<byte[]> search(@RequestParam String keyword) {
        // Lấy raw bytes
        byte[] responseBody = postSearchService.search(keyword);

        // Trả về ngay lập tức với Header JSON
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(responseBody);
    }

    @GetMapping("/hello")
    public String hello(){
        return "Hello world";
    }
}