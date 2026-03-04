package handler

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
	"github.com/nyoongoon/closest-v2/backend-go/internal/middleware"
	"github.com/nyoongoon/closest-v2/backend-go/internal/service"
)

type SubscriptionHandler struct {
	registerSvc *service.SubscriptionRegisterService
	querySvc    *service.SubscriptionQueryService
}

func NewSubscriptionHandler(registerSvc *service.SubscriptionRegisterService, querySvc *service.SubscriptionQueryService) *SubscriptionHandler {
	return &SubscriptionHandler{registerSvc: registerSvc, querySvc: querySvc}
}

type subscriptionPostRequest struct {
	RssUri string `json:"rssUri"`
}

func (h *SubscriptionHandler) Register(w http.ResponseWriter, r *http.Request) {
	email := middleware.GetUserEmail(r)
	var req subscriptionPostRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "잘못된 요청입니다.")
		return
	}

	if err := h.registerSvc.Register(email, req.RssUri); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	w.WriteHeader(http.StatusOK)
}

func (h *SubscriptionHandler) Unregister(w http.ResponseWriter, r *http.Request) {
	email := middleware.GetUserEmail(r)
	idStr := chi.URLParam(r, "id")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "잘못된 구독 ID입니다.")
		return
	}

	if err := h.registerSvc.Unregister(email, id); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	w.WriteHeader(http.StatusOK)
}

func (h *SubscriptionHandler) GetCloseBlogs(w http.ResponseWriter, r *http.Request) {
	email := middleware.GetUserEmail(r)

	var subs []service.SubscriptionResponse
	var err error

	if email != "" {
		subs, err = h.querySvc.GetCloseSubscriptions(email)
	} else {
		subs, err = h.querySvc.GetCloseSubscriptionsOfAll()
	}

	if err != nil {
		writeError(w, http.StatusInternalServerError, "서버 에러가 발생했습니다.")
		return
	}

	writeJSON(w, http.StatusOK, subs)
}

func (h *SubscriptionHandler) GetMyBlogs(w http.ResponseWriter, r *http.Request) {
	email := middleware.GetUserEmail(r)
	pageStr := r.URL.Query().Get("page")
	sizeStr := r.URL.Query().Get("size")

	page, _ := strconv.Atoi(pageStr)
	size, _ := strconv.Atoi(sizeStr)
	if size <= 0 {
		size = 10
	}

	subs, err := h.querySvc.GetRecentPublishedSubscriptions(email, page, size)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "서버 에러가 발생했습니다.")
		return
	}

	writeJSON(w, http.StatusOK, subs)
}
