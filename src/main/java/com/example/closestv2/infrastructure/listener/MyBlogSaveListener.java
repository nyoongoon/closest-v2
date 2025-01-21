package com.example.closestv2.infrastructure.listener;

import com.example.closestv2.api.service.MyBlogSaveService;
import com.example.closestv2.domain.blog.event.MyBlogSaveEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URL;

@Component
@RequiredArgsConstructor
public class MyBlogSaveListener {
    private final MyBlogSaveService myBlogSaveService;

    @EventListener
    public void onMyBlogCreationEvent(MyBlogSaveEvent event) {
        String memberEmail = event.memberEmail();
        URL blogUrl = event.blogUrl();
        myBlogSaveService.saveMyBlog(memberEmail, blogUrl);
    }
}
