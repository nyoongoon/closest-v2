package com.example.closestv2.api.usecases;


import com.example.closestv2.api.service.model.request.SubscriptionsPostServiceRequest;
import org.springframework.stereotype.Service;

@Service
public interface SubscriptionRegisterUsecase {
    void registerSubscription(SubscriptionsPostServiceRequest serviceRequest);
    void unregisterSubscription(String memberEmail, long subscriptionId);
}
