package com.example.closestv2.domain.subscription;

import com.example.closestv2.domain.subscription.event.SubscriptionsBlogVisitEvent;
import com.example.closestv2.domain.subscription.event.SubscriptionsPostVisitEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionRootTest {
    private final String ANY_MEMBER_EMAIL = "abc@naver.com";
    private final URL ANY_BLOG_URL = URI.create("https://example.com/blog123").toURL();
    private final URL ANY_POST_URL = URI.create("https://example.com/blog123/1").toURL();
    private final String ANY_BLOG_TITLE = "블로그 제목";
    private final LocalDateTime ANY_PUBLISHED_DATE_TIME = LocalDateTime.of(2022, 1, 1, 12, 3, 31);

    SubscriptionRootTest() throws MalformedURLException {
    }

    @Test
    @DisplayName("구독을 방문하면 구독 방문 횟수가 증가한다.")
    void increaseVisitCount() {
        //given
        SubscriptionRoot sut = SubscriptionRoot.create(ANY_MEMBER_EMAIL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME);
        ReflectionTestUtils.setField(sut, "id", 1L);
        //when
        sut.increaseVisitCount();
        sut.increaseVisitCount(); // 두번 방문
        //then
        assertThat(sut.getSubscriptionInfo().getSubscriptionVisitCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("구독을 방문하면 구독 블로그 방문 이벤트를 리턴한다.")
    void increaseVisitCountReturnEvent() {
        //given
        SubscriptionRoot sut = SubscriptionRoot.create(ANY_MEMBER_EMAIL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME);
        ReflectionTestUtils.setField(sut, "id", 1L);
        //when
        SubscriptionsBlogVisitEvent event = sut.increaseVisitCount();
        //then
        assertThat(event.blogUrl()).isEqualTo(ANY_BLOG_URL);
    }

    @Test
    @DisplayName("구독-포스트을 방문하면 구독 방문 횟수가 증가한다.")
    void increasePostVisitCount() {
        //given
        SubscriptionRoot sut = SubscriptionRoot.create(ANY_MEMBER_EMAIL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME);
        ReflectionTestUtils.setField(sut, "id", 1L);
        //when
        sut.increasePostVisitCount(ANY_POST_URL);
        //then
        assertThat(sut.getSubscriptionInfo().getSubscriptionVisitCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("구독-포스트를 방문하면 구독-포스트 블로그 방문 이벤트를 리턴한다.")
    void increasePostVisitCountReturnEvent() {
        //given
        SubscriptionRoot sut = SubscriptionRoot.create(ANY_MEMBER_EMAIL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME);
        ReflectionTestUtils.setField(sut, "id", 1L);
        //when
        SubscriptionsPostVisitEvent event = sut.increasePostVisitCount(ANY_POST_URL);
        //then
        assertThat(event.blogUrl()).isEqualTo(ANY_BLOG_URL);
        assertThat(event.postUrl()).isEqualTo(ANY_POST_URL);
    }

    @Test
    @DisplayName("해당 구독의 블로그 정보 업데이트 시 최근 발생시간과 새로운 포스트 개수를 설정한다.")
    void putRecentBlogInfo() {
        //given
        SubscriptionRoot sut = SubscriptionRoot.create(ANY_MEMBER_EMAIL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME);
        LocalDateTime recent = ANY_PUBLISHED_DATE_TIME.plusSeconds(1);
        int newPostCount = 12;
        //when
        sut.putRecentBlogInfo(recent, newPostCount); //발행 일자와 새로 받은 개수를 그대로 put
        //then
        SubscriptionBlog recentInfo = sut.getSubscriptionBlog();
        assertThat(recentInfo.getPublishedDateTime()).isEqualTo(ANY_PUBLISHED_DATE_TIME.plusSeconds(1));
        assertThat(recentInfo.getNewPostCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("publishedDateTime을 새로 받을 때, 기존 시간 보다 이전 시간이면 에러가 발생한다.")
    void publishedDateTimeWithPastDateTime() {
        //given
        SubscriptionRoot sut = SubscriptionRoot.create(ANY_MEMBER_EMAIL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME);
        LocalDateTime past = ANY_PUBLISHED_DATE_TIME.minusSeconds(1);
        //expected
        assertThatThrownBy(() -> sut.putRecentBlogInfo(past, 12))  //새로 받은 개수를 그대로 pu
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Subscription NickName을 변경할 수 있다.")
    void editSubscriptionNickName() {
        //given
        SubscriptionRoot sut = SubscriptionRoot.create(ANY_MEMBER_EMAIL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME);
        String editNickName = "수정된 닉네임";
        //when
        sut.editSubscriptionNickName(editNickName);
        //then
        assertThat(sut.getSubscriptionInfo().getSubscriptionNickName()).isEqualTo("수정된 닉네임");
    }
}