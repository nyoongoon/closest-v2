package com.example.closestv2.domain.blog;

import com.example.closestv2.util.constant.SpecificDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class BlogRootTest {
        // Blog
        private final URL ANY_RSS_URL = URI.create("https://example.com/rss").toURL();
        private final URL ANY_BLOG_URL = URI.create("https://example.com/blog123").toURL();
        private final String ANY_BLOG_TITLE = "제목";
        private final String ANY_AUTHOR = "작가";
        private final LocalDateTime ANY_PUBLISHED_DATE_TIME = LocalDateTime.of(2022, 1, 1, 12, 3, 31);
        // Post
        private final URL ANY_POST_URL = URI.create("https://example.com/blog123/123").toURL();
        private final String ANY_POST_TITLE = "포스트 제목";

        BlogRootTest() throws MalformedURLException {
        }

        @Test
        @DisplayName("BlogRoot 기본 생성 시 publishedDateTime을 전달하지 않으면 publishedDateTime은 에포크 타임으로 기본값 생성된다.")
        void createBlogRootWithPosts() {
                // given
                BlogRoot blogRoot = BlogRoot.create(
                                ANY_RSS_URL,
                                ANY_BLOG_URL,
                                ANY_BLOG_TITLE,
                                ANY_AUTHOR,
                                null);
                // when
                LocalDateTime publishedDateTime = blogRoot.getBlogInfo().getPublishedDateTime();
                // then
                assertThat(publishedDateTime).isEqualTo(SpecificDate.EPOCH_TIME.getLocalDateTime());
        }

        @Test
        @DisplayName("Blog는 BlogInfo의 상태메시지를 변경할 수 있다.")
        void editStatusMessage() {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                String statusMessage = "변경할 상태 메시지";
                // when
                sut.editStatusMessage(statusMessage);
                // then
                assertThat(sut.getBlogInfo().getStatusMessage()).isEqualTo("변경할 상태 메시지");
        }

        @Test
        @DisplayName("블로그가 자신이 참조하고 있는 포스트를 postUrl을 기준으로 제거할 수 있다.")
        void blogDeletePost() {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                Post post = sut.createPost(ANY_POST_URL, ANY_POST_TITLE, ANY_PUBLISHED_DATE_TIME);
                sut.getPosts().put(post.getPostUrl(), post);
                Post foundPost = sut.getPosts().get(ANY_POST_URL);
                // when
                sut.getPosts().remove(foundPost.getPostUrl());
                // then
                assertThat(sut.getPosts()).hasSize(0);
        }

        @Test
        @DisplayName("Blog 생성 시 BlogInfo의 blogVisitCount는 0이 된다.")
        void createBlogByBlogInfoWithZeroBlogVisitCount() {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                // expected
                assertThat(sut.getBlogInfo().getBlogVisitCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Post 생성 시 PostInfo의 postVisitCount는 0이 된다.")
        void createPostByPostInfoWithZeroPostVisitCount() {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                Post post = sut.createPost(ANY_POST_URL, ANY_POST_TITLE, ANY_PUBLISHED_DATE_TIME);
                // expected
                assertThat(post.getPostVisitCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("블로그 업데이트 확인 여부 시 BlogInfo의 blogTitle, author, publicshedDateTime을 비교한다. - true 케이스")
        void isUpdatedByEqualBlogInfoTrueTest() {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                BlogRoot updatedRecentBlogRoot1 = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, "업데이트 제목", ANY_AUTHOR,
                                null,
                                ANY_PUBLISHED_DATE_TIME);
                BlogRoot updatedRecentBlogRoot2 = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, "업데이트 작가",
                                null,
                                ANY_PUBLISHED_DATE_TIME);
                BlogRoot updatedRecentBlogRoot3 = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR,
                                null,
                                ANY_PUBLISHED_DATE_TIME);
                updatedRecentBlogRoot3.updatePublishedDateTime(ANY_PUBLISHED_DATE_TIME.plusMinutes(1));
                // when
                boolean isUpdated1 = sut.isBlogUpdated(updatedRecentBlogRoot1);
                boolean isUpdated2 = sut.isBlogUpdated(updatedRecentBlogRoot2);
                boolean isUpdated3 = sut.isBlogUpdated(updatedRecentBlogRoot3);
                // then
                assertThat(isUpdated1).isTrue();
                assertThat(isUpdated2).isTrue();
                assertThat(isUpdated3).isTrue();
        }

        @Test
        @DisplayName("블로그 업데이트 확인 여부 시 BlogInfo의 blogTitle, author, publicshedDateTime을 비교한다. - false 케이스")
        void isUpdatedByEqualBlogInfoFalseTest() {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                BlogRoot updatedRecentBlogRoot = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR,
                                null,
                                ANY_PUBLISHED_DATE_TIME);
                // when
                boolean isUpdated = sut.isBlogUpdated(updatedRecentBlogRoot);
                // then
                assertThat(isUpdated).isFalse();
        }

        @Test
        @DisplayName("블로그 업데이트 시 BlogInfo의 값들을 비교할 때 Rss URL이 일치하지 않으면 에러를 반환한다.")
        void updateBlogRootByUnEqualRssUrlThrowError() throws MalformedURLException {
                // given
                BlogRoot blogRoot = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                BlogRoot notSameUrlBlog = BlogRoot.create(URI.create("https://example.com/rssX").toURL(), ANY_BLOG_URL,
                                ANY_BLOG_TITLE, ANY_AUTHOR, null, ANY_PUBLISHED_DATE_TIME);
                // expected
                assertThatThrownBy(() -> blogRoot.updateBlogRoot(notSameUrlBlog))
                                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("블로그 업데이트 시 발생시간 비교 시 기존 블로그 발행시간보다 조회한 발행시간이 과거일 경우 에러가 발생한다.")
        void updateBlogRootWithPostPublishedDateTime() {
                // given
                BlogRoot blogRoot = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                blogRoot.updatePublishedDateTime(ANY_PUBLISHED_DATE_TIME);
                BlogRoot pastBlogRoot = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                pastBlogRoot.updatePublishedDateTime(ANY_PUBLISHED_DATE_TIME.minusNanos(1));
                // expected
                assertThatThrownBy(() -> blogRoot.updateBlogRoot(pastBlogRoot))
                                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("블로그 업데이트 시 blogTitle, author, publishedDateTime이 다르다면 해당 값들이 변경된다.")
        void updateBlogRoot() {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                BlogRoot compared = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, "변경 제목", "변경 작가", null,
                                ANY_PUBLISHED_DATE_TIME);
                compared.updatePublishedDateTime(ANY_PUBLISHED_DATE_TIME.plusSeconds(1));
                // when
                sut.updateBlogRoot(compared);
                // then
                assertThat(sut.getBlogInfo().getBlogTitle()).isEqualTo("변경 제목");
                assertThat(sut.getBlogInfo().getAuthor()).isEqualTo("변경 작가");
                assertThat(sut.getBlogInfo().getPublishedDateTime()).isEqualTo(ANY_PUBLISHED_DATE_TIME.plusSeconds(1));
        }

        @Test
        @DisplayName("블로그 업데이트 시 BlogInfo의 PublishedDate가 변경되었다면 포스트 정보도 변경된 것이다.")
        void isUpdatedPostsByPublishedDate() {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                // when
                BlogRoot compared = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                compared.updatePublishedDateTime(ANY_PUBLISHED_DATE_TIME.plusMinutes(1));
                // then
                assertThat(sut.isBlogUpdated(compared)).isTrue();
        }

        @Test
        @DisplayName("포스트 업데이트 시 postUrl이 기존에 존재하지 않았으면 BlogRoot의 posts에 전달된 Post를 추가한다.")
        void updatePosts() throws MalformedURLException {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                Map<URL, Post> sutPosts = sut.getPosts();
                sutPosts.put(URI.create("https://example.com/blog123/1").toURL(),
                                sut.createPost(URI.create("https://example.com/blog123/1").toURL(), "포스트 제목1",
                                                ANY_PUBLISHED_DATE_TIME.plusSeconds(1)));
                BlogRoot compared = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                Map<URL, Post> comparedPosts = compared.getPosts();
                comparedPosts.put(URI.create("https://example.com/blog123/2").toURL(),
                                compared.createPost(URI.create("https://example.com/blog123/2").toURL(), "포스트 제목2",
                                                ANY_PUBLISHED_DATE_TIME.plusSeconds(2)));
                comparedPosts.put(URI.create("https://example.com/blog123/3").toURL(),
                                compared.createPost(URI.create("https://example.com/blog123/3").toURL(), "포스트 제목3",
                                                ANY_PUBLISHED_DATE_TIME.plusSeconds(3)));
                comparedPosts.put(URI.create("https://example.com/blog123/4").toURL(),
                                compared.createPost(URI.create("https://example.com/blog123/4").toURL(), "포스트 제목4",
                                                ANY_PUBLISHED_DATE_TIME.plusSeconds(4)));
                compared.updatePublishedDateTime(LocalDateTime.of(2022, 1, 1, 12, 3, 31));
                // when
                sut.updateBlogRoot(compared);
                // then
                assertThat(sut.getPosts().entrySet())
                                .hasSize(4)
                                .extracting(
                                                e -> e.getValue().getPostUrl(), e -> e.getValue().getPostTitle(),
                                                e -> e.getValue().getPublishedDateTime())
                                .containsExactlyInAnyOrder(
                                                tuple(URI.create("https://example.com/blog123/1").toURL(), "포스트 제목1",
                                                                ANY_PUBLISHED_DATE_TIME.plusSeconds(1)),
                                                tuple(URI.create("https://example.com/blog123/2").toURL(), "포스트 제목2",
                                                                ANY_PUBLISHED_DATE_TIME.plusSeconds(2)),
                                                tuple(URI.create("https://example.com/blog123/3").toURL(), "포스트 제목3",
                                                                ANY_PUBLISHED_DATE_TIME.plusSeconds(3)),
                                                tuple(URI.create("https://example.com/blog123/4").toURL(), "포스트 제목4",
                                                                ANY_PUBLISHED_DATE_TIME.plusSeconds(4)));
        }

        @Test
        @DisplayName("Post 생성 시 BlogRoot의 posts에 추가되어 생성된다.")
        void createPost() {
                // given
                BlogRoot blogRoot = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                // when
                Post post = blogRoot.createPost(ANY_POST_URL, ANY_POST_TITLE, ANY_PUBLISHED_DATE_TIME);
                blogRoot.getPosts().put(post.getPostUrl(), post);
                // then
                Post savedPost = blogRoot.getPosts().get(ANY_POST_URL);

                assertThat(savedPost)
                                .extracting(Post::getPostUrl, Post::getPostTitle, Post::getPublishedDateTime)
                                .containsExactly(ANY_POST_URL, ANY_POST_TITLE, ANY_PUBLISHED_DATE_TIME);
        }

        @Test
        @DisplayName("블로그 업데이트 시 post의 URL이 같다면 포스트 제목과 포스트 발행시간 비교 후 다르면 업데이트한다.")
        void updatePostsWithSameUrl() {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                Map<URL, Post> sutPosts = sut.getPosts();
                sutPosts.put(ANY_POST_URL,
                                sut.createPost(ANY_POST_URL, ANY_POST_TITLE,
                                                ANY_PUBLISHED_DATE_TIME.plusSeconds(1)));

                BlogRoot compared = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                Map<URL, Post> comparedPosts = compared.getPosts();
                Post updatedPost = compared.createPost(ANY_POST_URL, "포스트 제목1 변경",
                                ANY_PUBLISHED_DATE_TIME.plusSeconds(2));
                comparedPosts.put(ANY_POST_URL, updatedPost);
                compared.updatePublishedDateTime(updatedPost.getPublishedDateTime()); // 발생시간 업데이트
                // when
                sut.updateBlogRoot(compared);
                // then
                Post post = sut.getPosts().get(ANY_POST_URL);
                assertThat(post.getPostTitle()).isEqualTo("포스트 제목1 변경");
                assertThat(post.getPublishedDateTime()).isEqualTo(ANY_PUBLISHED_DATE_TIME.plusSeconds(2));
        }

        @Test
        @DisplayName("블로그 업데이트 시 post의 URL이 다르면 posts에 추가한다.")
        void updatePostsWithNotSameUrl() throws MalformedURLException {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                Map<URL, Post> sutPosts = sut.getPosts();
                sutPosts.put(ANY_POST_URL,
                                sut.createPost(ANY_POST_URL, ANY_POST_TITLE,
                                                ANY_PUBLISHED_DATE_TIME.plusSeconds(1)));

                BlogRoot compared = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                Map<URL, Post> comparedPosts = compared.getPosts();
                Post updatedPost = compared.createPost(URI.create("https://example.com/blog123/124").toURL(),
                                ANY_POST_TITLE, ANY_PUBLISHED_DATE_TIME.plusSeconds(2));
                comparedPosts.put(URI.create("https://example.com/blog123/124").toURL(), updatedPost);
                compared.updatePublishedDateTime(updatedPost.getPublishedDateTime()); // 발생시간 업데이트
                // when
                sut.updateBlogRoot(compared);
                // then
                Map<URL, Post> posts = sut.getPosts();
                assertThat(posts.entrySet()).hasSize(2)
                                .extracting(e -> e.getValue().getPostUrl())
                                .containsExactlyInAnyOrder(
                                                ANY_POST_URL,
                                                URI.create("https://example.com/blog123/124").toURL());
        }

        @Test
        @DisplayName("BlogRoot의 visit()을 호출하면 blogInfo의 blogVisitCount가 1 증가한다.")
        void visit() {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                // when
                sut.visit();
                // then
                assertThat(sut.getBlogInfo().getBlogVisitCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("visitPost을 호출할 때 존재하지 않은 postUrl로 호출하면 에러가 발생한다.")
        void visitPostByNotExistsPostUrl() throws MalformedURLException {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                Map<URL, Post> posts = sut.getPosts();
                Post post = sut.createPost(ANY_POST_URL, "포스트 제목1 변경", ANY_PUBLISHED_DATE_TIME.plusSeconds(2));
                posts.put(URI.create("https://example.com/blog123/XXX").toURL(), post);
                // expected
                assertThatThrownBy(() -> sut.visitPost(ANY_POST_URL)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("postUrl로 visitPost을 호출하면 해당 postUrl에 해당하는 Post의 postVisitCount가 증가한다.")
        void visitPostByPostUrl() {
                // given
                BlogRoot sut = BlogRoot.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null,
                                ANY_PUBLISHED_DATE_TIME);
                Map<URL, Post> posts = sut.getPosts();
                Post post = sut.createPost(ANY_POST_URL, "포스트 제목1 변경", ANY_PUBLISHED_DATE_TIME.plusSeconds(2));
                posts.put(ANY_POST_URL, post);
                // then
                for (int i = 0; i < 13; i++) {
                        sut.visitPost(ANY_POST_URL);
                }
                assertThat(sut.getPosts().get(ANY_POST_URL).getPostVisitCount()).isEqualTo(13);
        }
}