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

CREATE INDEX IF NOT EXISTS idx_blog_rss_url ON blog(rss_url);
CREATE INDEX IF NOT EXISTS idx_blog_blog_url ON blog(blog_url);
CREATE INDEX IF NOT EXISTS idx_post_blog_id ON post(blog_id);
CREATE INDEX IF NOT EXISTS idx_subscription_member_email ON subscription(member_email);
CREATE INDEX IF NOT EXISTS idx_member_user_email ON member(user_email);
