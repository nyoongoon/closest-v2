package com.example.closestv2.api.service.model.request;

import lombok.Builder;
import lombok.Getter;

import java.net.URL;

@Getter
public class SubscriptionsPostServiceRequest {
    private String memberEmail;
    private URL rssUrl;

    @Builder
    public SubscriptionsPostServiceRequest(String memberEmail, URL rssUrl) {
        this.memberEmail = memberEmail;
        this.rssUrl = rssUrl;
    }
}
