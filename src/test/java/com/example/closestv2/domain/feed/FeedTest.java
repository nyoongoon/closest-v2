package com.example.closestv2.domain.feed;

import com.example.closestv2.domain.blog.BlogInfo;
import com.example.closestv2.domain.blog.BlogRoot;
import com.example.closestv2.domain.blog.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class FeedTest {
        private final URL ANY_RSS_URL = URI.create("https://example.com/rss").toURL();
        private final URL ANY_BLOG_URL = URI.create("https://example.com").toURL();
        private final String ANY_BLOG_TITLE = "블로그 제목";
        private final String ANY_AUTHOR = "블로그 작가";
        private final URL ANY_POST_URL1 = URI.create("https://example.com/1").toURL();
        private final URL ANY_POST_URL2 = URI.create("https://example.com/2").toURL();
        private final String ANY_POST_TITLE = "포스트 제목";
        private final LocalDateTime ANY_PUBLISHED_DATE_TIME = LocalDateTime.of(2030, 1, 1, 12, 3, 31);

        FeedTest() throws MalformedURLException {
        }

        @Test
        @DisplayName("Feed 생성 성공 테스트")
        void createFeedSuccessTest() {
                // given
                FeedItem feedItem1 = FeedItem.create(ANY_POST_URL1, ANY_POST_TITLE, ANY_PUBLISHED_DATE_TIME);
                FeedItem feedItem2 = FeedItem.create(ANY_POST_URL1, ANY_POST_TITLE, ANY_PUBLISHED_DATE_TIME);
                List<FeedItem> feedItems = List.of(feedItem1, feedItem2);
                // when
                Feed feed = Feed.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null, feedItems);
                // then
                assertThat(feed)
                                .extracting(Feed::getRssUrl, Feed::getBlogUrl, Feed::getBlogTitle, Feed::getAuthor)
                                .containsExactly(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR);
                assertThat(feed.getFeedItems()).isEqualTo(feedItems);
        }

        @Test
        @DisplayName("Feed 생성 시 FeedItems가 null로 전달되면 빈 리스트로 생성된다.")
        void createFeedWithNullFeedItems() {
                // given
                List<FeedItem> feedItems = null;
                // when
                Feed feed = Feed.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null, feedItems);
                // then
                assertThat(feed.getFeedItems())
                                .isNotNull()
                                .hasSize(0);
        }

        // @Test
        // @DisplayName("Feed 생성 시 FeedItem이 존재하지 않는 경우 publishedDateTime은 에포크 타임으로
        // 생성된다.")
        // void publishedDateTimeWithNullFeedItems(){
        // List<FeedItem> feedItems = null;
        // //when
        // Feed feed = Feed.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE,
        // ANY_AUTHOR, null, feedItems);
        // //then
        // assertThat(feed.getPublishedDateTime()).isEqualTo(EPOCH_LOCAL_DATE_TIME)
        // }

        // @Test
        // @DisplayName("Feed 생성 시 FeedItem 중 최신 publishedDateTime으로 생성된다.")
        // void(){
        // //given
        // //when
        // //then
        // }

        @Test
        @DisplayName("Feed는 toBlogRoot()로 BlogRoot 객체로 변환할 수 있다.")
        void toBlogRoot() {
                // given
                FeedItem feedItem1 = FeedItem.create(ANY_POST_URL1, ANY_POST_TITLE, ANY_PUBLISHED_DATE_TIME);
                FeedItem feedItem2 = FeedItem.create(ANY_POST_URL2, ANY_POST_TITLE,
                                ANY_PUBLISHED_DATE_TIME.plusHours(1));
                List<FeedItem> feedItems = List.of(feedItem1, feedItem2);
                Feed feed = Feed.create(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR, null, feedItems);
                // when
                BlogRoot blogRoot = feed.toBlogRoot();
                // then
                BlogInfo blogInfo = blogRoot.getBlogInfo();
                assertThat(blogInfo)
                                .extracting(BlogInfo::getRssUrl, BlogInfo::getBlogUrl, BlogInfo::getBlogTitle,
                                                BlogInfo::getAuthor)
                                .containsExactly(ANY_RSS_URL, ANY_BLOG_URL, ANY_BLOG_TITLE, ANY_AUTHOR);
                // 변환된 blogRoot의 publishedDateTime은 FeedItem 중 최신 publishedDateTime 으로 설정됨
                assertThat(blogInfo.getPublishedDateTime()).isEqualTo(ANY_PUBLISHED_DATE_TIME.plusHours(1));
                Map<URL, Post> posts = blogRoot.getPosts();
                assertThat(posts.entrySet())
                                .hasSize(2)
                                .extracting(e -> e.getValue().getPostUrl(), e -> e.getValue().getPostTitle(),
                                                e -> e.getValue().getPublishedDateTime())
                                .containsExactlyInAnyOrder(
                                                tuple(ANY_POST_URL1, ANY_POST_TITLE, ANY_PUBLISHED_DATE_TIME),
                                                tuple(ANY_POST_URL2, ANY_POST_TITLE,
                                                                ANY_PUBLISHED_DATE_TIME.plusHours(1)));
        }
}