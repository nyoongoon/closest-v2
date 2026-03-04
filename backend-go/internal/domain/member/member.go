package member

import "database/sql"

type Member struct {
	ID                int64          `db:"member_id"`
	UserEmail         string         `db:"user_email"`
	Password          string         `db:"password"`
	NickName          sql.NullString `db:"nick_name"`
	Authority         sql.NullString `db:"authority"`
	BlogURL           sql.NullString `db:"blog_url"`
	MyBlogVisitCount  int64          `db:"my_blog_visit_count"`
	StatusMessage     sql.NullString `db:"status_message"`
}

type Repository interface {
	Save(m *Member) (int64, error)
	FindByID(id int64) (*Member, error)
	FindByEmail(email string) (*Member, error)
	ExistsByEmail(email string) (bool, error)
	Update(m *Member) error
}
