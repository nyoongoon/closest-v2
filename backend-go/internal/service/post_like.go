package service

import (
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/likes"
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/member"
)

type PostLikeService struct {
	likesRepo  likes.Repository
	memberRepo member.Repository
}

func NewPostLikeService(likesRepo likes.Repository, memberRepo member.Repository) *PostLikeService {
	return &PostLikeService{likesRepo: likesRepo, memberRepo: memberRepo}
}

func (s *PostLikeService) LikePost(memberEmail, postURL string) error {
	m, err := s.memberRepo.FindByEmail(memberEmail)
	if err != nil {
		return err
	}
	if m == nil {
		return err
	}

	l := &likes.Likes{
		MemberID: m.ID,
		PostURL:  postURL,
	}
	_, err = s.likesRepo.Save(l)
	return err
}
