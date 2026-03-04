package blog

import (
	"crypto/rand"
	"math/big"
)

const characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

type AuthCode struct {
	MemberEmail string
	RssURL      string
	AuthMessage string
}

func NewAuthCode(memberEmail, rssURL string) *AuthCode {
	return &AuthCode{
		MemberEmail: memberEmail,
		RssURL:      rssURL,
		AuthMessage: generateRandomMessage(),
	}
}

func (a *AuthCode) Authenticate(blogTitle string) bool {
	return a.AuthMessage == blogTitle
}

func generateRandomMessage() string {
	result := make([]byte, 6)
	for i := range result {
		n, _ := rand.Int(rand.Reader, big.NewInt(int64(len(characters))))
		result[i] = characters[n.Int64()]
	}
	return string(result)
}
