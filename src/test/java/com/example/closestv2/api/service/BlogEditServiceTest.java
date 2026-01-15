package com.example.closestv2.api.service;

import com.example.closestv2.domain.blog.BlogRepository;
import com.example.closestv2.domain.blog.BlogRoot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

class BlogEditServiceTest {
    private final URL ANY_RSS_URL = URI.create("http://example.com/rss").toURL();
    private final URL ANY_BLOG_URL1 = URI.create("http://example1.com").toURL();
    private final URL ANY_BLOG_URL2 = URI.create("http://example2.com").toURL();
    private final String ANY_TITLE = "제목";
    private final String ANY_AUTHOR = "작가";
    private final String ANY_STATUS_MESSAGE = "변경된 상태 메시지";
    private final LocalDateTime ANY_PUBLISHED_TIME = LocalDateTime.of(2023, 01, 01, 01, 10, 55);

    private BlogEditService blogEditService;
    @Mock
    private BlogRepository blogRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        blogEditService = new BlogEditService(blogRepository);
    }

    BlogEditServiceTest() throws MalformedURLException {

    }

    @Test
    @DisplayName("블로그의 상태메시지를 수정할 수 있다.")
    void editStatueMessage() {
        // given
        saveBlogRoot(ANY_BLOG_URL1);
        // when
        blogEditService.editStatueMessage(ANY_BLOG_URL1, ANY_STATUS_MESSAGE);
        // then
        BlogRoot blogRoot = blogRepository.findByBlogInfoBlogUrl(ANY_BLOG_URL1).orElseThrow();
        assertThat(blogRoot.getBlogInfo().getStatusMessage()).isEqualTo(ANY_STATUS_MESSAGE);
    }

    @Test
    @DisplayName("존재하지 않는 BlogUrl로 상태메시지 수정 요청 시 에러가 발생한다.")
    void editStatueMessageWithNotExistsBlogUrl() {
        // given
        saveBlogRoot(ANY_BLOG_URL1);
        // expected
        assertThatThrownBy(() -> blogEditService.editStatueMessage(ANY_BLOG_URL2, ANY_STATUS_MESSAGE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("상태메지시가 empty여도 예외가 발생하지 않는다.")
    void editStatueMessageWithEmptyStatusMessage() {
        // given
        saveBlogRoot(ANY_BLOG_URL1);
        // expected
        assertThatCode(() -> blogEditService.editStatueMessage(ANY_BLOG_URL1, null)).doesNotThrowAnyException();
        assertThatCode(() -> blogEditService.editStatueMessage(ANY_BLOG_URL1, "")).doesNotThrowAnyException();
        assertThatCode(() -> blogEditService.editStatueMessage(ANY_BLOG_URL1, " ")).doesNotThrowAnyException();
    }

    private void saveBlogRoot(URL url) {
        BlogRoot blogRoot = BlogRoot.create(ANY_RSS_URL, url, ANY_TITLE, ANY_AUTHOR, null, ANY_PUBLISHED_TIME);
        when(blogRepository.findByBlogInfoBlogUrl(ANY_BLOG_URL1)).thenReturn(Optional.of(blogRoot));
    }
}