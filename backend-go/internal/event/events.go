package event

type MyBlogSaveEvent struct {
	MemberEmail string
	BlogURL     string
}

type StatusMessageEditEvent struct {
	BlogURL       string
	StatusMessage string
}

type SubscriptionsBlogVisitEvent struct {
	SubscriptionID int64
	BlogURL        string
}

type SubscriptionsPostVisitEvent struct {
	SubscriptionID int64
	BlogURL        string
	PostURL        string
}
