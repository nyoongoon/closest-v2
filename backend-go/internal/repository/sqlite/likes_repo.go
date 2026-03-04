package sqlite

import (
	"github.com/jmoiron/sqlx"
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/likes"
)

type LikesRepo struct {
	db *sqlx.DB
}

func NewLikesRepo(db *sqlx.DB) *LikesRepo {
	return &LikesRepo{db: db}
}

func (r *LikesRepo) Save(l *likes.Likes) (int64, error) {
	res, err := r.db.Exec(
		`INSERT INTO likes (member_id, post_url) VALUES (?, ?)`,
		l.MemberID, l.PostURL,
	)
	if err != nil {
		return 0, err
	}
	return res.LastInsertId()
}
