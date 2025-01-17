package com.example.closestv2.api.service;

import com.example.closestv2.domain.subscription.SubscriptionQueryRepository;
import com.example.closestv2.domain.subscription.SubscriptionRoot;
import com.example.closestv2.models.SubscriptionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class SubscriptionQueryServiceTest {
    private final String ANY_MEMBER_ID_1 = "email@naver.com";
    private final Long ANY_MEMBER_ID_2 = 2L;
    private final String ANY_BLOG_LINK = "https://example.com/";
    private final String ANY_BLOG_TITLE = "블로그 제목";
    private final LocalDateTime ANY_PUBLISHED_DATE_TIME = LocalDateTime.of(2022, 1, 1, 12, 3, 31);

    private SubscriptionQueryService sut;

    @Mock
    private SubscriptionQueryRepository subscriptionQueryRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sut = new SubscriptionQueryService(subscriptionQueryRepository);
    }


    @Test
    @DisplayName("전달받은 memberId로 구독된 블로그에서 방문횟수가 높은 20개 구독 정보를 방문횟수 내림차순으로 조회힌다.")
    void getCloseSubscriptionListHighest20SubscriptionByVisitCount() throws MalformedURLException {
        //given
        List<SubscriptionRoot> cloests = new ArrayList<>();
        // 제목 순서대로 visitcount 순서
        for (int i = 1; i <= 20; i++) {
            SubscriptionRoot subscriptionRoot = SubscriptionRoot.create(ANY_MEMBER_ID_1, URI.create(ANY_BLOG_LINK + i).toURL(), ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME);
            subscriptionRoot.putRecentBlogInfo(ANY_PUBLISHED_DATE_TIME, i);
            cloests.addLast(subscriptionRoot);
        }
        when(subscriptionQueryRepository.findByMemberIdVisitCountDesc(ANY_MEMBER_ID_1, 0, 20)).thenReturn(cloests);
        //when
        List<SubscriptionResponse> closeSubscriptions = sut.getCloseSubscriptions(ANY_MEMBER_ID_1);
        //then
        assertThat(closeSubscriptions).hasSize(20)
                .isSortedAccordingTo((x, y) -> y.getVisitCnt().compareTo(x.getVisitCnt()));
    }

    @Test
    @DisplayName("조회 결과 구독 블로그의 개수가 0개여도 에러가 발생하지 않는다.")
    void getSubscriptionZeroNoError() {
        //given
        when(subscriptionQueryRepository.findByMemberIdVisitCountDesc(ANY_MEMBER_ID_1, 0, 20)).thenReturn(new ArrayList<>());
        //when
        List<SubscriptionResponse> closeSubscriptions = sut.getCloseSubscriptions(ANY_MEMBER_ID_1);
        //then
        assertThat(closeSubscriptions).hasSize(0);
    }

    @Test
    @DisplayName("전달받은 memberId와 페이징 정보와 발행시간을 오름차순한 구독 정보 페이징하여 조회힌다.")
    void getCloseSubscriptionListByPaging() throws MalformedURLException {
        //given
        List<SubscriptionRoot> paging = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            paging.add(
                    SubscriptionRoot.create(ANY_MEMBER_ID_1, URI.create(ANY_BLOG_LINK + i).toURL(), ANY_BLOG_TITLE, ANY_PUBLISHED_DATE_TIME.minusSeconds(i))
            );
        }
        when(subscriptionQueryRepository.findByMemberIdPublishedDateTimeDesc(ANY_MEMBER_ID_1, 1, 13)).thenReturn(paging);
        //when
        List<SubscriptionResponse> subscriptions = sut.getRecentPublishedSubscriptions(ANY_MEMBER_ID_1, 1, 13);
        //then
        assertThat(subscriptions).hasSize(3)
                .isSortedAccordingTo((x, y) -> y.getPublishedDateTime().compareTo(x.getPublishedDateTime()));
    }

    @Test
    @DisplayName("발행시간 페이징 조회 결과 구독 블로그 개수가 0개여도 에러가 발생하지 않는다.")
    void getSubscriptionPagingZeroNoError() {
        //given
        when(subscriptionQueryRepository.findByMemberIdPublishedDateTimeDesc(ANY_MEMBER_ID_1, 0, 20)).thenReturn(new ArrayList<>());
        //when
        List<SubscriptionResponse> closeSubscriptions = sut.getCloseSubscriptions(ANY_MEMBER_ID_1);
        //then
        assertThat(closeSubscriptions).hasSize(0);
    }
}