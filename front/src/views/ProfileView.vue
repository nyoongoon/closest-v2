<template>
  <div class="profile-view">
    <div class="profile-view__card">
      <div class="profile-view__avatar-section">
        <img class="profile-view__avatar" :src="avatarUrl" alt="프로필" />
        <div class="profile-view__user-info">
          <h2 class="profile-view__name">{{ userEmail }}</h2>
          <span class="profile-view__role">블로거</span>
        </div>
      </div>

      <!-- 상태 메시지 -->
      <div class="profile-view__status-section">
        <h3 class="profile-view__section-title">상태 메시지</h3>
        <div class="profile-view__status-form">
          <textarea
            v-model="statusMessage"
            class="profile-view__textarea"
            placeholder="다른 사용자에게 보여질 상태 메시지를 입력하세요..."
            rows="3"
            maxlength="200"
          />
          <div class="profile-view__status-footer">
            <span class="profile-view__char-count">{{ statusMessage.length }}/200</span>
            <button
              class="profile-view__save-btn"
              :disabled="saving || statusMessage === savedStatus"
              @click="saveStatus"
            >
              {{ saving ? '저장 중...' : '저장' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 통계 -->
      <div class="profile-view__stats-section">
        <h3 class="profile-view__section-title">나의 활동</h3>
        <div class="profile-view__stats-grid">
          <div class="profile-view__stat-card">
            <span class="profile-view__stat-value">{{ subscriptionCount }}</span>
            <span class="profile-view__stat-label">구독 중</span>
          </div>
          <div class="profile-view__stat-card">
            <span class="profile-view__stat-value">{{ newPostsCount }}</span>
            <span class="profile-view__stat-label">새 포스트</span>
          </div>
          <div class="profile-view__stat-card">
            <span class="profile-view__stat-value">{{ totalVisits }}</span>
            <span class="profile-view__stat-label">총 방문</span>
          </div>
        </div>
      </div>

      <!-- 계정 관리 -->
      <div class="profile-view__account-section">
        <h3 class="profile-view__section-title">계정</h3>
        <div class="profile-view__account-actions">
          <button class="profile-view__action-btn" @click="handleLogout">로그아웃</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useSubscriptionStore } from '@/stores/subscription';
import { blogApi } from '@/services/api';
import { useToast } from '@/composables/useToast';
import { getAccessTokenFromCookie, deleteCookieFromBrowser } from '@/utils/cookie';

const router = useRouter();
const subscriptionStore = useSubscriptionStore();
const { showToast } = useToast();

const statusMessage = ref('');
const savedStatus = ref('');
const saving = ref(false);

// JWT에서 이메일 추출 시도
const userEmail = computed(() => {
  try {
    const token = getAccessTokenFromCookie();
    if (!token) return '사용자';
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub || payload.email || '사용자';
  } catch {
    return '사용자';
  }
});

const avatarUrl = computed(() => {
  return `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(userEmail.value)}&backgroundColor=007bff`;
});

const subscriptionCount = computed(() => subscriptionStore.allBlogs.length);
const newPostsCount = computed(() => subscriptionStore.totalNewPosts);
const totalVisits = computed(() =>
  subscriptionStore.allBlogs.reduce((sum, b) => sum + (b.visitCnt ?? 0), 0),
);

const saveStatus = async () => {
  if (saving.value) return;
  saving.value = true;
  try {
    await blogApi.updateStatus({ message: statusMessage.value });
    savedStatus.value = statusMessage.value;
    showToast('상태 메시지가 저장되었습니다.', 'success');
  } catch {
    showToast('상태 메시지 저장에 실패했습니다.', 'error');
  } finally {
    saving.value = false;
  }
};

const handleLogout = () => {
  deleteCookieFromBrowser('accessToken');
  deleteCookieFromBrowser('refreshToken');
  showToast('로그아웃되었습니다.', 'info');
  router.push('/');
};

onMounted(async () => {
  if (subscriptionStore.allBlogs.length === 0) {
    await subscriptionStore.fetchAllBlogs(true);
  }
});
</script>

<style lang="scss" scoped>
.profile-view {
  max-width: 600px;
  margin: 0 auto;
  padding: 80px 24px 40px;

  &__card {
    display: flex;
    flex-direction: column;
    gap: 32px;
  }

  &__avatar-section {
    display: flex;
    align-items: center;
    gap: 20px;
  }

  &__avatar {
    width: 72px;
    height: 72px;
    border-radius: 50%;
    border: 3px solid #eee;
    background: #f5f5f5;
  }

  &__user-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__name {
    font-size: 22px;
    font-weight: 700;
    color: #111;
    margin: 0;
  }

  &__role {
    font-size: 13px;
    color: #999;
  }

  &__section-title {
    font-size: 16px;
    font-weight: 700;
    color: #333;
    margin: 0 0 12px;
  }

  &__status-form {
    background: #f8f9fa;
    border-radius: 14px;
    padding: 16px;
  }

  &__textarea {
    width: 100%;
    border: 1px solid #e8e8e8;
    border-radius: 10px;
    padding: 12px 14px;
    font-size: 14px;
    font-family: inherit;
    resize: vertical;
    background: #fff;
    color: #333;
    transition: border-color 0.2s;
    box-sizing: border-box;

    &:focus {
      outline: none;
      border-color: #007bff;
    }

    &::placeholder {
      color: #bbb;
    }
  }

  &__status-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
  }

  &__char-count {
    font-size: 12px;
    color: #aaa;
  }

  &__save-btn {
    padding: 8px 24px;
    border: none;
    border-radius: 10px;
    background: #007bff;
    color: #fff;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;

    &:hover:not(:disabled) {
      background: #0056b3;
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  &__stats-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }

  &__stat-card {
    background: #f8f9fa;
    border-radius: 14px;
    padding: 20px 16px;
    text-align: center;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  &__stat-value {
    font-size: 24px;
    font-weight: 800;
    color: #007bff;
  }

  &__stat-label {
    font-size: 12px;
    color: #888;
    font-weight: 500;
  }

  &__account-actions {
    display: flex;
    gap: 12px;
  }

  &__action-btn {
    padding: 10px 24px;
    border: 1px solid #e0e0e0;
    border-radius: 10px;
    background: #fff;
    color: #666;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #f5f5f5;
      border-color: #ccc;
    }
  }
}

@media (max-width: 480px) {
  .profile-view {
    &__stats-grid {
      grid-template-columns: 1fr;
    }

    &__avatar-section {
      flex-direction: column;
      text-align: center;
    }
  }
}
</style>
