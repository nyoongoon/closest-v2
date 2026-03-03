package com.example.closestv2.infrastructure.domain.feed;

import com.example.closestv2.domain.feed.Feed;
import com.example.closestv2.domain.feed.FeedClient;
import com.example.closestv2.domain.feed.FeedItem;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class RssFeedClient implements FeedClient {

    private static final String USER_AGENT = "Closest/1.0 (RSS Reader; +https://github.com/nyoongoon/closest-v2)";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    @Override
    public Feed getFeed(URL rssUrl) {
        SyndFeed syndFeed = getSyndFeed(rssUrl);

        URL blogThumbnailUrl = Optional.ofNullable(syndFeed.getImage())
                .flatMap(image -> extractUrl(image.getUrl()))
                .orElse(null);

        List<SyndEntry> entries = syndFeed.getEntries();
        List<FeedItem> feedItems = new ArrayList<>();
        for (SyndEntry entry : entries) {
            FeedItem feedItem = FeedItem.create(
                    extractUrl(entry.getLink()).orElse(null),
                    entry.getTitle(),
                    toLocalDateTime(entry.getPublishedDate()));
            feedItems.add(feedItem);
        }

        Feed feed = Feed.create(
                rssUrl,
                extractUrl(syndFeed.getLink()).orElse(null),
                syndFeed.getTitle(),
                syndFeed.getAuthor(),
                blogThumbnailUrl,
                feedItems);

        return feed;
    }

    private SyndFeed getSyndFeed(URL rssUrl) {
        log.info("getSyndFeed() - rssUrl : {}", rssUrl);
        try {
            HttpURLConnection conn = (HttpURLConnection) rssUrl.openConnection();
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);

            InputStream inputStream = conn.getInputStream();
            XmlReader reader = new XmlReader(inputStream, conn.getContentType());
            return new SyndFeedInput().build(reader);
        } catch (FeedException | IOException e) {
            throw new IllegalStateException(e);
        } finally {
            log.info("getSyndFeed() - rssUrl completed : {}", rssUrl);
        }
    }

    private Optional<URL> extractUrl(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(URI.create(url).toURL());
        } catch (MalformedURLException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
