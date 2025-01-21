package com.example.closestv2.infrastructure.domain.blog;

import com.example.closestv2.domain.blog.BlogAuthCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class BlogAuthCodeRepositoryTest {
    private final String ANY_MEMBER_EMAIL = "abc@naver.com";
    private final URL ANY_RSS_URL = URI.create("https://example.com/rss").toURL();
    private final String ANY_AUTH_MESSAGE = "ABC123";

    @Autowired
    private BlogAuthCodeRepository blogAuthCodeRepository;

    BlogAuthCodeRepositoryTest() throws MalformedURLException {
    }

    @Test
    @DisplayName("BlogAuthCode를 저장할 때 memberId를 키로 사용하여 저장한다.")
    void saveBlogAuthCodeByMemberEmail() {
        //given
        BlogAuthCode blogAuthCode = new BlogAuthCode(ANY_MEMBER_EMAIL, ANY_RSS_URL, ANY_AUTH_MESSAGE);
        blogAuthCodeRepository.save(blogAuthCode);
        //when
        BlogAuthCode found = blogAuthCodeRepository.findByMemberEmail(ANY_MEMBER_EMAIL);
        //then
        assertThat(found).isEqualTo(blogAuthCode);
    }

    @Test
    @DisplayName("인증 코드 인증 요청 시 캐시에 존재하지 않는 memberId인 경우 에러가 발생한다.")
    void getBlogAuthCodeWithNotExistsMemberEmailInCache() {
        //given
        BlogAuthCode blogAuthCode = new BlogAuthCode(ANY_MEMBER_EMAIL, ANY_RSS_URL, ANY_AUTH_MESSAGE);
        blogAuthCodeRepository.save(blogAuthCode);
        //expected
        assertThatThrownBy(() -> blogAuthCodeRepository.findByMemberEmail("cba@naver.com")).isInstanceOf(InvalidDataAccessApiUsageException.class);
    }

    @Test
    @DisplayName("동일한 memberId로 캐시가 저장되면 기존 코드를 엎어쓰고 새로 생성한다.")
    void renewalBlogAuthCodeWithSameMemberEmail() {
        //given
        BlogAuthCode blogAuthCode1 = new BlogAuthCode(ANY_MEMBER_EMAIL, ANY_RSS_URL, ANY_AUTH_MESSAGE);
        blogAuthCodeRepository.save(blogAuthCode1);
        BlogAuthCode blogAuthCode2 = new BlogAuthCode(ANY_MEMBER_EMAIL, ANY_RSS_URL, "다른메시지");
        blogAuthCodeRepository.save(blogAuthCode2);
        //when
        BlogAuthCode blogAuthCode = blogAuthCodeRepository.findByMemberEmail(ANY_MEMBER_EMAIL);
        //then
        assertThat(blogAuthCode.authMessage()).isEqualTo("다른메시지");
    }
}