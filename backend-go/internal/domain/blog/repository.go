package blog

type Repository interface {
	Save(b *Blog) (int64, error)
	Update(b *Blog) error
	FindByID(id int64) (*Blog, error)
	FindByRssURL(rssURL string) (*Blog, error)
	FindByBlogURL(blogURL string) (*Blog, error)
	FindAll(page, size int) ([]*Blog, bool, error)
	SavePost(p *Post) (int64, error)
	UpdatePost(p *Post) error
	FindPostsByBlogID(blogID int64) ([]*Post, error)
	FindPostByBlogIDAndURL(blogID int64, postURL string) (*Post, error)
	FindAllPosts() ([]*Post, error)
}
