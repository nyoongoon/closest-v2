package com.example.closestv2.infrastructure.domain.subscription;

import com.example.closestv2.domain.subscription.SubscriptionQueryRepository;
import com.example.closestv2.domain.subscription.SubscriptionRoot;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.closestv2.domain.subscription.QSubscriptionRoot.subscriptionRoot;

@Repository
@RequiredArgsConstructor
public class SubscriptionQueryDslQueryRepository implements SubscriptionQueryRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<SubscriptionRoot> findByMemberIdVisitCountDesc(long memberId, int page, int size) {
                return queryFactory.selectFrom(subscriptionRoot)
                .where(subscriptionRoot.subscriptionInfo.memberId.eq(memberId))
                .orderBy(subscriptionRoot.subscriptionInfo.subscriptionVisitCount.desc())
                .offset(page * size)
                .limit(size)
                .fetch();
    }

    @Override
    public List<SubscriptionRoot> findByMemberIdPublishedDateTimeDesc(long memberId, int page, int size) {
        return queryFactory.selectFrom(subscriptionRoot)
                .where(subscriptionRoot.subscriptionInfo.memberId.eq(memberId))
                .orderBy(subscriptionRoot.subscriptionBlog.publishedDateTime.desc())
                .offset(page * size)
                .limit(size)
                .fetch();
    }
}
