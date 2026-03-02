package com.example.closestv2.api.controller;

import com.example.closestv2.domain.blog.BlogRepository;
import com.example.closestv2.domain.blog.BlogRoot;
import com.example.closestv2.domain.blog.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class RecentPostController {

    private final BlogRepository blogRepository;

    @GetMapping("/posts/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentPosts(
            @RequestParam(defaultValue = "30") int limit) {

        List<BlogRoot> blogs = blogRepository.findAll();

        List<Map<String, Object>> recentPosts = blogs.stream()
                .flatMap(blog -> blog.getPosts().values().stream()
                        .map(post -> {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("postTitle", post.getPostTitle());
                            item.put("postUrl", post.getPostUrl().toString());
                            item.put("publishedDateTime", post.getPublishedDateTime().toString());
                            item.put("blogTitle", blog.getBlogInfo().getBlogTitle());
                            item.put("blogUrl", blog.getBlogInfo().getBlogUrl().toString());
                            item.put("author", blog.getBlogInfo().getAuthor());
                            if (blog.getBlogInfo().getThumbnailUrl() != null) {
                                item.put("thumbnailUrl", blog.getBlogInfo().getThumbnailUrl().toString());
                            }
                            return item;
                        }))
                .sorted((a, b) -> b.get("publishedDateTime").toString()
                        .compareTo(a.get("publishedDateTime").toString()))
                .limit(limit)
                .collect(Collectors.toList());

        return ResponseEntity.ok(recentPosts);
    }
}
