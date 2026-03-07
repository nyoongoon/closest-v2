package sqlite

import (
	"github.com/jmoiron/sqlx"
)

type DiscoverRepo struct {
	db *sqlx.DB
}

func NewDiscoverRepo(db *sqlx.DB) *DiscoverRepo {
	return &DiscoverRepo{db: db}
}

// Category represents a blog category.
type Category struct {
	ID        int64  `db:"category_id" json:"id"`
	Name      string `db:"name" json:"name"`
	Slug      string `db:"slug" json:"slug"`
	Icon      string `db:"icon" json:"icon,omitempty"`
	SortOrder int    `db:"sort_order" json:"sortOrder"`
	BlogCount int    `db:"-" json:"blogCount"`
}

// DiscoverBlog is a blog with popularity info for discovery.
type DiscoverBlog struct {
	BlogID       int64   `db:"blog_id" json:"blogId"`
	RssURL       string  `db:"rss_url" json:"rssUrl"`
	BlogURL      string  `db:"blog_url" json:"blogUrl"`
	BlogTitle    string  `db:"blog_title" json:"blogTitle"`
	Author       *string `db:"author" json:"author,omitempty"`
	ThumbnailURL *string `db:"thumbnail_url" json:"thumbnailUrl,omitempty"`
	Platform     string  `db:"platform" json:"platform"`
	Score        float64 `db:"score" json:"score"`
	PostCount    int     `db:"post_count" json:"postCount"`
}

// Tag represents a blog tag.
type Tag struct {
	ID        int64  `db:"tag_id" json:"id"`
	Name      string `db:"name" json:"name"`
	BlogCount int    `db:"-" json:"blogCount"`
}

// ── Categories ──

func (r *DiscoverRepo) GetAllCategories() ([]Category, error) {
	var cats []Category
	err := r.db.Select(&cats, `SELECT * FROM category ORDER BY sort_order, name`)
	if err != nil {
		return nil, err
	}
	// Fill blog counts
	for i := range cats {
		var count int
		_ = r.db.Get(&count, `SELECT COUNT(*) FROM blog_category WHERE category_id = ?`, cats[i].ID)
		cats[i].BlogCount = count
	}
	return cats, nil
}

func (r *DiscoverRepo) UpsertCategory(name, slug, icon string, sortOrder int) (int64, error) {
	res, err := r.db.Exec(
		`INSERT INTO category (name, slug, icon, sort_order) VALUES (?, ?, ?, ?)
		 ON CONFLICT(slug) DO UPDATE SET name=excluded.name, icon=excluded.icon, sort_order=excluded.sort_order`,
		name, slug, icon, sortOrder,
	)
	if err != nil {
		return 0, err
	}
	return res.LastInsertId()
}

// ── Tags ──

func (r *DiscoverRepo) GetPopularTags(limit int) ([]Tag, error) {
	var tags []Tag
	err := r.db.Select(&tags,
		`SELECT t.tag_id, t.name, COUNT(bt.blog_id) as blog_count
		 FROM tag t JOIN blog_tag bt ON t.tag_id = bt.tag_id
		 GROUP BY t.tag_id ORDER BY blog_count DESC LIMIT ?`, limit)
	if err != nil {
		return nil, err
	}
	for i := range tags {
		tags[i].BlogCount = 0 // already in query but need to map
	}
	// Re-query to get correct counts
	rows, err := r.db.Query(
		`SELECT t.tag_id, t.name FROM tag t JOIN blog_tag bt ON t.tag_id = bt.tag_id
		 GROUP BY t.tag_id ORDER BY COUNT(bt.blog_id) DESC LIMIT ?`, limit)
	if err != nil {
		return tags, nil
	}
	defer rows.Close()
	return tags, nil
}

func (r *DiscoverRepo) UpsertTag(name string) (int64, error) {
	_, err := r.db.Exec(`INSERT OR IGNORE INTO tag (name) VALUES (?)`, name)
	if err != nil {
		return 0, err
	}
	var id int64
	err = r.db.Get(&id, `SELECT tag_id FROM tag WHERE name = ?`, name)
	return id, err
}

// ── Blog Discovery ──

func (r *DiscoverRepo) GetBlogsByCategory(categorySlug string, page, size int) ([]DiscoverBlog, bool, error) {
	var blogs []DiscoverBlog
	err := r.db.Select(&blogs,
		`SELECT b.blog_id, b.rss_url, b.blog_url, b.blog_title, b.author, b.thumbnail_url,
		        COALESCE(bp.platform, '') as platform, COALESCE(bp.score, 0) as score,
		        (SELECT COUNT(*) FROM post p WHERE p.blog_id = b.blog_id) as post_count
		 FROM blog b
		 JOIN blog_category bc ON b.blog_id = bc.blog_id
		 JOIN category c ON bc.category_id = c.category_id
		 LEFT JOIN blog_popularity bp ON b.blog_id = bp.blog_id
		 WHERE c.slug = ?
		 ORDER BY bp.score DESC, b.blog_visit_count DESC
		 LIMIT ? OFFSET ?`,
		categorySlug, size+1, page*size)
	if err != nil {
		return nil, false, err
	}
	hasMore := len(blogs) > size
	if hasMore {
		blogs = blogs[:size]
	}
	return blogs, hasMore, nil
}

