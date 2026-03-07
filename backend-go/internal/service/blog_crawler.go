package service

import (
	"fmt"
	"io"
	"log"
	"net/http"
	"regexp"
	"strings"
	"time"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/blog"
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/feed"
	"github.com/nyoongoon/closest-v2/backend-go/internal/repository/sqlite"
)

type BlogCrawlerService struct {
	blogRepo     blog.Repository
	discoverRepo *sqlite.DiscoverRepo
	feedClient   feed.Client
	httpClient   *http.Client
}

func NewBlogCrawlerService(
	blogRepo blog.Repository,
	discoverRepo *sqlite.DiscoverRepo,
	feedClient feed.Client,
) *BlogCrawlerService {
	return &BlogCrawlerService{
		blogRepo:     blogRepo,
		discoverRepo: discoverRepo,
		feedClient:   feedClient,
		httpClient: &http.Client{
			Timeout: 15 * time.Second,
		},
	}
}

// Start begins periodic crawling (every 2 hours, first run after 30 seconds)
func (s *BlogCrawlerService) Start() {
	go func() {
		// Seed categories on startup
		s.seedCategories()

		time.Sleep(30 * time.Second)
		s.crawlAll()

		ticker := time.NewTicker(2 * time.Hour)
		defer ticker.Stop()
		for range ticker.C {
			s.crawlAll()
		}
	}()
}

func (s *BlogCrawlerService) seedCategories() {
	categories := []struct {
		name, slug, icon string
		order            int
	}{
		{"개발/기술", "tech", "💻", 1},
		{"디자인", "design", "🎨", 2},
		{"마케팅", "marketing", "📢", 3},
		{"창업/비즈니스", "business", "💼", 4},
		{"일상/라이프", "life", "🌿", 5},
		{"여행", "travel", "✈️", 6},
		{"맛집/요리", "food", "🍳", 7},
		{"재테크/투자", "finance", "💰", 8},
		{"자기계발", "growth", "📚", 9},
		{"IT뉴스", "news", "📰", 10},
		{"데이터/AI", "ai", "🤖", 11},
		{"게임", "game", "🎮", 12},
	}

	for _, c := range categories {
		if _, err := s.discoverRepo.UpsertCategory(c.name, c.slug, c.icon, c.order); err != nil {
			log.Printf("카테고리 시딩 실패 (%s): %v", c.name, err)
		}
	}
	log.Println("카테고리 시딩 완료")
}

func (s *BlogCrawlerService) crawlAll() {
	log.Println("========== 블로그 크롤링 시작 ==========")
	start := time.Now()

	// 1. Reclassify blogs (improved keywords + post content)
	s.reclassifyBlogs()

	// 2. Curated popular blogs (all categories)
	s.crawlCuratedBlogs()

	// 3. Dynamic: Tistory trending pages
	s.crawlTistoryTrending()

	// 4. Dynamic: Velog trending
	s.crawlVelogTrending()

	// 5. Dynamic: GitHub.io tech blogs
	s.crawlGithubIOBlogs()

	elapsed := time.Since(start)
	log.Printf("========== 블로그 크롤링 완료 (소요시간: %v) ==========", elapsed)
}

// ──────────────────────────────────────────────────────────────────
// 1. Reclassify all blogs with improved algorithm
// ──────────────────────────────────────────────────────────────────

