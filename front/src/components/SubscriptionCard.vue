<template>
  <div class="sub-card" @click="$emit('select', subscription)">
    <div class="sub-card__avatar-wrap">
      <img class="sub-card__avatar" :src="avatarUrl" :alt="subscription.nickName" loading="lazy" />
      <span v-if="subscription.newPostsCnt > 0" class="sub-card__new-badge">
        {{ subscription.newPostsCnt }}
      </span>
    </div>

    <div class="sub-card__body">
      <div class="sub-card__name">{{ subscription.nickName }}</div>
      <div class="sub-card__url">{{ displayUrl }}</div>
      <div class="sub-card__meta">
        <span v-if="subscription.visitCnt" class="sub-card__stat">
          👁 {{ subscription.visitCnt }}회 방문
        </span>
        <span v-if="subscription.publishedDateTime" class="sub-card__stat">
          {{ formatDate(subscription.publishedDateTime) }}
        </span>
      </div>
    </div>

    <button
      class="sub-card__delete"
      @click.stop="handleDelete"
      :disabled="deleting"
      aria-label="구독 해제"
      title="구독 해제"
    >
      {{ deleting ? '...' : '✕' }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { SubscriptionResponse } from '@/types';

const props = defineProps<{
  subscription: SubscriptionResponse;
}>();

const emit = defineEmits<{
  (e: 'select', sub: SubscriptionResponse): void;
  (e: 'delete', subscriptionId: number): void;
}>();

const deleting = ref(false);

const avatarUrl = computed(() => {
  return (
    props.subscription.thumbnailUrl ||
    `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(props.subscription.nickName || 'B')}`
  );
});

const displayUrl = computed(() => {
  try {
    const url = new URL(props.subscription.uri);
    return url.hostname;
  } catch {
    return props.subscription.uri;
  }
});

const handleDelete = async () => {
  if (deleting.value) return;
  deleting.value = true;
  emit('delete', props.subscription.subscriptionId);
};

const formatDate = (dateStr: string): string => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  const now = new Date();
  const diffMs = now.getTime() - d.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  if (diffDays === 0) return '오늘';
  if (diffDays === 1) return '어제';
  if (diffDays < 7) return `${diffDays}일 전`;
  return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
};
</script>

<style lang="scss" scoped>
.sub-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #e0e0e0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
    transform: translateY(-2px);
  }

  &__avatar-wrap {
    position: relative;
    flex-shrink: 0;
  }

  &__avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    object-fit: cover;
    background: #f5f5f5;
    border: 2px solid #eee;
  }

  &__new-badge {
    position: absolute;
    top: -4px;
    right: -4px;
    min-width: 20px;
    height: 20px;
    padding: 0 5px;
    border-radius: 10px;
    background: #ef4444;
    color: #fff;
    font-size: 11px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 2px solid #fff;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 15px;
    font-weight: 600;
    color: #222;
    margin-bottom: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__url {
    font-size: 12px;
    color: #999;
    margin-bottom: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__meta {
    display: flex;
    gap: 12px;
  }

  &__stat {
    font-size: 12px;
    color: #aaa;
  }

  &__delete {
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    border-radius: 8px;
    border: none;
    background: transparent;
    color: #ccc;
    font-size: 14px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
    opacity: 0;

    .sub-card:hover & {
      opacity: 1;
    }

    &:hover {
      background: #fef2f2;
      color: #ef4444;
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}
</style>
