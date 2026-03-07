<template>
  <div class="discover">
    <!-- 헤더 -->
    <header class="discover__header">
      <button class="discover__back" @click="$router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <h1 class="discover__title">블로그 탐색</h1>
    </header>

    <!-- 검색바 -->
    <div class="discover__search">
      <svg class="discover__search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
      </svg>
      <input
        type="text"
        v-model="searchQuery"
        placeholder="블로그 검색..."
        class="discover__search-input"
        @keyup.enter="handleSearch"
      />
      <button v-if="searchQuery" class="discover__search-clear" @click="clearSearch">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
    </div>

    <!-- 카테고리 탭 -->
    <div class="discover__categories" ref="catScrollRef">
      <button
        class="cat-chip"
        :class="{ 'cat-chip--active': !activeCategory }"
        @click="selectCategory(null)"
      >
        전체
      </button>
      <button
        v-for="cat in categories"
        :key="cat.slug"
        class="cat-chip"
        :class="{ 'cat-chip--active': activeCategory === cat.slug }"
        @click="selectCategory(cat.slug)"
      >
        <span class="cat-chip__icon">{{ cat.icon }}</span>
        {{ cat.name }}
        <span v-if="cat.blogCount > 0" class="cat-chip__count">{{ cat.blogCount }}</span>
      </button>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="discover__loading">
      <div class="discover__spinner"></div>
    </div>

    <!-- 블로그 리스트 -->
    <div v-else class="discover__list">
      <div v-if="blogs.length === 0" class="discover__empty">
        <p>{{ searchQuery ? '검색 결과가 없습니다' : '아직 수집된 블로그가 없습니다' }}</p>
        <p class="discover__empty-sub">크롤러가 인기 블로그를 수집 중입니다. 잠시 후 새로고침해보세요.</p>
      </div>

      <div
        v-for="blog in blogs"
        :key="blog.blogId"
        class="blog-card"
        @click="openBlog(blog)"
      >
        <img
          class="blog-card__thumb"
          :src="getBlogThumb(blog)"
          :alt="blog.blogTitle"
          loading="lazy"
        />
        <div class="blog-card__body">
          <div class="blog-card__title-row">
            <span class="blog-card__title">{{ blog.blogTitle }}</span>
            <span class="blog-card__platform">{{ platformLabel(blog.platform) }}</span>
          </div>
          <span v-if="blog.author" class="blog-card__author">{{ blog.author }}</span>
          <div class="blog-card__meta">
            <span class="blog-card__posts">{{ blog.postCount }}개 글</span>
            <span v-if="blog.score > 0" class="blog-card__score">
              {{ Math.round(blog.score) }}점
            </span>
          </div>
          <div v-if="blog.tags && blog.tags.length > 0" class="blog-card__tags">
            <span v-for="tag in blog.tags.slice(0, 3)" :key="tag" class="blog-card__tag" @click.stop="selectTag(tag)">
              #{{ tag }}
            </span>
          </div>
        </div>
        <button class="blog-card__subscribe" @click.stop="subscribeBlog(blog)">
          구독
        </button>
      </div>

      <!-- 더보기 -->
      <button v-if="hasMore" class="discover__more" @click="loadMore" :disabled="loadingMore">
        {{ loadingMore ? '불러오는 중...' : '더보기' }}
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { discoverApi, subscriptionApi } from '@/services/api';
import type { DiscoverCategory, DiscoverBlog } from '@/services/api';
import { getFaviconUrl } from '@/utils/favicon';
import { useToast } from '@/composables/useToast';

export default defineComponent({
  name: 'DiscoverView',
  setup() {
    const router = useRouter();
    const { showToast } = useToast();

    const categories = ref<DiscoverCategory[]>([]);
    const blogs = ref<DiscoverBlog[]>([]);
    const loading = ref(true);
    const loadingMore = ref(false);
    const hasMore = ref(false);
    const currentPage = ref(0);
    const activeCategory = ref<string | null>(null);
    const activeTag = ref<string | null>(null);
    const searchQuery = ref('');
    const catScrollRef = ref<HTMLElement | null>(null);

    async function fetchCategories() {
      try {
        categories.value = await discoverApi.getCategories();
      } catch (e) {
        console.error('Failed to fetch categories:', e);
      }
    }

    async function fetchBlogs(reset = true) {
      if (reset) {
        currentPage.value = 0;
        loading.value = true;
      } else {
        loadingMore.value = true;
      }

      try {
        const params: any = { page: currentPage.value, size: 20 };
        if (activeCategory.value) params.category = activeCategory.value;
        if (activeTag.value) params.tag = activeTag.value;
        if (searchQuery.value) params.q = searchQuery.value;

        const res = await discoverApi.getBlogs(params);
        if (reset) {
          blogs.value = res.blogs || [];
        } else {
          blogs.value.push(...(res.blogs || []));
        }
        hasMore.value = res.hasMore;
      } catch (e) {
        console.error('Failed to fetch blogs:', e);
      } finally {
        loading.value = false;
        loadingMore.value = false;
      }
    }

    function selectCategory(slug: string | null) {
      activeCategory.value = slug;
      activeTag.value = null;
      searchQuery.value = '';
      fetchBlogs();
    }

    function selectTag(tag: string) {
      activeTag.value = tag;
      activeCategory.value = null;
      searchQuery.value = '';
      fetchBlogs();
    }

    function handleSearch() {
      if (!searchQuery.value.trim()) return;
      activeCategory.value = null;
      activeTag.value = null;
      fetchBlogs();
    }

    function clearSearch() {
      searchQuery.value = '';
      fetchBlogs();
    }

    function loadMore() {
      currentPage.value++;
      fetchBlogs(false);
    }

    function openBlog(blog: DiscoverBlog) {
      if (blog.blogUrl) {
        window.open(blog.blogUrl, '_blank', 'noopener,noreferrer');
      }
    }

    async function subscribeBlog(blog: DiscoverBlog) {
      try {
        await subscriptionApi.subscribe({ rssUri: blog.rssUrl });
        showToast(`${blog.blogTitle} 구독 완료!`, 'success');
      } catch (e) {
        showToast('구독에 실패했습니다. 로그인이 필요할 수 있습니다.', 'error');
      }
    }

    function getBlogThumb(blog: DiscoverBlog) {
      return blog.thumbnailUrl || getFaviconUrl(blog.blogUrl);
    }

    function platformLabel(platform: string) {
      const map: Record<string, string> = {
        tistory: '티스토리',
        velog: 'Velog',
        naver: '네이버',
        brunch: '브런치',
        medium: 'Medium',
        github: 'GitHub',
      };
      return map[platform] || '';
    }

    onMounted(async () => {
      await fetchCategories();
      await fetchBlogs();
    });

    return {
      categories,
      blogs,
      loading,
      loadingMore,
      hasMore,
      activeCategory,
      activeTag,
      searchQuery,
      catScrollRef,
      selectCategory,
      selectTag,
      handleSearch,
      clearSearch,
      loadMore,
      openBlog,
      subscribeBlog,
      getBlogThumb,
      platformLabel,
    };
  },
});
</script>