func (s *BlogCrawlerService) reclassifyBlogs() {
	log.Println("[분류] 블로그 재분류 시작")
	page := 0
	pageSize := 100
	classified := 0

	for {
		blogs, hasMore, err := s.blogRepo.FindAll(page, pageSize)
		if err != nil {
			log.Printf("[분류] DB 에러: %v", err)
			break
		}

		for _, b := range blogs {
			// Get posts for content-based classification
			posts, _ := s.blogRepo.FindPostsByBlogID(b.ID)

			// Classify based on URL, title, AND post titles
			category, tags := classifyBlogAdvanced(b.BlogURL, b.BlogTitle, b.Author.String, posts)

			if category != "" {
				// Check current categories
				currentCats, _ := s.discoverRepo.GetBlogCategories(b.ID)
				isDefault := len(currentCats) == 0
				if len(currentCats) == 1 {
					for _, cc := range currentCats {
						if cc.Slug == "life" {
							isDefault = true
						}
					}
				}
				// Only reclassify if currently uncategorized or default "life"
				if isDefault && category != "life" {
					catID, err := s.discoverRepo.GetCategoryIDBySlug(category)
					if err == nil {
						// Remove old default category
						s.discoverRepo.RemoveBlogCategories(b.ID)
						_ = s.discoverRepo.SetBlogCategory(b.ID, catID)
						classified++
					}
				} else if len(currentCats) == 0 {
					catID, err := s.discoverRepo.GetCategoryIDBySlug(category)
					if err == nil {
						_ = s.discoverRepo.SetBlogCategory(b.ID, catID)
						classified++
					}
				}
			}

			// Add/update tags
			for _, tag := range tags {
				tagID, err := s.discoverRepo.UpsertTag(tag)
				if err == nil {
					_ = s.discoverRepo.SetBlogTag(b.ID, tagID)
				}
			}

			// Update popularity score
			score := calculatePopularityScore(b, posts)
			platform := detectPlatform(b.BlogURL)
			_ = s.discoverRepo.UpsertBlogPopularity(b.ID, platform, score)
		}

		if !hasMore {
			break
		}
		page++
	}
	log.Printf("[분류] 블로그 재분류 완료: %d개 재분류됨", classified)
}

// ──────────────────────────────────────────────────────────────────
// 2. Curated popular blogs (ALL categories)
// ──────────────────────────────────────────────────────────────────

func (s *BlogCrawlerService) crawlCuratedBlogs() {
	log.Println("[큐레이션] 인기 블로그 수집 시작")

	type curatedFeed struct {
		rssURL   string
		category string
		tags     []string
	}

	feeds := []curatedFeed{
		// ── 개발/기술 (tech) ──
		{"https://jojoldu.tistory.com/rss", "tech", []string{"Java", "Spring", "백엔드"}},
		{"https://mangkyu.tistory.com/rss", "tech", []string{"Java", "Spring", "개발"}},
		{"https://coding-factory.tistory.com/rss", "tech", []string{"개발", "코딩", "튜토리얼"}},
		{"https://inpa.tistory.com/rss", "tech", []string{"개발", "프론트엔드", "백엔드"}},
		{"https://hudi.blog/rss.xml", "tech", []string{"개발", "웹", "프론트엔드"}},
		{"https://techblog.woowahan.com/feed/", "tech", []string{"우아한형제들", "백엔드", "인프라"}},
		{"https://d2.naver.com/d2.atom", "tech", []string{"네이버", "기술블로그", "AI"}},
		{"https://toss.tech/rss.xml", "tech", []string{"토스", "핀테크", "프론트엔드"}},
		{"https://zzsza.github.io/feed.xml", "ai", []string{"AI", "데이터", "MLOps"}},
		{"https://subicura.com/feed.xml", "tech", []string{"DevOps", "Docker", "Kubernetes"}},
		{"https://evan-moon.github.io/feed.xml", "tech", []string{"프론트엔드", "JavaScript", "웹"}},
		{"https://joshua1988.github.io/feed.xml", "tech", []string{"Vue", "프론트엔드", "JavaScript"}},
		{"https://meetup.nhncloud.com/rss", "tech", []string{"NHN", "기술블로그", "클라우드"}},
		{"https://tech.kakao.com/feed/", "tech", []string{"카카오", "기술블로그"}},
		{"https://hyperconnect.github.io/feed.xml", "tech", []string{"하이퍼커넥트", "기술블로그", "AI"}},
		{"https://netflixtechblog.com/feed", "tech", []string{"Netflix", "기술블로그", "마이크로서비스"}},
		{"https://blog.banksalad.com/feed.xml", "tech", []string{"뱅크샐러드", "기술블로그", "핀테크"}},
		{"https://ridicorp.com/feed/", "tech", []string{"리디", "기술블로그"}},
		{"https://programmers.co.kr/blog/feed", "tech", []string{"프로그래머스", "코딩테스트"}},

		// ── 디자인 (design) ──
		{"https://story.pxd.co.kr/rss", "design", []string{"UX", "디자인", "사용자경험"}},
		{"https://brunch.co.kr/rss/magazine/designspectrum", "design", []string{"디자인", "UI", "브런치"}},
		{"https://yozm.wishket.com/magazine/list/design/rss/", "design", []string{"디자인", "UX", "위시켓"}},

		// ── 마케팅 (marketing) ──
		{"https://brunch.co.kr/rss/magazine/marketingis", "marketing", []string{"마케팅", "브랜딩", "브런치"}},
		{"https://yozm.wishket.com/magazine/list/biz/rss/", "marketing", []string{"마케팅", "비즈니스", "위시켓"}},

		// ── 창업/비즈니스 (business) ──
		{"https://brunch.co.kr/rss/magazine/startup", "business", []string{"창업", "스타트업", "브런치"}},
		{"https://platum.kr/feed", "business", []string{"스타트업", "투자", "IT뉴스"}},
		{"https://www.besuccess.com/feed/", "business", []string{"스타트업", "비즈니스"}},
		{"https://byline.network/feed/", "news", []string{"IT뉴스", "테크", "비즈니스"}},

		// ── 여행 (travel) ──
		{"https://brunch.co.kr/rss/magazine/travel", "travel", []string{"여행", "해외여행", "브런치"}},

		// ── 맛집/요리 (food) ──
		{"https://brunch.co.kr/rss/magazine/food", "food", []string{"맛집", "요리", "브런치"}},

		// ── 재테크/투자 (finance) ──
		{"https://brunch.co.kr/rss/magazine/money", "finance", []string{"재테크", "투자", "브런치"}},

		// ── 자기계발 (growth) ──
		{"https://brunch.co.kr/rss/magazine/selfimprovement", "growth", []string{"자기계발", "독서", "브런치"}},
		{"https://brunch.co.kr/rss/magazine/bookstory", "growth", []string{"독서", "서평", "브런치"}},

		// ── IT뉴스 (news) ──
		{"https://www.bloter.net/feed", "news", []string{"IT뉴스", "테크", "블로터"}},
		{"https://zdnet.co.kr/rss/", "news", []string{"IT뉴스", "테크", "ZDNet"}},

		// ── 데이터/AI (ai) ──
		{"https://tensorflow.blog/feed/", "ai", []string{"AI", "텐서플로", "딥러닝"}},
		{"https://brunch.co.kr/rss/magazine/ai", "ai", []string{"AI", "인공지능", "브런치"}},

		// ── 게임 (game) ──
		{"https://brunch.co.kr/rss/magazine/game", "game", []string{"게임", "게임개발", "브런치"}},
	}

	registered := 0
	for _, f := range feeds {
		if s.registerDiscoverBlog(f.rssURL, f.category, f.tags) {
			registered++
		}
		time.Sleep(1 * time.Second) // be polite
	}
	log.Printf("[큐레이션] 인기 블로그 수집 완료: %d개 신규 등록", registered)
}

