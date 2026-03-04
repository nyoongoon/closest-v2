package service

import (
	"errors"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/subscription"
	"github.com/nyoongoon/closest-v2/backend-go/internal/event"
)

type SubscriptionVisitService struct {
	subRepo  subscription.Repository
	eventBus *event.Bus
}

func NewSubscriptionVisitService(subRepo subscription.Repository, eventBus *event.Bus) *SubscriptionVisitService {
	return &SubscriptionVisitService{subRepo: subRepo, eventBus: eventBus}
}

func (s *SubscriptionVisitService) VisitSubscription(subscriptionID int64) (string, error) {
	sub, err := s.subRepo.FindByID(subscriptionID)
	if err != nil {
		return "", err
	}
	if sub == nil {
		return "", errors.New("해당 구독정보를 찾을 수 없습니다.")
	}

	sub.SubscriptionVisitCount++
	if err := s.subRepo.Update(sub); err != nil {
		return "", err
	}

	s.eventBus.Publish(event.EventSubscriptionsBlogVisit, event.SubscriptionsBlogVisitEvent{
		SubscriptionID: sub.ID,
		BlogURL:        sub.BlogURL,
	})

	return sub.BlogURL, nil
}

func (s *SubscriptionVisitService) VisitSubscriptionPost(subscriptionID int64, postURL string) (string, error) {
	sub, err := s.subRepo.FindByID(subscriptionID)
	if err != nil {
		return "", err
	}
	if sub == nil {
		return "", errors.New("해당 구독정보를 찾을 수 없습니다.")
	}

	sub.SubscriptionVisitCount++
	if err := s.subRepo.Update(sub); err != nil {
		return "", err
	}

	s.eventBus.Publish(event.EventSubscriptionsPostVisit, event.SubscriptionsPostVisitEvent{
		SubscriptionID: sub.ID,
		BlogURL:        sub.BlogURL,
		PostURL:        postURL,
	})

	return postURL, nil
}
