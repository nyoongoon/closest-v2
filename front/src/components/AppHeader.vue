<template>
  <header class="app-header" :class="{ 'app-header--hidden': isHidden }">
    <div class="app-header__inner">
      <router-link to="/" class="app-header__logo">
        <span class="app-header__logo-icon">◉</span>
        <span class="app-header__logo-text">Closest</span>
      </router-link>

      <nav class="app-header__nav">
        <router-link to="/" class="app-header__link" active-class="app-header__link--active" exact>
          홈
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/subscriptions"
          class="app-header__link"
          active-class="app-header__link--active"
        >
          구독 목록
          <span v-if="newPostCount > 0" class="app-header__badge">{{ newPostCount }}</span>
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/profile"
          class="app-header__link"
          active-class="app-header__link--active"
        >
          프로필
        </router-link>
      </nav>

      <div class="app-header__actions">
        <template v-if="isLoggedIn">
          <button class="app-header__btn app-header__btn--subscribe" @click="$emit('subscribe')">
            + 구독
          </button>
          <button class="app-header__btn app-header__btn--logout" @click="handleLogout">
            로그아웃
          </button>
        </template>
        <template v-else>
          <button class="app-header__btn app-header__btn--login" @click="$emit('login')">
            로그인
          </button>
        </template>
      </div>

      <!-- 모바일 메뉴 토글 -->
      <button class="app-header__hamburger" @click="mobileOpen = !mobileOpen" aria-label="메뉴">
        <span :class="{ open: mobileOpen }"></span>
      </button>
    </div>

    <!-- 모바일 메뉴 -->
    <Transition name="slide-down">
      <div v-if="mobileOpen" class="app-header__mobile-menu">
        <router-link to="/" class="app-header__mobile-link" @click="mobileOpen = false">
          홈
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/subscriptions"
          class="app-header__mobile-link"
          @click="mobileOpen = false"
        >
          구독 목록
          <span v-if="newPostCount > 0" class="app-header__badge">{{ newPostCount }}</span>
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/profile"
          class="app-header__mobile-link"
          @click="mobileOpen = false"
        >
          프로필
        </router-link>
        <div class="app-header__mobile-actions">
          <template v-if="isLoggedIn">
            <button
              class="app-header__btn app-header__btn--subscribe"
              @click="$emit('subscribe'); mobileOpen = false"
            >
              + 구독
            </button>
            <button
              class="app-header__btn app-header__btn--logout"
              @click="handleLogout(); mobileOpen = false"
            >
              로그아웃
            </button>
          </template>
          <template v-else>
            <button
              class="app-header__btn app-header__btn--login"
              @click="$emit('login'); mobileOpen = false"
            >
              로그인
            </button>
          </template>
        </div>
      </div>
    </Transition>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';

defineProps<{
  isLoggedIn: boolean;
  newPostCount: number;
}>();

const emit = defineEmits<{
  (e: 'login'): void;
  (e: 'subscribe'): void;
  (e: 'logout'): void;
}>();

const mobileOpen = ref(false);
const isHidden = ref(false);
let lastScrollY = 0;

const handleLogout = () => {
  emit('logout');
};

const handleScroll = () => {
  const currentY = window.scrollY;
  isHidden.value = currentY > 60 && currentY > lastScrollY;
  lastScrollY = currentY;
};

onMounted(() => window.addEventListener('scroll', handleScroll, { passive: true }));
onUnmounted(() => window.removeEventListener('scroll', handleScroll));
</script>

<style lang="scss" scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 900;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  transition: transform 0.3s ease;

  &--hidden {
    transform: translateY(-100%);
  }

  &__inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    height: 56px;
    display: flex;
    align-items: center;
    gap: 32px;
  }

  &__logo {
    display: flex;
    align-items: center;
    gap: 8px;
    text-decoration: none;
    flex-shrink: 0;
  }

  &__logo-icon {
    font-size: 22px;
    color: #007bff;
  }

  &__logo-text {
    font-size: 18px;
    font-weight: 800;
    color: #222;
    letter-spacing: -0.5px;
  }

  &__nav {
    display: flex;
    gap: 4px;
    flex: 1;

    @media (max-width: 767px) {
      display: none;
    }
  }

  &__link {
    padding: 6px 14px;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    color: #666;
    text-decoration: none;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    gap: 6px;

    &:hover {
      color: #222;
      background: rgba(0, 0, 0, 0.04);
    }

    &--active {
      color: #007bff;
      background: rgba(0, 123, 255, 0.08);
      font-weight: 600;
    }
  }

  &__badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: 9px;
    background: #ef4444;
    color: #fff;
    font-size: 11px;
    font-weight: 700;
    line-height: 1;
  }

  &__actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;

    @media (max-width: 767px) {
      display: none;
    }
  }

  &__btn {
    padding: 7px 16px;
    border: none;
    border-radius: 10px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;

    &--login,
    &--subscribe {
      background: #007bff;
      color: #fff;

      &:hover {
        background: #0056b3;
        transform: translateY(-1px);
      }
    }

    &--logout {
      background: #f5f5f5;
      color: #666;

      &:hover {
        background: #eee;
        color: #333;
      }
    }
  }

  &__hamburger {
    display: none;
    background: none;
    border: none;
    cursor: pointer;
    width: 32px;
    height: 32px;
    position: relative;
    flex-shrink: 0;

    @media (max-width: 767px) {
      display: flex;
      align-items: center;
      justify-content: center;
    }

    span {
      display: block;
      width: 20px;
      height: 2px;
      background: #333;
      border-radius: 1px;
      transition: all 0.3s;
      position: relative;

      &::before,
      &::after {
        content: '';
        position: absolute;
        width: 20px;
        height: 2px;
        background: #333;
        border-radius: 1px;
        transition: all 0.3s;
      }

      &::before {
        top: -6px;
      }

      &::after {
        top: 6px;
      }

      &.open {
        background: transparent;

        &::before {
          top: 0;
          transform: rotate(45deg);
        }

        &::after {
          top: 0;
          transform: rotate(-45deg);
        }
      }
    }
  }

  &__mobile-menu {
    display: none;
    flex-direction: column;
    padding: 8px 24px 16px;
    border-top: 1px solid rgba(0, 0, 0, 0.06);

    @media (max-width: 767px) {
      display: flex;
    }
  }

  &__mobile-link {
    padding: 12px 0;
    font-size: 15px;
    font-weight: 500;
    color: #444;
    text-decoration: none;
    border-bottom: 1px solid #f5f5f5;
    display: flex;
    align-items: center;
    gap: 8px;

    &:last-of-type {
      border-bottom: none;
    }
  }

  &__mobile-actions {
    display: flex;
    gap: 8px;
    padding-top: 12px;
  }
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.25s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
