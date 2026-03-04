package service

import (
	"database/sql"
	"errors"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/member"
	"github.com/nyoongoon/closest-v2/backend-go/internal/event"
)

type MyBlogEditService struct {
	memberRepo member.Repository
	eventBus   *event.Bus
}

func NewMyBlogEditService(memberRepo member.Repository, eventBus *event.Bus) *MyBlogEditService {
	return &MyBlogEditService{memberRepo: memberRepo, eventBus: eventBus}
}

func (s *MyBlogEditService) EditStatusMessage(memberEmail, message string) error {
	m, err := s.memberRepo.FindByEmail(memberEmail)
	if err != nil {
		return err
	}
	if m == nil {
		return errors.New("존재하지 않는 회원입니다.")
	}
	if !m.BlogURL.Valid {
		return errors.New("나의 블로그가 존재하지 않습니다.")
	}

	m.StatusMessage = sql.NullString{String: message, Valid: true}
	if err := s.memberRepo.Update(m); err != nil {
		return err
	}

	s.eventBus.Publish(event.EventStatusMessageEdit, event.StatusMessageEditEvent{
		BlogURL:       m.BlogURL.String,
		StatusMessage: message,
	})

	return nil
}
