package handler

import (
	"encoding/json"
	"net/http"

	"github.com/nyoongoon/closest-v2/backend-go/internal/middleware"
	"github.com/nyoongoon/closest-v2/backend-go/internal/service"
)

type BlogAuthHandler struct {
	svc *service.BlogAuthService
}

func NewBlogAuthHandler(svc *service.BlogAuthService) *BlogAuthHandler {
	return &BlogAuthHandler{svc: svc}
}

type authMessageRequest struct {
	RssUri string `json:"rssUri"`
}

type authMessageResponse struct {
	AuthMessage string `json:"authMessage"`
}

func (h *BlogAuthHandler) GetAuthMessage(w http.ResponseWriter, r *http.Request) {
	email := middleware.GetUserEmail(r)
	rssURI := r.URL.Query().Get("rssUri")
	if rssURI == "" {
		writeError(w, http.StatusBadRequest, "RSS URL은 필수값입니다.")
		return
	}

	msg, err := h.svc.CreateAuthMessage(email, rssURI)
	if err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	writeJSON(w, http.StatusOK, authMessageResponse{AuthMessage: msg})
}

func (h *BlogAuthHandler) VerifyAuth(w http.ResponseWriter, r *http.Request) {
	email := middleware.GetUserEmail(r)

	if err := h.svc.VerifyAuthMessage(email); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	w.WriteHeader(http.StatusOK)
}

// blogAuthMessageGetRequest for JSON body based request
type blogAuthMessageGetRequest struct {
	RssUri string `json:"rssUri"`
}

func (h *BlogAuthHandler) PostAuthMessage(w http.ResponseWriter, r *http.Request) {
	email := middleware.GetUserEmail(r)
	var req blogAuthMessageGetRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "잘못된 요청입니다.")
		return
	}

	msg, err := h.svc.CreateAuthMessage(email, req.RssUri)
	if err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	writeJSON(w, http.StatusOK, authMessageResponse{AuthMessage: msg})
}
