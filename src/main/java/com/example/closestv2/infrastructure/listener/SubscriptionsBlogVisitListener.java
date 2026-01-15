package com.example.closestv2.infrastructure.listener;

import com.example.closestv2.api.service.BlogVisitService;
import com.example.closestv2.domain.subscription.event.SubscriptionsBlogVisitEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URL;

@Component
@RequiredArgsConstructor
public class SubscriptionsBlogVisitListener {
    private final BlogVisitService blogVisitService;

    @EventListener
    public void onSubscriptionsBlogVisitEvent(SubscriptionsBlogVisitEvent event) {
        URL blogUrl = event.blogUrl();
        blogVisitService.visitBlog(blogUrl);
    }
}
