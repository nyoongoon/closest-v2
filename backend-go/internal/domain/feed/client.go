package feed

type Client interface {
	GetFeed(rssURL string) (*Feed, error)
}
