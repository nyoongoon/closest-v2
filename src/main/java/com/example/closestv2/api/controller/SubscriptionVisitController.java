package com.example.closestv2.api.controller;

import com.example.closestv2.api.SubscriptionVisitApi;
import com.example.closestv2.api.service.model.response.VisitSubscriptionResponse;
import com.example.closestv2.api.usecases.SubscriptionVisitUsecase;
import com.example.closestv2.util.url.UrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class SubscriptionVisitController implements SubscriptionVisitApi {
    private final SubscriptionVisitUsecase subscriptionVisitUsecase;

    @Override
    public ResponseEntity<Void> subscriptionsSubscriptionsIdVisitGet(Integer subscriptionsId) {
        VisitSubscriptionResponse response = subscriptionVisitUsecase.visitSubscription(subscriptionsId);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(UrlUtils.toUri(response.getRedirectUrl()));
        return new ResponseEntity<>(httpHeaders, HttpStatus.MOVED_PERMANENTLY);
    }

    @Override
    public ResponseEntity<Void> subscriptionsSubscriptionsIdVisitPostUrlGet(Integer subscriptionsId, URI postUrl) {
        VisitSubscriptionResponse response = subscriptionVisitUsecase.visitSubscription(subscriptionsId, UrlUtils.from(postUrl));
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(UrlUtils.toUri(response.getRedirectUrl()));
        return new ResponseEntity<>(httpHeaders, HttpStatus.MOVED_PERMANENTLY);
    }
}
