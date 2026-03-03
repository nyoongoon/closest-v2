package com.example.closestv2.api.controller;

import com.example.closestv2.api.SubscriptionQueryApi;
import com.example.closestv2.api.usecases.SubscriptionQueryUsecase;
import com.example.closestv2.models.SubscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubscriptionQueryController implements SubscriptionQueryApi {
    private final SubscriptionQueryUsecase subscriptionQueryUsecase;

    @Override
    public ResponseEntity<List<SubscriptionResponse>> subscriptionsBlogsCloseGet() {
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        // AnonymousAuthenticationToken도 isAuthenticated()가 true를 반환하므로 principal 타입으로 판별
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            List<SubscriptionResponse> closeSubscriptionsOfAll = subscriptionQueryUsecase.getCloseSubscriptionsOfAll();
            return ResponseEntity.ok(closeSubscriptionsOfAll);
        }
        List<SubscriptionResponse> closeSubscriptions = subscriptionQueryUsecase.getCloseSubscriptions(user.getUsername());
        return ResponseEntity.ok(closeSubscriptions);
    }

    @Override
    public ResponseEntity<List<SubscriptionResponse>> subscriptionsBlogsGet(Integer page, Integer size) {
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        List<SubscriptionResponse> subscriptions = subscriptionQueryUsecase.getRecentPublishedSubscriptions(user.getUsername(), page, size);
        return ResponseEntity.ok(subscriptions);
    }
}
