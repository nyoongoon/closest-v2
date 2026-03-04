package sqlite

import (
	"database/sql"

	"github.com/jmoiron/sqlx"
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/member"
)

type MemberRepo struct {
	db *sqlx.DB
}

func NewMemberRepo(db *sqlx.DB) *MemberRepo {
	return &MemberRepo{db: db}
}

func (r *MemberRepo) Save(m *member.Member) (int64, error) {
	res, err := r.db.Exec(
		`INSERT INTO member (user_email, password, nick_name, authority, blog_url, my_blog_visit_count, status_message)
		 VALUES (?, ?, ?, ?, ?, ?, ?)`,
		m.UserEmail, m.Password, m.NickName, m.Authority, m.BlogURL, m.MyBlogVisitCount, m.StatusMessage,
	)
	if err != nil {
		return 0, err
	}
	return res.LastInsertId()
}

func (r *MemberRepo) FindByID(id int64) (*member.Member, error) {
	var m member.Member
	err := r.db.Get(&m, `SELECT * FROM member WHERE member_id = ?`, id)
	if err != nil {
		return nil, err
	}
	return &m, nil
}

func (r *MemberRepo) FindByEmail(email string) (*member.Member, error) {
	var m member.Member
	err := r.db.Get(&m, `SELECT * FROM member WHERE user_email = ?`, email)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &m, nil
}

func (r *MemberRepo) ExistsByEmail(email string) (bool, error) {
	var count int
	err := r.db.Get(&count, `SELECT COUNT(*) FROM member WHERE user_email = ?`, email)
	if err != nil {
		return false, err
	}
	return count > 0, nil
}

func (r *MemberRepo) Update(m *member.Member) error {
	_, err := r.db.Exec(
		`UPDATE member SET user_email=?, password=?, nick_name=?, authority=?, blog_url=?, my_blog_visit_count=?, status_message=?
		 WHERE member_id=?`,
		m.UserEmail, m.Password, m.NickName, m.Authority, m.BlogURL, m.MyBlogVisitCount, m.StatusMessage, m.ID,
	)
	return err
}
