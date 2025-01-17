package com.example.closestv2.api.usecases;

import com.example.closestv2.models.SubscriptionResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SubscriptionQueryUsecase {
    List<SubscriptionResponse> getCloseSubscriptions(String memberEmail);
    List<SubscriptionResponse> getRecentPublishedSubscriptions(String memberEmail, int page, int size);
}
