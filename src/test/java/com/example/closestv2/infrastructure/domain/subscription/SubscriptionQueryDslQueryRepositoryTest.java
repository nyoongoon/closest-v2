package com.example.closestv2.infrastructure.domain.subscription;

import com.example.closestv2.domain.subscription.SubscriptionRepository;
import com.example.closestv2.domain.subscription.SubscriptionRoot;
import com.example.closestv2.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionQueryDslQueryRepositoryTest extends RepositoryTestSupport {
    private final String ANY_MEMBER_EMAIL = "abc@naver.com";
    private final String ANY_BLOG_LINK = "https://example.com/";
    private final String ANY_BLOG_TITLE = "블로그 제목";
    private final LocalDateTime ANY_PUBLISHED_DATE_TIME = LocalDateTime.of(2022, 1, 1, 12, 3, 31);

    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private SubscriptionQueryDslQueryRepository sut;

    SubscriptionQueryDslQueryRepositoryTest() throws MalformedURLException {
    }

    @Test
    @DisplayName("memberId로 visitCount 내림차순 정렬하여 조회한다.")
    void findByMemberIdVisitCountDesc() throws MalformedURLException {
        // given
        SubscriptionRoot subscriptionRoot1 = SubscriptionRoot.create(ANY_MEMBER_EMAIL,
                URI.create(ANY_BLOG_LINK + 1).toURL(), ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME.minusSeconds(1), null);
        subscriptionRepository.save(subscriptionRoot1);
        subscriptionRoot1.increaseVisitCount();
        SubscriptionRoot subscriptionRoot2 = SubscriptionRoot.create(ANY_MEMBER_EMAIL,
                URI.create(ANY_BLOG_LINK + 2).toURL(), ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME.minusSeconds(2), null);
        subscriptionRepository.save(subscriptionRoot2);
        for (int i = 0; i < 3; i++) {
            subscriptionRoot2.increaseVisitCount();
        }
        SubscriptionRoot subscriptionRoot3 = SubscriptionRoot.create(ANY_MEMBER_EMAIL,
                URI.create(ANY_BLOG_LINK + 3).toURL(), ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME.minusSeconds(3), null);
        subscriptionRepository.save(subscriptionRoot3);
        for (int i = 0; i < 2; i++) {
            subscriptionRoot3.increaseVisitCount();
        }

        // when
        List<SubscriptionRoot> subscriptionRoots = sut.findByMemberIdVisitCountDesc(ANY_MEMBER_EMAIL, 0, 10);
        // then
        assertThat(subscriptionRoots).hasSize(3)
                .isSortedAccordingTo((x, y) -> Math.toIntExact(y.getSubscriptionInfo().getSubscriptionVisitCount()
                        - x.getSubscriptionInfo().getSubscriptionVisitCount()));
    }

    @Test
    @DisplayName("memberId로 visitCount 내림차순 정렬 조회 시 페이징한다.")
    void findByMemberIdVisitCountDescPaging() throws MalformedURLException {
        // given
        for (int i = 0; i < 17; i++) {
            SubscriptionRoot subscriptionRoot1 = SubscriptionRoot.create(ANY_MEMBER_EMAIL,
                    URI.create(ANY_BLOG_LINK + i).toURL(), ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME, null);
            subscriptionRepository.save(subscriptionRoot1);
            subscriptionRoot1.increaseVisitCount();
        }
        // when
        List<SubscriptionRoot> subscriptionRoots = sut.findByMemberIdVisitCountDesc(ANY_MEMBER_EMAIL, 1, 10);
        // then
        assertThat(subscriptionRoots).hasSize(7);
    }

    @Test
    @DisplayName("memberId로 publishedDateTime 내림차순 정렬하여 조회힌다.")
    void findByMemberIdPublishedDateTimeDesc() throws MalformedURLException {
        // given
        SubscriptionRoot subscriptionRoot1 = SubscriptionRoot.create(ANY_MEMBER_EMAIL,
                URI.create(ANY_BLOG_LINK + 1).toURL(), ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME.minusSeconds(1), null);
        subscriptionRepository.save(subscriptionRoot1);
        SubscriptionRoot subscriptionRoot2 = SubscriptionRoot.create(ANY_MEMBER_EMAIL,
                URI.create(ANY_BLOG_LINK + 2).toURL(), ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME.minusSeconds(2), null);
        subscriptionRepository.save(subscriptionRoot2);
        SubscriptionRoot subscriptionRoot3 = SubscriptionRoot.create(ANY_MEMBER_EMAIL,
                URI.create(ANY_BLOG_LINK + 3).toURL(), ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME.minusSeconds(3), null);
        subscriptionRepository.save(subscriptionRoot3);
        // when
        List<SubscriptionRoot> subscriptionRoots = sut.findByMemberIdPublishedDateTimeDesc(ANY_MEMBER_EMAIL, 0, 10);
        // then
        assertThat(subscriptionRoots).hasSize(3)
                .isSortedAccordingTo((x, y) -> y.getSubscriptionBlog().getPublishedDateTime()
                        .compareTo(x.getSubscriptionBlog().getPublishedDateTime()));
    }

    @Test
    @DisplayName("memberId로 publishedDateTime 내림차순 정렬 조회 시 페이징한다.")
    void findByMemberIdPublishedDateTimeDescPaging() throws MalformedURLException {
        // given
        // when
        // then
    }
}