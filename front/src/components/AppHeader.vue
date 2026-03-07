<template>
  <div class="floating-ui">
    <!-- 좌상단: 로고 -->
    <router-link to="/" class="floating-ui__logo">
      <span class="floating-ui__logo-icon">◉</span>
      <span class="floating-ui__logo-text">Closest</span>
    </router-link>

    <!-- 우상단: 액션 -->
    <div class="floating-ui__actions">
      <router-link to="/discover" class="floating-ui__pill" title="블로그 탐색">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
      </router-link>
      <template v-if="isLoggedIn">
        <router-link
          to="/subscriptions"
          class="floating-ui__pill"
          title="구독 목록"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/>
            <line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>
          </svg>
          <span v-if="newPostCount > 0" class="floating-ui__badge">{{ newPostCount }}</span>
        </router-link>
        <button class="floating-ui__pill floating-ui__pill--primary" @click="$emit('subscribe')">
          +
        </button>
        <button class="floating-ui__pill" @click="$emit('logout')" title="로그아웃">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
        </button>
      </template>
      <template v-else>
        <button class="floating-ui__pill floating-ui__pill--primary" @click="$emit('login')">
          로그인
        </button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  isLoggedIn: boolean;
  newPostCount: number;
}>();

defineEmits<{
  (e: 'login'): void;
  (e: 'subscribe'): void;
  (e: 'logout'): void;
}>();
</script>

<style lang="scss" scoped>
.floating-ui {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 900;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  pointer-events: none;

  // 자식만 클릭 가능 (캔버스 이벤트 통과)
  > * {
    pointer-events: auto;
  }

  &__logo {
    display: flex;
    align-items: center;
    gap: 6px;
    text-decoration: none;
    opacity: 0.7;
    transition: opacity 0.2s;

    &:hover {
      opacity: 1;
    }
  }

  &__logo-icon {
    font-size: 20px;
    color: #007bff;
  }

  &__logo-text {
    font-size: 17px;
    font-weight: 800;
    color: #333;
    letter-spacing: -0.5px;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__pill {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    border: none;
    background: rgba(255, 255, 255, 0.75);
    backdrop-filter: blur(8px);
    color: #555;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    text-decoration: none;
    box-shadow: 0 1px 8px rgba(0, 0, 0, 0.08);

    &:hover {
      background: rgba(255, 255, 255, 0.95);
      color: #222;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
      transform: translateY(-1px);
    }

    &--primary {
      background: #007bff;
      color: #fff;
      font-size: 18px;
      font-weight: 300;

      &:hover {
        background: #0062d6;
        color: #fff;
      }
    }
  }

  &__badge {
    position: absolute;
    top: -3px;
    right: -3px;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    border-radius: 8px;
    background: #ef4444;
    color: #fff;
    font-size: 10px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
    border: 2px solid #fff;
  }
}

// 모바일 미세 조정
@media (max-width: 767px) {
  .floating-ui {
    padding: 12px 16px;
  }
}
</style>
