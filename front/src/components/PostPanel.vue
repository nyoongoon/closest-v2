<template>
  <Transition name="slide-panel">
    <div class="post-panel" v-if="blogInfo">
      <!-- 오버레이 -->
      <div class="post-panel__overlay" @click="$emit('close')" />

      <!-- 패널 본문 -->
      <div class="post-panel__drawer">
        <!-- 헤더 -->
        <div class="post-panel__header">
          <div class="post-panel__blog-info">
            <img
              class="post-panel__avatar"
              :src="avatarUrl"
              :alt="blogInfo.nickName"
              loading="lazy"
            />
            <div class="post-panel__blog-meta">
              <span class="post-panel__nick">{{ blogInfo.nickName }}</span>
              <span class="post-panel__post-count">{{ posts.length }}개 포스트</span>
            </div>
          </div>
          <button class="post-panel__close" @click="$emit('close')" aria-label="닫기">
            ✕
          </button>
        </div>

        <!-- 포스트 목록 -->
        <div class="post-panel__body">
          <div v-if="isLoading" class="post-panel__spinner">
            <span class="spinner" />
          </div>

          <div v-else-if="error" class="post-panel__error">
            <span class="post-panel__error-icon">!</span>
            <p>{{ error }}</p>
            <button class="post-panel__retry" @click="retry">다시 시도</button>
          </div>

          <ul v-else-if="posts.length > 0" class="post-panel__list">
            <li
              v-for="post in posts"
              :key="post.id"
              class="post-panel__item"
              @click="openPost(post)"
            >
              <div class="post-panel__item-content">
                <p class="post-panel__title">{{ post.title }}</p>
                <span class="post-panel__date">{{ formatDate(post.publishedAt) }}</span>
              </div>
              <button
                class="post-panel__like"
                :class="{ 'post-panel__like--active': likedPosts.has(post.id) }"
                @click.stop="toggleLike(post.id)"
                :disabled="likingPost === post.id"
                aria-label="좋아요"
              >
                {{ likedPosts.has(post.id) ? '♥' : '♡' }}
              </button>
            </li>
          </ul>

          <div v-else class="post-panel__empty">
            <span class="post-panel__empty-icon">📭</span>
            <p>포스트가 없습니다.</p>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { subscriptionApi, postApi } from '@/services/api';
import { useToast } from '@/composables/useToast';
import type { BlogInfo, Post } from '@/types';

const props = defineProps<{
  blogInfo: BlogInfo | null;
}>();

defineEmits<{
  (e: 'close'): void;
}>();

const { showToast } = useToast();

const posts = ref<Post[]>([]);
const isLoading = ref(false);
const error = ref<string | null>(null);
const likedPosts = ref<Set<number>>(new Set());
const likingPost = ref<number | null>(null);

const avatarUrl = computed(() => {
  if (!props.blogInfo) return '';
  return (
    props.blogInfo.thumbnailUrl ||
    `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(props.blogInfo.nickName || 'B')}`
  );
});

const fetchPosts = async (info: BlogInfo) => {
  isLoading.value = true;
  error.value = null;
  posts.value = [];

  try {
    // 방문 추적
    if (info.subscriptionId) {
      subscriptionApi.visit(info.subscriptionId).catch(() => {});
    }

    if (info.subscriptionId) {
      posts.value = await subscriptionApi.getPosts(info.subscriptionId);
    } else {
      posts.value = await subscriptionApi.getPostsByBlogUrl(info.blogUrl);
    }
  } catch (err) {
    error.value = '포스트를 불러오지 못했습니다.';
    console.error(err);
  } finally {
    isLoading.value = false;
  }
};

const retry = () => {
  if (props.blogInfo) fetchPosts(props.blogInfo);
};

watch(
  () => props.blogInfo,
  (info) => {
    if (info) fetchPosts(info);
    else posts.value = [];
  },
  { immediate: true }
);

