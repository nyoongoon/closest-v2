<template>
  <div class="explore" @touchstart="onTouchStart" @touchend="onTouchEnd">
    <!-- 닫기 버튼 -->
    <button class="explore__close" @click="$router.back()">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
    </button>

    <!-- 로딩 -->
    <div v-if="loading" class="explore__loading">
      <div class="explore__spinner"></div>
    </div>

    <!-- 포스트 없음 -->
    <div v-else-if="posts.length === 0" class="explore__empty">
      <p>추천할 게시글이 없습니다</p>
      <button class="explore__back-btn" @click="$router.back()">돌아가기</button>
    </div>

    <!-- 숏츠 카드 -->
    <Transition :name="slideDirection" mode="out-in">
      <div v-if="currentPost" :key="currentIndex" class="shorts-card">
        <!-- 블러 배경 (분위기용) -->
        <div class="shorts-card__bg-blur" :style="bgBlurStyle"></div>
        <!-- 선명한 이미지 (적당한 크기) -->
        <div class="shorts-card__bg-img" :style="bgImgStyle"></div>
        <div class="shorts-card__overlay"></div>

        <div class="shorts-card__content">
          <!-- 블로그 정보 -->
          <div class="shorts-card__blog">
            <img class="shorts-card__favicon" :src="getFavicon(currentPost)" :alt="currentPost.blogTitle" />
            <div class="shorts-card__blog-info">
              <span class="shorts-card__blog-name">{{ currentPost.blogTitle }}</span>
              <span class="shorts-card__author">{{ currentPost.author || '' }}</span>
            </div>
          </div>

          <!-- 제목 -->
          <h2 class="shorts-card__title">{{ currentPost.postTitle }}</h2>

          <!-- 시간 -->
          <span class="shorts-card__time">{{ formatTime(currentPost.publishedDateTime) }}</span>

          <!-- 방문 버튼 -->
          <a :href="currentPost.postUrl" target="_blank" rel="noopener noreferrer" class="shorts-card__visit">
            읽으러 가기 ↗
          </a>
        </div>

        <!-- 상하 네비 -->
        <button class="shorts-card__nav shorts-card__nav--up" @click="prev" v-if="currentIndex > 0">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 15 12 9 18 15"/></svg>
        </button>
        <button class="shorts-card__nav shorts-card__nav--down" @click="next" v-if="currentIndex < posts.length - 1">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
        </button>

        <!-- 타이머 프로그레스 바 (하단) -->
        <div class="shorts-card__timer-bar">
          <div class="shorts-card__timer-fill" :style="{ width: timerProgress + '%' }"></div>
        </div>
      </div>
    </Transition>

    <!-- 일시정지 표시 -->
    <Transition name="fade">
      <div v-if="paused && !loading && posts.length > 0" class="explore__paused" @click="resume">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="white"><polygon points="5 3 19 12 5 21"/></svg>
      </div>
    </Transition>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { postApi } from '@/services/api';
import { useExploreStore } from '@/stores/explore';
import { getFaviconUrl } from '@/utils/favicon';
import type { RecentPost } from '@/types';

const TIMER_DURATION = 5000;
const TIMER_INTERVAL = 50;

