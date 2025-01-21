package com.example.closestv2.infrastructure.rss;

import com.example.closestv2.domain.blog.BlogAuthCode;
import com.example.closestv2.domain.blog.BlogAuthenticator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

class BlogAuthenticatorTest {
    private final String ANY_MEMBER_EMAIL = "abc@naver.com";
    private final URL ANY_RSS_URL = URI.create("https://example.com/rss").toURL();

    private BlogAuthenticator blogAuthenticator;

    BlogAuthenticatorTest() throws MalformedURLException {
    }

    @BeforeEach
    void setUp() {
        blogAuthenticator = new BlogAuthenticator();
    }

    @Test
    @DisplayName("memberId와 rssUrl, authMessage를 포함한 인증 코드 객체 BlogAuthCode를 생성한다.")
    void createAuthCode() {
        //given
        //when
        BlogAuthCode authCode = blogAuthenticator.createAuthCode(ANY_MEMBER_EMAIL, ANY_RSS_URL);
        //then
        assertThat(authCode.memberEmail()).isEqualTo(ANY_MEMBER_EMAIL);
        assertThat(authCode.rssUrl()).isEqualTo(ANY_RSS_URL);
        assertThat(authCode.authMessage()).isNotNull();
    }


    @Test
    @DisplayName("전달받은 BlogAuthCode의 authMessage와 blogTitle이 동일하면 인증에 성공한다.")
    void authenticate() {
        //given
        String authMessage = "ABC123";
        BlogAuthCode blogAuthCode = new BlogAuthCode(ANY_MEMBER_EMAIL, ANY_RSS_URL, authMessage);
        String blogTitle = "ABC123";
        //when
        boolean isAuthenticated = blogAuthenticator.authenticate(blogAuthCode, blogTitle);
        //then
        assertThat(isAuthenticated).isTrue();
    }

    @Test
    @DisplayName("전달받은 BlogAuthCode의 authMessage와 blogTitle이 동일하지 않으면 인증에 실패한다.")
    void authenticateFail() {
        //given
        String authMessage = "ABC123";
        BlogAuthCode blogAuthCode = new BlogAuthCode(ANY_MEMBER_EMAIL, ANY_RSS_URL, authMessage);
        String blogTitle = "ABC456";
        //when
        boolean isAuthenticated = blogAuthenticator.authenticate(blogAuthCode, blogTitle);
        //then
        assertThat(isAuthenticated).isFalse();
    }
}