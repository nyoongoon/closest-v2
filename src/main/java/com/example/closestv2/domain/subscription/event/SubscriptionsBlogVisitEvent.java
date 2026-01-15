package com.example.closestv2.domain.subscription.event;

import java.net.URL;

public record SubscriptionsBlogVisitEvent(
        long subscriptionsId,
        URL blogUrl
) {
}
