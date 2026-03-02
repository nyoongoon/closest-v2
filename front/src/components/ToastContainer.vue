<template>
  <Teleport to="body">
    <div class="toast-container" aria-live="polite" aria-atomic="false">
      <TransitionGroup name="toast" tag="div">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="toast-item"
          :class="`toast-item--${toast.type}`"
          role="alert"
          @click="removeToast(toast.id)"
        >
          <span class="toast-item__icon">{{ iconMap[toast.type] }}</span>
          <span class="toast-item__message">{{ toast.message }}</span>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { useToast } from '@/composables/useToast';
import type { ToastType } from '@/composables/useToast';

const { toasts, removeToast } = useToast();

const iconMap: Record<ToastType, string> = {
  success: '✓',
  error: '✕',
  info: 'ℹ',
};
</script>

<style lang="scss" scoped>
.toast-container {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}

.toast-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-radius: 12px;
  min-width: 240px;
  max-width: 360px;
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  pointer-events: auto;
  line-height: 1.5;

  &--success {
    background: #22c55e;
  }

  &--error {
    background: #ef4444;
  }

  &--info {
    background: #3b82f6;
  }

  &__icon {
    font-size: 16px;
    font-weight: 700;
    flex-shrink: 0;
  }

  &__message {
    flex: 1;
    word-break: break-word;
  }
}

// 트랜지션
.toast-enter-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toast-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 1, 1);
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(40px);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(40px);
}

.toast-move {
  transition: transform 0.25s ease;
}

@media (max-width: 480px) {
  .toast-container {
    top: 12px;
    right: 12px;
    left: 12px;
  }

  .toast-item {
    max-width: 100%;
  }
}
</style>
