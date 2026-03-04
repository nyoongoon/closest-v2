package main

import (
	"log"
	"net/http"

	"github.com/go-chi/chi/v5"
	chimw "github.com/go-chi/chi/v5/middleware"

	"github.com/nyoongoon/closest-v2/backend-go/internal/config"
	"github.com/nyoongoon/closest-v2/backend-go/internal/event"
	"github.com/nyoongoon/closest-v2/backend-go/internal/handler"
	"github.com/nyoongoon/closest-v2/backend-go/internal/infrastructure/cache"
	"github.com/nyoongoon/closest-v2/backend-go/internal/infrastructure/jwt"
	"github.com/nyoongoon/closest-v2/backend-go/internal/infrastructure/rss"
	"github.com/nyoongoon/closest-v2/backend-go/internal/middleware"
	"github.com/nyoongoon/closest-v2/backend-go/internal/repository/sqlite"
	"github.com/nyoongoon/closest-v2/backend-go/internal/service"
)

func main() {
	cfg := config.Load()

	// DB
	db, err := sqlite.NewDB(cfg.DBPath)
	if err != nil {
		log.Fatalf("DB 초기화 실패: %v", err)
	}
	defer db.Close()

	// Repositories
	memberRepo := sqlite.NewMemberRepo(db)
	blogRepo := sqlite.NewBlogRepo(db)
	subRepo := sqlite.NewSubscriptionRepo(db)
	likesRepo := sqlite.NewLikesRepo(db)

	// Infrastructure
	jwtProvider := jwt.NewProvider(cfg.AccessSecretKey, cfg.RefreshSecretKey)
	feedClient := rss.NewFeedClient()
	authCodeCache := cache.NewAuthCodeCache()

	// Event Bus
	eventBus := event.NewBus()
	event.RegisterListeners(eventBus, memberRepo, blogRepo)

	// Services
	memberAuthSvc := service.NewMemberAuthService(memberRepo, jwtProvider)
	blogAuthSvc := service.NewBlogAuthService(feedClient, authCodeCache, eventBus)
	subRegisterSvc := service.NewSubscriptionRegisterService(feedClient, blogRepo, subRepo)
	subQuerySvc := service.NewSubscriptionQueryService(subRepo)
	subVisitSvc := service.NewSubscriptionVisitService(subRepo, eventBus)
	myBlogEditSvc := service.NewMyBlogEditService(memberRepo, eventBus)
	postLikeSvc := service.NewPostLikeService(likesRepo, memberRepo)
	blogSchedulerSvc := service.NewBlogSchedulerService(blogRepo, feedClient)

	// Handlers
	memberAuthH := handler.NewMemberAuthHandler(memberAuthSvc)
	blogAuthH := handler.NewBlogAuthHandler(blogAuthSvc)
	subH := handler.NewSubscriptionHandler(subRegisterSvc, subQuerySvc)
	subVisitH := handler.NewSubscriptionVisitHandler(subVisitSvc)
	postH := handler.NewPostHandler(blogRepo)
	postLikeH := handler.NewPostLikeHandler(postLikeSvc)
	myBlogH := handler.NewMyBlogHandler(myBlogEditSvc)

	// Start RSS scheduler
	blogSchedulerSvc.Start()

	// Router
	r := chi.NewRouter()
	r.Use(chimw.Recoverer)
	r.Use(chimw.RealIP)
	r.Use(middleware.LoggingMiddleware)

	// Public routes (no auth required)
	r.Post("/member/auth/signup", memberAuthH.Signup)
	r.Post("/member/auth/signin", memberAuthH.Signin)
	r.Get("/posts/recent", postH.GetRecentPosts)

	// Close blogs - optional auth
	r.Group(func(r chi.Router) {
		r.Use(middleware.OptionalAuthMiddleware(jwtProvider))
		r.Get("/subscriptions/blogs/close", subH.GetCloseBlogs)
	})

	// Visit routes (no auth required)
	r.Get("/subscriptions/{id}/visit", subVisitH.VisitBlog)
	r.Get("/subscriptions/{id}/visit/*", subVisitH.VisitPost)

	// Authenticated routes
	r.Group(func(r chi.Router) {
		r.Use(middleware.AuthMiddleware(jwtProvider))

		r.Get("/blog/auth/message", blogAuthH.GetAuthMessage)
		r.Post("/blog/auth/message", blogAuthH.PostAuthMessage)
		r.Post("/blog/auth/verification", blogAuthH.VerifyAuth)

		r.Post("/subscriptions", subH.Register)
		r.Delete("/subscriptions/{id}", subH.Unregister)
		r.Get("/subscriptions/blogs", subH.GetMyBlogs)

		r.Post("/posts/like", postLikeH.LikePost)

		r.Patch("/my-blog/status", myBlogH.PatchStatus)
	})

	log.Printf("서버 시작 - :%s", cfg.ServerPort)
	if err := http.ListenAndServe(":"+cfg.ServerPort, r); err != nil {
		log.Fatalf("서버 에러: %v", err)
	}
}
