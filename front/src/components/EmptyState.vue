<template>
  <div class="empty-state">
    <div class="empty-state__icon">{{ icon }}</div>
    <h3 class="empty-state__title">{{ title }}</h3>
    <p v-if="description" class="empty-state__desc">{{ description }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon?: string;
  title: string;
  description?: string;
}>();
</script>

<style lang="scss" scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  text-align: center;

  &__icon {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.6;
  }

  &__title {
    font-size: 18px;
    font-weight: 600;
    color: #333;
    margin: 0 0 8px;
  }

  &__desc {
    font-size: 14px;
    color: #999;
    margin: 0 0 24px;
    max-width: 320px;
    line-height: 1.6;
  }
}
</style>
