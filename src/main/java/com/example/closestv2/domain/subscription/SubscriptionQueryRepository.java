package com.example.closestv2.domain.subscription;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionQueryRepository {
    List<SubscriptionRoot> findByMemberIdVisitCountDesc(String memberEmail, int page, int size);

    List<SubscriptionRoot> findByMemberIdPublishedDateTimeDesc(String memberEmail, int page, int size);
}
