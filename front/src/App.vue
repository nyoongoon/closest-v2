<script setup lang="ts">
import { ref, computed, onUnmounted, watch, provide } from 'vue';
import AppHeader from '@/components/AppHeader.vue';
import ToastContainer from '@/components/ToastContainer.vue';
import { initToastBus, useToast } from '@/composables/useToast';
import { getAccessTokenFromCookie, deleteCookieFromBrowser } from '@/utils/cookie';
import { useSubscriptionStore } from '@/stores/subscription';

// 전역 토스트 버스 초기화
const unsubscribeToastBus = initToastBus();
onUnmounted(() => unsubscribeToastBus());

const subscriptionStore = useSubscriptionStore();
const { showToast } = useToast();

const isLoggedIn = ref(!!getAccessTokenFromCookie());

const newPostCount = computed(() => subscriptionStore.totalNewPosts);

// 로그인 상태 변경 감지
const checkAuth = () => {
  isLoggedIn.value = !!getAccessTokenFromCookie();
};

let authCheckInterval: ReturnType<typeof setInterval> | null = null;
authCheckInterval = setInterval(checkAuth, 5000);
onUnmounted(() => {
  if (authCheckInterval) clearInterval(authCheckInterval);
});

// 로그인 상태에 따라 구독 목록 로드 (비회원도 기본 유명 블로거 표시)
watch(isLoggedIn, (val) => {
  if (val) {
    subscriptionStore.fetchCloseBlogs();
  } else {
    subscriptionStore.$reset();
    subscriptionStore.fetchCloseBlogs(); // 비회원: 기본 유명 블로거 데이터 로드
  }
}, { immediate: true });

// 모달 상태 (자식 컴포넌트에서 공유)
const showLoginModal = ref(false);
const showSubscribeModal = ref(false);

const handleLogin = () => {
  showLoginModal.value = true;
};

const handleSubscribe = () => {
  showSubscribeModal.value = true;
};

const handleLogout = () => {
  deleteCookieFromBrowser('accessToken');
  deleteCookieFromBrowser('refreshToken');
  isLoggedIn.value = false;
  showToast('로그아웃되었습니다.', 'info');
};

provide('isLoggedIn', isLoggedIn);
provide('checkAuth', checkAuth);
</script>

<template>
  <AppHeader
    :isLoggedIn="isLoggedIn"
    :newPostCount="newPostCount"
    @login="handleLogin"
    @subscribe="handleSubscribe"
    @logout="handleLogout"
  />
  <router-view
    :isLoggedIn="isLoggedIn"
    :showLoginModal="showLoginModal"
    :showSubscribeModal="showSubscribeModal"
    @update:isLoggedIn="(val: boolean) => isLoggedIn = val"
    @update:showLoginModal="(val: boolean) => showLoginModal = val"
    @update:showSubscribeModal="(val: boolean) => showSubscribeModal = val"
  />
  <ToastContainer />
</template>

<style>
body {
  font-family: Pretendard, -apple-system, BlinkMacSystemFont, system-ui, Roboto,
  "Helvetica Neue", "Segoe UI", "Apple SD Gothic Neo", "Noto Sans KR",
  "Malgun Gothic", "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", sans-serif;
  margin: 0;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

* {
  box-sizing: border-box;
}
</style>
