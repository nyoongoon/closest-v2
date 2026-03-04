package handler

import (
	"net/http"
	"sort"
	"strconv"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/blog"
)

type PostHandler struct {
	blogRepo blog.Repository
}

func NewPostHandler(blogRepo blog.Repository) *PostHandler {
	return &PostHandler{blogRepo: blogRepo}
}

type recentPostResponse struct {
	PostTitle         string `json:"postTitle"`
	PostURL           string `json:"postUrl"`
	PublishedDateTime string `json:"publishedDateTime"`
	BlogTitle         string `json:"blogTitle"`
	BlogURL           string `json:"blogUrl"`
	Author            string `json:"author,omitempty"`
	ThumbnailURL      string `json:"thumbnailUrl,omitempty"`
}

func (h *PostHandler) GetRecentPosts(w http.ResponseWriter, r *http.Request) {
	limitStr := r.URL.Query().Get("limit")
	limit, _ := strconv.Atoi(limitStr)
	if limit <= 0 {
		limit = 30
	}

	blogs, _, err := h.blogRepo.FindAll(0, 10000)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "서버 에러가 발생했습니다.")
		return
	}

	results := make([]recentPostResponse, 0)
	for _, b := range blogs {
		posts, err := h.blogRepo.FindPostsByBlogID(b.ID)
		if err != nil {
			continue
		}
		for _, p := range posts {
			item := recentPostResponse{
				PostTitle:         p.PostTitle,
				PostURL:           p.PostURL,
				PublishedDateTime: p.PublishedDateTime,
				BlogTitle:         b.BlogTitle,
				BlogURL:           b.BlogURL,
			}
			if b.Author.Valid {
				item.Author = b.Author.String
			}
			if b.ThumbnailURL.Valid {
				item.ThumbnailURL = b.ThumbnailURL.String
			}
			results = append(results, item)
		}
	}

	sort.Slice(results, func(i, j int) bool {
		return results[i].PublishedDateTime > results[j].PublishedDateTime
	})

	if len(results) > limit {
		results = results[:limit]
	}

	writeJSON(w, http.StatusOK, results)
}
