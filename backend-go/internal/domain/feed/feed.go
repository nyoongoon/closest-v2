package feed

import "time"

type Feed struct {
	RssURL            string
	BlogURL           string
	BlogTitle         string
	Author            string
	ThumbnailURL      string
	PublishedDateTime time.Time
	Items             []FeedItem
}

type FeedItem struct {
	PostURL           string
	PostTitle         string
	PublishedDateTime time.Time
	ThumbnailURL      string
}

func (f *Feed) ExtractRecentPublishedDateTime() time.Time {
	epoch := time.Date(1970, 1, 1, 0, 0, 0, 0, time.UTC)
	recent := epoch
	for _, item := range f.Items {
		if item.PublishedDateTime.After(recent) {
			recent = item.PublishedDateTime
		}
	}
	return recent
}