// ──────────────────────────────────────────────────────────────────
// 3. Velog trending users
// ──────────────────────────────────────────────────────────────────

func (s *BlogCrawlerService) crawlVelogTrending() {
	log.Println("[Velog] 트렌딩 블로그 수집 시작")

	velogUsers := []struct {
		username string
		category string
		tags     []string
	}{
		{"velopert", "tech", []string{"React", "프론트엔드", "JavaScript"}},
		{"teo", "tech", []string{"프론트엔드", "개발문화"}},
		{"mowinckel", "tech", []string{"개발", "커리어"}},
		{"soryeongk", "tech", []string{"프론트엔드", "React"}},
		{"wooder2050", "tech", []string{"알고리즘", "코딩테스트"}},
		{"yh20studio", "tech", []string{"백엔드", "Spring"}},
		{"hang_kem_0531", "tech", []string{"프론트엔드", "TypeScript"}},
		{"juno7803", "tech", []string{"프론트엔드", "React"}},
		{"hyemin916", "design", []string{"디자인", "UX"}},
		{"taeha7b", "tech", []string{"백엔드", "Java"}},
		{"joshuara7235", "tech", []string{"개발", "취업"}},
		{"supergone", "tech", []string{"알고리즘", "Python"}},
	}

	registered := 0
	for _, vu := range velogUsers {
		rssURL := fmt.Sprintf("https://v2.velog.io/rss/%s", vu.username)
		if s.registerDiscoverBlog(rssURL, vu.category, vu.tags) {
			registered++
		}
		time.Sleep(1 * time.Second)
	}
	log.Printf("[Velog] 트렌딩 블로그 수집 완료: %d개 신규 등록", registered)
}

