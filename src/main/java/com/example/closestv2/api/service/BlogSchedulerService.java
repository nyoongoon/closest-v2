package com.example.closestv2.api.service;

import com.example.closestv2.domain.blog.BlogRepository;
import com.example.closestv2.domain.blog.BlogRoot;
import com.example.closestv2.domain.feed.Feed;
import com.example.closestv2.domain.feed.FeedClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.NOT_EXISTS_BLOG;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogSchedulerService {
    private static final int PAGE_SIZE = 100;
    private final BlogRepository blogRepository;
    private final FeedClient rssFeedClient;

    // CompletableFuture<Void>를 반환하게 하여 테스트 시 완료 시점을 체크할 수 있도록 한다.
    @Transactional(readOnly = true)
//    @Scheduled(fixedDelay = 10000) //todo 개발 편의를 위해 일단 스케듈 멈춤
    public CompletableFuture<Void> pollingUpdatedBlogs() {
        return CompletableFuture.runAsync(() -> {
            int page = 0;
            boolean hasMore = true;

            while (hasMore) {
                Page<BlogRoot> blogPage = blogRepository.findAll(PageRequest.of(page, PAGE_SIZE));
                List<BlogRoot> blogRoots = blogPage.getContent();
                hasMore = blogPage.hasNext();

                // 비동기 처리
                List<CompletableFuture<Void>> futures = blogRoots.stream()
                        .map(blogRoot ->
                                CompletableFuture
                                        .supplyAsync(
                                                // 비동기 호출
                                                () -> rssFeedClient.getFeed(blogRoot.getBlogInfo().getRssUrl()))
                                        .thenAccept(feed -> {
                                            try {
                                                // 콜백
                                                updateBlogBySyndFeed(blogRoot.getId(), feed);
                                            } catch (MalformedURLException | URISyntaxException e) {
                                                log.error("BlogSchedulerService#pollingUpdatedBlogs : {} - {}", e.getClass(), e.getMessage());
                                            }
                                        }))
                        .toList();

                // 모든 CompletableFuture가 끝날 때까지 기다림
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join(); // 이렇게하면 여기까지 동기인데 .. 페이징 구간이 비동기

                page++;
            }
        });
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateBlogBySyndFeed(long blogId, Feed feed) throws MalformedURLException, URISyntaxException {
        BlogRoot blogRoot = blogRepository.findById(blogId).orElseThrow(() -> new IllegalStateException(NOT_EXISTS_BLOG));
        log.info("updateBlogBySyndFeed() - RssUrl : {}", blogRoot.getBlogInfo().getRssUrl());
        BlogRoot recentBlogRoot = feed.toBlogRoot();
        blogRoot.updateBlogRoot(recentBlogRoot);
        blogRepository.save(blogRoot);
    }
}
