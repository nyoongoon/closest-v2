// Content script: detect RSS feeds on the current page

(function () {
  const feeds = [];
  const hostname = location.hostname;

  // 1. <link> tags with RSS/Atom types
  const linkSelectors = [
    'link[type="application/rss+xml"]',
    'link[type="application/atom+xml"]',
    'link[type="application/feed+json"]',
    'link[type="application/xml"][rel="alternate"]',
    'link[type="text/xml"][rel="alternate"]',
  ];

  linkSelectors.forEach((sel) => {
    document.querySelectorAll(sel).forEach((el) => {
      const href = el.getAttribute('href');
      if (href) {
        const url = new URL(href, document.location.href).href;
        const title = el.getAttribute('title') || document.title;
        if (!feeds.find((f) => f.url === url)) {
          feeds.push({ url, title, source: 'link' });
        }
      }
    });
  });

  // 2. Platform-specific RSS URL patterns (확정 피드)
  const platformFeeds = [];

  // Tistory
  if (hostname.endsWith('.tistory.com')) {
    platformFeeds.push({ url: `${location.origin}/rss`, title: document.title, source: 'link' });
  }

  // Naver Blog
  if (hostname === 'blog.naver.com') {
    const blogId = location.pathname.split('/')[1];
    if (blogId) {
      platformFeeds.push({ url: `https://rss.blog.naver.com/${blogId}.xml`, title: document.title, source: 'link' });
    }
  }

  // Velog
  if (hostname === 'velog.io') {
    const username = location.pathname.split('/')[1]?.replace('@', '');
    if (username) {
      platformFeeds.push({ url: `https://v2.velog.io/rss/${username}`, title: `${username} - velog`, source: 'link' });
    }
  }

  // Brunch
  if (hostname === 'brunch.co.kr') {
    const match = location.pathname.match(/\/@(\w+)/);
    if (match) {
      platformFeeds.push({ url: `https://brunch.co.kr/rss/@@${match[1]}`, title: document.title, source: 'link' });
    }
  }

  // Medium
  if (hostname === 'medium.com' || hostname.endsWith('.medium.com')) {
    const pathParts = location.pathname.split('/').filter(Boolean);
    if (pathParts.length > 0 && pathParts[0].startsWith('@')) {
      platformFeeds.push({ url: `https://medium.com/feed/${pathParts[0]}`, title: document.title, source: 'link' });
    } else if (pathParts.length > 0) {
      platformFeeds.push({ url: `https://medium.com/feed/${pathParts[0]}`, title: document.title, source: 'guess' });
    }
  }

  // GitHub.io (Jekyll blogs)
  if (hostname.endsWith('.github.io')) {
    platformFeeds.push({ url: `${location.origin}/feed.xml`, title: document.title, source: 'guess' });
    platformFeeds.push({ url: `${location.origin}/atom.xml`, title: document.title, source: 'guess' });
  }

  // Add platform feeds if not already found via <link>
  platformFeeds.forEach((pf) => {
    if (!feeds.find((f) => f.url === pf.url)) {
      feeds.push(pf);
    }
  });

  // 3. Common fallback patterns (only if no feeds found yet)
  if (feeds.length === 0) {
    const commonPaths = ['/feed', '/rss', '/feed.xml', '/rss.xml', '/atom.xml', '/index.xml'];
    commonPaths.forEach((path) => {
      feeds.push({ url: `${location.origin}${path}`, title: document.title, source: 'guess' });
    });
  }

  // Send to background
  if (feeds.length > 0) {
    chrome.runtime.sendMessage({
      type: 'RSS_DETECTED',
      feeds,
      pageTitle: document.title,
      pageUrl: location.href,
    });
  }
})();
