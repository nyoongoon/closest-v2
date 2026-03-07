CREATE TABLE IF NOT EXISTS member (
    member_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_email TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    nick_name TEXT,
    authority TEXT,
    blog_url TEXT UNIQUE,
    my_blog_visit_count INTEGER NOT NULL DEFAULT 0,
    status_message TEXT
);

CREATE TABLE IF NOT EXISTS blog (
    blog_id INTEGER PRIMARY KEY AUTOINCREMENT,
    rss_url TEXT NOT NULL,
    blog_url TEXT NOT NULL,
    blog_title TEXT NOT NULL,
    author TEXT,
    thumbnail_url TEXT,
    published_date_time TEXT NOT NULL,
    blog_visit_count INTEGER NOT NULL DEFAULT 0,
    status_message TEXT
);

CREATE TABLE IF NOT EXISTS post (
    post_id INTEGER PRIMARY KEY AUTOINCREMENT,
    blog_id INTEGER NOT NULL,
    post_url TEXT NOT NULL,
    post_title TEXT NOT NULL,
    published_date_time TEXT NOT NULL,
    post_visit_count INTEGER NOT NULL DEFAULT 0,
    thumbnail_url TEXT,
    FOREIGN KEY (blog_id) REFERENCES blog(blog_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS subscription (
    subscription_id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_email TEXT NOT NULL,
    subscription_visit_count INTEGER NOT NULL DEFAULT 0,
    subscription_nick_name TEXT,
    blog_url TEXT NOT NULL,
    blog_title TEXT NOT NULL,
    published_date_time TEXT NOT NULL,
    new_post_count INTEGER NOT NULL DEFAULT 0,
    thumbnail_url TEXT
);

CREATE TABLE IF NOT EXISTS likes (
    likes_id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id INTEGER NOT NULL,
    post_url TEXT NOT NULL
);

-- 카테고리
CREATE TABLE IF NOT EXISTS category (
    category_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    slug TEXT NOT NULL UNIQUE,
    icon TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0
);

-- 태그
CREATE TABLE IF NOT EXISTS tag (
    tag_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

-- 블로그-카테고리 매핑
CREATE TABLE IF NOT EXISTS blog_category (
    blog_id INTEGER NOT NULL,
    category_id INTEGER NOT NULL,
    PRIMARY KEY (blog_id, category_id),
    FOREIGN KEY (blog_id) REFERENCES blog(blog_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES category(category_id) ON DELETE CASCADE
);

-- 블로그-태그 매핑
CREATE TABLE IF NOT EXISTS blog_tag (
    blog_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    PRIMARY KEY (blog_id, tag_id),
    FOREIGN KEY (blog_id) REFERENCES blog(blog_id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(tag_id) ON DELETE CASCADE
);

-- 블로그 인기도/메타 정보
CREATE TABLE IF NOT EXISTS blog_popularity (
    blog_id INTEGER PRIMARY KEY,
    platform TEXT NOT NULL DEFAULT '',
    subscriber_count INTEGER NOT NULL DEFAULT 0,
    score REAL NOT NULL DEFAULT 0,
    post_frequency REAL NOT NULL DEFAULT 0,
    last_crawled_at TEXT,
    FOREIGN KEY (blog_id) REFERENCES blog(blog_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_blog_rss_url ON blog(rss_url);
CREATE INDEX IF NOT EXISTS idx_blog_blog_url ON blog(blog_url);
CREATE INDEX IF NOT EXISTS idx_post_blog_id ON post(blog_id);
CREATE INDEX IF NOT EXISTS idx_subscription_member_email ON subscription(member_email);
CREATE INDEX IF NOT EXISTS idx_member_user_email ON member(user_email);
CREATE INDEX IF NOT EXISTS idx_blog_category_cid ON blog_category(category_id);
CREATE INDEX IF NOT EXISTS idx_blog_tag_tid ON blog_tag(tag_id);
CREATE INDEX IF NOT EXISTS idx_blog_popularity_score ON blog_popularity(score DESC);
