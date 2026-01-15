package com.example.closestv2.api.usecases;

import com.example.closestv2.api.service.model.response.VisitSubscriptionResponse;
import org.springframework.stereotype.Service;

import java.net.URL;

@Service
public interface SubscriptionVisitUsecase {
    VisitSubscriptionResponse visitSubscription(long subscriptionsId);

    //todo post 방문 이벤트 -> subscription, blog, post visitCount 증가
    VisitSubscriptionResponse visitSubscription(long subscriptionsId, URL postUrl);
}
