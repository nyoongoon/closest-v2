package com.example.closestv2.api.service;

import com.example.closestv2.api.usecases.BlogAuthUsecase;
import com.example.closestv2.domain.blog.BlogAuthCode;
import com.example.closestv2.domain.blog.BlogAuthenticator;
import com.example.closestv2.domain.blog.event.MyBlogSaveEvent;
import com.example.closestv2.domain.feed.Feed;
import com.example.closestv2.domain.feed.FeedClient;
import com.example.closestv2.domain.feed.FeedItem;
import com.example.closestv2.infrastructure.domain.blog.BlogAuthCodeRepository;
import com.example.closestv2.models.AuthMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogAuthService implements BlogAuthUsecase {
    private final FeedClient feedClient;
    private final ApplicationEventPublisher eventPublisher;
    private final BlogAuthenticator blogAuthenticator;
    private final BlogAuthCodeRepository blogAuthCodeRepository;

    @Override
    public AuthMessageResponse createBlogAuthMessage(String memberEmail, URI rssUri) {
        try {
            Feed feed = feedClient.getFeed(rssUri.toURL());
            BlogAuthCode blogAuthCode = blogAuthenticator.createAuthCode(memberEmail, feed.getRssUrl());
            blogAuthCodeRepository.save(blogAuthCode);
            return new AuthMessageResponse().authMessage(blogAuthCode.authMessage());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(WRONG_RSS_URL_FORMAT);
        } catch (Exception e) {
            throw new IllegalStateException(SERVER_ERROR);
        }
    }

    /**
     * MyBlogSave 이벤트 발생
     */
    @Override
    public void verifyBlogAuthMessage(String memberEmail) {
        BlogAuthCode blogAuthCode = blogAuthCodeRepository.findByMemberEmail(memberEmail);
        URL rssUrl = blogAuthCode.rssUrl();
        Feed feed;
        try {
            feed = feedClient.getFeed(rssUrl);
        } catch (IllegalStateException e) {
            log.error("BlogAuthService#verifyBlogAuthMessage() :{} ", e.getMessage());
            throw new IllegalStateException(RSS_CLIENT_ERROR);
        } catch (Exception e) {
            log.error("BlogAuthService#verifyBlogAuthMessage() :{} ", e.getMessage());
            throw new IllegalStateException(SERVER_ERROR);
        }
        List<FeedItem> feedItems = feed.getFeedItems();
        FeedItem recentFeedItem = getRecentFeedItem(feedItems);
        boolean isAuthenticated = blogAuthenticator.authenticate(blogAuthCode, recentFeedItem.getPostTitle());

        if (isAuthenticated) {
            // MyBlog 생성 이벤트 발행
            eventPublisher.publishEvent(new MyBlogSaveEvent(memberEmail, feed.getBlogUrl()));
        } else {
            throw new IllegalArgumentException(FAIL_BLOG_AUTHENTICATE);
        }
    }

    private FeedItem getRecentFeedItem(List<FeedItem> feedItems) {
        feedItems.sort((x, y) -> y.getPublishedDateTime().compareTo(x.getPublishedDateTime()));
        return feedItems.getFirst();
    }
}
