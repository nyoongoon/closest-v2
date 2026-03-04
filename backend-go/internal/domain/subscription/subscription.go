package subscription

import "database/sql"

type Subscription struct {
	ID                     int64          `db:"subscription_id"`
	MemberEmail            string         `db:"member_email"`
	SubscriptionVisitCount int64          `db:"subscription_visit_count"`
	SubscriptionNickName   sql.NullString `db:"subscription_nick_name"`
	BlogURL                string         `db:"blog_url"`
	BlogTitle              string         `db:"blog_title"`
	PublishedDateTime      string         `db:"published_date_time"`
	NewPostCount           int            `db:"new_post_count"`
	ThumbnailURL           sql.NullString `db:"thumbnail_url"`
}