// ──────────────────────────────────────────────────────────────────
// 4. Tistory trending — scrape popular blog lists
// ──────────────────────────────────────────────────────────────────

func (s *BlogCrawlerService) crawlTistoryTrending() {
	log.Println("[Tistory] 트렌딩 블로그 탐색 시작")

	// Tistory trending categories
	tistoryCategories := []struct {
		url      string
		category string
	}{
		{"https://www.tistory.com/category/it-tech", "tech"},
		{"https://www.tistory.com/category/living", "life"},
		{"https://www.tistory.com/category/travel-leisure", "travel"},
		{"https://www.tistory.com/category/culture-arts", "growth"},
		{"https://www.tistory.com/category/economy-business", "finance"},
		{"https://www.tistory.com/category/entertainment", "game"},
	}

	discovered := 0
	for _, tc := range tistoryCategories {
		html, err := s.fetchHTML(tc.url)
		if err != nil {
			log.Printf("[Tistory] 트렌딩 페이지 fetch 실패 (%s): %v", tc.url, err)
			continue
		}

		// Extract blog URLs from trending page
		blogURLs := extractTistoryBlogURLs(html)
		for _, blogURL := range blogURLs {
			rssURL := blogURL + "/rss"
			if s.registerDiscoverBlog(rssURL, tc.category, []string{"티스토리", "트렌딩"}) {
				discovered++
			}
			time.Sleep(500 * time.Millisecond)
		}
	}
	log.Printf("[Tistory] 트렌딩 블로그 탐색 완료: %d개 신규 발견", discovered)
}

// ──────────────────────────────────────────────────────────────────
// 5. GitHub.io tech blogs
// ──────────────────────────────────────────────────────────────────

func (s *BlogCrawlerService) crawlGithubIOBlogs() {
	log.Println("[GitHub.io] 기술 블로그 수집 시작")

	githubBlogs := []struct {
		rssURL string
		tags   []string
	}{
		{"https://woowabros.github.io/feed.xml", []string{"우아한형제들", "기술블로그"}},
		{"https://tech.kakaoentertainment.com/feed", []string{"카카오엔터", "기술블로그"}},
		{"https://engineering.ab180.co/feed", []string{"AB180", "기술블로그"}},
		{"https://blog.mathpresso.com/feed", []string{"매스프레소", "기술블로그"}},
		{"https://medium.com/feed/musinsa-tech", []string{"무신사", "기술블로그"}},
		{"https://medium.com/feed/coupang-engineering", []string{"쿠팡", "기술블로그"}},
		{"https://medium.com/feed/watcha", []string{"왓챠", "기술블로그"}},
		{"https://medium.com/feed/29cm", []string{"29CM", "기술블로그"}},
		{"https://medium.com/feed/tving-team", []string{"티빙", "기술블로그"}},
		{"https://green-labs.github.io/feed.xml", []string{"그린랩스", "기술블로그", "Clojure"}},
		{"https://devblog.kakaostyle.com/feed", []string{"카카오스타일", "기술블로그"}},
	}

	registered := 0
	for _, gb := range githubBlogs {
		if s.registerDiscoverBlog(gb.rssURL, "tech", gb.tags) {
			registered++
		}
		time.Sleep(1 * time.Second)
	}
	log.Printf("[GitHub.io] 기술 블로그 수집 완료: %d개 신규 등록", registered)
}

// ──────────────────────────────────────────────────────────────────
// Blog registration helper
// ──────────────────────────────────────────────────────────────────

