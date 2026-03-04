// Service worker for Closest Chrome Extension

// Listen for messages from content script
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'RSS_DETECTED') {
    // Update badge to show RSS found
    const count = message.feeds.length;
    chrome.action.setBadgeText({
      text: count > 0 ? String(count) : '',
      tabId: sender.tab.id,
    });
    chrome.action.setBadgeBackgroundColor({
      color: '#007bff',
      tabId: sender.tab.id,
    });

    // Store detected feeds for the popup
    chrome.storage.session.set({
      [`feeds_${sender.tab.id}`]: message.feeds,
      [`pageTitle_${sender.tab.id}`]: message.pageTitle,
      [`pageUrl_${sender.tab.id}`]: message.pageUrl,
    });
  }

  if (message.type === 'SUBSCRIBE') {
    handleSubscribe(message.rssUrl)
      .then((result) => sendResponse(result))
      .catch((err) => sendResponse({ success: false, error: err.message }));
    return true; // async response
  }

  if (message.type === 'LOGIN') {
    handleLogin(message.email, message.password)
      .then((result) => sendResponse(result))
      .catch((err) => sendResponse({ success: false, error: err.message }));
    return true;
  }

  if (message.type === 'AUTH_TOKEN') {
    // Received token from web app via OAuth flow
    chrome.storage.local.set({
      accessToken: message.accessToken,
      userEmail: message.email,
    });
    sendResponse({ success: true });
  }
});

// Clear badge when tab is updated
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
  const res = await fetch(`${serverUrl}/auth/signin`, {
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
    await chrome.storage.local.set({
      accessToken: token,
      userEmail: email,
    });
    return { success: true, email };
  }

  throw new Error('토큰을 받지 못했습니다');
}

async function handleSubscribe(rssUrl) {
  const serverUrl = await getServerUrl();
  const { accessToken } = await chrome.storage.local.get('accessToken');

  if (!accessToken) {
    throw new Error('로그인이 필요합니다');
  }

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
