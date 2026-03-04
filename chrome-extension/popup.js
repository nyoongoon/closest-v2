// Popup script for Closest Chrome Extension

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

let serverUrl = 'http://localhost:8081';

// ── Init ──
document.addEventListener('DOMContentLoaded', async () => {
  // Load server URL
  const data = await chrome.storage.local.get(['serverUrl', 'accessToken', 'userEmail']);
  if (data.serverUrl) {
    serverUrl = data.serverUrl;
    $('#serverUrl').value = serverUrl;
  }

  // Check login state
  if (data.accessToken && data.userEmail) {
    showLoggedIn(data.userEmail);
  } else {
    showLoginForm();
  }

  // Event listeners
  $('#loginForm').addEventListener('submit', handleLogin);
  $('#logoutBtn').addEventListener('click', handleLogout);
  $('#oauthBtn').addEventListener('click', handleOAuthLogin);
  $('#openSignup').addEventListener('click', handleOpenSignup);
  $('#saveServerUrl').addEventListener('click', handleSaveServerUrl);
  $('#manualBtn').addEventListener('click', handleManualSubscribe);
});

// ── UI State ──
function showLoginForm() {
  $('#loginSection').style.display = '';
  $('#feedSection').style.display = 'none';
  $('#userInfo').style.display = 'none';
}

function showLoggedIn(email) {
  $('#loginSection').style.display = 'none';
  $('#feedSection').style.display = '';
  $('#userInfo').style.display = 'flex';
  $('#userEmail').textContent = email;
  loadFeeds();
}

// ── Auth ──
async function handleLogin(e) {
  e.preventDefault();
  const email = $('#email').value.trim();
  const password = $('#password').value;
  const btn = $('#loginBtn');

  if (!email || !password) return;

  btn.disabled = true;
  btn.textContent = '로그인 중...';

  try {
    const result = await chrome.runtime.sendMessage({
      type: 'LOGIN',
      email,
      password,
    });

    if (result.success) {
      showToast('로그인 성공!', 'success');
      showLoggedIn(result.email);
    } else {
      showToast(result.error || '로그인 실패', 'error');
    }
  } catch (err) {
    showToast('로그인 요청 실패', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '로그인';
  }
}

async function handleLogout() {
  await chrome.storage.local.remove(['accessToken', 'userEmail']);
  showToast('로그아웃 되었습니다', 'info');
  showLoginForm();
}

function handleOAuthLogin() {
  // Open closest web app for OAuth login
  const authUrl = serverUrl.replace(':8081', ':5173') + '?ext_auth=1';
  chrome.tabs.create({ url: authUrl });
  showToast('웹에서 로그인 후 돌아오세요', 'info');
}

function handleOpenSignup() {
  const signupUrl = serverUrl.replace(':8081', ':5173') + '?signup=1';
  chrome.tabs.create({ url: signupUrl });
}

// ── Feeds ──
async function loadFeeds() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab) return;

  const data = await chrome.storage.session.get([
    `feeds_${tab.id}`,
    `pageTitle_${tab.id}`,
  ]);

  const feeds = data[`feeds_${tab.id}`] || [];
  const feedList = $('#feedList');
  const noFeeds = $('#noFeeds');

  feedList.innerHTML = '';

  if (feeds.length === 0) {
    noFeeds.style.display = '';
    return;
  }

  noFeeds.style.display = 'none';

  feeds.forEach((feed) => {
    const item = document.createElement('div');
    item.className = 'feed-item';

    const shortUrl = feed.url.length > 40
      ? feed.url.slice(0, 37) + '...'
      : feed.url;

    const badgeText = feed.source === 'link' ? 'RSS' : '추정';
    const badgeClass = feed.source === 'link' ? '' : ' style="background:#fff3cd;color:#856404"';

    item.innerHTML = `
      <div class="feed-item__info">
        <div class="feed-item__title">${escapeHtml(feed.title)}</div>
        <div class="feed-item__url" title="${escapeHtml(feed.url)}">${escapeHtml(shortUrl)}</div>
      </div>
      <span class="feed-item__badge"${badgeClass}>${badgeText}</span>
      <button class="feed-item__btn" data-url="${escapeHtml(feed.url)}">구독</button>
    `;

    const btn = item.querySelector('.feed-item__btn');
    btn.addEventListener('click', () => subscribeFeed(feed.url, btn));

    feedList.appendChild(item);
  });
}

async function subscribeFeed(rssUrl, btn) {
  btn.disabled = true;
  btn.textContent = '...';

  try {
    const result = await chrome.runtime.sendMessage({
      type: 'SUBSCRIBE',
      rssUrl,
    });

    if (result.success) {
      btn.textContent = '완료';
      btn.classList.add('feed-item__btn--done');
      showToast('구독 완료!', 'success');
    } else {
      btn.disabled = false;
      btn.textContent = '구독';
      showToast(result.error || '구독 실패', 'error');
    }
  } catch (err) {
    btn.disabled = false;
    btn.textContent = '구독';
    showToast('요청 실패', 'error');
  }
}

async function handleManualSubscribe() {
  const url = $('#manualUrl').value.trim();
  if (!url) return;

  const btn = $('#manualBtn');
  btn.disabled = true;

  try {
    const result = await chrome.runtime.sendMessage({
      type: 'SUBSCRIBE',
      rssUrl: url,
    });

    if (result.success) {
      showToast('구독 완료!', 'success');
      $('#manualUrl').value = '';
    } else {
      showToast(result.error || '구독 실패', 'error');
    }
  } catch (err) {
    showToast('요청 실패', 'error');
  } finally {
    btn.disabled = false;
  }
}

// ── Settings ──
async function handleSaveServerUrl() {
  const url = $('#serverUrl').value.trim();
  if (url) {
    serverUrl = url;
    await chrome.storage.local.set({ serverUrl: url });
    showToast('서버 URL 저장됨', 'info');
  }
}

// ── Toast ──
function showToast(msg, type = 'info') {
  const toast = $('#toast');
  toast.textContent = msg;
  toast.className = `toast toast--${type}`;
  toast.style.display = '';
  setTimeout(() => {
    toast.style.display = 'none';
  }, 2500);
}

// ── Util ──
function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}
