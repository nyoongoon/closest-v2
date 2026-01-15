package com.example.closestv2.api.service;

import com.example.closestv2.api.service.model.response.VisitSubscriptionResponse;
import com.example.closestv2.domain.subscription.SubscriptionRepository;
import com.example.closestv2.domain.subscription.SubscriptionRoot;
import com.example.closestv2.domain.subscription.event.SubscriptionsBlogVisitEvent;
import com.example.closestv2.domain.subscription.event.SubscriptionsPostVisitEvent;
import com.example.closestv2.util.url.UrlUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SubscriptionVisitServiceTest {
    private final long ANY_SUBSCRIPTIONS_ID = 1L;
    private final String ANY_MEMBER_EMAIL = "abc@naver.com";
    private final URL ANY_BLOG_URL = UrlUtils.from("https://example.com/blog123");
    private final URL ANY_POST_URL = UrlUtils.from("https://example.com/blog123/1");
    private final String ANY_BLOG_TITLE = "블로그 제목";
    private final LocalDateTime ANY_PUBLISHED_DATE_TIME = LocalDateTime.of(2022, 1, 1, 12, 3, 31);
    private SubscriptionVisitService sut;

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private ApplicationEventPublisher mockPublisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sut = new SubscriptionVisitService(subscriptionRepository, mockPublisher);
    }

    private SubscriptionRoot setMock() {
        SubscriptionRoot subscriptionRoot = SubscriptionRoot.create(ANY_MEMBER_EMAIL, ANY_BLOG_URL, ANY_BLOG_TITLE,
                ANY_PUBLISHED_DATE_TIME, null);
        ReflectionTestUtils.setField(subscriptionRoot, "id", 1L);
        when(subscriptionRepository.findById(ANY_SUBSCRIPTIONS_ID)).thenReturn(Optional.of(subscriptionRoot));
        return subscriptionRoot;
    }

    @Test
    @DisplayName("구독 방문 성공 테스트")
    void visitSubscription() {
        // given
        setMock();
        // when
        VisitSubscriptionResponse response = sut.visitSubscription(ANY_SUBSCRIPTIONS_ID);
        // then
        URL redirectUrl = response.getRedirectUrl();
        assertThat(ANY_BLOG_URL).isEqualTo(redirectUrl);
    }

    @Test
    @DisplayName("구독 방문 성공 테스트 - Subscription 조회수가 1 증가한다.")
    void visitSubscriptionIncreaseVitsitCnt() {
        // given
        SubscriptionRoot subscriptionRoot = setMock();
        long beforeVisitCnt = subscriptionRoot.getSubscriptionInfo().getSubscriptionVisitCount();
        // when
        sut.visitSubscription(ANY_SUBSCRIPTIONS_ID);
        // then
        long afterVisitCnt = subscriptionRoot.getSubscriptionInfo().getSubscriptionVisitCount();
        assertThat(beforeVisitCnt + 1).isEqualTo(afterVisitCnt);
    }

    @Test
    @DisplayName("구독 방문 성공 테스트 - redirectUrl을 반환한다.")
    void visitSubscriptionGetRedirectUrl() {
        // given
        SubscriptionRoot subscriptionRoot = setMock();
        // when
        VisitSubscriptionResponse response = sut.visitSubscription(ANY_SUBSCRIPTIONS_ID);
        // then
        URL redirectUrl = response.getRedirectUrl();
        assertThat(subscriptionRoot.getSubscriptionBlog().getBlogUrl()).isEqualTo(redirectUrl);
    }

    @Test
    @DisplayName("구독 방문 실패 테스트 - 존재하지 않는 subscriptionsId")
    void visitSubscriptionFail() {
        // given
        // expected
        assertThatThrownBy(() -> sut.visitSubscription(2L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("블로그 방문시 블로그 방문 이벤트 발생")
    void visitSubscriptionPublishBlogVisitEvent() {
        // given
        setMock();
        ArgumentCaptor<SubscriptionsBlogVisitEvent> captor = ArgumentCaptor.forClass(SubscriptionsBlogVisitEvent.class);
        // when
        sut.visitSubscription(ANY_SUBSCRIPTIONS_ID);
        // then
        verify(mockPublisher, times(1)).publishEvent(captor.capture());
        SubscriptionsBlogVisitEvent value = captor.getValue();
        assertThat(value.subscriptionsId()).isEqualTo(ANY_SUBSCRIPTIONS_ID);
        assertThat(value.blogUrl()).isEqualTo(ANY_BLOG_URL);
    }

    @Test
    @DisplayName("구독 포스트 방문 테스트 - Subscription 조회수가 1 증가한다.")
    void visitSubscriptionPostIncreaseVisitCnt() {
        // given
        SubscriptionRoot subscriptionRoot = setMock();
        // when
        sut.visitSubscription(ANY_SUBSCRIPTIONS_ID, ANY_POST_URL);
        // then
        assertThat(subscriptionRoot.getSubscriptionInfo().getSubscriptionVisitCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("구독 포스트 방문 테스트 - 요청한 Post URL를  리턴한다.")
    void visitSubscriptionPostURL() {
        // given
        setMock();
        // when
        VisitSubscriptionResponse response = sut.visitSubscription(ANY_SUBSCRIPTIONS_ID, ANY_POST_URL);
        // then
        assertThat(response.getRedirectUrl()).isEqualTo(ANY_POST_URL);
    }

    @Test
    @DisplayName("포스트 방문 이벤트 발생")
    void visitSubscriptionPostPublishPostVisitEvent() {
        // given
        setMock();
        ArgumentCaptor<SubscriptionsPostVisitEvent> captor = ArgumentCaptor.forClass(SubscriptionsPostVisitEvent.class);
        // when
        sut.visitSubscription(ANY_SUBSCRIPTIONS_ID, ANY_POST_URL);
        // then
        verify(mockPublisher, times(1)).publishEvent(captor.capture());
        SubscriptionsPostVisitEvent value = captor.getValue();
        assertThat(value.subscriptionsId()).isEqualTo(ANY_SUBSCRIPTIONS_ID);
        assertThat(value.blogUrl()).isEqualTo(ANY_BLOG_URL);
        assertThat(value.postUrl()).isEqualTo(ANY_POST_URL);
    }
}
