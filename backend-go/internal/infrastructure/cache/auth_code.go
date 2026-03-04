package cache

import (
	"time"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/blog"
	gocache "github.com/patrickmn/go-cache"
)

type AuthCodeCache struct {
	c *gocache.Cache
}

func NewAuthCodeCache() *AuthCodeCache {
	return &AuthCodeCache{
		c: gocache.New(10*time.Minute, 15*time.Minute),
	}
}

func (ac *AuthCodeCache) Save(code *blog.AuthCode) {
	ac.c.Set(code.MemberEmail, code, gocache.DefaultExpiration)
}

func (ac *AuthCodeCache) FindByMemberEmail(email string) *blog.AuthCode {
	val, found := ac.c.Get(email)
	if !found {
		return nil
	}
	code, ok := val.(*blog.AuthCode)
	if !ok {
		return nil
	}
	return code
}
