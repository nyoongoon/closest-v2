package com.example.closestv2.api.controller;

import com.example.closestv2.api.SubscriptionRegisterApi;
import com.example.closestv2.api.service.model.request.SubscriptionsPostServiceRequest;
import com.example.closestv2.api.usecases.SubscriptionRegisterUsecase;
import com.example.closestv2.models.SubscriptionsPostRequest;
import com.example.closestv2.util.url.UrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubscriptionRegisterController implements SubscriptionRegisterApi {
    private SubscriptionRegisterUsecase subscriptionRegisterUsecase;

    @Override
    public ResponseEntity<Void> subscriptionsPost(SubscriptionsPostRequest subscriptionsPostRequest) {
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        String memberEmail = (String) authentication.getPrincipal();
        SubscriptionsPostServiceRequest serviceRequest = toServiceRequest(memberEmail, subscriptionsPostRequest);
        subscriptionRegisterUsecase.registerSubscription(serviceRequest);
        return ResponseEntity.ok().build();
    }

    private SubscriptionsPostServiceRequest toServiceRequest(String memberEmail, SubscriptionsPostRequest request) {
        return SubscriptionsPostServiceRequest.builder()
                .memberEmail(memberEmail)
                .rssUrl(UrlUtils.from(request.getRssUri()))
                .build();
    }

    @Override
    public ResponseEntity<Void> subscriptionsSubscriptionsIdDelete(Long subscriptionsId) {
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        String memberEmail = (String) authentication.getPrincipal();
        subscriptionRegisterUsecase.unregisterSubscription(memberEmail, subscriptionsId);
        return ResponseEntity.ok().build();
    }
}
