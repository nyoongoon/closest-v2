package handler

import (
	"encoding/json"
	"net/http"

	"github.com/nyoongoon/closest-v2/backend-go/internal/middleware"
	"github.com/nyoongoon/closest-v2/backend-go/internal/service"
)

type PostLikeHandler struct {
	svc *service.PostLikeService
}

func NewPostLikeHandler(svc *service.PostLikeService) *PostLikeHandler {
	return &PostLikeHandler{svc: svc}
}

type likePostRequest struct {
	PostUri string `json:"postUri"`
}

func (h *PostLikeHandler) LikePost(w http.ResponseWriter, r *http.Request) {
	email := middleware.GetUserEmail(r)
	var req likePostRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "잘못된 요청입니다.")
		return
	}

	if err := h.svc.LikePost(email, req.PostUri); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	w.WriteHeader(http.StatusOK)
}
