package service

import (
	"database/sql"
	"errors"
	"time"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/blog"
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/feed"
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/subscription"
)

type SubscriptionRegisterService struct {
	feedClient  feed.Client
	blogRepo    blog.Repository
	subRepo     subscription.Repository
}

func NewSubscriptionRegisterService(feedClient feed.Client, blogRepo blog.Repository, subRepo subscription.Repository) *SubscriptionRegisterService {
	return &SubscriptionRegisterService{
		feedClient: feedClient,
		blogRepo:   blogRepo,
		subRepo:    subRepo,
	}
}

func (s *SubscriptionRegisterService) Register(memberEmail, rssURL string) error {
	existingBlog, err := s.blogRepo.FindByRssURL(rssURL)
	if err != nil {
		return err
	}

	var blogURL, blogTitle, thumbnailURL string
	var publishedDateTime time.Time

	if existingBlog != nil {
		blogURL = existingBlog.BlogURL
		blogTitle = existingBlog.BlogTitle
		publishedDateTime = existingBlog.GetPublishedTime()
		if existingBlog.ThumbnailURL.Valid {
			thumbnailURL = existingBlog.ThumbnailURL.String
		}
	} else {
		f, err := s.feedClient.GetFeed(rssURL)
		if err != nil {
			return err
		}
		blogURL = f.BlogURL
		blogTitle = f.BlogTitle
		publishedDateTime = f.ExtractRecentPublishedDateTime()
		thumbnailURL = f.ThumbnailURL

		newBlog := &blog.Blog{
			RssURL:            rssURL,
			BlogURL:           blogURL,
			BlogTitle:         blogTitle,
			Author:            sql.NullString{String: f.Author, Valid: f.Author != ""},
			ThumbnailURL:      sql.NullString{String: thumbnailURL, Valid: thumbnailURL != ""},
			PublishedDateTime: publishedDateTime.Format(time.RFC3339),
			BlogVisitCount:    0,
		}
		blogID, err := s.blogRepo.Save(newBlog)
		if err != nil {
			return err
		}
		for _, item := range f.Items {
			post := &blog.Post{
				BlogID:            blogID,
				PostURL:           item.PostURL,
				PostTitle:         item.PostTitle,
				PublishedDateTime: item.PublishedDateTime.Format(time.RFC3339),
				PostVisitCount:    0,
			}
			if _, err := s.blogRepo.SavePost(post); err != nil {
				return err
			}
		}
	}

	sub := &subscription.Subscription{
		MemberEmail:            memberEmail,
		SubscriptionVisitCount: 0,
		SubscriptionNickName:   sql.NullString{String: blogTitle, Valid: true},
		BlogURL:                blogURL,
		BlogTitle:              blogTitle,
		PublishedDateTime:      publishedDateTime.Format(time.RFC3339),
		NewPostCount:           0,
		ThumbnailURL:           sql.NullString{String: thumbnailURL, Valid: thumbnailURL != ""},
	}
	_, err = s.subRepo.Save(sub)
	return err
}

func (s *SubscriptionRegisterService) Unregister(memberEmail string, subscriptionID int64) error {
	sub, err := s.subRepo.FindByID(subscriptionID)
	if err != nil {
		return err
	}
	if sub == nil {
		return errors.New("해당 구독정보를 찾을 수 없습니다.")
	}
	if sub.MemberEmail != memberEmail {
		return errors.New("권한이 부족합니다 - memberId가 일치하지 않음")
	}
	return s.subRepo.Delete(subscriptionID)
}
