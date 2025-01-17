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
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.ACCESS_DENIED_BY_MEMBER_ID;
import static com.example.closestv2.api.exception.ExceptionMessageConstants.NOT_FOUND_SUBSCRIPTION;

@Service
@RequiredArgsConstructor
public class SubscriptionRegisterService implements SubscriptionRegisterUsecase {
    private final FeedClient feedClient;
    private final BlogRepository blogRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional
    public void registerSubscription(SubscriptionsPostServiceRequest serviceRequest) {
        String memberEmail = serviceRequest.getMemberEmail();
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
        SubscriptionRoot subscriptionRoot = SubscriptionRoot.create(memberEmail, blogUrl, blogTitle, publishedDateTime);
        subscriptionRepository.save(subscriptionRoot);
    }

    @Override
    public void unregisterSubscription(String memberEmail, long subscriptionId) {
        SubscriptionRoot subscriptionRoot = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_SUBSCRIPTION));

        String foundMemberEmail = subscriptionRoot.getSubscriptionInfo().getMemberEmail();
        if (!memberEmail.equals(foundMemberEmail)) {
            throw new AccessDeniedException(ACCESS_DENIED_BY_MEMBER_ID);
        }

        subscriptionRepository.delete(subscriptionRoot);
    }
}
