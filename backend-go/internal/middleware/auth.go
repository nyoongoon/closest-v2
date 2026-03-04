package middleware

import (
	"context"
	"net/http"

	"github.com/nyoongoon/closest-v2/backend-go/internal/infrastructure/jwt"
)

type contextKey string

const UserEmailKey contextKey = "userEmail"

func AuthMiddleware(provider *jwt.Provider) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			accessTokenStr := getCookieValue(r, "accessToken")
			refreshTokenStr := getCookieValue(r, "refreshToken")

			if accessTokenStr == "" && refreshTokenStr == "" {
				http.Error(w, `{"error":"인증이 필요합니다"}`, http.StatusUnauthorized)
				return
			}

			if provider.ValidateToken(accessTokenStr, jwt.AccessToken) {
				email, err := provider.GetSubject(accessTokenStr, jwt.AccessToken)
				if err == nil {
					ctx := context.WithValue(r.Context(), UserEmailKey, email)
					next.ServeHTTP(w, r.WithContext(ctx))
					return
				}
			}

			if provider.ValidateToken(refreshTokenStr, jwt.RefreshToken) {
				newAccessToken, err := provider.IssueAccessFromRefresh(refreshTokenStr)
				if err == nil {
					email, err := provider.GetSubject(newAccessToken, jwt.AccessToken)
					if err == nil {
						http.SetCookie(w, &http.Cookie{
							Name:     "accessToken",
							Value:    newAccessToken,
							Path:     "/",
							MaxAge:   60 * 30,
							HttpOnly: true,
							SameSite: http.SameSiteStrictMode,
						})
						ctx := context.WithValue(r.Context(), UserEmailKey, email)
						next.ServeHTTP(w, r.WithContext(ctx))
						return
					}
				}
			}

			http.Error(w, `{"error":"인증이 필요합니다"}`, http.StatusUnauthorized)
		})
	}
}

func OptionalAuthMiddleware(provider *jwt.Provider) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			accessTokenStr := getCookieValue(r, "accessToken")
			refreshTokenStr := getCookieValue(r, "refreshToken")

			if provider.ValidateToken(accessTokenStr, jwt.AccessToken) {
				email, err := provider.GetSubject(accessTokenStr, jwt.AccessToken)
				if err == nil {
					ctx := context.WithValue(r.Context(), UserEmailKey, email)
					next.ServeHTTP(w, r.WithContext(ctx))
					return
				}
			}

			if provider.ValidateToken(refreshTokenStr, jwt.RefreshToken) {
				newAccessToken, err := provider.IssueAccessFromRefresh(refreshTokenStr)
				if err == nil {
					email, err := provider.GetSubject(newAccessToken, jwt.AccessToken)
					if err == nil {
						http.SetCookie(w, &http.Cookie{
							Name:     "accessToken",
							Value:    newAccessToken,
							Path:     "/",
							MaxAge:   60 * 30,
							HttpOnly: true,
							SameSite: http.SameSiteStrictMode,
						})
						ctx := context.WithValue(r.Context(), UserEmailKey, email)
						next.ServeHTTP(w, r.WithContext(ctx))
						return
					}
				}
			}

			next.ServeHTTP(w, r)
		})
	}
}

func GetUserEmail(r *http.Request) string {
	val := r.Context().Value(UserEmailKey)
	if val == nil {
		return ""
	}
	return val.(string)
}

func getCookieValue(r *http.Request, name string) string {
	c, err := r.Cookie(name)
	if err != nil {
		return ""
	}
	return c.Value
}