func (s *BlogCrawlerService) registerDiscoverBlog(rssURL, categorySlug string, tags []string) bool {
	// Check if blog already exists
	existing, _ := s.blogRepo.FindByRssURL(rssURL)
	var blogID int64

	if existing != nil {
		blogID = existing.ID
	} else {
		// Fetch feed info
		f, err := s.feedClient.GetFeed(rssURL)
		if err != nil {
			log.Printf("[등록] 피드 조회 실패 (%s): %v", rssURL, err)
			return false
		}

		// Save blog
		newBlog := &blog.Blog{
			RssURL:            rssURL,
			BlogURL:           f.BlogURL,
			BlogTitle:         f.BlogTitle,
			PublishedDateTime: f.PublishedDateTime.Format(time.RFC3339),
		}
		if f.Author != "" {
			newBlog.Author.String = f.Author
			newBlog.Author.Valid = true
		}
		if f.ThumbnailURL != "" {
			newBlog.ThumbnailURL.String = f.ThumbnailURL
			newBlog.ThumbnailURL.Valid = true
		}

		id, err := s.blogRepo.Save(newBlog)
		if err != nil {
			log.Printf("[등록] 블로그 저장 실패 (%s): %v", rssURL, err)
			return false
		}
		blogID = id
		log.Printf("[등록] 새 블로그: %s (%s)", f.BlogTitle, rssURL)

		// Also save posts for the new blog
		for _, item := range f.Items {
			post := &blog.Post{
				BlogID:            blogID,
				PostURL:           item.PostURL,
				PostTitle:         item.PostTitle,
				PublishedDateTime: item.PublishedDateTime.Format(time.RFC3339),
			}
			if item.ThumbnailURL != "" {
				post.ThumbnailURL.String = item.ThumbnailURL
				post.ThumbnailURL.Valid = true
			}
			s.blogRepo.SavePost(post)
		}
	}

	// Set category
	catID, err := s.discoverRepo.GetCategoryIDBySlug(categorySlug)
	if err == nil {
		_ = s.discoverRepo.SetBlogCategory(blogID, catID)
	}

	// Set tags
	for _, tag := range tags {
		tagID, err := s.discoverRepo.UpsertTag(tag)
		if err == nil {
			_ = s.discoverRepo.SetBlogTag(blogID, tagID)
		}
	}

	// Calculate score
	posts, _ := s.blogRepo.FindPostsByBlogID(blogID)
	b, _ := s.blogRepo.FindByID(blogID)
	score := 50.0 // base score for curated blogs
	if b != nil {
		score = calculatePopularityScore(b, posts)
		if score < 50 {
			score = 50 // minimum for curated
		}
	}
	platform := ""
	if b != nil {
		platform = detectPlatform(b.BlogURL)
	}
	_ = s.discoverRepo.UpsertBlogPopularity(blogID, platform, score)

	return existing == nil // true if newly registered
}

// ──────────────────────────────────────────────────────────────────
// Improved classification: URL + title + post titles
// ──────────────────────────────────────────────────────────────────

