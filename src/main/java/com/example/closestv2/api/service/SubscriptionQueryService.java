package com.example.closestv2.api.service;

import com.example.closestv2.api.usecases.SubscriptionQueryUsecase;
import com.example.closestv2.domain.subscription.SubscriptionQueryRepository;
import com.example.closestv2.domain.subscription.SubscriptionRoot;
import com.example.closestv2.models.SubscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.SERVER_ERROR;

@Service
@RequiredArgsConstructor
public class SubscriptionQueryService implements SubscriptionQueryUsecase {
    private final SubscriptionQueryRepository subscriptionQueryRepository;

    @Override
    public List<SubscriptionResponse> getCloseSubscriptionsOfAll() {
        List<SubscriptionRoot> subscriptionRoots = subscriptionQueryRepository.findAllOrderByVisitCountDesc(0, 20);
        return extractSubscriptionResponses(subscriptionRoots, new ArrayList<>());
    }

    @Override
    public List<SubscriptionResponse> getCloseSubscriptions(String memberEmail) {
        List<SubscriptionRoot> subscriptionRoots = subscriptionQueryRepository.findByMemberIdVisitCountDesc(memberEmail,
                0, 20);
        return extractSubscriptionResponses(subscriptionRoots, new ArrayList<>());
    }

    @Override
    public List<SubscriptionResponse> getRecentPublishedSubscriptions(String memberEmail, int page, int size) {
        List<SubscriptionRoot> subscriptionRoots = subscriptionQueryRepository
                .findByMemberIdPublishedDateTimeDesc(memberEmail, page, size);
        return extractSubscriptionResponses(subscriptionRoots, new ArrayList<>());
    }

    private List<SubscriptionResponse> extractSubscriptionResponses(List<SubscriptionRoot> subscriptionRoots,
            List<SubscriptionResponse> responses) {
        for (SubscriptionRoot subscriptionRoot : subscriptionRoots) {
            URI uri;
            URI thumbnailUrl = null;
            try {
                uri = subscriptionRoot.getSubscriptionBlog().getBlogUrl().toURI();
                if (subscriptionRoot.getSubscriptionBlog().getThumbnailUrl() != null) {
                    thumbnailUrl = subscriptionRoot.getSubscriptionBlog().getThumbnailUrl().toURI();
                }
            } catch (URISyntaxException e) {
                throw new IllegalStateException(SERVER_ERROR);
            }
            responses.add(
                    new SubscriptionResponse()
                            .subscriptionId(subscriptionRoot.getId())
                            .uri(uri)
                            .thumbnailUrl(thumbnailUrl)
                            .nickName(subscriptionRoot.getSubscriptionInfo().getSubscriptionNickName())
                            .newPostsCnt(subscriptionRoot.getSubscriptionBlog().getNewPostCount())
                            .visitCnt(subscriptionRoot.getSubscriptionInfo().getSubscriptionVisitCount())
                            .publishedDateTime(subscriptionRoot.getSubscriptionBlog().getPublishedDateTime()));
        }
        return responses;
    }
}
