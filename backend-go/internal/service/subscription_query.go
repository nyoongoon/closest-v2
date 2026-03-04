package service

import (
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/subscription"
)

type SubscriptionQueryService struct {
	subRepo subscription.Repository
}

func NewSubscriptionQueryService(subRepo subscription.Repository) *SubscriptionQueryService {
	return &SubscriptionQueryService{subRepo: subRepo}
}

type SubscriptionResponse struct {
	SubscriptionID int64  `json:"subscriptionId"`
	URI            string `json:"uri"`
	ThumbnailURL   string `json:"thumbnailUrl,omitempty"`
	NickName       string `json:"nickName,omitempty"`
	NewPostsCnt    int    `json:"newPostsCnt"`
	VisitCnt       int64  `json:"visitCnt"`
	PublishedDateTime string `json:"publishedDateTime,omitempty"`
}

func (s *SubscriptionQueryService) GetCloseSubscriptionsOfAll() ([]SubscriptionResponse, error) {
	subs, err := s.subRepo.FindAllOrderByVisitCountDesc(0, 200)
	if err != nil {
		return nil, err
	}
	return toResponses(subs), nil
}

func (s *SubscriptionQueryService) GetCloseSubscriptions(memberEmail string) ([]SubscriptionResponse, error) {
	subs, err := s.subRepo.FindByMemberEmailOrderByVisitCountDesc(memberEmail, 0, 20)
	if err != nil {
		return nil, err
	}
	return toResponses(subs), nil
}

func (s *SubscriptionQueryService) GetRecentPublishedSubscriptions(memberEmail string, page, size int) ([]SubscriptionResponse, error) {
	subs, err := s.subRepo.FindByMemberEmailOrderByPublishedDateTimeDesc(memberEmail, page, size)
	if err != nil {
		return nil, err
	}
	return toResponses(subs), nil
}

func toResponses(subs []*subscription.Subscription) []SubscriptionResponse {
	responses := make([]SubscriptionResponse, 0, len(subs))
	for _, sub := range subs {
		resp := SubscriptionResponse{
			SubscriptionID:    sub.ID,
			URI:               sub.BlogURL,
			NickName:          sub.SubscriptionNickName.String,
			NewPostsCnt:       sub.NewPostCount,
			VisitCnt:          sub.SubscriptionVisitCount,
			PublishedDateTime: sub.PublishedDateTime,
		}
		if sub.ThumbnailURL.Valid {
			resp.ThumbnailURL = sub.ThumbnailURL.String
		}
		responses = append(responses, resp)
	}
	return responses
}
