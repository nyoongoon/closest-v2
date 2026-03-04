package rss

import (
	"time"

	"github.com/mmcdole/gofeed"
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/feed"
)

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

		items = append(items, feed.FeedItem{
			PostURL:           entry.Link,
			PostTitle:         entry.Title,
			PublishedDateTime: pubTime,
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
