package blog

import (
	"database/sql"
	"time"
)

type Blog struct {
	ID                int64          `db:"blog_id"`
	RssURL            string         `db:"rss_url"`
	BlogURL           string         `db:"blog_url"`
	BlogTitle         string         `db:"blog_title"`
	Author            sql.NullString `db:"author"`
	ThumbnailURL      sql.NullString `db:"thumbnail_url"`
	PublishedDateTime string         `db:"published_date_time"`
	BlogVisitCount    int64          `db:"blog_visit_count"`
	StatusMessage     sql.NullString `db:"status_message"`
}

type Post struct {
	ID                int64  `db:"post_id"`
	BlogID            int64  `db:"blog_id"`
	PostURL           string `db:"post_url"`
	PostTitle         string `db:"post_title"`
	PublishedDateTime string `db:"published_date_time"`
	PostVisitCount    int64  `db:"post_visit_count"`
}

func (b *Blog) GetPublishedTime() time.Time {
	t, _ := time.Parse(time.RFC3339, b.PublishedDateTime)
	return t
}

func (p *Post) GetPublishedTime() time.Time {
	t, _ := time.Parse(time.RFC3339, p.PublishedDateTime)
	return t
}
