package handler

import (
	"math/rand"
	"net/http"
	"strconv"
	"time"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/blog"
	"github.com/nyoongoon/closest-v2/backend-go/internal/repository/sqlite"
)

type FeedMixHandler struct {
	blogRepo     blog.Repository
	discoverRepo *sqlite.DiscoverRepo
}

func NewFeedMixHandler(blogRepo blog.Repository, discoverRepo *sqlite.DiscoverRepo) *FeedMixHandler {
	return &FeedMixHandler{blogRepo: blogRepo, discoverRepo: discoverRepo}
}

type mixPostResponse struct {
	PostTitle         string `json:"postTitle"`
	PostURL           string `json:"postUrl"`
	PublishedDateTime string `json:"publishedDateTime"`
	BlogTitle         string `json:"blogTitle"`
	BlogURL           string `json:"blogUrl"`
	Author            string `json:"author,omitempty"`
	ThumbnailURL      string `json:"thumbnailUrl,omitempty"`
	Reason            string `json:"reason"` // 왜 추천됐는지: "popular", "recent", "category", "random"
	Category          string `json:"category,omitempty"`
}

// GET /posts/feed?limit=100
// 인기순, 카테고리별, 최신순, 랜덤을 섞어서 반환
func (h *FeedMixHandler) GetMixedFeed(w http.ResponseWriter, r *http.Request) {
	limitStr := r.URL.Query().Get("limit")
	limit, _ := strconv.Atoi(limitStr)
	if limit <= 0 {
		limit = 100
	}

	rng := rand.New(rand.NewSource(time.Now().UnixNano()))

	// 1. 인기 블로그 게시글 (score 높은 블로그)
	popularPosts := h.getPopularPosts(30)

	// 2. 최신 게시글
	recentPosts := h.getRecentPosts(30)

	// 3. 카테고리별 게시글 (랜덤 카테고리에서 수집)
	categoryPosts := h.getCategoryPosts(rng, 30)

	// 4. 랜덤 게시글 (덜 알려진 블로그)
	randomPosts := h.getRandomPosts(rng, 20)

	// 모든 풀을 합치고 중복 제거
	seen := make(map[string]bool)
	var all []mixPostResponse

	pools := [][]mixPostResponse{popularPosts, recentPosts, categoryPosts, randomPosts}

	// 라운드 로빈으로 섞기: 인기 → 최신 → 카테고리 → 랜덤 반복
	maxLen := 0
	for _, p := range pools {
		if len(p) > maxLen {
			maxLen = len(p)
		}
	}

	for i := 0; i < maxLen && len(all) < limit; i++ {
		for _, pool := range pools {
			if i < len(pool) {
				p := pool[i]
				if !seen[p.PostURL] {
					seen[p.PostURL] = true
					all = append(all, p)
				}
			}
		}
	}

	// 최종 셔플 (완전 고정 순서 방지, 하지만 대략적인 믹스 유지)
	// 5개씩 그룹으로 나눠서 그룹 내 셔플 (전체 셔플하면 너무 랜덤)
	for i := 0; i < len(all); i += 5 {
		end := i + 5
		if end > len(all) {
			end = len(all)
		}
		group := all[i:end]
		rng.Shuffle(len(group), func(a, b int) {
			group[a], group[b] = group[b], group[a]
		})
	}

	if len(all) > limit {
		all = all[:limit]
	}

	writeJSON(w, http.StatusOK, all)
}

func (h *FeedMixHandler) getPopularPosts(limit int) []mixPostResponse {
	// 인기 블로그에서 최신 게시글 수집
	blogs, _, _ := h.discoverRepo.GetPopularBlogs(0, 50)
	var results []mixPostResponse

	for _, db := range blogs {
		if len(results) >= limit {
			break
		}
		b, err := h.blogRepo.FindByID(db.BlogID)
		if err != nil || b == nil {
			continue
		}
		posts, _ := h.blogRepo.FindPostsByBlogID(b.ID)
		if len(posts) == 0 {
			continue
		}
		// 가장 최신 게시글 1개
		latest := posts[0]
		for _, p := range posts[1:] {
			if p.PublishedDateTime > latest.PublishedDateTime {
				latest = p
			}
		}
		results = append(results, h.toMixPost(b, latest, "popular", ""))
	}
	return results
}

