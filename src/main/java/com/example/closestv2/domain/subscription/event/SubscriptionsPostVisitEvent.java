package com.example.closestv2.domain.subscription.event;

import java.net.URL;

public record SubscriptionsPostVisitEvent(
        long subscriptionsId,
        URL blogUrl,
        URL postUrl
) {
}
