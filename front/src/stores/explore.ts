import { defineStore } from 'pinia';
import { ref } from 'vue';
import { postApi } from '@/services/api';
import type { RecentPost } from '@/types';

/**
 * Explore 스토어 — MainView에서 미리 데이터 + 이미지를 로드해서
 * ExploreView 진입 시 즉시 표시할 수 있도록 캐싱
 */
export const useExploreStore = defineStore('explore', () => {
  const posts = ref<RecentPost[]>([]);
  const loading = ref(false);
  const loaded = ref(false);
  const imagePreloaded = ref(false);
  const preloadProgress = ref(0); // 0~100

  // 캐시 유효 시간 (5분)
  const CACHE_TTL = 5 * 60 * 1000;
  let lastFetchTime = 0;

  /** 데이터 fetch + 이미지 프리로드 */
  async function preload() {
    // 이미 로딩 중이거나 캐시가 유효하면 스킵
    if (loading.value) return;
    if (loaded.value && Date.now() - lastFetchTime < CACHE_TTL) return;

    loading.value = true;
    preloadProgress.value = 0;

    try {
      // 1. API에서 데이터 가져오기
      const data = await postApi.getMixedFeed(100);
      posts.value = data;
      loaded.value = true;
      lastFetchTime = Date.now();
      preloadProgress.value = 30;

      // 2. 썸네일 이미지 프리로드 (브라우저 캐시에 저장)
      await preloadImages(data);
      imagePreloaded.value = true;
      preloadProgress.value = 100;
    } catch (e) {
      console.error('[Explore preload] 실패:', e);
    } finally {
      loading.value = false;
    }
  }

  /** 강제 새로고침 */
  async function refresh() {
    loaded.value = false;
    imagePreloaded.value = false;
    await preload();
  }

  /** 이미지 프리로드 (병렬, 최대 6개 동시) */
  async function preloadImages(items: RecentPost[]) {
    const urls = items
      .map((p) => p.thumbnailUrl)
      .filter((url): url is string => !!url);

    if (urls.length === 0) {
      preloadProgress.value = 100;
      return;
    }

    const CONCURRENCY = 6;
    let completed = 0;

    const loadOne = (url: string): Promise<void> =>
      new Promise((resolve) => {
        const img = new Image();
        img.onload = () => {
          completed++;
          // 30~100 구간에 매핑
          preloadProgress.value = 30 + Math.round((completed / urls.length) * 70);
          resolve();
        };
        img.onerror = () => {
          completed++;
          preloadProgress.value = 30 + Math.round((completed / urls.length) * 70);
          resolve(); // 실패해도 계속 진행
        };
        img.src = url;
      });

    // 청크로 나눠서 병렬 로드
    for (let i = 0; i < urls.length; i += CONCURRENCY) {
      const chunk = urls.slice(i, i + CONCURRENCY);
      await Promise.all(chunk.map(loadOne));
    }
  }

  return {
    posts,
    loading,
    loaded,
    imagePreloaded,
    preloadProgress,
    preload,
    refresh,
  };
});
