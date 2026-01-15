package com.example.closestv2.api.service;

import com.example.closestv2.domain.blog.BlogRepository;
import com.example.closestv2.domain.blog.BlogRoot;
import com.example.closestv2.domain.blog.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.NOT_EXISTS_BLOG;
import static com.example.closestv2.api.exception.ExceptionMessageConstants.NOT_EXISTS_POST_URL;

@Service
@RequiredArgsConstructor
public class BlogVisitService {
    private final BlogRepository blogRepository;

    @Transactional
    public void visitBlog(URL blogUrl){
        BlogRoot blogRoot = blogRepository.findByBlogInfoBlogUrl(blogUrl).orElseThrow(() -> new IllegalArgumentException(NOT_EXISTS_BLOG));
        blogRoot.visit();
    }

    @Transactional
    public void visitPost(URL blogUrl, URL postUrl){
        BlogRoot blogRoot = blogRepository.findByBlogInfoBlogUrl(blogUrl).orElseThrow(() -> new IllegalArgumentException(NOT_EXISTS_BLOG));
        blogRoot.visit();
        blogRoot.visitPost(postUrl);
    }
}