export default defineComponent({
  name: 'ExploreView',
  setup() {
    const posts = ref<RecentPost[]>([]);
    const currentIndex = ref(0);
    const loading = ref(true);
    const paused = ref(false);
    const timerProgress = ref(0);
    const slideDirection = ref('slide-left');

    let timerId: number | null = null;
    let touchStartX = 0;
    let touchStartY = 0;

    const currentPost = computed(() => posts.value[currentIndex.value] || null);

    // 블러 배경 (분위기용, 확대+블러)
    const bgBlurStyle = computed(() => {
      const p = currentPost.value;
      if (!p) return {};
      if (p.thumbnailUrl) {
        return { backgroundImage: `url(${p.thumbnailUrl})` };
      }
      const hash = hashCode(p.blogTitle || '');
      const h1 = Math.abs(hash) % 360;
      const h2 = (h1 + 40) % 360;
      return {
        background: `linear-gradient(135deg, hsl(${h1}, 60%, 25%) 0%, hsl(${h2}, 50%, 15%) 100%)`,
        filter: 'none',
      };
    });

    // 선명한 이미지 (적당한 크기, contain)
    const bgImgStyle = computed(() => {
      const p = currentPost.value;
      if (!p || !p.thumbnailUrl) return { display: 'none' };
      return { backgroundImage: `url(${p.thumbnailUrl})` };
    });

    function hashCode(s: string): number {
      let h = 0;
      for (let i = 0; i < s.length; i++) {
        h = ((h << 5) - h + s.charCodeAt(i)) | 0;
      }
      return h;
    }

    function getFavicon(post: RecentPost) {
      return post.thumbnailUrl || getFaviconUrl(post.blogUrl);
    }

    function formatTime(dateStr: string): string {
      if (!dateStr) return '';
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      const now = new Date();
      const diffMs = now.getTime() - d.getTime();
      const diffMin = Math.floor(diffMs / 60000);
      if (diffMin < 1) return '방금';
      if (diffMin < 60) return `${diffMin}분 전`;
      const diffHours = Math.floor(diffMin / 60);
      if (diffHours < 24) return `${diffHours}시간 전`;
      const diffDays = Math.floor(diffHours / 24);
      if (diffDays < 7) return `${diffDays}일 전`;
      return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
    }

    function startTimer() {
      stopTimer();
      timerProgress.value = 0;
      let elapsed = 0;
      timerId = window.setInterval(() => {
        if (paused.value) return;
        elapsed += TIMER_INTERVAL;
        timerProgress.value = Math.min(100, (elapsed / TIMER_DURATION) * 100);
        if (elapsed >= TIMER_DURATION) {
          next();
        }
      }, TIMER_INTERVAL);
    }

    function stopTimer() {
      if (timerId !== null) {
        clearInterval(timerId);
        timerId = null;
      }
    }

    function next() {
      if (currentIndex.value < posts.value.length - 1) {
        slideDirection.value = 'slide-up';
        currentIndex.value++;
      } else {
        currentIndex.value = 0;
        slideDirection.value = 'slide-up';
      }
      startTimer();
    }

    function prev() {
      if (currentIndex.value > 0) {
        slideDirection.value = 'slide-down';
        currentIndex.value--;
        startTimer();
      }
    }

    function togglePause() {
      paused.value = !paused.value;
    }

    function resume() {
      paused.value = false;
    }

    // Touch / swipe
    function onTouchStart(e: TouchEvent) {
      touchStartX = e.touches[0].clientX;
      touchStartY = e.touches[0].clientY;
    }

    function onTouchEnd(e: TouchEvent) {
      const dx = e.changedTouches[0].clientX - touchStartX;
      const dy = e.changedTouches[0].clientY - touchStartY;

      // 우로 밀면 뒤로가기 (피드로 복귀)
      if (dx > 80 && Math.abs(dx) > Math.abs(dy) * 1.5) {
        window.history.back();
        return;
      }

      // 상하 스와이프로 게시글 전환
      if (Math.abs(dy) > 50 && Math.abs(dy) > Math.abs(dx)) {
        if (dy < 0) next();  // 위로 밀면 다음
        else prev();         // 아래로 밀면 이전
        return;
      }

      // 탭 = 일시정지/재개
      if (Math.abs(dx) < 10 && Math.abs(dy) < 10) {
        togglePause();
      }
    }

    // Keyboard
    function onKeydown(e: KeyboardEvent) {
      if (e.key === 'ArrowRight' || e.key === 'ArrowDown') next();
      else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') prev();
      else if (e.key === ' ') { e.preventDefault(); togglePause(); }
      else if (e.key === 'Escape') window.history.back();
    }

    onMounted(async () => {
      window.addEventListener('keydown', onKeydown);

      const exploreStore = useExploreStore();

      // 스토어에 캐시된 데이터가 있으면 즉시 사용
      if (exploreStore.loaded && exploreStore.posts.length > 0) {
        posts.value = [...exploreStore.posts];
        loading.value = false;
        if (posts.value.length > 0) startTimer();
        return;
      }

      // 캐시 없으면 직접 fetch
      try {
        posts.value = await postApi.getMixedFeed(100);
      } catch (e) {
        console.error('Failed to fetch posts:', e);
      } finally {
        loading.value = false;
      }
      if (posts.value.length > 0) {
        startTimer();
      }
    });

    onUnmounted(() => {
      stopTimer();
      window.removeEventListener('keydown', onKeydown);
    });

    return {
      posts,
      currentIndex,
      currentPost,
      loading,
      paused,
      timerProgress,
      slideDirection,
      bgBlurStyle,
      bgImgStyle,
      getFavicon,
      formatTime,
      next,
      prev,
      resume,
      onTouchStart,
      onTouchEnd,
    };
  },
});
</script>

