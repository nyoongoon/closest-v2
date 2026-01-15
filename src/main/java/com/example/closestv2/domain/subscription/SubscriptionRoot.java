package com.example.closestv2.domain.subscription;

import com.example.closestv2.domain.subscription.event.SubscriptionsBlogVisitEvent;
import com.example.closestv2.domain.subscription.event.SubscriptionsPostVisitEvent;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URL;
import java.time.LocalDateTime;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.RECENT_PUBLISHED_DATETIME_IS_PAST;

@Getter
@Entity
@Table(name = "subscription")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionRoot {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "subscription_id")
        private Long id;

        @Valid
        @Embedded
        private SubscriptionInfo subscriptionInfo;

        @Valid
        @Embedded
        private SubscriptionBlog subscriptionBlog;

        @Builder(access = AccessLevel.PRIVATE)
        private SubscriptionRoot(
                        SubscriptionInfo subscriptionInfo,
                        SubscriptionBlog subscriptionBlog) {
                this.subscriptionInfo = subscriptionInfo;
                this.subscriptionBlog = subscriptionBlog;
        }

        public static SubscriptionRoot create(
                        String memberEmail,
                        URL blogUrl,
                        String blogTitle,
                        LocalDateTime publishedDateTime,
                        URL thumbnailUrl) {
                SubscriptionInfo subscriptionInfo = SubscriptionInfo.builder()
                                .memberEmail(memberEmail)
                                .subscriptionVisitCount(0L)
                                .subscriptionNickName(blogTitle)
                                .build();
                SubscriptionBlog subscriptionBlog = SubscriptionBlog.builder()
                                .blogUrl(blogUrl)
                                .blogTitle(blogTitle)
                                .publishedDateTime(publishedDateTime)
                                .newPostCount(0)
                                .thumbnailUrl(thumbnailUrl)
                                .build();
                return SubscriptionRoot.builder()
                                .subscriptionInfo(subscriptionInfo)
                                .subscriptionBlog(subscriptionBlog)
                                .build();
        }

        // TODO 구독 방문 이벤트 발생 !
        public SubscriptionsBlogVisitEvent increaseVisitCount() {
                long plusedVisitCount = subscriptionInfo.getSubscriptionVisitCount() + 1;

                subscriptionInfo = SubscriptionInfo.builder()
                                .memberEmail(subscriptionInfo.getMemberEmail())
                                .subscriptionVisitCount(plusedVisitCount)
                                .build();
                return new SubscriptionsBlogVisitEvent(id, subscriptionBlog.getBlogUrl());
        }

        // TODO 구독 방문 이벤트 발생 !
        public SubscriptionsPostVisitEvent increasePostVisitCount(URL postUrl) {
                long plusedVisitCount = subscriptionInfo.getSubscriptionVisitCount() + 1;

                subscriptionInfo = SubscriptionInfo.builder()
                                .memberEmail(subscriptionInfo.getMemberEmail())
                                .subscriptionVisitCount(plusedVisitCount)
                                .build();
                return new SubscriptionsPostVisitEvent(id, subscriptionBlog.getBlogUrl(), postUrl);
        }

        public void putRecentBlogInfo(LocalDateTime publishedDateTime, int newPostCount) {
                if (subscriptionBlog.getPublishedDateTime().isAfter(publishedDateTime)) {
                        throw new IllegalArgumentException(RECENT_PUBLISHED_DATETIME_IS_PAST);
                }

                subscriptionBlog = SubscriptionBlog.builder()
                                .blogUrl(subscriptionBlog.getBlogUrl())
                                .blogTitle(subscriptionBlog.getBlogTitle())
                                .publishedDateTime(publishedDateTime)
                                .newPostCount(newPostCount)
                                .build();
        }

        public void editSubscriptionNickName(String editNickName) {
                subscriptionInfo = SubscriptionInfo.builder()
                                .memberEmail(subscriptionInfo.getMemberEmail())
                                .subscriptionNickName(editNickName)
                                .subscriptionVisitCount(subscriptionInfo.getSubscriptionVisitCount())
                                .build();
        }
}
