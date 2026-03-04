package service

import (
	"errors"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/member"
	"github.com/nyoongoon/closest-v2/backend-go/internal/infrastructure/jwt"
)

type MemberAuthService struct {
	memberRepo member.Repository
	jwtProvider *jwt.Provider
}

func NewMemberAuthService(memberRepo member.Repository, jwtProvider *jwt.Provider) *MemberAuthService {
	return &MemberAuthService{memberRepo: memberRepo, jwtProvider: jwtProvider}
}

func (s *MemberAuthService) SignUp(email, password, confirmPassword string) error {
	if password != confirmPassword {
		return errors.New("비밀번호와 확인 비밀번호가 다릅니다.")
	}

	exists, err := s.memberRepo.ExistsByEmail(email)
	if err != nil {
		return err
	}
	if exists {
		return errors.New("이미 사용 중인 이메일입니다.")
	}

	m := &member.Member{
		UserEmail: email,
		Password:  password,
	}
	_, err = s.memberRepo.Save(m)
	return err
}

type TokenPair struct {
	AccessToken  string
	RefreshToken string
}

func (s *MemberAuthService) SignIn(email, password string) (*TokenPair, error) {
	m, err := s.memberRepo.FindByEmail(email)
	if err != nil {
		return nil, err
	}
	if m == nil {
		return nil, errors.New("유효하지 않은 회원 정보입니다.")
	}
	if password != m.Password {
		return nil, errors.New("유효하지 않은 회원 정보입니다.")
	}

	accessToken, err := s.jwtProvider.IssueToken(jwt.AccessToken, email)
	if err != nil {
		return nil, err
	}
	refreshToken, err := s.jwtProvider.IssueToken(jwt.RefreshToken, email)
	if err != nil {
		return nil, err
	}

	return &TokenPair{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
	}, nil
}
