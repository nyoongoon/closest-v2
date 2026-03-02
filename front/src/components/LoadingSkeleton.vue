<template>
  <div class="skeleton-list">
    <div v-for="i in count" :key="i" class="skeleton-card">
      <div class="skeleton-card__avatar skeleton-pulse" />
      <div class="skeleton-card__body">
        <div class="skeleton-card__line skeleton-card__line--name skeleton-pulse" />
        <div class="skeleton-card__line skeleton-card__line--url skeleton-pulse" />
        <div class="skeleton-card__line skeleton-card__line--meta skeleton-pulse" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  count?: number;
}>();
</script>

<style lang="scss" scoped>
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border: 1px solid #f0f0f0;
  border-radius: 16px;

  &__avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__line {
    border-radius: 4px;
    height: 12px;

    &--name {
      width: 40%;
      height: 14px;
    }

    &--url {
      width: 60%;
    }

    &--meta {
      width: 30%;
    }
  }
}

.skeleton-pulse {
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
