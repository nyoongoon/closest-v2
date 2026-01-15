package com.example.closestv2.api.service;

import com.example.closestv2.domain.blog.BlogAuthCode;
import com.example.closestv2.domain.blog.BlogAuthenticator;
import com.example.closestv2.domain.blog.event.MyBlogSaveEvent;
import com.example.closestv2.domain.feed.Feed;
import com.example.closestv2.domain.feed.FeedClient;
import com.example.closestv2.domain.feed.FeedItem;
import com.example.closestv2.infrastructure.domain.blog.BlogAuthCodeRepository;
import com.example.closestv2.models.AuthMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.FAIL_BLOG_AUTHENTICATE;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlogAuthServiceTest {
    private final String ANY_MEMBER_EMAIL = "abc@naver.com";
    private final URI ANY_RSS_URI = URI.create("http://www.example.com/rss");
    private final URI ANY_BLOG_URI = URI.create("http://www.example.com");
    private final URI ANY_POST_URI_1 = URI.create("http://www.example.com/1");
    private final URI ANY_POST_URI_2 = URI.create("http://www.example.com/2");
    private final LocalDateTime ANY_PUBLISHED_DATE_TIME = LocalDateTime.of(2023, 01, 01, 01, 10, 55);
    private final URI WRONG_RSS_URI = URI.create("http://www.example.com/fail");
    private final String ANY_AUTH_CODE = "ABC123";
    private final String ANY_BLOG_TITLE = "블로그 제목";
    private final String ANY_POST_TITLE = "포스트 제목";
    private final String WRONG_POST_TITLE = "123ABC";
    private final String ANY_AUTHOR = "블로그 작가";

    private BlogAuthService sut;
    private BlogAuthenticator blogAuthenticator; // 의존성 없으므로 mock 안함

    @Mock
    private FeedClient feedClient;
    @Mock
    private ApplicationEventPublisher mockPublisher;
    @Mock
    private BlogAuthCodeRepository blogAuthCodeRepository;

    BlogAuthServiceTest() throws IOException {
    }

    @BeforeEach
    void setUp() throws MalformedURLException {
        MockitoAnnotations.openMocks(this);

        List<FeedItem> feedItems = new ArrayList<>();
        FeedItem feedItem1 = FeedItem.create(ANY_POST_URI_1.toURL(), ANY_POST_TITLE, ANY_PUBLISHED_DATE_TIME);
        FeedItem feedItem2 = FeedItem.create(ANY_POST_URI_2.toURL(), "ABC123", ANY_PUBLISHED_DATE_TIME.plusSeconds(1));
        feedItems.add(feedItem1);
        feedItems.add(feedItem2);
        Feed feed = Feed.create(ANY_RSS_URI.toURL(), ANY_BLOG_URI.toURL(), ANY_BLOG_TITLE, ANY_AUTHOR, null, feedItems);

        when(feedClient.getFeed(ANY_RSS_URI.toURL())).thenReturn(feed);
        when(feedClient.getFeed(WRONG_RSS_URI.toURL())).thenThrow(new IllegalStateException());

        blogAuthenticator = new BlogAuthenticator();

        BlogAuthCode blogAuthCode = new BlogAuthCode(ANY_MEMBER_EMAIL, ANY_RSS_URI.toURL(), ANY_AUTH_CODE);
        when(blogAuthCodeRepository.save(blogAuthCode)).thenReturn(blogAuthCode);
        when(blogAuthCodeRepository.findByMemberEmail(ANY_MEMBER_EMAIL)).thenReturn(blogAuthCode);
        when(blogAuthCodeRepository.findByMemberEmail("cba@naver.com"))
                .thenThrow(new IllegalArgumentException(FAIL_BLOG_AUTHENTICATE));

        sut = new BlogAuthService(feedClient, mockPublisher, blogAuthenticator, blogAuthCodeRepository);
    }

    @Test
    @DisplayName("memberId와 rssUri로 인증 메시지를 요청한다. - 인증 메시지 생성 및 저장 후 리턴")
    void createBlogAuthMessage() throws MalformedURLException {
        // given
        AuthMessageResponse response = sut.createBlogAuthMessage(ANY_MEMBER_EMAIL, ANY_RSS_URI);
        BlogAuthCode blogAuthCode = new BlogAuthCode(ANY_MEMBER_EMAIL, ANY_RSS_URI.toURL(), response.getAuthMessage());
        when(blogAuthCodeRepository.findByMemberEmail(ANY_MEMBER_EMAIL)).thenReturn(blogAuthCode);
        // when
        BlogAuthCode found = blogAuthCodeRepository.findByMemberEmail(ANY_MEMBER_EMAIL);
        // then
        assertThat(response.getAuthMessage()).isEqualTo(found.authMessage());
    }

    @Test
    @DisplayName("rss가 조회되지 않은 rssUrl을 요청 시 예외가 발생한다.")
    void createAuthCodeWithNotRssUrl() {
        // given
        // expected
        assertThatThrownBy(() -> sut.createBlogAuthMessage(ANY_MEMBER_EMAIL, WRONG_RSS_URI))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("memberId로 블로그 인증 요청을 하여 RSS 조회 결과의 최신 포스트명과 저장된 BlogAuthCode를 비교한다.")
    void verifyBlogAuthMessage() {
        // given
        sut.createBlogAuthMessage(ANY_MEMBER_EMAIL, ANY_RSS_URI);
        // expected
        assertThatCode(() -> sut.verifyBlogAuthMessage(ANY_MEMBER_EMAIL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("memberId로 블로그 인증 요청을 할 때 RSS 결과 포스트가 한개인 경우 해당 포스트를 반환한다.")
    void verifyBlogAuthMessageWithOnePost() throws MalformedURLException {
        // given
        AuthMessageResponse blogAuthMessage = sut.createBlogAuthMessage(ANY_MEMBER_EMAIL, ANY_RSS_URI);
        // mocking
        when(blogAuthCodeRepository.findByMemberEmail(ANY_MEMBER_EMAIL))
                .thenReturn(new BlogAuthCode(ANY_MEMBER_EMAIL, ANY_RSS_URI.toURL(), blogAuthMessage.getAuthMessage()));
        mockingFeedByFeedClient(blogAuthMessage.getAuthMessage());
        // expected
        assertThatCode(() -> sut.verifyBlogAuthMessage(ANY_MEMBER_EMAIL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("인증 요청 시 저장되어 있던 BlogAuthCode의 Url로 조회 요청하고 결과가 없으면 에러를 발생한다.")
    void verifyBlogAuthMessageWithNotExistsRssResult() throws MalformedURLException {
        // given
        sut.createBlogAuthMessage(ANY_MEMBER_EMAIL, ANY_RSS_URI);
        when(feedClient.getFeed(ANY_RSS_URI.toURL())).thenThrow(IllegalStateException.class);
        // expected
        assertThatThrownBy(() -> sut.verifyBlogAuthMessage(ANY_MEMBER_EMAIL)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("인증 요청 시 저장되어 있던 BlogAuthCode 값과 rss 조회해온 최신 포스트명의 값이 같지 않으면 에러가 발생한다.")
    void verifyBlogAuthMessageWithNotSameRecentPostTitle() throws MalformedURLException {
        // given
        sut.createBlogAuthMessage(ANY_MEMBER_EMAIL, ANY_RSS_URI);
        mockingFeedByFeedClient(WRONG_POST_TITLE);
        // expected
        assertThatThrownBy(() -> sut.verifyBlogAuthMessage(ANY_MEMBER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("인증 성공시 MyBlogCreateionEvent가 발생한다.")
    void myBlogCreationEventByAuthenticated() throws MalformedURLException {
        // given
        sut.createBlogAuthMessage(ANY_MEMBER_EMAIL, ANY_RSS_URI);
        ArgumentCaptor<MyBlogSaveEvent> captor = ArgumentCaptor.forClass(MyBlogSaveEvent.class);
        // when
        sut.verifyBlogAuthMessage(ANY_MEMBER_EMAIL);
        // then
        verify(mockPublisher, times(1)).publishEvent(captor.capture());
        MyBlogSaveEvent value = captor.getValue();
        assertThat(value.memberEmail()).isEqualTo(ANY_MEMBER_EMAIL);
        assertThat(value.blogUrl()).isEqualTo(ANY_BLOG_URI.toURL());
    }

    @Test
    @DisplayName("인증 실패시 MyBlogCreateionEvent가 발생하지 않는다.")
    void myBlogCreationEventFailed() {
        // given
        sut.createBlogAuthMessage(ANY_MEMBER_EMAIL, ANY_RSS_URI);
        // expected
        assertThatThrownBy(() -> sut.verifyBlogAuthMessage("cba@naver.com"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mockPublisher, never()).publishEvent(any(MyBlogSaveEvent.class));
    }

    private void mockingFeedByFeedClient(String postTitle) throws MalformedURLException {
        List<FeedItem> feedItems = new ArrayList<>();
        FeedItem feedItem1 = FeedItem.create(ANY_POST_URI_1.toURL(), postTitle, ANY_PUBLISHED_DATE_TIME);
        feedItems.add(feedItem1);
        Feed feed = Feed.create(ANY_RSS_URI.toURL(), ANY_BLOG_URI.toURL(), ANY_BLOG_TITLE, ANY_AUTHOR, null, feedItems);
        when(feedClient.getFeed(ANY_RSS_URI.toURL())).thenReturn(feed);
    }
}