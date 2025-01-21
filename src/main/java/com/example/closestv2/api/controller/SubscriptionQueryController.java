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
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.MEMBER_NOT_FOUND;

@RestController
@RequiredArgsConstructor
public class SubscriptionQueryController implements SubscriptionQueryApi {
    private final SubscriptionQueryUsecase subscriptionQueryUsecase;

    @Override
    public ResponseEntity<List<SubscriptionResponse>> subscriptionsBlogsCloseGet() {
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        if(!authentication.isAuthenticated()){
            throw new IllegalArgumentException(MEMBER_NOT_FOUND);
        }
        String email = (String) authentication.getPrincipal();
        List<SubscriptionResponse> closeSubscriptions = subscriptionQueryUsecase.getCloseSubscriptions(email);
        return ResponseEntity.ok(closeSubscriptions);
    }

    @Override
    public ResponseEntity<List<SubscriptionResponse>> subscriptionsBlogsGet(Integer page, Integer size) {
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        String email = (String) authentication.getPrincipal();
        List<SubscriptionResponse> subscriptions = subscriptionQueryUsecase.getRecentPublishedSubscriptions(email, page, size);
        return ResponseEntity.ok(subscriptions);
    }
}