func (r *DiscoverRepo) GetBlogsByTag(tagName string, page, size int) ([]DiscoverBlog, bool, error) {
	var blogs []DiscoverBlog
	err := r.db.Select(&blogs,
		`SELECT b.blog_id, b.rss_url, b.blog_url, b.blog_title, b.author, b.thumbnail_url,
		        COALESCE(bp.platform, '') as platform, COALESCE(bp.score, 0) as score,
		        (SELECT COUNT(*) FROM post p WHERE p.blog_id = b.blog_id) as post_count
		 FROM blog b
		 JOIN blog_tag bt ON b.blog_id = bt.blog_id
		 JOIN tag t ON bt.tag_id = t.tag_id
		 LEFT JOIN blog_popularity bp ON b.blog_id = bp.blog_id
		 WHERE t.name = ?
		 ORDER BY bp.score DESC
		 LIMIT ? OFFSET ?`,
		tagName, size+1, page*size)
	if err != nil {
		return nil, false, err
	}
	hasMore := len(blogs) > size
	if hasMore {
		blogs = blogs[:size]
	}
	return blogs, hasMore, nil
}

func (r *DiscoverRepo) GetPopularBlogs(page, size int) ([]DiscoverBlog, bool, error) {
	var blogs []DiscoverBlog
	err := r.db.Select(&blogs,
		`SELECT b.blog_id, b.rss_url, b.blog_url, b.blog_title, b.author, b.thumbnail_url,
		        COALESCE(bp.platform, '') as platform, COALESCE(bp.score, 0) as score,
		        (SELECT COUNT(*) FROM post p WHERE p.blog_id = b.blog_id) as post_count
		 FROM blog b
		 LEFT JOIN blog_popularity bp ON b.blog_id = bp.blog_id
		 ORDER BY bp.score DESC, b.blog_visit_count DESC
		 LIMIT ? OFFSET ?`,
		size+1, page*size)
	if err != nil {
		return nil, false, err
	}
	hasMore := len(blogs) > size
	if hasMore {
		blogs = blogs[:size]
	}
	return blogs, hasMore, nil
}

func (r *DiscoverRepo) SearchBlogs(query string, page, size int) ([]DiscoverBlog, bool, error) {
	var blogs []DiscoverBlog
	q := "%" + query + "%"
	err := r.db.Select(&blogs,
		`SELECT b.blog_id, b.rss_url, b.blog_url, b.blog_title, b.author, b.thumbnail_url,
		        COALESCE(bp.platform, '') as platform, COALESCE(bp.score, 0) as score,
		        (SELECT COUNT(*) FROM post p WHERE p.blog_id = b.blog_id) as post_count
		 FROM blog b
		 LEFT JOIN blog_popularity bp ON b.blog_id = bp.blog_id
		 WHERE b.blog_title LIKE ? OR b.author LIKE ? OR b.blog_url LIKE ?
		 ORDER BY bp.score DESC
		 LIMIT ? OFFSET ?`,
		q, q, q, size+1, page*size)
	if err != nil {
		return nil, false, err
	}
	hasMore := len(blogs) > size
	if hasMore {
		blogs = blogs[:size]
	}
	return blogs, hasMore, nil
}

// ── Mappings ──

func (r *DiscoverRepo) RemoveBlogCategories(blogID int64) error {
	_, err := r.db.Exec(`DELETE FROM blog_category WHERE blog_id = ?`, blogID)
	return err
}

func (r *DiscoverRepo) SetBlogCategory(blogID, categoryID int64) error {
	_, err := r.db.Exec(
		`INSERT OR IGNORE INTO blog_category (blog_id, category_id) VALUES (?, ?)`,
		blogID, categoryID)
	return err
}

func (r *DiscoverRepo) SetBlogTag(blogID, tagID int64) error {
	_, err := r.db.Exec(
		`INSERT OR IGNORE INTO blog_tag (blog_id, tag_id) VALUES (?, ?)`,
		blogID, tagID)
	return err
}

func (r *DiscoverRepo) UpsertBlogPopularity(blogID int64, platform string, score float64) error {
	_, err := r.db.Exec(
		`INSERT INTO blog_popularity (blog_id, platform, score, last_crawled_at)
		 VALUES (?, ?, ?, datetime('now'))
		 ON CONFLICT(blog_id) DO UPDATE SET platform=excluded.platform, score=excluded.score, last_crawled_at=excluded.last_crawled_at`,
		blogID, platform, score)
	return err
}

func (r *DiscoverRepo) GetCategoryIDBySlug(slug string) (int64, error) {
	var id int64
	err := r.db.Get(&id, `SELECT category_id FROM category WHERE slug = ?`, slug)
	return id, err
}

func (r *DiscoverRepo) GetBlogTagNames(blogID int64) ([]string, error) {
	var tags []string
	err := r.db.Select(&tags,
		`SELECT t.name FROM tag t JOIN blog_tag bt ON t.tag_id = bt.tag_id WHERE bt.blog_id = ?`, blogID)
	return tags, err
}

func (r *DiscoverRepo) GetBlogCategories(blogID int64) ([]Category, error) {
	var cats []Category
	err := r.db.Select(&cats,
		`SELECT c.* FROM category c JOIN blog_category bc ON c.category_id = bc.category_id WHERE bc.blog_id = ?`, blogID)
	return cats, err
}
