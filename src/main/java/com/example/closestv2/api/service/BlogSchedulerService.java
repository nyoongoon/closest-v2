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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.NOT_EXISTS_BLOG;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogSchedulerService {
    private static final int PAGE_SIZE = 100;
    private static final long REQUEST_DELAY_MS = 1000; // 요청 간 1초 딜레이 (서버 부하 방지)
    private final BlogRepository blogRepository;
    private final FeedClient rssFeedClient;

    // CompletableFuture<Void>를 반환하게 하여 테스트 시 완료 시점을 체크할 수 있도록 한다.
    @Transactional(readOnly = true)
    @Scheduled(fixedDelay = 600_000, initialDelay = 10_000) // 10분 간격, 서버 시작 10초 후 첫 실행
    public CompletableFuture<Void> pollingUpdatedBlogs() {
        log.info("RSS 폴링 스케줄러 시작");
        return CompletableFuture.runAsync(() -> {
            int page = 0;
            boolean hasMore = true;

            while (hasMore) {
                Page<BlogRoot> blogPage = blogRepository.findAll(PageRequest.of(page, PAGE_SIZE));
                List<BlogRoot> blogRoots = blogPage.getContent();
                hasMore = blogPage.hasNext();

                // 순차 처리 (요청 간 딜레이를 두어 공격으로 의심받지 않도록)
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (BlogRoot blogRoot : blogRoots) {
                    try {
                        Feed feed = rssFeedClient.getFeed(blogRoot.getBlogInfo().getRssUrl());
                        updateBlogBySyndFeed(blogRoot.getId(), feed);
                    } catch (Exception e) {
                        log.warn("RSS 폴링 실패 - blogId: {}, url: {}, error: {}",
                                blogRoot.getId(), blogRoot.getBlogInfo().getRssUrl(), e.getMessage());
                    }
                    // 요청 간 딜레이
                    try {
                        Thread.sleep(REQUEST_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                page++;
            }
            log.info("RSS 폴링 스케줄러 완료");
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
