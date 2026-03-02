import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { subscriptionApi } from '@/services/api';
import type { SubscriptionResponse } from '@/types';

export const useSubscriptionStore = defineStore('subscription', () => {
  // ─── State ──────────────────────────────────────────────
  const closeBlogs = ref<SubscriptionResponse[]>([]);
  const allBlogs = ref<SubscriptionResponse[]>([]);
  const isLoading = ref(false);
  const currentPage = ref(0);
  const hasMore = ref(true);
  const pageSize = 20;

  // ─── Getters ────────────────────────────────────────────
  const totalSubscriptions = computed(() => allBlogs.value.length);
  const totalNewPosts = computed(() =>
    closeBlogs.value.reduce((sum, b) => sum + (b.newPostsCnt ?? 0), 0),
  );

  // ─── Actions ────────────────────────────────────────────
  async function fetchCloseBlogs() {
    try {
      closeBlogs.value = await subscriptionApi.getCloseBlogs();
    } catch (e) {
      console.error('Failed to fetch close blogs:', e);
    }
  }

  async function fetchAllBlogs(reset = false) {
    if (isLoading.value) return;
    if (reset) {
      currentPage.value = 0;
      allBlogs.value = [];
      hasMore.value = true;
    }
    if (!hasMore.value) return;

    isLoading.value = true;
    try {
      const data = await subscriptionApi.getBlogs(currentPage.value, pageSize);
      if (data.length < pageSize) hasMore.value = false;
      allBlogs.value.push(...data);
      currentPage.value++;
    } catch (e) {
      console.error('Failed to fetch blogs:', e);
    } finally {
      isLoading.value = false;
    }
  }

  async function subscribe(rssUri: string) {
    await subscriptionApi.subscribe({ rssUri });
    // 구독 후 목록 갱신
    await fetchCloseBlogs();
  }

  async function unsubscribe(subscriptionId: number) {
    await subscriptionApi.unsubscribe(subscriptionId);
    closeBlogs.value = closeBlogs.value.filter((b) => b.subscriptionId !== subscriptionId);
    allBlogs.value = allBlogs.value.filter((b) => b.subscriptionId !== subscriptionId);
  }

  function $reset() {
    closeBlogs.value = [];
    allBlogs.value = [];
    isLoading.value = false;
    currentPage.value = 0;
    hasMore.value = true;
  }

  return {
    closeBlogs,
    allBlogs,
    isLoading,
    hasMore,
    totalSubscriptions,
    totalNewPosts,
    fetchCloseBlogs,
    fetchAllBlogs,
    subscribe,
    unsubscribe,
    $reset,
  };
});
