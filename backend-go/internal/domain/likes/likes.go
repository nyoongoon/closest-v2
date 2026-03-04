package likes

type Likes struct {
	ID       int64  `db:"likes_id"`
	MemberID int64  `db:"member_id"`
	PostURL  string `db:"post_url"`
}

type Repository interface {
	Save(l *Likes) (int64, error)
}
