// Content script: detect RSS feeds on the current page

(function () {
  const feeds = [];

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

  // 2. Common RSS URL patterns (heuristic)
  const hostname = location.hostname;
  const commonPatterns = [];

  // Tistory
  if (hostname.includes('tistory.com')) {
    commonPatterns.push(`${location.origin}/rss`);
  }

  // Naver Blog
  if (hostname === 'blog.naver.com') {
    const blogId = location.pathname.split('/')[1];
    if (blogId) {
      commonPatterns.push(`https://rss.blog.naver.com/${blogId}.xml`);
    }
  }

  // Velog
  if (hostname === 'velog.io') {
    const username = location.pathname.split('/')[1]?.replace('@', '');
    if (username) {
      commonPatterns.push(`https://v2.velog.io/rss/${username}`);
    }
  }

  // Brunch
  if (hostname === 'brunch.co.kr') {
    const match = location.pathname.match(/\/@(\w+)/);
    if (match) {
      commonPatterns.push(`https://brunch.co.kr/rss/@@${match[1]}`);
    }
  }

  // WordPress common
  if (!feeds.length) {
    commonPatterns.push(`${location.origin}/feed`);
    commonPatterns.push(`${location.origin}/rss`);
    commonPatterns.push(`${location.origin}/feed.xml`);
    commonPatterns.push(`${location.origin}/rss.xml`);
    commonPatterns.push(`${location.origin}/atom.xml`);
  }

  // Add heuristic patterns (not validated, marked as guess)
  commonPatterns.forEach((url) => {
    if (!feeds.find((f) => f.url === url)) {
      feeds.push({ url, title: document.title, source: 'guess' });
    }
  });

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
