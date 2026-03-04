package service

import (
	"errors"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/blog"
)

type BlogVisitService struct {
	blogRepo blog.Repository
}

func NewBlogVisitService(blogRepo blog.Repository) *BlogVisitService {
	return &BlogVisitService{blogRepo: blogRepo}
}

func (s *BlogVisitService) VisitBlog(blogURL string) error {
	b, err := s.blogRepo.FindByBlogURL(blogURL)
	if err != nil {
		return err
	}
	if b == nil {
		return errors.New("존재하지 않는 블로그입니다.")
	}
	b.BlogVisitCount++
	return s.blogRepo.Update(b)
}

func (s *BlogVisitService) VisitPost(blogURL, postURL string) error {
	b, err := s.blogRepo.FindByBlogURL(blogURL)
	if err != nil {
		return err
	}
	if b == nil {
		return errors.New("존재하지 않는 블로그입니다.")
	}
	b.BlogVisitCount++
	if err := s.blogRepo.Update(b); err != nil {
		return err
	}

	post, err := s.blogRepo.FindPostByBlogIDAndURL(b.ID, postURL)
	if err != nil {
		return err
	}
	if post == nil {
		return errors.New("존재하지 않는 포스트 URL입니다.")
	}
	post.PostVisitCount++
	return s.blogRepo.UpdatePost(post)
}