func classifyBlogAdvanced(blogURL, title, author string, posts []*blog.Post) (category string, tags []string) {
	url := strings.ToLower(blogURL)
	titleLower := strings.ToLower(title)
	combined := url + " " + titleLower + " " + strings.ToLower(author)

	// Also analyze post titles for better classification
	postTitles := ""
	limit := 20
	if len(posts) < limit {
		limit = len(posts)
	}
	for _, p := range posts[:limit] {
		postTitles += " " + strings.ToLower(p.PostTitle)
	}

	// Category keyword maps (more comprehensive)
	categoryKeywords := map[string][]string{
		"tech": {
			"개발", "코딩", "프로그래밍", "developer", "engineering", "tech", "code", "dev",
			"java", "python", "javascript", "react", "vue", "angular", "spring", "node",
			"backend", "frontend", "api", "서버", "클라이언트", "프론트엔드", "백엔드",
			"ios", "android", "모바일", "flutter", "swift", "kotlin", "go", "rust",
			"docker", "kubernetes", "devops", "cicd", "git", "linux",
			"알고리즘", "자료구조", "코딩테스트", "프로그래머스", "leetcode",
			"소프트웨어", "software", "infrastructure", "인프라", "클라우드", "aws", "gcp", "azure",
			"database", "sql", "nosql", "mongodb", "redis", "mysql", "postgresql",
			"css", "html", "typescript", "webpack", "nextjs", "nuxt",
		},
		"ai": {
			"ai", "머신러닝", "딥러닝", "데이터", "machine learning", "deep learning", "data",
			"tensorflow", "pytorch", "gpt", "llm", "chatgpt", "인공지능", "neural",
			"nlp", "자연어처리", "컴퓨터비전", "추천시스템", "빅데이터", "big data",
			"데이터분석", "analytics", "ml", "mlops", "모델", "학습",
		},
		"design": {
			"디자인", "design", "ui", "ux", "figma", "sketch", "adobe",
			"그래픽", "일러스트", "포토샵", "타이포", "브랜딩", "로고",
			"css", "웹디자인", "앱디자인", "프로덕트디자인", "product design",
			"인터랙션", "프로토타입", "wireframe", "사용자경험",
		},
		"marketing": {
			"마케팅", "marketing", "seo", "광고", "퍼포먼스", "그로스",
			"콘텐츠마케팅", "소셜미디어", "인스타그램", "유튜브", "블로그마케팅",
			"브랜드", "cpa", "cpc", "roi", "전환율", "구글애즈", "페이스북광고",
			"이메일마케팅", "crm", "ab테스트",
		},
		"finance": {
			"투자", "재테크", "주식", "finance", "금융", "부동산",
			"etf", "펀드", "적금", "예금", "경제", "코인", "비트코인",
			"배당", "매매", "차트", "증권", "은행", "대출", "절약",
			"가계부", "연금", "보험", "세금", "부업", "수입",
		},
		"travel": {
			"여행", "travel", "trip", "관광", "해외여행", "국내여행",
			"맛집", "호텔", "숙소", "에어비앤비", "비행기", "항공",
			"유럽", "일본", "태국", "베트남", "발리", "하와이",
			"배낭여행", "캠핑", "글램핑", "제주", "부산",
		},
		"food": {
			"맛집", "요리", "레시피", "food", "cooking", "베이킹",
			"카페", "디저트", "음식", "식당", "맛있는", "먹방",
			"밀키트", "건강식", "다이어트식", "홈쿠킹", "빵",
		},
		"business": {
			"창업", "스타트업", "startup", "비즈니스", "사업",
			"투자유치", "vc", "엑셀러레이터", "피칭", "bm",
			"린스타트업", "mvp", "pmf", "스케일업", "exit",
			"ceo", "대표", "경영", "조직문화", "리더십",
		},
		"growth": {
			"자기계발", "독서", "서평", "책리뷰", "성장", "습관",
			"영어", "공부", "학습", "목표", "동기부여", "멘탈",
			"생산성", "시간관리", "미니멀리즘", "명상", "마인드셋",
			"블로그챌린지", "일기", "회고", "인사이트",
		},
		"news": {
			"뉴스", "news", "속보", "이슈", "트렌드", "리포트",
			"산업", "시장", "전망", "분석", "칼럼", "opinion",
			"테크뉴스", "it뉴스", "사설", "기사",
		},
		"game": {
			"게임", "game", "gaming", "리뷰", "공략",
			"스팀", "닌텐도", "플레이스테이션", "xbox", "pc게임",
			"모바일게임", "인디게임", "게임개발", "유니티", "언리얼",
			"esports", "롤", "배그", "오버워치", "발로란트",
		},
	}

	// Score each category by matching both URL/title AND post titles
	type catScore struct {
		slug  string
		score int
	}
	var scores []catScore

	for slug, keywords := range categoryKeywords {
		sc := 0
		for _, k := range keywords {
			// URL/title match (weight 3)
			if strings.Contains(combined, k) {
				sc += 3
			}
			// Post title match (weight 1)
			if strings.Contains(postTitles, k) {
				sc += 1
			}
		}
		if sc > 0 {
			scores = append(scores, catScore{slug, sc})
		}
	}

	// Find best category
	best := ""
	bestScore := 0
	for _, cs := range scores {
		if cs.score > bestScore {
			bestScore = cs.score
			best = cs.slug
		}
	}

	// Platform-based tags
	if strings.Contains(url, "tistory") {
		tags = append(tags, "티스토리")
	} else if strings.Contains(url, "velog") {
		tags = append(tags, "velog")
	} else if strings.Contains(url, "naver") {
		tags = append(tags, "네이버")
	} else if strings.Contains(url, "brunch") {
		tags = append(tags, "브런치")
	} else if strings.Contains(url, "github.io") {
		tags = append(tags, "GitHub")
	} else if strings.Contains(url, "medium.com") {
		tags = append(tags, "Medium")
	}

	// Content-based tags from post titles
	contentTags := map[string][]string{
		"React":      {"react"},
		"Vue":        {"vue", "nuxt"},
		"Spring":     {"spring", "springboot"},
		"Node.js":    {"node", "express", "nestjs"},
		"Python":     {"python", "django", "flask"},
		"Java":       {"java", "jvm"},
		"Go":         {"golang", " go "},
		"Kubernetes": {"kubernetes", "k8s"},
		"Docker":     {"docker", "컨테이너"},
		"AWS":        {"aws", "amazon"},
		"Next.js":    {"nextjs", "next.js"},
		"TypeScript": {"typescript"},
	}
	for tagName, keywords := range contentTags {
		for _, k := range keywords {
			if strings.Contains(postTitles, k) || strings.Contains(combined, k) {
				tags = append(tags, tagName)
				break
			}
		}
	}

	if best == "" {
		best = "life" // default category
	}

	return best, tags
}

