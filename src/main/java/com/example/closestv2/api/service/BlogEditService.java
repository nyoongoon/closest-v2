package com.example.closestv2.api.service;

import com.example.closestv2.domain.blog.BlogRepository;
import com.example.closestv2.domain.blog.BlogRoot;
import com.example.closestv2.infrastructure.listener.StatusMessageChangeEditListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.NOT_EXISTS_BLOG;

@Service
@RequiredArgsConstructor
public class BlogEditService {
    private final BlogRepository blogRepository;

    /**
     * @see StatusMessageChangeEditListener
     */
    @Transactional
    public void editStatueMessage(URL blogUrl, String statusMessage) {
        BlogRoot blogRoot = blogRepository.findByBlogInfoBlogUrl(blogUrl).orElseThrow(() -> new IllegalArgumentException(NOT_EXISTS_BLOG));
        blogRoot.editStatusMessage(statusMessage);
    }
}
