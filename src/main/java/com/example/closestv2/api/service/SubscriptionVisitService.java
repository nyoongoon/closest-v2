package com.example.closestv2.api.service;

import com.example.closestv2.api.service.model.response.VisitSubscriptionResponse;
import com.example.closestv2.api.usecases.SubscriptionVisitUsecase;
import com.example.closestv2.domain.subscription.SubscriptionRepository;
import com.example.closestv2.domain.subscription.SubscriptionRoot;
import com.example.closestv2.domain.subscription.event.SubscriptionsBlogVisitEvent;
import com.example.closestv2.domain.subscription.event.SubscriptionsPostVisitEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.net.URL;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.NOT_FOUND_SUBSCRIPTION;

@Service
@RequiredArgsConstructor
public class SubscriptionVisitService implements SubscriptionVisitUsecase {
    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    //todo 블로그 방문 이벤트 발생
    //todo blog 방문 이벤트 -> subscription, blog visitCount 증가

    /**
     * blog 방문 이벤트 -> subscription, blog visitCount 증가
     *
     * @param subscriptionsId
     * @return
     * @see SubscriptionsBlogVisitEvent
     * @see BlogVisitService#visitBlog(URL)
     */
    @Override
    public VisitSubscriptionResponse visitSubscription(long subscriptionsId) {
        SubscriptionRoot subscriptionRoot = subscriptionRepository.findById(subscriptionsId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_SUBSCRIPTION));
        URL blogUrl = subscriptionRoot.getSubscriptionBlog().getBlogUrl();

        SubscriptionsBlogVisitEvent event = subscriptionRoot.increaseVisitCount();
        eventPublisher.publishEvent(event);
        return new VisitSubscriptionResponse(blogUrl);
    }

    /**
     * post 방문 이벤트 -> subscription, blog, post visitCount 증가
     *
     * @param subscriptionsId
     * @param postUrl
     * @return
     * @see SubscriptionsPostVisitEvent
     * @see BlogVisitService#visitPost(URL, URL)
     */
    @Override
    public VisitSubscriptionResponse visitSubscription(long subscriptionsId, URL postUrl) {
        SubscriptionRoot subscriptionRoot = subscriptionRepository.findById(subscriptionsId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_SUBSCRIPTION));
        SubscriptionsPostVisitEvent event = subscriptionRoot.increasePostVisitCount(postUrl);
        eventPublisher.publishEvent(event);
        return new VisitSubscriptionResponse(postUrl);
    }
}
