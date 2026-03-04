package service

import (
	"errors"
	"sort"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/blog"
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/feed"
	"github.com/nyoongoon/closest-v2/backend-go/internal/event"
	"github.com/nyoongoon/closest-v2/backend-go/internal/infrastructure/cache"
)

type BlogAuthService struct {
	feedClient    feed.Client
	authCodeCache *cache.AuthCodeCache
	eventBus      *event.Bus
}

func NewBlogAuthService(feedClient feed.Client, authCodeCache *cache.AuthCodeCache, eventBus *event.Bus) *BlogAuthService {
	return &BlogAuthService{
		feedClient:    feedClient,
		authCodeCache: authCodeCache,
		eventBus:      eventBus,
	}
}

func (s *BlogAuthService) CreateAuthMessage(memberEmail, rssURL string) (string, error) {
	_, err := s.feedClient.GetFeed(rssURL)
	if err != nil {
		return "", errors.New("RSS 조회 중 에러가 발생하였습니다.")
	}

	authCode := blog.NewAuthCode(memberEmail, rssURL)
	s.authCodeCache.Save(authCode)
	return authCode.AuthMessage, nil
}

func (s *BlogAuthService) VerifyAuthMessage(memberEmail string) error {
	authCode := s.authCodeCache.FindByMemberEmail(memberEmail)
	if authCode == nil {
		return errors.New("블로그 인증에 실패하였습니다.")
	}

	f, err := s.feedClient.GetFeed(authCode.RssURL)
	if err != nil {
		return errors.New("RSS 조회 중 에러가 발생하였습니다.")
	}

	if len(f.Items) == 0 {
		return errors.New("블로그 인증에 실패하였습니다.")
	}

	sort.Slice(f.Items, func(i, j int) bool {
		return f.Items[i].PublishedDateTime.After(f.Items[j].PublishedDateTime)
	})

	recentTitle := f.Items[0].PostTitle
	if !authCode.Authenticate(recentTitle) {
		return errors.New("블로그 인증에 실패하였습니다.")
	}

	s.eventBus.Publish(event.EventMyBlogSave, event.MyBlogSaveEvent{
		MemberEmail: memberEmail,
		BlogURL:     f.BlogURL,
	})

	return nil
}
