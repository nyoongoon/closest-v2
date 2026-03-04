package service

import (
	"database/sql"
	"errors"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/blog"
)

type BlogEditService struct {
	blogRepo blog.Repository
}

func NewBlogEditService(blogRepo blog.Repository) *BlogEditService {
	return &BlogEditService{blogRepo: blogRepo}
}

func (s *BlogEditService) EditStatusMessage(blogURL, statusMessage string) error {
	b, err := s.blogRepo.FindByBlogURL(blogURL)
	if err != nil {
		return err
	}
	if b == nil {
		return errors.New("존재하지 않는 블로그입니다.")
	}
	b.StatusMessage = sql.NullString{String: statusMessage, Valid: true}
	return s.blogRepo.Update(b)
}
