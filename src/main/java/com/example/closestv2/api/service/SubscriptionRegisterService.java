package com.example.closestv2.api.service;


import com.example.closestv2.api.service.model.request.SubscriptionsPostServiceRequest;
import com.example.closestv2.api.usecases.SubscriptionRegisterUsecase;
import com.example.closestv2.domain.blog.BlogRepository;
import com.example.closestv2.domain.blog.BlogRoot;
import com.example.closestv2.domain.feed.Feed;
import com.example.closestv2.domain.feed.FeedClient;
import com.example.closestv2.domain.subscription.SubscriptionRepository;
import com.example.closestv2.domain.subscription.SubscriptionRoot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionRegisterService implements SubscriptionRegisterUsecase {
    private final FeedClient feedClient;
    private final BlogRepository blogRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional
    public void registerSubscription(SubscriptionsPostServiceRequest serviceRequest) {
        long memberId = serviceRequest.getMemberId();
        URL rssUrl = serviceRequest.getRssUrl();

        BlogRoot blogRoot;
        Optional<BlogRoot> blogRootOptional = blogRepository.findByBlogInfoRssUrl(rssUrl);
        if (blogRootOptional.isPresent()) {
            blogRoot = blogRootOptional.get();
        } else {
            Feed feed = feedClient.getFeed(rssUrl);
            blogRoot = feed.toBlogRoot();
        }

        URL blogUrl = blogRoot.getBlogInfo().getBlogUrl();
        String blogTitle = blogRoot.getBlogInfo().getBlogTitle();
        LocalDateTime publishedDateTime = blogRoot.getBlogInfo().getPublishedDateTime();
        SubscriptionRoot subscriptionRoot = SubscriptionRoot.create(memberId, blogUrl, blogTitle, publishedDateTime);
        subscriptionRepository.save(subscriptionRoot);
    }

    @Override
    public void unregisterSubscription(long memberId, long subscriptionId) {

    }
}
