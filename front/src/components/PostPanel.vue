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
              <span class="post-panel__blog-url">{{ displayUrl }}</span>
            </div>
          </div>
          <button class="post-panel__close" @click="$emit('close')" aria-label="닫기">
            ✕
          </button>
        </div>

        <!-- 블로그 정보 -->
        <div class="post-panel__body">
          <!-- 통계 카드 -->
          <div class="post-panel__stats">
            <div class="post-panel__stat-card">
              <span class="post-panel__stat-icon">👁</span>
              <div class="post-panel__stat-detail">
                <span class="post-panel__stat-value">{{ blogInfo.visitCnt ?? 0 }}</span>
                <span class="post-panel__stat-label">방문 횟수</span>
              </div>
            </div>
            <div class="post-panel__stat-card">
              <span class="post-panel__stat-icon">📝</span>
              <div class="post-panel__stat-detail">
                <span class="post-panel__stat-value">{{ blogInfo.newPostsCnt ?? 0 }}</span>
                <span class="post-panel__stat-label">새 포스트</span>
              </div>
            </div>
          </div>

          <!-- 최근 업데이트 -->
          <div v-if="blogInfo.publishedDateTime" class="post-panel__last-update">
            <span class="post-panel__update-label">최근 업데이트</span>
            <span class="post-panel__update-value">{{ formatDate(blogInfo.publishedDateTime) }}</span>
          </div>

          <!-- 블로그 URL 정보 -->
          <div class="post-panel__url-section">
            <span class="post-panel__url-label">블로그 주소</span>
            <a
              :href="blogInfo.blogUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="post-panel__url-link"
            >
              {{ blogInfo.blogUrl }}
              <span class="post-panel__external-icon">↗</span>
            </a>
          </div>

          <!-- 방문 버튼 -->
          <div class="post-panel__actions">
            <button class="post-panel__visit-btn" @click="visitBlog">
              블로그 방문하기
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { getFaviconUrl } from '@/utils/favicon';
import type { BlogInfo } from '@/types';

const props = defineProps<{
  blogInfo: BlogInfo | null;
}>();

defineEmits<{
  (e: 'close'): void;
}>();

const avatarUrl = computed(() => {
  if (!props.blogInfo) return '';
  return (
    props.blogInfo.thumbnailUrl ||
    getFaviconUrl(props.blogInfo.blogUrl)
  );
});

const displayUrl = computed(() => {
  if (!props.blogInfo?.blogUrl) return '';
  try {
    const url = new URL(props.blogInfo.blogUrl);
    return url.hostname;
  } catch {
    return props.blogInfo.blogUrl;
  }
});

const visitBlog = () => {
  if (props.blogInfo?.blogUrl) {
    window.open(props.blogInfo.blogUrl, '_blank', 'noopener,noreferrer');
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
  if (diffDays === 0) return '오늘';
  if (diffDays === 1) return '어제';
  if (diffDays < 7) return `${diffDays}일 전`;
  if (diffDays < 30) return `${Math.floor(diffDays / 7)}주 전`;
  return d.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' });
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
    min-width: 0;
    flex: 1;
  }

  &__blog-meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    object-fit: cover;
    border: 2px solid #eee;
    background: #f5f5f5;
    flex-shrink: 0;
  }

  &__nick {
    font-size: 16px;
    font-weight: 700;
    color: #222;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__blog-url {
    font-size: 12px;
    color: #999;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
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
    flex-shrink: 0;

    &:hover {
      background: #f0f0f0;
      color: #333;
    }
  }

  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 24px;
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  &__stats {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  &__stat-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background: #f8f9fa;
    border-radius: 14px;
    border: 1px solid #f0f0f0;
  }

  &__stat-icon {
    font-size: 24px;
    flex-shrink: 0;
  }

  &__stat-detail {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__stat-value {
    font-size: 20px;
    font-weight: 700;
    color: #222;
    line-height: 1.2;
  }

  &__stat-label {
    font-size: 11px;
    color: #999;
    font-weight: 500;
  }

  &__last-update {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 16px;
    background: #f8f9fa;
    border-radius: 12px;
    border: 1px solid #f0f0f0;
  }

  &__update-label {
    font-size: 13px;
    color: #888;
    font-weight: 500;
  }

  &__update-value {
    font-size: 13px;
    color: #444;
    font-weight: 600;
  }

  &__url-section {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__url-label {
    font-size: 12px;
    color: #999;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  &__url-link {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #007bff;
    text-decoration: none;
    word-break: break-all;
    line-height: 1.5;
    transition: color 0.2s;

    &:hover {
      color: #0056b3;
      text-decoration: underline;
    }
  }

  &__external-icon {
    flex-shrink: 0;
    font-size: 14px;
  }

  &__actions {
    margin-top: auto;
    padding-top: 16px;
  }

  &__visit-btn {
    width: 100%;
    padding: 14px 24px;
    border: none;
    border-radius: 14px;
    background: #007bff;
    color: #fff;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;

    &:hover {
      background: #0056b3;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(0, 123, 255, 0.3);
    }

    &:active {
      transform: translateY(0);
    }
  }
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
