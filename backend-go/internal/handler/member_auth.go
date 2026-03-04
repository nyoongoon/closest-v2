package handler

import (
	"encoding/json"
	"net/http"

	"github.com/nyoongoon/closest-v2/backend-go/internal/service"
)

type MemberAuthHandler struct {
	svc *service.MemberAuthService
}

func NewMemberAuthHandler(svc *service.MemberAuthService) *MemberAuthHandler {
	return &MemberAuthHandler{svc: svc}
}

type signupRequest struct {
	Email           string `json:"email"`
	Password        string `json:"password"`
	ConfirmPassword string `json:"confirmPassword"`
}

type signinRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

func (h *MemberAuthHandler) Signup(w http.ResponseWriter, r *http.Request) {
	var req signupRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "잘못된 요청입니다.")
		return
	}

	if err := h.svc.SignUp(req.Email, req.Password, req.ConfirmPassword); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	w.WriteHeader(http.StatusOK)
}

func (h *MemberAuthHandler) Signin(w http.ResponseWriter, r *http.Request) {
	var req signinRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "잘못된 요청입니다.")
		return
	}

	tokens, err := h.svc.SignIn(req.Email, req.Password)
	if err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	http.SetCookie(w, &http.Cookie{
		Name:     "accessToken",
		Value:    tokens.AccessToken,
		Path:     "/",
		MaxAge:   60 * 30,
		HttpOnly: false,
		SameSite: http.SameSiteStrictMode,
	})
	http.SetCookie(w, &http.Cookie{
		Name:     "refreshToken",
		Value:    tokens.RefreshToken,
		Path:     "/",
		MaxAge:   60 * 60 * 24 * 30,
		HttpOnly: true,
		SameSite: http.SameSiteStrictMode,
	})

	w.WriteHeader(http.StatusOK)
}
