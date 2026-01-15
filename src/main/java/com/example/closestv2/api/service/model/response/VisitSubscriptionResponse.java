package com.example.closestv2.api.service.model.response;

import lombok.Getter;

import java.net.URL;

@Getter
public class VisitSubscriptionResponse {
    URL redirectUrl;

    public VisitSubscriptionResponse(URL redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}
