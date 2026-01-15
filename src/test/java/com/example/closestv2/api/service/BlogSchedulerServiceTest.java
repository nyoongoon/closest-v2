package com.example.closestv2.api.service;

import com.example.closestv2.domain.blog.BlogRepository;
import com.example.closestv2.domain.blog.BlogRoot;
import com.example.closestv2.domain.blog.Post;
import com.example.closestv2.support.IntegrationTestSupport;
import com.example.closestv2.util.file.FileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class BlogSchedulerServiceTest extends IntegrationTestSupport {
        private final URL ANY_RSS_URL = URI.create("http://localhost:8888").toURL();
        private final String RSS_FEED_RESPONSE = FileUtil.readFileAsString("rssResponse.xml");
        private final String ANY_LINK = "http://localhost:8888/";
        private final String ANY_AUTHOR = "블로그 작가";
        private final String ANY_BLOG_TITLE = "블로그 제목";
        private final LocalDateTime ANY_PUBLISHED_DATE_TIME = LocalDateTime.of(2030, 1, 1, 12, 3, 31);
        private final String POST_ONE_TITLE = "포스트 제목 1";
        private final String POST_ONE_LINK = "http://localhost:8888/1";
        private final String POST_TWO_TITLE = "포스트 제목 2";
        private final String POST_TWO_LINK = "http://localhost:8888/2";
        private ClientAndServer mockServer;
        @Autowired
        private BlogRepository blogRepository;
        @Autowired
        private BlogSchedulerService blogSchedulerService;

        BlogSchedulerServiceTest() throws IOException {
        }

        // ClientAndServer 객체를 이용해서 mockServer를 킵니다
        // MockServerClient 객체에서 목킹할 요청과 응답을 설정합니다
        @BeforeEach
        void setUp() {

                mockServer = ClientAndServer.startClientAndServer(8888);
                new MockServerClient("localhost", 8888)
                                .when(
                                                request()
                                                                .withMethod("GET"))
                                .respond(
                                                response()
                                                                .withHeader(new Header("Content-Type",
                                                                                "text/xml;charset=utf-8"))
                                                                .withBody(RSS_FEED_RESPONSE));
        }

        // ClientAndServer 객체를 이용해서 mockServer를 끕니다
        @AfterEach
        void shutDown() {
                mockServer.stop();
        }

        @Test
        @DisplayName("업데이트 된 블로그를 확인 후 블로그 정보를 업데이트한다.")
        void pollingUpdatedBlogs() throws MalformedURLException {
                // given
                BlogRoot blogRoot = BlogRoot.create(ANY_RSS_URL, URI.create(ANY_LINK).toURL(), ANY_BLOG_TITLE,
                                ANY_AUTHOR, null, ANY_PUBLISHED_DATE_TIME);
                blogRepository.save(blogRoot);

                // when
                // 비동기 작업
                CompletableFuture<Void> future = blogSchedulerService.pollingUpdatedBlogs();
                // CompletableFuture 완료 대기
                future.join();

                // then
                // 비동기 작업이 완전히 끝나고 나서 then을 실행
                BlogRoot found = blogRepository.findById(blogRoot.getId()).orElseThrow();
                found.getBlogInfo().getPublishedDateTime().isEqual(ANY_PUBLISHED_DATE_TIME.plusMinutes(3));
                Map<URL, Post> posts = found.getPosts();
                assertThat(posts.entrySet())
                                .hasSize(2)
                                .extracting(e -> e.getValue().getPostUrl(), e -> e.getValue().getPostTitle(),
                                                e -> e.getValue().getPublishedDateTime())
                                .containsExactlyInAnyOrder(
                                                tuple(URI.create(POST_ONE_LINK).toURL(), POST_ONE_TITLE,
                                                                ANY_PUBLISHED_DATE_TIME.plusMinutes(1)),
                                                tuple(URI.create(POST_TWO_LINK).toURL(), POST_TWO_TITLE,
                                                                ANY_PUBLISHED_DATE_TIME.plusMinutes(2)));
        }
}
