package handler

import (
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
	"github.com/nyoongoon/closest-v2/backend-go/internal/service"
)

type SubscriptionVisitHandler struct {
	svc *service.SubscriptionVisitService
}

func NewSubscriptionVisitHandler(svc *service.SubscriptionVisitService) *SubscriptionVisitHandler {
	return &SubscriptionVisitHandler{svc: svc}
}

func (h *SubscriptionVisitHandler) VisitBlog(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "잘못된 구독 ID입니다.")
		return
	}

	redirectURL, err := h.svc.VisitSubscription(id)
	if err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	http.Redirect(w, r, redirectURL, http.StatusMovedPermanently)
}

func (h *SubscriptionVisitHandler) VisitPost(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "잘못된 구독 ID입니다.")
		return
	}

	postURL := chi.URLParam(r, "*")
	if postURL == "" {
		writeError(w, http.StatusBadRequest, "포스트 URL은 필수값입니다.")
		return
	}

	redirectURL, err := h.svc.VisitSubscriptionPost(id, postURL)
	if err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	http.Redirect(w, r, redirectURL, http.StatusMovedPermanently)
}