<style lang="scss" scoped>
.discover {
  min-height: 100vh;
  background: #fafbff;
  padding-bottom: 40px;

  &__header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 20px;
    background: #fff;
    border-bottom: 1px solid #f0f0f0;
    position: sticky;
    top: 0;
    z-index: 10;
  }

  &__back {
    background: none;
    border: none;
    color: #555;
    cursor: pointer;
    padding: 4px;
    display: flex;
  }

  &__title {
    font-size: 18px;
    font-weight: 700;
    color: #222;
    margin: 0;
  }

  &__search {
    position: relative;
    margin: 12px 20px;
  }

  &__search-icon {
    position: absolute;
    left: 12px;
    top: 50%;
    transform: translateY(-50%);
    color: #bbb;
  }

  &__search-input {
    width: 100%;
    padding: 10px 36px 10px 38px;
    border: 1px solid #e8e8e8;
    border-radius: 12px;
    font-size: 14px;
    background: #fff;
    transition: border-color 0.2s;

    &:focus {
      outline: none;
      border-color: #007bff;
    }
  }

  &__search-clear {
    position: absolute;
    right: 10px;
    top: 50%;
    transform: translateY(-50%);
    background: none;
    border: none;
    color: #bbb;
    cursor: pointer;
    padding: 4px;
  }

  &__categories {
    display: flex;
    gap: 8px;
    padding: 0 20px 12px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    &::-webkit-scrollbar { display: none; }
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 60px 0;
  }

  &__spinner {
    width: 32px;
    height: 32px;
    border: 3px solid #e8e8e8;
    border-top-color: #007bff;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  &__list {
    padding: 0 20px;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__empty {
    text-align: center;
    padding: 40px 0;
    color: #999;

    p { margin: 0; }
  }

  &__empty-sub {
    font-size: 13px;
    margin-top: 8px !important;
    color: #bbb;
  }

  &__more {
    margin: 16px auto 0;
    padding: 10px 32px;
    border: 1px solid #e0e0e0;
    border-radius: 20px;
    background: #fff;
    color: #555;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;

    &:hover:not(:disabled) { background: #f5f7ff; border-color: #007bff; color: #007bff; }
    &:disabled { opacity: 0.5; }
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

// Category chips
.cat-chip {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border: 1px solid #e8e8e8;
  border-radius: 20px;
  background: #fff;
  color: #666;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;

  &:hover { border-color: #007bff; color: #007bff; }

  &--active {
    background: #007bff;
    border-color: #007bff;
    color: #fff;

    &:hover { background: #0062d6; }
  }

  &__icon { font-size: 14px; }

  &__count {
    font-size: 11px;
    background: rgba(0,0,0,0.08);
    padding: 1px 5px;
    border-radius: 8px;
  }

  &--active &__count {
    background: rgba(255,255,255,0.25);
  }
}

// Blog card
.blog-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    border-color: #d8e4ff;
    box-shadow: 0 2px 12px rgba(0, 123, 255, 0.06);
  }

  &__thumb {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    object-fit: cover;
    background: #f5f5f5;
    border: 1px solid #eee;
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;
  }

  &__title-row {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
    color: #222;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__platform {
    font-size: 10px;
    color: #007bff;
    background: #f0f4ff;
    padding: 2px 6px;
    border-radius: 4px;
    font-weight: 500;
    flex-shrink: 0;
    white-space: nowrap;
  }

  &__author {
    font-size: 12px;
    color: #999;
  }

  &__meta {
    display: flex;
    gap: 8px;
    font-size: 12px;
    color: #aaa;
  }

  &__score {
    color: #f59e0b;
    font-weight: 600;
  }

  &__tags {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
    margin-top: 2px;
  }

  &__tag {
    font-size: 11px;
    color: #007bff;
    cursor: pointer;

    &:hover { text-decoration: underline; }
  }

  &__subscribe {
    flex-shrink: 0;
    padding: 8px 14px;
    border: 1px solid #007bff;
    border-radius: 8px;
    background: transparent;
    color: #007bff;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.15s;

    &:hover {
      background: #007bff;
      color: #fff;
    }
  }
}

@media (max-width: 480px) {
  .blog-card {
    padding: 12px;

    &__thumb { width: 36px; height: 36px; }
    &__subscribe { padding: 6px 10px; }
  }
}
</style>
