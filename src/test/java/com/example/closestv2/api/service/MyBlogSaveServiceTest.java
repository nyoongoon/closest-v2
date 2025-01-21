package com.example.closestv2.api.service;

import com.example.closestv2.domain.member.MemberRepository;
import com.example.closestv2.domain.member.MemberRoot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MyBlogSaveServiceTest {
    private final String ANY_MEMBER_EMAIL = "abc@naver.com";
    private final URL ANY_BLOG_URL = URI.create("http://example.com").toURL();
    private final String ANY_USER_EMAIL = "abc@naver.com";
    private final String ANY_PASSWORD = "Abc1234!!";
    private final String ANY_NICK_NAME = "닉네임";

    private MyBlogSaveService myBlogSaveService;
    @Mock
    private MemberRepository memberRepository;

    MyBlogSaveServiceTest() throws MalformedURLException {
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Optional<MemberRoot> memberRoot = Optional.of(MemberRoot.create(ANY_USER_EMAIL, ANY_PASSWORD, ANY_NICK_NAME));
        when(memberRepository.findByMemberInfoUserEmail(ANY_MEMBER_EMAIL)).thenReturn(memberRoot);
        myBlogSaveService = new MyBlogSaveService(memberRepository);
    }

    @Test
    @DisplayName("memberId와 blogUrl로 MemberRoot에 MyBlog를 생성한다. ")
    void createMyBlog() {
        //given
        //when
        myBlogSaveService.saveMyBlog(ANY_MEMBER_EMAIL, ANY_BLOG_URL);
        //then
        MemberRoot memberRoot = memberRepository.findByMemberInfoUserEmail(ANY_MEMBER_EMAIL).orElseThrow();
        assertThat(memberRoot.getMyBlog().getBlogUrl()).isEqualTo(ANY_BLOG_URL);
        assertThat(memberRoot.getMyBlog().getMyBlogVisitCount()).isEqualTo(0L);
    }
}