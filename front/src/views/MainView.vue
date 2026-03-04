<template>
  <div class="main-page">
  <div class="main-canvas" ref="canvasRef" @click="handleCanvasClick"
    @touchstart="onCanvasTouchStart" @touchend="onCanvasTouchEnd">
    <svg class="main-canvas__svg">
      <!-- 노드 연결선 (그라디언트) -->
      <defs>
        <linearGradient id="lineGrad" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="#007bff" stop-opacity="0.15" />
          <stop offset="100%" stop-color="#007bff" stop-opacity="0.05" />
        </linearGradient>
      </defs>
      <line
        v-for="(node, index) in visibleNodes"
        :key="'line' + index"
        :x1="centerNode.x"
        :y1="centerNode.y"
        :x2="node.position.x + currentNodeSize / 2"
        :y2="node.position.y + currentNodeSize / 2"
        stroke="url(#lineGrad)"
        stroke-width="1"
      />
    </svg>

    <!-- 중앙 노드 -->
    <div
      class="node center-node"
      :style="{
        width: currentCenterNodeSize + 'px',
        height: currentCenterNodeSize + 'px',
        top: (centerNode.y - currentCenterNodeSize / 2) + 'px',
        left: (centerNode.x - currentCenterNodeSize / 2) + 'px',
        backgroundImage: isLoggedIn
          ? 'url(https://api.dicebear.com/7.x/initials/svg?seed=Me&backgroundColor=007bff)'
          : 'url(https://api.dicebear.com/7.x/icons/svg?seed=guest&icon=person&backgroundColor=94a3b8)'
      }"
    >
      <div class="center-node__pulse"></div>
    </div>

    <!-- 서브 노드들 -->
    <div
      v-for="(node, index) in visibleNodes"
      :key="index"
      class="node sub-node"
      :class="{ 'sub-node--hovered': hoveredIndex === index }"
      :style="{ ...node.style, width: currentNodeSize + 'px', height: currentNodeSize + 'px' }"
      @mouseover="handleMouseOver(index)"
      @mouseleave="handleMouseLeave(index)"
      @click.stop="handleNodeClick(index)"
    >
      <!-- 새 포스트 뱃지 -->
      <span v-if="node.newPostsCnt && node.newPostsCnt > 0" class="sub-node__badge">
        {{ node.newPostsCnt }}
      </span>
      <!-- 블로그명 라벨 -->
      <span class="sub-node__label">{{ truncName(node.nickName) }}</span>
    </div>

    <!-- 호버 시 닉네임 툴팁 -->
    <Transition name="fade">
      <div
        v-if="hoveredIndex !== null && visibleNodes[hoveredIndex]"
        class="node-tooltip"
        :style="{
          top: (visibleNodes[hoveredIndex].position.y + currentNodeSize + 8) + 'px',
          left: (visibleNodes[hoveredIndex].position.x + currentNodeSize / 2) + 'px',
        }"
      >
        {{ visibleNodes[hoveredIndex].nickName || '블로그' }}
      </div>
    </Transition>

    <!-- 모달: 로그인/로그아웃 -->
    <Transition name="modal">
      <div
        v-if="loginModalVisible"
        class="modal-backdrop"
        @click.self="closeLoginModal"
      >
        <div v-if="!isLoggedIn" class="login-modal modal-content">
          <h2>로그인</h2>
          <form @submit.prevent="handleSigninRequest">
            <label for="loginEmail">이메일</label>
            <input type="email" id="loginEmail" v-model="loginForm.email" autocomplete="email" placeholder="example@email.com" />
            <label for="loginPassword">비밀번호</label>
            <input type="password" id="loginPassword" v-model="loginForm.password" autocomplete="current-password" placeholder="비밀번호 입력" />
            <div class="button-group">
              <button @click="handleOpenSignup()" type="button" class="btn btn--secondary">회원가입</button>
              <button type="submit" class="btn btn--primary" :disabled="loginLoading">
                {{ loginLoading ? '로그인 중...' : '로그인' }}
              </button>
            </div>
          </form>
        </div>
        <div v-else class="login-modal modal-content">
          <h2>로그아웃</h2>
          <div class="logout-info">
            <p>이미 로그인되어 있습니다.</p>
            <button @click="handleLogout" class="btn btn--primary" style="width: 100%;">로그아웃</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 모달: 회원가입 -->
    <Transition name="modal">
      <div
        v-if="showSignupModal"
        class="modal-backdrop"
        @click.self="closeSignupModal"
      >
        <div class="signup-modal modal-content">
          <h2>회원가입</h2>
          <form @submit.prevent="handleSignupRequest">
            <label for="signupEmail">이메일</label>
            <input type="email" id="signupEmail" v-model="signupForm.email" autocomplete="email" placeholder="example@email.com" />
            <label for="signupPassword">비밀번호</label>
            <input type="password" id="signupPassword" v-model="signupForm.password" autocomplete="new-password" placeholder="8자 이상" />
            <label for="signupConfirmPassword">비밀번호 확인</label>
            <input type="password" id="signupConfirmPassword" v-model="signupForm.confirmPassword" autocomplete="new-password" placeholder="비밀번호 재입력" />
            <div class="button-group">
              <button type="button" class="btn btn--secondary" @click="closeSignupModal">취소</button>
              <button type="submit" class="btn btn--primary" :disabled="signupLoading">
                {{ signupLoading ? '가입 중...' : '회원가입' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- 모달: 블로그 구독 -->
    <Transition name="modal">
      <div
        v-if="subscribeModalVisible"
        class="modal-backdrop"
        @click.self="closeSubscribeModal"
      >
        <div class="subscribe-modal modal-content">
          <h2>블로그 구독</h2>
          <form @submit.prevent="handleSubscribeRequest">
            <label for="rssUri">RSS / 블로그 URL</label>
            <input type="url" id="rssUri" v-model="subscribeForm.rssUri" placeholder="https://blog.example.com/rss" />
            <p class="modal-hint">블로그 RSS 피드 주소를 입력해주세요.</p>
            <div class="button-group">
              <button type="button" class="btn btn--secondary" @click="closeSubscribeModal">취소</button>
              <button type="submit" class="btn btn--primary" :disabled="subscribeLoading">
                {{ subscribeLoading ? '구독 중...' : '구독하기' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- 노드 클릭 시 팝오버 -->
    <Transition name="popover">
      <div
        v-if="selectedIndex !== null && visibleNodes[selectedIndex]"
        class="node-popover"
        :style="popoverStyle"
        @click.stop
      >
        <div class="node-popover__header">
          <img
            class="node-popover__avatar"
            :src="popoverAvatarUrl"
            :alt="visibleNodes[selectedIndex].nickName"
          />
          <div class="node-popover__title">
            <span class="node-popover__nick">{{ visibleNodes[selectedIndex].nickName || '블로그' }}</span>
            <span class="node-popover__url">{{ popoverDisplayUrl }}</span>
          </div>
        </div>
        <div class="node-popover__stats">
          <span class="node-popover__stat">
            <span class="node-popover__stat-icon">👁</span>
            {{ visibleNodes[selectedIndex].visitCnt ?? 0 }}회
          </span>
          <span class="node-popover__stat">
            <span class="node-popover__stat-icon">📝</span>
            {{ visibleNodes[selectedIndex].newPostsCnt ?? 0 }}개 새글
          </span>
        </div>
        <button class="node-popover__visit" @click="visitSelectedBlog">
          블로그 방문 ↗
        </button>
      </div>
    </Transition>

    <!-- 노드 없을 때 안내 -->
    <div v-if="visibleNodes.length === 0 && !isLoadingNodes" class="main-canvas__empty">
      <div class="main-canvas__empty-icon">◉</div>
      <p class="main-canvas__empty-title">아직 구독 중인 블로그가 없어요</p>
      <p class="main-canvas__empty-desc">상단의 '+ 구독' 버튼으로 블로그를 추가해보세요</p>
    </div>

    <!-- 페이지 네비게이션 -->
    <div v-if="totalPages > 1" class="page-nav" @click.stop>
      <button class="page-nav__btn" @click="handlePrevPage" aria-label="이전">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <span class="page-nav__indicator">{{ nodePage + 1 }} / {{ totalPages }}</span>
      <button class="page-nav__btn page-nav__btn--primary" @click="handleNextPage" aria-label="다음">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
      </button>
    </div>

    <!-- 스와이프 힌트 (우측 중앙) -->
    <div class="canvas-swipe-hint">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
      <span>밀어서 탐색</span>
    </div>

    <!-- 스크롤 유도 -->
    <div v-if="recentPosts.length > 0" class="main-canvas__scroll-hint">
      <span>최신 글 보기</span>
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="6 9 12 15 18 9"/>
      </svg>
    </div>
  </div>

  <!-- 최신 글 피드 -->
  <section
    v-if="recentPosts.length > 0"
    class="recent-feed"
    ref="feedSectionRef"
    @touchstart="onFeedTouchStart"
    @touchend="onFeedTouchEnd"
  >
    <!-- 상단 중앙: 페이지 네비게이션 -->
    <div v-if="postTotalPages > 1" class="feed-paging">
      <button
        class="feed-paging__btn"
        :disabled="postPage === 0"
        @click="handlePostPrevPage"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <span class="feed-paging__info">{{ postPage + 1 }} / {{ postTotalPages }}</span>
      <button
        class="feed-paging__btn"
        :disabled="postPage >= postTotalPages - 1"
        @click="handlePostNextPage"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
      </button>
    </div>

    <div class="recent-feed__inner">
      <h2 class="recent-feed__title">최신 글</h2>
      <div class="recent-feed__list">
        <a
          v-for="(post, i) in pagedPosts"
          :key="postPage + '-' + i"
          :href="post.postUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="post-card"
        >
          <img
            class="post-card__favicon"
            :src="getPostFavicon(post)"
            :alt="post.blogTitle"
            loading="lazy"
          />
          <div class="post-card__body">
            <span class="post-card__title">{{ post.postTitle }}</span>
            <div class="post-card__meta">
              <span class="post-card__blog">{{ post.blogTitle }}</span>
              <span class="post-card__dot">&middot;</span>
              <span class="post-card__time">{{ formatRelativeTime(post.publishedDateTime) }}</span>
            </div>
          </div>
          <span class="post-card__arrow">↗</span>
        </a>
      </div>
    </div>

    <!-- 좌로 스와이프 힌트 -->
    <div class="feed-swipe-hint">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
      <span>밀어서 탐색</span>
    </div>
  </section>
  </div>
</template>

<script lang="ts">
import { defineComponent, onMounted, onUnmounted, reactive, ref, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores';
import { useSubscriptionStore } from '@/stores/subscription';
import { useToast } from '@/composables/useToast';
import { getAccessTokenFromCookie, deleteCookieFromBrowser } from '@/utils/cookie';
import { authApi, postApi } from '@/services/api';
import { getFaviconUrl } from '@/utils/favicon';
import type { BlogInfo, BlogNode, RecentPost } from '@/types';

export default defineComponent({
  name: 'MainView',
  components: {},
  props: {
    isLoggedIn: { type: Boolean, default: false },
    showLoginModal: { type: Boolean, default: false },
    showSubscribeModal: { type: Boolean, default: false },
  },
  emits: ['update:isLoggedIn', 'update:showLoginModal', 'update:showSubscribeModal'],
  setup(props, { emit }) {
    const router = useRouter();
    const authStore = useAuthStore();
    const subscriptionStore = useSubscriptionStore();
    const { showToast } = useToast();

    const canvasRef = ref<HTMLElement | null>(null);
    const feedSectionRef = ref<HTMLElement | null>(null);

    // 반응형
    const isMobile = ref(window.innerWidth < 768);
    const currentCenterNodeSize = computed(() => isMobile.value ? 48 : 64);
    const currentNodeSize = computed(() => isMobile.value ? 36 : 44);

    const centerNode = reactive({ x: window.innerWidth / 2, y: window.innerHeight / 2 });
    const minDistance = computed(() => isMobile.value ? 120 : 180);
    const maxDistance = computed(() => isMobile.value ? 200 : 280);
    const PAGE_SIZE = 8;
    const range = 80;
    const initialSpeed = 0.6;

    const hoveredIndex = ref<number | null>(null);
    const isLoadingNodes = ref(true);
    const allBlogs = ref<any[]>([]);
    const nodePage = ref(0);
    const totalPages = computed(() => Math.max(1, Math.ceil(allBlogs.value.length / PAGE_SIZE)));

    const getRandomPosition = () => {
      const angle = Math.random() * Math.PI * 2;
      const r = minDistance.value + Math.random() * (maxDistance.value - minDistance.value);
      return {
        x: centerNode.x + r * Math.cos(angle),
        y: centerNode.y + r * Math.sin(angle),
      };
    };

    const getRandomVelocity = () => ({
      x: (Math.random() * 2 - 1) * initialSpeed,
      y: (Math.random() * 2 - 1) * initialSpeed,
    });

    const getInitialBounds = (initialPosition: { x: number; y: number }) => ({
      minX: initialPosition.x - range,
      maxX: initialPosition.x + range,
      minY: initialPosition.y - range,
      maxY: initialPosition.y + range,
    });

    const nodes = reactive<BlogNode[]>([]);
    const visibleNodes = ref<BlogNode[]>([]);

    const syncNodesFromStore = () => {
      allBlogs.value = subscriptionStore.closeBlogs;
      applyPage();
      isLoadingNodes.value = false;
    };

    const fetchBlogSubscriptions = async () => {
      isLoadingNodes.value = true;
      try {
        await subscriptionStore.fetchCloseBlogs();
        syncNodesFromStore();
      } catch (error) {
        console.error('Error fetching blog subscriptions:', error);
        isLoadingNodes.value = false;
      }
    };

    const applyPage = () => {
      const start = nodePage.value * PAGE_SIZE;
      const pageBlogs = allBlogs.value.slice(start, start + PAGE_SIZE);
      nodes.splice(0, nodes.length, ...pageBlogs.map(createNodeFromBlog));
      visibleNodes.value = [...nodes];
      // 팝오버 닫기
      selectedIndex.value = null;
    };

    const handleNextPage = () => {
      nodePage.value = (nodePage.value + 1) % totalPages.value;
      applyPage();
    };

    const handlePrevPage = () => {
      nodePage.value = (nodePage.value - 1 + totalPages.value) % totalPages.value;
      applyPage();
    };

    const createNodeFromBlog = (blog: any): BlogNode => {
      const initialPosition = getRandomPosition();
      const bounds = getInitialBounds(initialPosition);
      const blogUrl = blog.blogUrl ?? blog.uri ?? blog.url;
      const displayThumb =
        blog.thumbnailUrl ||
        getFaviconUrl(blogUrl);

      return {
        position: { ...initialPosition },
        velocity: getRandomVelocity(),
        initialPosition,
        bounds,
        style: {
          backgroundImage: `url(${displayThumb})`,
          top: `${initialPosition.y}px`,
          left: `${initialPosition.x}px`,
        },
        isStopped: false,
        subscriptionId: blog.subscriptionId ?? blog.id,
        thumbnailUrl: blog.thumbnailUrl,
        nickName: blog.nickName,
        blogUrl: blog.blogUrl ?? blog.uri ?? blog.url,
        newPostsCnt: blog.newPostsCnt ?? 0,
        visitCnt: blog.visitCnt ?? 0,
        publishedDateTime: blog.publishedDateTime ?? null,
      };
    };

    let animFrameId: number | null = null;
    let lastTime = 0;

    const animate = (timestamp: number) => {
      if (timestamp - lastTime < 50) {
        animFrameId = requestAnimationFrame(animate);
        return;
      }
      lastTime = timestamp;

      nodes.forEach((node) => {
        if (!node.isStopped) {
          node.position.x += node.velocity.x;
          node.position.y += node.velocity.y;

          node.velocity.x += (Math.random() * 2 - 1) * 0.008;
          node.velocity.y += (Math.random() * 2 - 1) * 0.008;

          const maxSpeed = 0.8;
          node.velocity.x = Math.max(Math.min(node.velocity.x, maxSpeed), -maxSpeed);
          node.velocity.y = Math.max(Math.min(node.velocity.y, maxSpeed), -maxSpeed);

          if (node.position.x < node.bounds.minX || node.position.x > node.bounds.maxX) {
            node.position.x = Math.max(Math.min(node.position.x, node.bounds.maxX), node.bounds.minX);
            node.velocity.x *= -1;
          }
          if (node.position.y < node.bounds.minY || node.position.y > node.bounds.maxY) {
            node.position.y = Math.max(Math.min(node.position.y, node.bounds.maxY), node.bounds.minY);
            node.velocity.y *= -1;
          }

          node.style.top = `${node.position.y}px`;
          node.style.left = `${node.position.x}px`;
        }
      });

      visibleNodes.value = [...nodes];
      animFrameId = requestAnimationFrame(animate);
    };

    const handleMouseOver = (index: number) => {
      hoveredIndex.value = index;
      nodes.forEach((node, i) => {
        if (i !== index) node.isStopped = true;
      });
      nodes[index].style.transform = 'scale(1.4)';
      nodes[index].style.zIndex = '50';
    };

    const handleMouseLeave = (index: number) => {
      hoveredIndex.value = null;
      nodes.forEach((node) => {
        node.isStopped = false;
      });
      nodes[index].style.transform = 'scale(1)';
      nodes[index].style.zIndex = '0';
    };

    // 노드 클릭 → 팝오버 표시 & 모든 노드 멈춤
    const selectedIndex = ref<number | null>(null);

    const handleNodeClick = (index: number) => {
      if (selectedIndex.value === index) {
        closePopover();
        return;
      }
      selectedIndex.value = index;
      // 모든 노드 멈춤
      nodes.forEach((node) => { node.isStopped = true; });
    };

    const closePopover = () => {
      selectedIndex.value = null;
      // 모든 노드 다시 움직임
      nodes.forEach((node) => { node.isStopped = false; });
    };

    const popoverStyle = computed(() => {
      if (selectedIndex.value === null || !visibleNodes.value[selectedIndex.value]) return {};
      const node = visibleNodes.value[selectedIndex.value];
      const popoverWidth = 240;
      const popoverHeight = 160;
      const gap = 8;
      let x = node.position.x + currentNodeSize.value + gap;
      let y = node.position.y - gap;

      // 오른쪽 화면 밖으로 나가면 왼쪽에 표시
      if (x + popoverWidth > window.innerWidth - 16) {
        x = node.position.x - popoverWidth - gap;
      }
      // 위로 나가면 아래로
      if (y < 16) {
        y = 16;
      }
      // 아래로 나가면 올림
      if (y + popoverHeight > window.innerHeight - 16) {
        y = window.innerHeight - popoverHeight - 16;
      }
      return { top: `${y}px`, left: `${x}px` };
    });

    const popoverAvatarUrl = computed(() => {
      if (selectedIndex.value === null || !visibleNodes.value[selectedIndex.value]) return '';
      const node = visibleNodes.value[selectedIndex.value];
      return node.thumbnailUrl || getFaviconUrl(node.blogUrl);
    });

    const popoverDisplayUrl = computed(() => {
      if (selectedIndex.value === null || !visibleNodes.value[selectedIndex.value]) return '';
      const blogUrl = visibleNodes.value[selectedIndex.value].blogUrl;
      if (!blogUrl) return '';
      try { return new URL(blogUrl).hostname; } catch { return blogUrl; }
    });

    const visitSelectedBlog = () => {
      if (selectedIndex.value === null || !visibleNodes.value[selectedIndex.value]) return;
      const blogUrl = visibleNodes.value[selectedIndex.value].blogUrl;
      if (blogUrl) window.open(blogUrl, '_blank', 'noopener,noreferrer');
    };

    // 캔버스 클릭 시 팝오버 닫기
    const handleCanvasClick = () => {
      if (selectedIndex.value !== null) closePopover();
    };

    // props 기반 모달 상태
    const loginModalVisible = computed(() => props.showLoginModal);
    const subscribeModalVisible = computed(() => props.showSubscribeModal);
    const showSignupModal = ref(false);

    // 폼 상태
    const loginForm = reactive({ email: '', password: '' });
    const signupForm = reactive({ email: '', password: '', confirmPassword: '' });
    const subscribeForm = reactive({ rssUri: '' });

    const loginLoading = ref(false);
    const signupLoading = ref(false);
    const subscribeLoading = ref(false);

    const closeLoginModal = () => {
      emit('update:showLoginModal', false);
    };
    const closeSignupModal = () => { showSignupModal.value = false; };
    const closeSubscribeModal = () => {
      emit('update:showSubscribeModal', false);
    };

    // ESC 키로 모달 닫기
    const handleKeydown = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return;
      if (props.showLoginModal) closeLoginModal();
      else if (showSignupModal.value) closeSignupModal();
      else if (props.showSubscribeModal) closeSubscribeModal();
      else if (selectedIndex.value !== null) closePopover();
    };

    const handleResize = () => {
      isMobile.value = window.innerWidth < 768;
      centerNode.x = window.innerWidth / 2;
      centerNode.y = window.innerHeight / 2;
    };

    const handleSigninRequest = async (event: Event) => {
      event.preventDefault();
      if (loginLoading.value) return;
      loginLoading.value = true;

      try {
        await authStore.login(loginForm.email, loginForm.password);
        showToast('로그인되었습니다.', 'success');
        emit('update:isLoggedIn', true);
        emit('update:showLoginModal', false);
        loginForm.email = '';
        loginForm.password = '';
        fetchBlogSubscriptions();
      } catch (err) {
        const msg = typeof err === 'string' ? err : '로그인에 실패했습니다.';
        showToast(msg, 'error');
      } finally {
        loginLoading.value = false;
      }
    };

    const handleLogout = () => {
      deleteCookieFromBrowser('accessToken');
      deleteCookieFromBrowser('refreshToken');
      emit('update:isLoggedIn', false);
      emit('update:showLoginModal', false);
      showToast('로그아웃되었습니다.', 'info');
    };

    const handleOpenSignup = () => {
      emit('update:showLoginModal', false);
      showSignupModal.value = true;
    };

    const handleSignupRequest = async (event: Event) => {
      event.preventDefault();
      if (signupLoading.value) return;

      if (signupForm.password !== signupForm.confirmPassword) {
        showToast('패스워드가 일치하지 않습니다.', 'error');
        return;
      }

      signupLoading.value = true;
      try {
        await authApi.signup({
          email: signupForm.email,
          password: signupForm.password,
          confirmPassword: signupForm.confirmPassword,
        });
        showToast('회원가입이 완료되었습니다. 로그인해주세요.', 'success');
        showSignupModal.value = false;
        signupForm.email = '';
        signupForm.password = '';
        signupForm.confirmPassword = '';
        emit('update:showLoginModal', true);
      } catch (err) {
        const msg = typeof err === 'string' ? err : '회원가입에 실패했습니다.';
        showToast(msg, 'error');
      } finally {
        signupLoading.value = false;
      }
    };

    const handleSubscribeRequest = async (event: Event) => {
      event.preventDefault();
      if (subscribeLoading.value) return;
      subscribeLoading.value = true;

      try {
        await subscriptionStore.subscribe(subscribeForm.rssUri);
        showToast('구독이 추가되었습니다.', 'success');
        emit('update:showSubscribeModal', false);
        subscribeForm.rssUri = '';
        fetchBlogSubscriptions();
      } catch (err) {
        const msg = typeof err === 'string' ? err : '구독 처리 중 오류가 발생했습니다.';
        showToast(msg, 'error');
      } finally {
        subscribeLoading.value = false;
      }
    };

    const truncName = (name?: string) => {
      if (!name) return '';
      return name.length > 6 ? name.slice(0, 6) : name;
    };

    // 최신 글 피드 + 페이징
    const recentPosts = ref<RecentPost[]>([]);
    const POST_PAGE_SIZE = 10;
    const postPage = ref(0);
    const postTotalPages = computed(() => Math.max(1, Math.ceil(recentPosts.value.length / POST_PAGE_SIZE)));
    const pagedPosts = computed(() => {
      const start = postPage.value * POST_PAGE_SIZE;
      return recentPosts.value.slice(start, start + POST_PAGE_SIZE);
    });

    // 좌 스와이프 → 탐색 (캔버스 + 피드 공통)
    let swipeTouchStartX = 0;
    let swipeTouchStartY = 0;

    const handleSwipeStart = (e: TouchEvent) => {
      swipeTouchStartX = e.touches[0].clientX;
      swipeTouchStartY = e.touches[0].clientY;
    };

    const handleSwipeEnd = (e: TouchEvent) => {
      const dx = e.changedTouches[0].clientX - swipeTouchStartX;
      const dy = e.changedTouches[0].clientY - swipeTouchStartY;
      if (dx < -80 && Math.abs(dx) > Math.abs(dy) * 1.5) {
        router.push('/explore');
      }
    };

    const onCanvasTouchStart = handleSwipeStart;
    const onCanvasTouchEnd = handleSwipeEnd;
    const onFeedTouchStart = handleSwipeStart;
    const onFeedTouchEnd = handleSwipeEnd;

    const scrollToFeedTop = () => {
      if (feedSectionRef.value) {
        feedSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    };

    const handlePostNextPage = () => {
      if (postPage.value < postTotalPages.value - 1) {
        postPage.value++;
        scrollToFeedTop();
      }
    };

    const handlePostPrevPage = () => {
      if (postPage.value > 0) {
        postPage.value--;
        scrollToFeedTop();
      }
    };

    const fetchRecentPosts = async () => {
      try {
        recentPosts.value = await postApi.getRecentPosts(100);
      } catch (e) {
        console.error('Failed to fetch recent posts:', e);
      }
    };

    const getPostFavicon = (post: RecentPost) => {
      return post.thumbnailUrl || getFaviconUrl(post.blogUrl);
    };

    const formatRelativeTime = (dateStr: string): string => {
      if (!dateStr) return '';
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      const now = new Date();
      const diffMs = now.getTime() - d.getTime();
      const diffMin = Math.floor(diffMs / 60000);
      if (diffMin < 1) return '방금';
      if (diffMin < 60) return `${diffMin}분 전`;
      const diffHours = Math.floor(diffMin / 60);
      if (diffHours < 24) return `${diffHours}시간 전`;
      const diffDays = Math.floor(diffHours / 24);
      if (diffDays < 7) return `${diffDays}일 전`;
      if (diffDays < 30) return `${Math.floor(diffDays / 7)}주 전`;
      return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
    };

    // App.vue에서 fetchCloseBlogs 완료 시 자동 동기화
    watch(() => subscriptionStore.closeBlogs, (blogs) => {
      if (blogs.length > 0) {
        syncNodesFromStore();
      }
    }, { deep: true });

    onMounted(() => {
      // store에 이미 데이터가 있으면 바로 동기화
      if (subscriptionStore.closeBlogs.length > 0) {
        syncNodesFromStore();
      }
      fetchRecentPosts();
      animFrameId = requestAnimationFrame(animate);
      window.addEventListener('keydown', handleKeydown);
      window.addEventListener('resize', handleResize);
    });

    onUnmounted(() => {
      if (animFrameId !== null) cancelAnimationFrame(animFrameId);
      window.removeEventListener('keydown', handleKeydown);
      window.removeEventListener('resize', handleResize);
    });

    return {
      canvasRef,
      centerNode,
      currentCenterNodeSize,
      currentNodeSize,
      nodes,
      hoveredIndex,
      isLoadingNodes,
      handleMouseOver,
      handleMouseLeave,
      handleNodeClick,
      handleOpenSignup,
      handleSignupRequest,
      handleSigninRequest,
      handleSubscribeRequest,
      handleLogout,
      loginModalVisible,
      subscribeModalVisible,
      showSignupModal,
      isLoggedIn: computed(() => props.isLoggedIn),
      visibleNodes,
      nodePage,
      totalPages,
      handleNextPage,
      handlePrevPage,
      selectedIndex,
      popoverStyle,
      popoverAvatarUrl,
      popoverDisplayUrl,
      visitSelectedBlog,
      handleCanvasClick,
      truncName,
      feedSectionRef,
      onCanvasTouchStart,
      onCanvasTouchEnd,
      onFeedTouchStart,
      onFeedTouchEnd,
      recentPosts,
      pagedPosts,
      postPage,
      postTotalPages,
      handlePostNextPage,
      handlePostPrevPage,
      getPostFavicon,
      formatRelativeTime,
      loginForm,
      signupForm,
      subscribeForm,
      loginLoading,
      signupLoading,
      subscribeLoading,
      closeLoginModal,
      closeSignupModal,
      closeSubscribeModal,
    };
  },
});
</script>

<style lang="scss" scoped>
.main-page {
  width: 100%;
  height: 100vh;
  overflow-y: auto;
  scroll-snap-type: y mandatory;
  -webkit-overflow-scrolling: touch;
}

.main-canvas {
  position: relative;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(145deg, #fafbff 0%, #f5f7ff 50%, #fafbff 100%);
  overflow: hidden;
  scroll-snap-align: start;

  &__svg {
    position: absolute;
    width: 100%;
    height: 100%;
    pointer-events: none;
  }

  &__empty {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    text-align: center;
    z-index: 5;
  }

  &__empty-icon {
    font-size: 56px;
    color: #007bff;
    opacity: 0.3;
    margin-bottom: 16px;
  }

  &__empty-title {
    font-size: 18px;
    font-weight: 600;
    color: #555;
    margin: 0 0 8px;
  }

  &__empty-desc {
    font-size: 14px;
    color: #999;
    margin: 0;
  }

}

// ── 캔버스 스와이프 힌트 (우측 중앙) ──
.canvas-swipe-hint {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #bbb;
  font-size: 11px;
  font-weight: 500;
  pointer-events: none;
  animation: swipe-hint 2.5s ease-in-out infinite;

  @media (min-width: 769px) {
    // 데스크탑에서도 보여줌 (클릭 불가, 힌트만)
    font-size: 12px;
    color: #ccc;
  }
}

.main-canvas {
  &__scroll-hint {
    position: absolute;
    bottom: 24px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    color: #bbb;
    font-size: 12px;
    font-weight: 500;
    animation: bounce-hint 2s ease-in-out infinite;
    z-index: 5;
    pointer-events: none;
  }
}

// 페이지 네비게이션
.page-nav {
  position: absolute;
  bottom: 64px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 50;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  border-radius: 20px;
  padding: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  &__btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border: none;
    border-radius: 50%;
    background: transparent;
    color: #888;
    cursor: pointer;
    transition: all 0.15s;

    &:hover {
      background: #f0f0f0;
      color: #333;
    }

    &--primary {
      background: #007bff;
      color: #fff;

      &:hover {
        background: #0062d6;
        color: #fff;
      }
    }
  }

  &__indicator {
    font-size: 12px;
    font-weight: 600;
    color: #666;
    padding: 0 8px;
    min-width: 40px;
    text-align: center;
    user-select: none;
  }
}

@keyframes bounce-hint {
  0%, 100% { transform: translateX(-50%) translateY(0); }
  50% { transform: translateX(-50%) translateY(6px); }
}

// ── 최신 글 피드 ──
.recent-feed {
  position: relative;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  min-height: 100vh;
  min-height: 100dvh;
  scroll-snap-align: start;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 24px;
  box-sizing: border-box;

  &__inner {
    max-width: 680px;
    width: 100%;
    margin: 0 auto;
  }

  &__title {
    font-size: 20px;
    font-weight: 800;
    color: #222;
    margin: 0 0 20px;
    letter-spacing: -0.3px;
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
}

// ── 피드 페이징 (상단 중앙) ──
.feed-paging {
  position: absolute;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border-radius: 20px;
  padding: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #e8e8e8;

  &__btn {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    border: none;
    background: transparent;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #555;
    cursor: pointer;
    transition: all 0.15s;

    &:hover:not(:disabled) {
      background: #f0f4ff;
      color: #007bff;
    }

    &:disabled {
      opacity: 0.25;
      cursor: not-allowed;
    }
  }

  &__info {
    font-size: 13px;
    font-weight: 600;
    color: #888;
    min-width: 48px;
    text-align: center;
  }
}

// ── 스와이프 힌트 (우하단) ──
.feed-swipe-hint {
  position: absolute;
  bottom: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #ccc;
  font-size: 11px;
  font-weight: 500;
  animation: swipe-hint 2.5s ease-in-out infinite;
  pointer-events: none;
  z-index: 5;

  @media (min-width: 769px) {
    display: none;
  }
}

@keyframes swipe-hint {
  0%, 100% { transform: translateX(0); opacity: 0.5; }
  50% { transform: translateX(-6px); opacity: 1; }
}

.post-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 12px;
  text-decoration: none;
  color: inherit;
  transition: background 0.15s;

  &:hover {
    background: #f8f9fb;

    .post-card__arrow {
      opacity: 1;
      color: #007bff;
    }
  }

  &__favicon {
    width: 32px;
    height: 32px;
    border-radius: 8px;
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

  &__title {
    font-size: 14px;
    font-weight: 600;
    color: #222;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 1.4;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #999;
  }

  &__blog {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 160px;
  }

  &__dot {
    flex-shrink: 0;
  }

  &__time {
    flex-shrink: 0;
  }

  &__arrow {
    flex-shrink: 0;
    font-size: 14px;
    color: #ccc;
    opacity: 0;
    transition: all 0.15s;
  }
}

.node {
  position: absolute;
  background-color: #f0f0f0;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  border: 3px solid white;
  box-shadow: 0 2px 12px rgba(0,0,0,0.12);
  transition: transform 0.25s ease, box-shadow 0.25s ease, top 0.08s linear, left 0.08s linear;
  overflow: visible;
}

.center-node {
  z-index: 10;
  box-shadow: 0 4px 24px rgba(0, 123, 255, 0.25);
  border: 3px solid rgba(0, 123, 255, 0.3);

  &__pulse {
    position: absolute;
    inset: -6px;
    border-radius: 50%;
    border: 2px solid rgba(0, 123, 255, 0.15);
    animation: pulse-ring 2.5s ease-out infinite;
  }
}

@keyframes pulse-ring {
  0% { transform: scale(1); opacity: 1; }
  100% { transform: scale(1.6); opacity: 0; }
}

.sub-node {
  cursor: pointer;
  overflow: visible;

  &--hovered {
    box-shadow: 0 6px 24px rgba(0, 123, 255, 0.2);
    border-color: rgba(0, 123, 255, 0.4);
  }

  &__badge {
    position: absolute;
    top: -6px;
    right: -6px;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: 9px;
    background: #ef4444;
    color: #fff;
    font-size: 10px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 2px solid #fff;
    box-shadow: 0 1px 4px rgba(239, 68, 68, 0.3);
    z-index: 1;
  }

  &__label {
    position: absolute;
    bottom: -18px;
    left: 50%;
    transform: translateX(-50%);
    font-size: 10px;
    font-weight: 600;
    color: #888;
    white-space: nowrap;
    pointer-events: none;
    letter-spacing: -0.3px;
    text-shadow: 0 0 3px #fff, 0 0 3px #fff;
  }
}

.node-tooltip {
  position: absolute;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 6px;
  white-space: nowrap;
  pointer-events: none;
  z-index: 100;
}

// 노드 팝오버
.node-popover {
  position: absolute;
  width: 240px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(16px);
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px;
  z-index: 200;
  display: flex;
  flex-direction: column;
  gap: 12px;

  &__header {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &__avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    object-fit: cover;
    border: 2px solid #eee;
    background: #f5f5f5;
    flex-shrink: 0;
  }

  &__title {
    display: flex;
    flex-direction: column;
    gap: 1px;
    min-width: 0;
  }

  &__nick {
    font-size: 14px;
    font-weight: 700;
    color: #222;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__url {
    font-size: 11px;
    color: #999;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__stats {
    display: flex;
    gap: 12px;
  }

  &__stat {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #666;
    font-weight: 500;
  }

  &__stat-icon {
    font-size: 13px;
  }

  &__visit {
    width: 100%;
    padding: 9px 0;
    border: none;
    border-radius: 10px;
    background: #007bff;
    color: #fff;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.2s;
    text-align: center;

    &:hover {
      background: #0056b3;
    }
  }
}

// 팝오버 트랜지션
.popover-enter-active,
.popover-leave-active {
  transition: opacity 0.2s ease, transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.popover-enter-from,
.popover-leave-to {
  opacity: 0;
  transform: scale(0.92) translateY(4px);
}

// 모달
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(4px);
}

.login-modal,
.signup-modal,
.subscribe-modal {
  width: 340px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12);
  padding: 32px;
}

.modal-content {
  width: 100%;

  h2 {
    margin: 0 0 24px;
    text-align: center;
    font-weight: 700;
    font-size: 20px;
    color: #222;
  }

  form {
    display: flex;
    flex-direction: column;
  }

  label {
    margin-bottom: 6px;
    font-size: 13px;
    font-weight: 500;
    color: #555;
  }

  input {
    margin-bottom: 16px;
    padding: 11px 14px;
    border: 1px solid #e8e8e8;
    border-radius: 10px;
    background-color: #f8f9fa;
    font-size: 14px;
    transition: border-color 0.2s, background-color 0.2s;

    &:focus {
      outline: none;
      border-color: #007bff;
      background-color: #fff;
    }

    &::placeholder {
      color: #bbb;
    }
  }
}

.modal-hint {
  font-size: 12px;
  color: #999;
  margin: -8px 0 16px;
  line-height: 1.4;
}

.logout-info {
  p {
    text-align: center;
    color: #666;
    margin-bottom: 20px;
  }
}

.button-group {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}

.btn {
  flex: 1;
  padding: 11px 16px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &--primary {
    background: #007bff;
    color: #fff;

    &:hover:not(:disabled) {
      background: #0056b3;
      transform: translateY(-1px);
    }
  }

  &--secondary {
    background: #f0f0f0;
    color: #666;

    &:hover:not(:disabled) {
      background: #e5e5e5;
    }
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

// 트랜지션
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.25s ease;

  .modal-content {
    transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;

  .modal-content {
    transform: scale(0.95) translateY(8px);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

// 모바일
@media (max-width: 767px) {
  .login-modal,
  .signup-modal,
  .subscribe-modal {
    width: calc(100vw - 48px);
    max-width: 340px;
  }

  .node-tooltip {
    display: none;
  }
}
</style>
