package sqlite

import (
	"database/sql"

	"github.com/jmoiron/sqlx"
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/blog"
)

type BlogRepo struct {
	db *sqlx.DB
}

func NewBlogRepo(db *sqlx.DB) *BlogRepo {
	return &BlogRepo{db: db}
}

func (r *BlogRepo) Save(b *blog.Blog) (int64, error) {
	res, err := r.db.Exec(
		`INSERT INTO blog (rss_url, blog_url, blog_title, author, thumbnail_url, published_date_time, blog_visit_count, status_message)
		 VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
		b.RssURL, b.BlogURL, b.BlogTitle, b.Author, b.ThumbnailURL, b.PublishedDateTime, b.BlogVisitCount, b.StatusMessage,
	)
	if err != nil {
		return 0, err
	}
	return res.LastInsertId()
}

func (r *BlogRepo) Update(b *blog.Blog) error {
	_, err := r.db.Exec(
		`UPDATE blog SET rss_url=?, blog_url=?, blog_title=?, author=?, thumbnail_url=?, published_date_time=?, blog_visit_count=?, status_message=?
		 WHERE blog_id=?`,
		b.RssURL, b.BlogURL, b.BlogTitle, b.Author, b.ThumbnailURL, b.PublishedDateTime, b.BlogVisitCount, b.StatusMessage, b.ID,
	)
	return err
}

func (r *BlogRepo) FindByID(id int64) (*blog.Blog, error) {
	var b blog.Blog
	err := r.db.Get(&b, `SELECT * FROM blog WHERE blog_id = ?`, id)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &b, nil
}

func (r *BlogRepo) FindByRssURL(rssURL string) (*blog.Blog, error) {
	var b blog.Blog
	err := r.db.Get(&b, `SELECT * FROM blog WHERE rss_url = ?`, rssURL)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &b, nil
}

func (r *BlogRepo) FindByBlogURL(blogURL string) (*blog.Blog, error) {
	var b blog.Blog
	err := r.db.Get(&b, `SELECT * FROM blog WHERE blog_url = ?`, blogURL)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &b, nil
}

func (r *BlogRepo) FindAll(page, size int) ([]*blog.Blog, bool, error) {
	var blogs []*blog.Blog
	err := r.db.Select(&blogs, `SELECT * FROM blog LIMIT ? OFFSET ?`, size+1, page*size)
	if err != nil {
		return nil, false, err
	}
	hasMore := len(blogs) > size
	if hasMore {
		blogs = blogs[:size]
	}
	return blogs, hasMore, nil
}

func (r *BlogRepo) SavePost(p *blog.Post) (int64, error) {
	res, err := r.db.Exec(
		`INSERT INTO post (blog_id, post_url, post_title, published_date_time, post_visit_count, thumbnail_url)
		 VALUES (?, ?, ?, ?, ?, ?)`,
		p.BlogID, p.PostURL, p.PostTitle, p.PublishedDateTime, p.PostVisitCount, p.ThumbnailURL,
	)
	if err != nil {
		return 0, err
	}
	return res.LastInsertId()
}

func (r *BlogRepo) UpdatePost(p *blog.Post) error {
	_, err := r.db.Exec(
		`UPDATE post SET post_title=?, published_date_time=?, post_visit_count=?, thumbnail_url=?
		 WHERE post_id=?`,
		p.PostTitle, p.PublishedDateTime, p.PostVisitCount, p.ThumbnailURL, p.ID,
	)
	return err
}

func (r *BlogRepo) FindPostsByBlogID(blogID int64) ([]*blog.Post, error) {
	var posts []*blog.Post
	err := r.db.Select(&posts, `SELECT * FROM post WHERE blog_id = ?`, blogID)
	if err != nil {
		return nil, err
	}
	return posts, nil
}

func (r *BlogRepo) FindPostByBlogIDAndURL(blogID int64, postURL string) (*blog.Post, error) {
	var p blog.Post
	err := r.db.Get(&p, `SELECT * FROM post WHERE blog_id = ? AND post_url = ?`, blogID, postURL)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &p, nil
}

func (r *BlogRepo) FindAllPosts() ([]*blog.Post, error) {
	var posts []*blog.Post
	err := r.db.Select(&posts, `SELECT * FROM post ORDER BY published_date_time DESC`)
	if err != nil {
		return nil, err
	}
	return posts, nil
}
