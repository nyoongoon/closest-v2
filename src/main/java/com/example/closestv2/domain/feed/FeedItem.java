package com.example.closestv2.domain.feed;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.net.URL;
import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PROTECTED)
public class FeedItem {
    private URL postUrl;
    private String postTitle;
    private LocalDateTime publishedDateTime;

    public static FeedItem create(
            URL postUrl,
            String postTitle,
            LocalDateTime publishedDateTime) {
        return FeedItem.builder()
                .postUrl(postUrl)
                .postTitle(postTitle)
                .publishedDateTime(publishedDateTime)
                .build();
    }
}
