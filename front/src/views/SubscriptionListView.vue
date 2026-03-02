<template>
  <div class="subs-view">
    <div class="subs-view__header">
      <h1 class="subs-view__title">구독 목록</h1>
      <p class="subs-view__summary">
        총 <strong>{{ subscriptionStore.allBlogs.length }}</strong>개 구독 중
        <template v-if="subscriptionStore.totalNewPosts > 0">
          · 새 포스트 <strong>{{ subscriptionStore.totalNewPosts }}</strong>개
        </template>
      </p>
    </div>

    <!-- 검색/필터 -->
    <div class="subs-view__toolbar">
      <div class="subs-view__search">
        <span class="subs-view__search-icon">🔍</span>
        <input
          v-model="searchQuery"
          type="text"
          class="subs-view__search-input"
          placeholder="블로그 검색..."
        />
      </div>
      <div class="subs-view__sort">
        <select v-model="sortBy" class="subs-view__select">
          <option value="recent">최신 포스트순</option>
          <option value="visits">방문순</option>
          <option value="name">이름순</option>
          <option value="newPosts">새 글순</option>
        </select>
      </div>
    </div>

    <!-- 로딩 스켈레톤 -->
    <LoadingSkeleton v-if="initialLoading" :count="6" />

    <!-- 빈 상태 -->
    <EmptyState
      v-else-if="filteredBlogs.length === 0 && !searchQuery"
      icon="📡"
      title="구독 중인 블로그가 없습니다"
      description="오른쪽 상단의 '+ 구독' 버튼으로 관심 있는 블로그를 추가해보세요."
    />

    <!-- 검색 결과 없음 -->
    <EmptyState
      v-else-if="filteredBlogs.length === 0 && searchQuery"
      icon="🔍"
      title="검색 결과가 없습니다"
      :description="`'${searchQuery}'에 해당하는 블로그를 찾지 못했습니다.`"
    />

    <!-- 구독 목록 -->
    <div v-else class="subs-view__list">
      <TransitionGroup name="list" tag="div" class="subs-view__grid">
        <SubscriptionCard
          v-for="blog in filteredBlogs"
          :key="blog.subscriptionId"
          :subscription="blog"
          @select="handleSelect"
          @delete="handleDelete"
        />
      </TransitionGroup>

      <!-- 더 보기 -->
      <div v-if="subscriptionStore.hasMore" class="subs-view__load-more">
        <button
          class="subs-view__load-btn"
          :disabled="subscriptionStore.isLoading"
          @click="loadMore"
        >
          {{ subscriptionStore.isLoading ? '불러오는 중...' : '더 보기' }}
        </button>
      </div>
    </div>

    <!-- PostPanel -->
    <PostPanel v-if="selectedBlog" :blogInfo="selectedBlog" @close="selectedBlog = null" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useSubscriptionStore } from '@/stores/subscription';
import { useToast } from '@/composables/useToast';
import type { SubscriptionResponse, BlogInfo } from '@/types';
import SubscriptionCard from '@/components/SubscriptionCard.vue';
import EmptyState from '@/components/EmptyState.vue';
import LoadingSkeleton from '@/components/LoadingSkeleton.vue';
import PostPanel from '@/components/PostPanel.vue';

const subscriptionStore = useSubscriptionStore();
const { showToast } = useToast();

const searchQuery = ref('');
const sortBy = ref<'recent' | 'visits' | 'name' | 'newPosts'>('recent');
const selectedBlog = ref<BlogInfo | null>(null);
const initialLoading = ref(true);

const filteredBlogs = computed(() => {
  let list = [...subscriptionStore.allBlogs];

  // 검색 필터
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase();
    list = list.filter(
      (b) =>
        b.nickName?.toLowerCase().includes(q) || b.uri?.toLowerCase().includes(q),
    );
  }

  // 정렬
  switch (sortBy.value) {
    case 'recent':
      list.sort((a, b) => {
        const da = a.publishedDateTime ? new Date(a.publishedDateTime).getTime() : 0;
        const db = b.publishedDateTime ? new Date(b.publishedDateTime).getTime() : 0;
        return db - da;
      });
      break;
    case 'visits':
      list.sort((a, b) => (b.visitCnt ?? 0) - (a.visitCnt ?? 0));
      break;
    case 'name':
      list.sort((a, b) => (a.nickName ?? '').localeCompare(b.nickName ?? ''));
      break;
    case 'newPosts':
      list.sort((a, b) => (b.newPostsCnt ?? 0) - (a.newPostsCnt ?? 0));
      break;
  }

  return list;
});

const handleSelect = (sub: SubscriptionResponse) => {
  selectedBlog.value = {
    subscriptionId: sub.subscriptionId,
    blogUrl: sub.uri ?? '',
    nickName: sub.nickName ?? '',
    thumbnailUrl: sub.thumbnailUrl ?? undefined,
  };
};

const handleDelete = async (subscriptionId: number) => {
  try {
    await subscriptionStore.unsubscribe(subscriptionId);
    showToast('구독이 해제되었습니다.', 'success');
  } catch {
    showToast('구독 해제에 실패했습니다.', 'error');
  }
};

const loadMore = () => {
  subscriptionStore.fetchAllBlogs();
};

onMounted(async () => {
  await subscriptionStore.fetchAllBlogs(true);
  initialLoading.value = false;
});
</script>

<style lang="scss" scoped>
.subs-view {
  max-width: 720px;
  margin: 0 auto;
  padding: 80px 24px 40px;

  &__header {
    margin-bottom: 24px;
  }

  &__title {
    font-size: 28px;
    font-weight: 800;
    color: #111;
    margin: 0 0 6px;
    letter-spacing: -0.5px;
  }

  &__summary {
    font-size: 14px;
    color: #888;
    margin: 0;

    strong {
      color: #007bff;
      font-weight: 600;
    }
  }

  &__toolbar {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;

    @media (max-width: 520px) {
      flex-direction: column;
    }
  }

  &__search {
    flex: 1;
    display: flex;
    align-items: center;
    background: #f8f8f8;
    border: 1px solid #eee;
    border-radius: 12px;
    padding: 0 14px;
    transition: border-color 0.2s;

    &:focus-within {
      border-color: #007bff;
      background: #fff;
    }
  }

  &__search-icon {
    font-size: 14px;
    opacity: 0.5;
    margin-right: 8px;
  }

  &__search-input {
    flex: 1;
    border: none;
    background: transparent;
    padding: 10px 0;
    font-size: 14px;
    color: #333;
    outline: none;

    &::placeholder {
      color: #bbb;
    }
  }

  &__select {
    padding: 10px 14px;
    border: 1px solid #eee;
    border-radius: 12px;
    background: #f8f8f8;
    font-size: 13px;
    color: #555;
    cursor: pointer;
    outline: none;

    &:focus {
      border-color: #007bff;
    }
  }

  &__grid {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__load-more {
    display: flex;
    justify-content: center;
    padding: 24px 0;
  }

  &__load-btn {
    padding: 10px 32px;
    border: 1px solid #e0e0e0;
    border-radius: 12px;
    background: #fff;
    color: #555;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;

    &:hover:not(:disabled) {
      background: #f5f5f5;
      border-color: #ccc;
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
}

// 리스트 트랜지션
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.list-move {
  transition: transform 0.3s ease;
}
</style>
