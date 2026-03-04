package sqlite

import (
	"database/sql"

	"github.com/jmoiron/sqlx"
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/subscription"
)

type SubscriptionRepo struct {
	db *sqlx.DB
}

func NewSubscriptionRepo(db *sqlx.DB) *SubscriptionRepo {
	return &SubscriptionRepo{db: db}
}

func (r *SubscriptionRepo) Save(s *subscription.Subscription) (int64, error) {
	res, err := r.db.Exec(
		`INSERT INTO subscription (member_email, subscription_visit_count, subscription_nick_name, blog_url, blog_title, published_date_time, new_post_count, thumbnail_url)
		 VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
		s.MemberEmail, s.SubscriptionVisitCount, s.SubscriptionNickName, s.BlogURL, s.BlogTitle, s.PublishedDateTime, s.NewPostCount, s.ThumbnailURL,
	)
	if err != nil {
		return 0, err
	}
	return res.LastInsertId()
}

func (r *SubscriptionRepo) FindByID(id int64) (*subscription.Subscription, error) {
	var s subscription.Subscription
	err := r.db.Get(&s, `SELECT * FROM subscription WHERE subscription_id = ?`, id)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &s, nil
}

func (r *SubscriptionRepo) Delete(id int64) error {
	_, err := r.db.Exec(`DELETE FROM subscription WHERE subscription_id = ?`, id)
	return err
}

func (r *SubscriptionRepo) Update(s *subscription.Subscription) error {
	_, err := r.db.Exec(
		`UPDATE subscription SET member_email=?, subscription_visit_count=?, subscription_nick_name=?, blog_url=?, blog_title=?, published_date_time=?, new_post_count=?, thumbnail_url=?
		 WHERE subscription_id=?`,
		s.MemberEmail, s.SubscriptionVisitCount, s.SubscriptionNickName, s.BlogURL, s.BlogTitle, s.PublishedDateTime, s.NewPostCount, s.ThumbnailURL, s.ID,
	)
	return err
}

func (r *SubscriptionRepo) FindAllOrderByVisitCountDesc(page, size int) ([]*subscription.Subscription, error) {
	var subs []*subscription.Subscription
	err := r.db.Select(&subs,
		`SELECT * FROM subscription ORDER BY subscription_visit_count DESC LIMIT ? OFFSET ?`,
		size, page*size,
	)
	if err != nil {
		return nil, err
	}
	return subs, nil
}

func (r *SubscriptionRepo) FindByMemberEmailOrderByVisitCountDesc(email string, page, size int) ([]*subscription.Subscription, error) {
	var subs []*subscription.Subscription
	err := r.db.Select(&subs,
		`SELECT * FROM subscription WHERE member_email = ? ORDER BY subscription_visit_count DESC LIMIT ? OFFSET ?`,
		email, size, page*size,
	)
	if err != nil {
		return nil, err
	}
	return subs, nil
}

func (r *SubscriptionRepo) FindByMemberEmailOrderByPublishedDateTimeDesc(email string, page, size int) ([]*subscription.Subscription, error) {
	var subs []*subscription.Subscription
	err := r.db.Select(&subs,
		`SELECT * FROM subscription WHERE member_email = ? ORDER BY published_date_time DESC LIMIT ? OFFSET ?`,
		email, size, page*size,
	)
	if err != nil {
		return nil, err
	}
	return subs, nil
}
