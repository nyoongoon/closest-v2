package service

import (
	"log"
	"time"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/blog"
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/feed"
)

type BlogSchedulerService struct {
	blogRepo   blog.Repository
	feedClient feed.Client
}

func NewBlogSchedulerService(blogRepo blog.Repository, feedClient feed.Client) *BlogSchedulerService {
	return &BlogSchedulerService{blogRepo: blogRepo, feedClient: feedClient}
}

func (s *BlogSchedulerService) Start() {
	go func() {
		time.Sleep(10 * time.Second)
		s.poll()

		ticker := time.NewTicker(10 * time.Minute)
		defer ticker.Stop()
		for range ticker.C {
			s.poll()
		}
	}()
}

func (s *BlogSchedulerService) poll() {
	log.Println("RSS 폴링 스케줄러 시작")
	page := 0
	pageSize := 100

	for {
		blogs, hasMore, err := s.blogRepo.FindAll(page, pageSize)
		if err != nil {
			log.Printf("RSS 폴링 DB 에러: %v", err)
			break
		}

		for _, b := range blogs {
			s.updateBlog(b)
			time.Sleep(1 * time.Second)
		}

		if !hasMore {
			break
		}
		page++
	}
	log.Println("RSS 폴링 스케줄러 완료")
}

func (s *BlogSchedulerService) updateBlog(b *blog.Blog) {
	f, err := s.feedClient.GetFeed(b.RssURL)
	if err != nil {
		log.Printf("RSS 폴링 실패 - blogId: %d, url: %s, error: %v", b.ID, b.RssURL, err)
		return
	}

	recentPub := f.ExtractRecentPublishedDateTime()
	existingPub := b.GetPublishedTime()

	if recentPub.After(existingPub) {
		b.BlogTitle = f.BlogTitle
		if f.Author != "" {
			b.Author.String = f.Author
			b.Author.Valid = true
		}
		b.PublishedDateTime = recentPub.Format(time.RFC3339)
		if err := s.blogRepo.Update(b); err != nil {
			log.Printf("블로그 업데이트 실패 - blogId: %d: %v", b.ID, err)
		}
	}

	existingPosts, err := s.blogRepo.FindPostsByBlogID(b.ID)
	if err != nil {
		log.Printf("포스트 조회 실패 - blogId: %d: %v", b.ID, err)
		return
	}
	postMap := make(map[string]*blog.Post)
	for _, p := range existingPosts {
		postMap[p.PostURL] = p
	}

	for _, item := range f.Items {
		if existing, ok := postMap[item.PostURL]; ok {
			if existing.PostTitle != item.PostTitle || existing.PublishedDateTime != item.PublishedDateTime.Format(time.RFC3339) {
				existing.PostTitle = item.PostTitle
				existing.PublishedDateTime = item.PublishedDateTime.Format(time.RFC3339)
				if err := s.blogRepo.UpdatePost(existing); err != nil {
					log.Printf("포스트 업데이트 실패: %v", err)
				}
			}
		} else {
			newPost := &blog.Post{
				BlogID:            b.ID,
				PostURL:           item.PostURL,
				PostTitle:         item.PostTitle,
				PublishedDateTime: item.PublishedDateTime.Format(time.RFC3339),
				PostVisitCount:    0,
			}
			if _, err := s.blogRepo.SavePost(newPost); err != nil {
				log.Printf("포스트 저장 실패: %v", err)
			}
		}
	}
}
