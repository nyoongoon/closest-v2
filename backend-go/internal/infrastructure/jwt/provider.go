package jwt

import (
	"errors"
	"time"

	gojwt "github.com/golang-jwt/jwt/v5"
)

const (
	AccessTokenExpiry  = 30 * time.Minute
	RefreshTokenExpiry = 24 * time.Hour
)

type TokenType int

const (
	AccessToken TokenType = iota
	RefreshToken
)

type Provider struct {
	accessSecret  []byte
	refreshSecret []byte
}

func NewProvider(accessSecret, refreshSecret string) *Provider {
	return &Provider{
		accessSecret:  []byte(accessSecret),
		refreshSecret: []byte(refreshSecret),
	}
}

func (p *Provider) IssueToken(tokenType TokenType, email string) (string, error) {
	var secret []byte
	var expiry time.Duration

	switch tokenType {
	case AccessToken:
		secret = p.accessSecret
		expiry = AccessTokenExpiry
	case RefreshToken:
		secret = p.refreshSecret
		expiry = RefreshTokenExpiry
	default:
		return "", errors.New("invalid token type")
	}

	claims := gojwt.MapClaims{
		"sub":   email,
		"roles": []string{"ROLE_USER"},
		"iat":   time.Now().Unix(),
		"exp":   time.Now().Add(expiry).Unix(),
	}

	token := gojwt.NewWithClaims(gojwt.SigningMethodHS256, claims)
	return token.SignedString(secret)
}

func (p *Provider) IssueAccessFromRefresh(refreshTokenStr string) (string, error) {
	claims, err := p.parseClaims(refreshTokenStr, p.refreshSecret)
	if err != nil {
		return "", err
	}
	email, _ := claims.GetSubject()
	return p.IssueToken(AccessToken, email)
}

func (p *Provider) ValidateToken(tokenStr string, tokenType TokenType) bool {
	if tokenStr == "" {
		return false
	}
	var secret []byte
	if tokenType == AccessToken {
		secret = p.accessSecret
	} else {
		secret = p.refreshSecret
	}
	_, err := p.parseClaims(tokenStr, secret)
	return err == nil
}

func (p *Provider) GetSubject(tokenStr string, tokenType TokenType) (string, error) {
	var secret []byte
	if tokenType == AccessToken {
		secret = p.accessSecret
	} else {
		secret = p.refreshSecret
	}
	claims, err := p.parseClaims(tokenStr, secret)
	if err != nil {
		return "", err
	}
	return claims.GetSubject()
}

func (p *Provider) parseClaims(tokenStr string, secret []byte) (gojwt.MapClaims, error) {
	token, err := gojwt.Parse(tokenStr, func(t *gojwt.Token) (interface{}, error) {
		if _, ok := t.Method.(*gojwt.SigningMethodHMAC); !ok {
			return nil, errors.New("unexpected signing method")
		}
		return secret, nil
	})
	if err != nil {
		return nil, err
	}
	claims, ok := token.Claims.(gojwt.MapClaims)
	if !ok || !token.Valid {
		return nil, errors.New("invalid token")
	}
	return claims, nil
}