// Keep old function for backward compatibility
func classifyBlog(blogURL, title, author string) (category string, tags []string) {
	return classifyBlogAdvanced(blogURL, title, author, nil)
}

// ──────────────────────────────────────────────────────────────────
// Tistory page scraper helpers
// ──────────────────────────────────────────────────────────────────

var tistoryBlogURLRe = regexp.MustCompile(`https?://([a-zA-Z0-9_-]+)\.tistory\.com`)

func extractTistoryBlogURLs(html string) []string {
	matches := tistoryBlogURLRe.FindAllStringSubmatch(html, -1)
	seen := make(map[string]bool)
	var urls []string
	for _, m := range matches {
		blogURL := m[0]
		// Normalize to https
		blogURL = strings.Replace(blogURL, "http://", "https://", 1)
		if !seen[blogURL] && !strings.Contains(blogURL, "tistory.com/category") {
			seen[blogURL] = true
			urls = append(urls, blogURL)
		}
	}
	return urls
}

// ──────────────────────────────────────────────────────────────────
// Popularity scoring
// ──────────────────────────────────────────────────────────────────

func calculatePopularityScore(b *blog.Blog, posts []*blog.Post) float64 {
	score := 0.0

	// Post count factor (more posts = more established)
	postCount := float64(len(posts))
	if postCount > 0 {
		score += min(postCount*2, 40) // max 40 points from posts
	}

	// Visit count factor
	score += min(float64(b.BlogVisitCount)/10, 20) // max 20 points

	// Recency factor: recent posts boost score
	if len(posts) > 0 {
		latest := posts[0].GetPublishedTime()
		for _, p := range posts[1:] {
			t := p.GetPublishedTime()
			if t.After(latest) {
				latest = t
			}
		}
		daysSince := time.Since(latest).Hours() / 24
		if daysSince < 7 {
			score += 30
		} else if daysSince < 30 {
			score += 20
		} else if daysSince < 90 {
			score += 10
		}
	}

	// Post frequency (posts per month)
	if len(posts) >= 2 {
		earliest := posts[0].GetPublishedTime()
		latest := earliest
		for _, p := range posts {
			t := p.GetPublishedTime()
			if t.Before(earliest) {
				earliest = t
			}
			if t.After(latest) {
				latest = t
			}
		}
		months := latest.Sub(earliest).Hours() / 24 / 30
		if months > 0 {
			freq := postCount / months
			score += min(freq*3, 10) // max 10 points
		}
	}

	return score
}

func detectPlatform(blogURL string) string {
	url := strings.ToLower(blogURL)
	switch {
	case strings.Contains(url, "tistory"):
		return "tistory"
	case strings.Contains(url, "velog"):
		return "velog"
	case strings.Contains(url, "naver"):
		return "naver"
	case strings.Contains(url, "brunch"):
		return "brunch"
	case strings.Contains(url, "medium"):
		return "medium"
	case strings.Contains(url, "github.io"):
		return "github"
	default:
		return "other"
	}
}

func min(a, b float64) float64 {
	if a < b {
		return a
	}
	return b
}

// fetchHTML fetches HTML content from a URL
func (s *BlogCrawlerService) fetchHTML(url string) (string, error) {
	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return "", err
	}
	req.Header.Set("User-Agent", "Mozilla/5.0 (compatible; ClosestBot/1.0)")

	resp, err := s.httpClient.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}
	return string(body), nil
}

// extractRSSFromHTML tries to find RSS feed URL in HTML
var rssLinkRe = regexp.MustCompile(`<link[^>]+type=["'](application/rss\+xml|application/atom\+xml)["'][^>]+href=["']([^"']+)["']`)

func extractRSSFromHTML(html, baseURL string) string {
	matches := rssLinkRe.FindStringSubmatch(html)
	if len(matches) > 2 {
		href := matches[2]
		if strings.HasPrefix(href, "/") {
			return baseURL + href
		}
		return href
	}
	return ""
}
