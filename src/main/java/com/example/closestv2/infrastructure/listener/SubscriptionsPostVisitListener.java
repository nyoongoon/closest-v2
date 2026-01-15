package com.example.closestv2.infrastructure.listener;


import com.example.closestv2.api.service.BlogVisitService;
import com.example.closestv2.domain.subscription.event.SubscriptionsPostVisitEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URL;

@Component
@RequiredArgsConstructor
public class SubscriptionsPostVisitListener {
    private final BlogVisitService blogVisitService;

    @EventListener
    public void onSubscriptionsPostVisitEvent(SubscriptionsPostVisitEvent event) {
        URL blogUrl = event.blogUrl();
        URL postUrl = event.postUrl();
        blogVisitService.visitPost(blogUrl, postUrl);
    }
}
