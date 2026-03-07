package rss

import (
	"regexp"
	"strings"
	"time"

	"github.com/mmcdole/gofeed"
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/feed"
)

var imgSrcRe = regexp.MustCompile(`<img[^>]+src=["']([^"']+)["']`)

const userAgent = "Closest/1.0 (RSS Reader; +https://github.com/nyoongoon/closest-v2)"

type FeedClient struct {
	parser *gofeed.Parser
}

func NewFeedClient() *FeedClient {
	p := gofeed.NewParser()
	p.UserAgent = userAgent
	return &FeedClient{parser: p}
}

func (c *FeedClient) GetFeed(rssURL string) (*feed.Feed, error) {
	f, err := c.parser.ParseURL(rssURL)
	if err != nil {
		return nil, err
	}

	var thumbnailURL string
	if f.Image != nil {
		thumbnailURL = f.Image.URL
	}

	items := make([]feed.FeedItem, 0, len(f.Items))
	for _, entry := range f.Items {
		pubTime := time.Now()
		if entry.PublishedParsed != nil {
			pubTime = *entry.PublishedParsed
		} else if entry.UpdatedParsed != nil {
			pubTime = *entry.UpdatedParsed
		}

		entryThumb := extractEntryImage(entry)

		items = append(items, feed.FeedItem{
			PostURL:           entry.Link,
			PostTitle:         entry.Title,
			PublishedDateTime: pubTime,
			ThumbnailURL:      entryThumb,
		})
	}

	result := &feed.Feed{
		RssURL:       rssURL,
		BlogURL:      f.Link,
		BlogTitle:    f.Title,
		Author:       "",
		ThumbnailURL: thumbnailURL,
		Items:        items,
	}

	if f.Author != nil {
		result.Author = f.Author.Name
	}

	result.PublishedDateTime = result.ExtractRecentPublishedDateTime()

	return result, nil
}

// extractEntryImage tries to find the first image URL from an RSS entry.
func extractEntryImage(entry *gofeed.Item) string {
	// 1. entry.Image (Atom/RSS media)
	if entry.Image != nil && entry.Image.URL != "" {
		return entry.Image.URL
	}

	// 2. Media extensions (media:content, media:thumbnail)
	if entry.Extensions != nil {
		if media, ok := entry.Extensions["media"]; ok {
			for _, key := range []string{"thumbnail", "content"} {
				if items, ok := media[key]; ok && len(items) > 0 {
					if url := items[0].Attrs["url"]; url != "" {
						return url
					}
				}
			}
		}
	}

	// 3. Enclosures (image/*)
	for _, enc := range entry.Enclosures {
		if strings.HasPrefix(enc.Type, "image/") && enc.URL != "" {
			return enc.URL
		}
	}

	// 4. Parse <img> from content/description HTML
	for _, html := range []string{entry.Content, entry.Description} {
		if html == "" {
			continue
		}
		if matches := imgSrcRe.FindStringSubmatch(html); len(matches) > 1 {
			return matches[1]
		}
	}

	return ""
}