func (h *FeedMixHandler) getRecentPosts(limit int) []mixPostResponse {
	blogs, _, _ := h.blogRepo.FindAll(0, 10000)
	type postWithBlog struct {
		post *blog.Post
		blog *blog.Blog
	}

	var all []postWithBlog
	for _, b := range blogs {
		posts, _ := h.blogRepo.FindPostsByBlogID(b.ID)
		for _, p := range posts {
			all = append(all, postWithBlog{post: p, blog: b})
		}
	}

	// 시간순 정렬
	for i := 0; i < len(all)-1; i++ {
		for j := i + 1; j < len(all); j++ {
			if all[j].post.PublishedDateTime > all[i].post.PublishedDateTime {
				all[i], all[j] = all[j], all[i]
			}
		}
	}

	if len(all) > limit {
		all = all[:limit]
	}

	var results []mixPostResponse
	for _, item := range all {
		results = append(results, h.toMixPost(item.blog, item.post, "recent", ""))
	}
	return results
}

func (h *FeedMixHandler) getCategoryPosts(rng *rand.Rand, limit int) []mixPostResponse {
	cats, _ := h.discoverRepo.GetAllCategories()
	if len(cats) == 0 {
		return nil
	}

	// 랜덤 카테고리 3개 선택
	rng.Shuffle(len(cats), func(i, j int) { cats[i], cats[j] = cats[j], cats[i] })
	selected := cats
	if len(selected) > 3 {
		selected = selected[:3]
	}

	var results []mixPostResponse
	perCat := limit / len(selected)

	for _, cat := range selected {
		blogs, _, _ := h.discoverRepo.GetBlogsByCategory(cat.Slug, 0, 20)
		// 셔플
		rng.Shuffle(len(blogs), func(i, j int) { blogs[i], blogs[j] = blogs[j], blogs[i] })

		count := 0
		for _, db := range blogs {
			if count >= perCat {
				break
			}
			b, err := h.blogRepo.FindByID(db.BlogID)
			if err != nil || b == nil {
				continue
			}
			posts, _ := h.blogRepo.FindPostsByBlogID(b.ID)
			if len(posts) == 0 {
				continue
			}
			// 랜덤 게시글
			p := posts[rng.Intn(len(posts))]
			results = append(results, h.toMixPost(b, p, "category", cat.Name))
			count++
		}
	}
	return results
}

func (h *FeedMixHandler) getRandomPosts(rng *rand.Rand, limit int) []mixPostResponse {
	blogs, _, _ := h.blogRepo.FindAll(0, 10000)
	if len(blogs) == 0 {
		return nil
	}

	rng.Shuffle(len(blogs), func(i, j int) { blogs[i], blogs[j] = blogs[j], blogs[i] })

	var results []mixPostResponse
	for _, b := range blogs {
		if len(results) >= limit {
			break
		}
		posts, _ := h.blogRepo.FindPostsByBlogID(b.ID)
		if len(posts) == 0 {
			continue
		}
		p := posts[rng.Intn(len(posts))]
		results = append(results, h.toMixPost(b, p, "random", ""))
	}
	return results
}

func (h *FeedMixHandler) toMixPost(b *blog.Blog, p *blog.Post, reason, category string) mixPostResponse {
	item := mixPostResponse{
		PostTitle:         p.PostTitle,
		PostURL:           p.PostURL,
		PublishedDateTime: p.PublishedDateTime,
		BlogTitle:         b.BlogTitle,
		BlogURL:           b.BlogURL,
		Reason:            reason,
		Category:          category,
	}
	if b.Author.Valid {
		item.Author = b.Author.String
	}
	if p.ThumbnailURL.Valid && p.ThumbnailURL.String != "" {
		item.ThumbnailURL = p.ThumbnailURL.String
	} else if b.ThumbnailURL.Valid {
		item.ThumbnailURL = b.ThumbnailURL.String
	}
	return item
}
