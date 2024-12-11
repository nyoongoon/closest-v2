package com.example.closestv2.api.controller;

import com.example.closestv2.api.SubscriptionQueryApi;
import com.example.closestv2.api.usecases.SubscriptionQueryUsecase;
import com.example.closestv2.models.SubscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubscriptionQueryController implements SubscriptionQueryApi {
    private SubscriptionQueryUsecase subscriptionQueryUsecase;

    @Override
    public ResponseEntity<List<SubscriptionResponse>> subscriptionsBlogsCloseGet() {
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        List<SubscriptionResponse> closeSubscriptionList = subscriptionQueryUsecase.getCloseSubscriptions((long) principal);
        return ResponseEntity.ok(closeSubscriptionList);
    }

    @Override
    public ResponseEntity<List<SubscriptionResponse>> subscriptionsBlogsGet(Integer page, Integer size) {
        return null;
    }
}
