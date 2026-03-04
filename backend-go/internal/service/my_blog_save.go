package service

import (
	"database/sql"
	"errors"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/member"
)

type MyBlogSaveService struct {
	memberRepo member.Repository
}

func NewMyBlogSaveService(memberRepo member.Repository) *MyBlogSaveService {
	return &MyBlogSaveService{memberRepo: memberRepo}
}

func (s *MyBlogSaveService) SaveMyBlog(memberEmail, blogURL string) error {
	m, err := s.memberRepo.FindByEmail(memberEmail)
	if err != nil {
		return err
	}
	if m == nil {
		return errors.New("존재하지 않는 회원입니다.")
	}
	m.BlogURL = sql.NullString{String: blogURL, Valid: true}
	m.MyBlogVisitCount = 0
	return s.memberRepo.Update(m)
}
