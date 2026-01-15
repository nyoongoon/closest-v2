package com.example.closestv2.api.service;

import com.example.closestv2.domain.blog.BlogRepository;
import com.example.closestv2.domain.blog.BlogRoot;
import com.example.closestv2.domain.blog.Post;
import com.example.closestv2.util.url.UrlUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class BlogVisitServiceTest {
    private final URL ANY_BLOG_URL = UrlUtils.from("https://example.com/blog123");
    private final URL NOT_EXISTS_BLOG_URL = UrlUtils.from("https://example.com/blog123/non");
    private final URL ANY_RSS_URL = UrlUtils.from("http://example.com/rss");
    private final String ANY_BLOG_TITLE = "블로그 제목";
    private final String ANY_AUTHOR = "작가";
    private final URL ANY_POST_URL = UrlUtils.from("https://example.com/blog123/1");
    private final URL NOT_EXISTS_POST_URL = UrlUtils.from("https://example.com/blog123/no");
    private final String ANY_POST_TITLE = "포스트 제목";
    private final LocalDateTime ANY_PUBLISHED_DATE_TIME = LocalDateTime.of(2022, 1, 1, 12, 3, 31);

    private BlogVisitService sut;
    @Mock
    private BlogRepository blogRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sut = new BlogVisitService(blogRepository);
    }

    @Test
    @DisplayName("블로그를 방문하면 BlogInfo의 blogVisitCount가 1증가한다.")
    void visitBlog() {
        // given
        BlogRoot blogRoot = setMock();
        // when
        sut.visitBlog(ANY_BLOG_URL);
        // then
        assertThat(blogRoot.getBlogInfo().getBlogVisitCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 블로그를 방문하면 에러가 발행한다.")
    void visitBlogFailNotFoundBlog() {
        // given
        when(blogRepository.findByBlogInfoBlogUrl(NOT_EXISTS_BLOG_URL)).thenReturn(Optional.empty());
        // expected
        assertThatThrownBy(() -> sut.visitBlog(ANY_BLOG_URL)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("포스트를 방문하면 Blog의 visitCount와 Post의 postVisitCount가 1증가한다.")
    void visitPost() {
        // given
        BlogRoot blogRoot = setMock();
        // when
        sut.visitPost(ANY_BLOG_URL, ANY_POST_URL);
        // then
        assertThat(blogRoot.getBlogInfo().getBlogVisitCount()).isEqualTo(1L);
        Post post = blogRoot.getPosts().get(ANY_POST_URL);
        assertThat(post.getPostVisitCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("포스트 방문 시 존재하지 않는 블로그를 방문하면 에러가 발행한다.")
    void visitPostFailNotFoundBlog() {
        // given
        setMock();
        // expected
        assertThatThrownBy(() -> sut.visitPost(NOT_EXISTS_BLOG_URL, ANY_POST_URL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("존재하지 않는 포스트를 방문하면 에러가 발행한다.")
    void visitPostFailNotFoundPost() {
        // given
        setMock();
        // expected
        assertThatThrownBy(() -> sut.visitPost(ANY_BLOG_URL, NOT_EXISTS_POST_URL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private BlogRoot setMock() {
        BlogRoot blogRoot = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                ANY_PUBLISHED_DATE_TIME);
        Map<URL, Post> posts = blogRoot.getPosts();
        Post post = blogRoot.createPost(
                ANY_POST_URL,
                ANY_POST_TITLE,
                ANY_PUBLISHED_DATE_TIME);
        posts.put(post.getPostUrl(), post);

        when(blogRepository.findByBlogInfoBlogUrl(ANY_BLOG_URL)).thenReturn(Optional.of(blogRoot));
        return blogRoot;
    }
}