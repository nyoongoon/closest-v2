<template>
  <div class="main-canvas" ref="canvasRef">
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
        backgroundImage: 'url(https://api.dicebear.com/7.x/initials/svg?seed=Me&backgroundColor=007bff)'
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
      @click="handleNodeClick(index)"
    >
      <!-- 새 포스트 뱃지 -->
      <span v-if="node.newPostsCnt && node.newPostsCnt > 0" class="sub-node__badge">
        {{ node.newPostsCnt }}
      </span>
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

    <!-- PostPanel -->
    <PostPanel
      v-if="selectedBlog"
      :blogInfo="selectedBlog"
      @close="selectedBlog = null"
    />

    <!-- 노드 없을 때 안내 -->
    <div v-if="visibleNodes.length === 0 && !isLoadingNodes" class="main-canvas__empty">
      <div class="main-canvas__empty-icon">◉</div>
      <p class="main-canvas__empty-title">아직 구독 중인 블로그가 없어요</p>
      <p class="main-canvas__empty-desc">상단의 '+ 구독' 버튼으로 블로그를 추가해보세요</p>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, onMounted, onUnmounted, reactive, ref, computed, watch } from 'vue';
import { useAuthStore } from '@/stores';
import { useSubscriptionStore } from '@/stores/subscription';
import { useToast } from '@/composables/useToast';
import { getAccessTokenFromCookie, deleteCookieFromBrowser } from '@/utils/cookie';
import { authApi } from '@/services/api';
import PostPanel from '@/components/PostPanel.vue';
import type { BlogInfo, BlogNode } from '@/types';

export default defineComponent({
  name: 'MainView',
  components: { PostPanel },
  props: {
    isLoggedIn: { type: Boolean, default: false },
    showLoginModal: { type: Boolean, default: false },
    showSubscribeModal: { type: Boolean, default: false },
  },
  emits: ['update:isLoggedIn', 'update:showLoginModal', 'update:showSubscribeModal'],
  setup(props, { emit }) {
    const authStore = useAuthStore();
    const subscriptionStore = useSubscriptionStore();
    const { showToast } = useToast();

    const canvasRef = ref<HTMLElement | null>(null);

    // 반응형
    const isMobile = ref(window.innerWidth < 768);
    const currentCenterNodeSize = computed(() => isMobile.value ? 48 : 64);
    const currentNodeSize = computed(() => isMobile.value ? 36 : 44);

    const centerNode = reactive({ x: window.innerWidth / 2, y: (window.innerHeight / 2) + 28 });
    const minDistance = computed(() => isMobile.value ? 120 : 180);
    const maxDistance = computed(() => isMobile.value ? 200 : 280);
    const visibleNodeCount = ref(20);
    const range = 80;
    const initialSpeed = 0.6;

    const hoveredIndex = ref<number | null>(null);
    const isLoadingNodes = ref(true);

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

    const fetchBlogSubscriptions = async () => {
      isLoadingNodes.value = true;
      try {
        await subscriptionStore.fetchCloseBlogs();
        const blogs = subscriptionStore.closeBlogs;
        nodes.splice(0, nodes.length, ...blogs.map(createNodeFromBlog));
        visibleNodes.value = nodes.slice(0, visibleNodeCount.value);
      } catch (error) {
        console.error('Error fetching blog subscriptions:', error);
      } finally {
        isLoadingNodes.value = false;
      }
    };

    const createNodeFromBlog = (blog: any): BlogNode => {
      const initialPosition = getRandomPosition();
      const bounds = getInitialBounds(initialPosition);
      const displayThumb =
        blog.thumbnailUrl ||
        `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(blog.nickName || 'B')}`;

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

      visibleNodes.value = nodes.slice(0, visibleNodeCount.value);
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

    // 노드 클릭 → PostPanel 오픈
    const selectedBlog = ref<BlogInfo | null>(null);

    const handleNodeClick = (index: number) => {
      const node = nodes[index];
      if (!node) return;
      selectedBlog.value = {
        subscriptionId: node.subscriptionId,
        blogUrl: node.blogUrl ?? '',
        nickName: node.nickName ?? '',
        thumbnailUrl: node.thumbnailUrl,
      };
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
      else if (selectedBlog.value) selectedBlog.value = null;
    };

    const handleResize = () => {
      isMobile.value = window.innerWidth < 768;
      centerNode.x = window.innerWidth / 2;
      centerNode.y = (window.innerHeight / 2) + 28;
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

    onMounted(() => {
      fetchBlogSubscriptions();
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
      visibleNodeCount,
      selectedBlog,
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
.main-canvas {
  position: relative;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(145deg, #fafbff 0%, #f5f7ff 50%, #fafbff 100%);
  overflow: hidden;

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
