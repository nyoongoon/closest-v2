// Service worker for Closest Chrome Extension

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'RSS_DETECTED') {
    // 확인된 RSS만 카운트 (추정 피드 제외)
    const confirmed = message.feeds.filter(f => f.source === 'link').length;
    const count = confirmed || message.feeds.length;
    chrome.action.setBadgeText({
      text: count > 0 ? String(count) : '',
      tabId: sender.tab.id,
    });
    chrome.action.setBadgeBackgroundColor({
      color: confirmed > 0 ? '#007bff' : '#ffc107',
      tabId: sender.tab.id,
    });

    chrome.storage.session.set({
      [`feeds_${sender.tab.id}`]: message.feeds,
      [`pageTitle_${sender.tab.id}`]: message.pageTitle,
      [`pageUrl_${sender.tab.id}`]: message.pageUrl,
    });
  }

  if (message.type === 'SUBSCRIBE') {
    handleSubscribe(message.rssUrl)
      .then(r => sendResponse(r))
      .catch(e => sendResponse({ success: false, error: e.message }));
    return true;
  }

  if (message.type === 'LOGIN') {
    handleLogin(message.email, message.password)
      .then(r => sendResponse(r))
      .catch(e => sendResponse({ success: false, error: e.message }));
    return true;
  }

  if (message.type === 'VALIDATE_FEED') {
    validateFeed(message.url)
      .then(r => sendResponse(r))
      .catch(() => sendResponse({ valid: false }));
    return true;
  }

  if (message.type === 'AUTH_TOKEN') {
    chrome.storage.local.set({
      accessToken: message.accessToken,
      userEmail: message.email,
    });
    sendResponse({ success: true });
  }
});

chrome.tabs.onUpdated.addListener((tabId, changeInfo) => {
  if (changeInfo.status === 'loading') {
    chrome.action.setBadgeText({ text: '', tabId });
  }
});

async function getServerUrl() {
  const data = await chrome.storage.local.get('serverUrl');
  return data.serverUrl || 'http://localhost:8081';
}

async function handleLogin(email, password) {
  const serverUrl = await getServerUrl();
  const res = await fetch(`${serverUrl}/member/auth/signin`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || '로그인 실패');
  }

  const data = await res.json();
  const token = data.accessToken || data.token;

  if (token) {
    await chrome.storage.local.set({ accessToken: token, userEmail: email });
    return { success: true, email };
  }
  throw new Error('토큰을 받지 못했습니다');
}

async function handleSubscribe(rssUrl) {
  const serverUrl = await getServerUrl();
  const { accessToken } = await chrome.storage.local.get('accessToken');
  if (!accessToken) throw new Error('로그인이 필요합니다');

  const res = await fetch(`${serverUrl}/subscriptions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ rssUri: rssUrl }),
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || '구독 실패');
  }
  return { success: true };
}

// RSS/Atom 피드 URL 유효성 검증
async function validateFeed(url) {
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 5000);
    const res = await fetch(url, {
      method: 'GET',
      headers: { Accept: 'application/rss+xml, application/atom+xml, text/xml' },
      signal: controller.signal,
    });
    clearTimeout(timeout);

    if (!res.ok) return { valid: false };
    const text = await res.text();
    const isRSS = text.includes('<rss') || text.includes('<feed') || text.includes('<channel');
    return { valid: isRSS, url };
  } catch {
    return { valid: false };
  }
}
