import { createRouter, createWebHistory } from 'vue-router';
import MainView from '@/views/MainView.vue';
import { getAccessTokenFromCookie } from '@/utils/cookie';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: MainView,
    },
    {
      path: '/subscriptions',
      name: 'subscriptions',
      component: () => import('@/views/SubscriptionListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/explore',
      name: 'explore',
      component: () => import('@/views/ExploreView.vue'),
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
  ],
});

// 인증 가드
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !getAccessTokenFromCookie()) {
    return { name: 'home' };
  }
});

export default router;
