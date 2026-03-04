package handler

import (
	"encoding/json"
	"net/http"

	"github.com/nyoongoon/closest-v2/backend-go/internal/middleware"
	"github.com/nyoongoon/closest-v2/backend-go/internal/service"
)

type MyBlogHandler struct {
	editSvc *service.MyBlogEditService
}

func NewMyBlogHandler(editSvc *service.MyBlogEditService) *MyBlogHandler {
	return &MyBlogHandler{editSvc: editSvc}
}

type statusPatchRequest struct {
	Message string `json:"message"`
}

func (h *MyBlogHandler) PatchStatus(w http.ResponseWriter, r *http.Request) {
	email := middleware.GetUserEmail(r)
	var req statusPatchRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "잘못된 요청입니다.")
		return
	}

	if err := h.editSvc.EditStatusMessage(email, req.Message); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	w.WriteHeader(http.StatusOK)
}
