package handler

import (
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
	"github.com/nyoongoon/closest-v2/backend-go/internal/repository/sqlite"
)

type DiscoverHandler struct {
	repo *sqlite.DiscoverRepo
}

func NewDiscoverHandler(repo *sqlite.DiscoverRepo) *DiscoverHandler {
	return &DiscoverHandler{repo: repo}
}

// GET /discover/categories
func (h *DiscoverHandler) GetCategories(w http.ResponseWriter, r *http.Request) {
	cats, err := h.repo.GetAllCategories()
	if err != nil {
		writeError(w, http.StatusInternalServerError, "카테고리 조회 실패")
		return
	}
	writeJSON(w, http.StatusOK, cats)
}

// GET /discover/blogs?category=tech&tag=python&q=검색어&page=0&size=20
func (h *DiscoverHandler) GetBlogs(w http.ResponseWriter, r *http.Request) {
	page, _ := strconv.Atoi(r.URL.Query().Get("page"))
	size, _ := strconv.Atoi(r.URL.Query().Get("size"))
	if size <= 0 {
		size = 20
	}
	if size > 100 {
		size = 100
	}

	category := r.URL.Query().Get("category")
	tag := r.URL.Query().Get("tag")
	query := r.URL.Query().Get("q")

	var blogs []sqlite.DiscoverBlog
	var hasMore bool
	var err error

	switch {
	case query != "":
		blogs, hasMore, err = h.repo.SearchBlogs(query, page, size)
	case category != "":
		blogs, hasMore, err = h.repo.GetBlogsByCategory(category, page, size)
	case tag != "":
		blogs, hasMore, err = h.repo.GetBlogsByTag(tag, page, size)
	default:
		blogs, hasMore, err = h.repo.GetPopularBlogs(page, size)
	}

	if err != nil {
		writeError(w, http.StatusInternalServerError, "블로그 조회 실패")
		return
	}

	// Enrich with tags
	type enrichedBlog struct {
		sqlite.DiscoverBlog
		Tags       []string `json:"tags"`
		Categories []string `json:"categories"`
	}

	result := make([]enrichedBlog, len(blogs))
	for i, b := range blogs {
		tags, _ := h.repo.GetBlogTagNames(b.BlogID)
		cats, _ := h.repo.GetBlogCategories(b.BlogID)
		catNames := make([]string, len(cats))
		for j, c := range cats {
			catNames[j] = c.Name
		}
		if tags == nil {
			tags = []string{}
		}
		result[i] = enrichedBlog{
			DiscoverBlog: b,
			Tags:         tags,
			Categories:   catNames,
		}
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"blogs":   result,
		"hasMore": hasMore,
		"page":    page,
	})
}

// GET /discover/blogs/{id}/tags
func (h *DiscoverHandler) GetBlogTags(w http.ResponseWriter, r *http.Request) {
	id, _ := strconv.ParseInt(chi.URLParam(r, "id"), 10, 64)
	tags, err := h.repo.GetBlogTagNames(id)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "태그 조회 실패")
		return
	}
	writeJSON(w, http.StatusOK, tags)
}

// GET /discover/tags?limit=30
func (h *DiscoverHandler) GetTags(w http.ResponseWriter, r *http.Request) {
	limit, _ := strconv.Atoi(r.URL.Query().Get("limit"))
	if limit <= 0 {
		limit = 30
	}
	tags, err := h.repo.GetPopularTags(limit)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "태그 조회 실패")
		return
	}
	writeJSON(w, http.StatusOK, tags)
}