const openPost = (post: Post) => {
  // 포스트별 방문 추적
  if (props.blogInfo?.subscriptionId) {
    subscriptionApi.visitPost(props.blogInfo.subscriptionId, post.link).catch(() => {});
  }
  window.open(post.link, '_blank', 'noopener,noreferrer');
};

const toggleLike = async (postId: number) => {
  if (likingPost.value === postId) return;
  likingPost.value = postId;
  try {
    await postApi.like(postId);
    if (likedPosts.value.has(postId)) {
      likedPosts.value.delete(postId);
    } else {
      likedPosts.value.add(postId);
    }
  } catch {
    showToast('좋아요 처리에 실패했습니다.', 'error');
  } finally {
    likingPost.value = null;
  }
};

const formatDate = (dateStr: string): string => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  const now = new Date();
  const diffMs = now.getTime() - d.getTime();
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  if (diffHours < 1) return '방금 전';
  if (diffHours < 24) return `${diffHours}시간 전`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays}일 전`;
  return d.toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' });
};
</script>

<style lang="scss" scoped>
.post-panel {
  position: fixed;
  inset: 0;
  z-index: 2000;

  &__overlay {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.25);
    backdrop-filter: blur(2px);
  }

  &__drawer {
    position: absolute;
    top: 0;
    right: 0;
    width: 380px;
    height: 100%;
    background: #fff;
    box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 24px;
    border-bottom: 1px solid #f0f0f0;
    flex-shrink: 0;
  }

  &__blog-info {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__blog-meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__avatar {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    object-fit: cover;
    border: 2px solid #eee;
    background: #f5f5f5;
  }

  &__nick {
    font-size: 16px;
    font-weight: 700;
    color: #222;
  }

  &__post-count {
    font-size: 12px;
    color: #aaa;
  }

  &__close {
    background: none;
    border: none;
    font-size: 18px;
    color: #999;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: 8px;
    transition: background 0.2s, color 0.2s;

    &:hover {
      background: #f0f0f0;
      color: #333;
    }
  }

  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 16px 0;
  }

  &__list {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  &__item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 24px;
    cursor: pointer;
    border-bottom: 1px solid #f5f5f5;
    transition: background 0.15s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: #f9f9f9;
    }
  }

  &__item-content {
    flex: 1;
    min-width: 0;
  }

  &__title {
    margin: 0 0 4px;
    font-size: 14px;
    font-weight: 500;
    color: #222;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__date {
    font-size: 12px;
    color: #aaa;
  }

  &__like {
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    border: none;
    background: transparent;
    font-size: 18px;
    cursor: pointer;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
    color: #ccc;

    &:hover {
      background: #fef2f2;
      color: #ef4444;
    }

    &--active {
      color: #ef4444;
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  &__retry {
    margin-top: 12px;
    padding: 8px 20px;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    background: #fff;
    color: #555;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #f5f5f5;
    }
  }

  &__spinner {
    display: flex;
    justify-content: center;
    padding: 48px 0;
  }

  &__error {
    text-align: center;
    padding: 48px 24px;
    font-size: 14px;
    color: #e55;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__error-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: #fef2f2;
    color: #ef4444;
    font-size: 18px;
    font-weight: 700;
    margin-bottom: 12px;
  }

  &__empty {
    text-align: center;
    padding: 48px 24px;
    font-size: 14px;
    color: #aaa;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__empty-icon {
    font-size: 36px;
    margin-bottom: 8px;
    opacity: 0.6;
  }
}

.spinner {
  display: inline-block;
  width: 32px;
  height: 32px;
  border: 3px solid #eee;
  border-top-color: #007bff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

// 슬라이드 트랜지션
.slide-panel-enter-active,
.slide-panel-leave-active {
  transition: opacity 0.25s ease;

  .post-panel__drawer {
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

.slide-panel-enter-from,
.slide-panel-leave-to {
  opacity: 0;

  .post-panel__drawer {
    transform: translateX(100%);
  }
}

// 모바일 대응
@media (max-width: 767px) {
  .post-panel__drawer {
    width: 100%;
  }
}
</style>