<style lang="scss" scoped>
.explore {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: #000;
  overflow: hidden;
  user-select: none;

  &__close {
    position: absolute;
    top: 16px;
    right: 16px;
    z-index: 20;
    background: rgba(255, 255, 255, 0.15);
    backdrop-filter: blur(8px);
    border: none;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    cursor: pointer;
    transition: background 0.2s;

    &:hover { background: rgba(255, 255, 255, 0.3); }
  }

  &__loading {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__spinner {
    width: 40px;
    height: 40px;
    border: 3px solid rgba(255, 255, 255, 0.2);
    border-top-color: white;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  &__empty {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: white;
    gap: 16px;

    p { font-size: 16px; opacity: 0.7; }
  }

  &__back-btn {
    padding: 10px 24px;
    border-radius: 20px;
    border: 1px solid rgba(255, 255, 255, 0.3);
    background: transparent;
    color: white;
    font-size: 14px;
    cursor: pointer;

    &:hover { background: rgba(255, 255, 255, 0.1); }
  }

  &__paused {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.3);
    z-index: 15;
    cursor: pointer;

    svg { opacity: 0.8; }
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

// ── 숏츠 카드 ──
.shorts-card {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;

  // 블러 배경: 확대해서 화면 채움 + 블러 + 어둡게
  &__bg-blur {
    position: absolute;
    inset: -20px; // 블러 edge 방지용 확장
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
    filter: blur(30px) brightness(0.4);
    transform: scale(1.1);
  }

  // 선명한 이미지: contain으로 비율 유지, 상단 40% 영역에 표시
  &__bg-img {
    position: absolute;
    top: 5%;
    left: 5%;
    right: 5%;
    bottom: 40%;
    background-size: contain;
    background-position: center;
    background-repeat: no-repeat;
    z-index: 1;
    border-radius: 12px;
  }

  &__overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(
      to bottom,
      rgba(0, 0, 0, 0.1) 0%,
      transparent 25%,
      transparent 45%,
      rgba(0, 0, 0, 0.6) 70%,
      rgba(0, 0, 0, 0.85) 100%
    );
    z-index: 2;
  }

  &__content {
    position: relative;
    z-index: 5;
    padding: 24px 24px 80px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  &__blog {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__favicon {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
    background: rgba(255, 255, 255, 0.1);
    border: 2px solid rgba(255, 255, 255, 0.2);
  }

  &__blog-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__blog-name {
    font-size: 15px;
    font-weight: 700;
    color: white;
  }

  &__author {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.6);
  }

  &__title {
    font-size: 22px;
    font-weight: 800;
    color: white;
    line-height: 1.4;
    margin: 0;
    display: -webkit-box;
    -webkit-line-clamp: 4;
    -webkit-box-orient: vertical;
    overflow: hidden;
    letter-spacing: -0.3px;

    @media (max-width: 480px) {
      font-size: 19px;
    }
  }

  &__time {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.5);
  }

  &__visit {
    display: inline-flex;
    align-items: center;
    align-self: flex-start;
    gap: 4px;
    padding: 10px 24px;
    border-radius: 24px;
    background: rgba(255, 255, 255, 0.15);
    backdrop-filter: blur(8px);
    color: white;
    font-size: 14px;
    font-weight: 600;
    text-decoration: none;
    transition: background 0.2s;

    &:hover { background: rgba(255, 255, 255, 0.25); }
  }

  // 상하 네비 버튼
  &__nav {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    z-index: 10;
    width: 44px;
    height: 44px;
    border-radius: 50%;
    border: none;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(4px);
    color: white;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background 0.2s;
    opacity: 0.6;

    &:hover { opacity: 1; background: rgba(255, 255, 255, 0.2); }

    &--up { top: 16px; }
    &--down { bottom: 16px; }

    @media (max-width: 480px) {
      width: 36px;
      height: 36px;
      opacity: 0.4;
    }
  }

  // 하단 프로그레스 바
  &__timer-bar {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: rgba(255, 255, 255, 0.1);
    z-index: 10;
  }

  &__timer-fill {
    height: 100%;
    background: linear-gradient(90deg, #007bff, #00c6ff);
    transition: width 0.05s linear;
    border-radius: 0 2px 2px 0;
  }
}

// ── 슬라이드 트랜지션 (상하) ──
.slide-up-enter-active,
.slide-up-leave-active,
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from { transform: translateY(100%); opacity: 0; }
.slide-up-leave-to { transform: translateY(-30%); opacity: 0; }
.slide-down-enter-from { transform: translateY(-100%); opacity: 0; }
.slide-down-leave-to { transform: translateY(30%); opacity: 0; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
