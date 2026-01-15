<template>
  <div id="app">
    <svg id="svg">
      <!-- 다른 노드와 중앙 노드를 선으로 연결 -->
      <line
          v-for="(node, index) in visibleNodes"
          :key="'line' + index"
          :x1="centerNode.x"
          :y1="centerNode.y"
          :x2="node.position.x + nodeSize / 2"
          :y2="node.position.y + nodeSize / 2"
          stroke="black"
          stroke-width="0.5"
          stroke-opacity="0.2"
      />
    </svg>
    
    <!-- 중앙 노드 (사용자 본인) -->
    <div
      class="node center-node"
      :style="{
        width: centerNodeSize + 'px',
        height: centerNodeSize + 'px',
        top: (centerNode.y - centerNodeSize / 2) + 'px',
        left: (centerNode.x - centerNodeSize / 2) + 'px',
        backgroundImage: 'url(https://api.dicebear.com/7.x/initials/svg?seed=Me&backgroundColor=000000)'
      }"
    ></div>

    <!-- 서브 노드들 (구독 블로그) -->
    <div
        v-for="(node, index) in visibleNodes"
        :key="index"
        class="node sub-node"
        :style="{ ...node.style, width: nodeSize + 'px', height: nodeSize + 'px' }"
        @mouseover="handleMouseOver(index)"
        @mouseleave="handleMouseLeave(index)"
    >
    </div>

    <!-- 모달: 로그인/로그아웃 -->
    <div v-if="showLoginModal" class="login-modal">
      <div v-if="!isLoggedIn" class="modal-content">
        <h2>Login</h2>
        <form @submit.prevent="handleSigninRequest">
          <label for="userEmail">이메일:</label>
          <input type="text" id="userEmail" name="userEmail"/>
          <label for="password">비밀번호:</label>
          <input type="password" id="password" name="password"/>
          <div class="button-group">
            <button @click="handleOpenSignup()" type="button" class="signup-button">회원가입</button>
            <button type="submit" class="login-button">로그인</button>
          </div>
        </form>
      </div>
      <div v-else class="modal-content">
        <h2>Logout</h2>
        <div class="logout-info">
          <p>이미 로그인되어 있습니다.</p>
          <div class="button-group" style="justify-content: center;">
            <button @click="handleLogout" class="login-button" style="width: 100%;">로그아웃</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 모달: 회원가입 -->
    <div v-if="showSignupModal" class="signup-modal">
      <div class="modal-content">
        <h2>Sign Up</h2>
        <form @submit.prevent="handleSignupRequest">
          <label for="userEmail">메일:</label>
          <input type="text" id="userEmail" name="userEmail"/>
          <label for="password">비밀번호:</label>
          <input type="password" id="password" name="password"/>
          <label for="confirm-password">비밀번호 확인:</label>
          <input type="password" id="confirm-password" name="confirm-password"/>
          <div class="button-group">
            <button type="submit" class="signup-button signup-request-button">회원가입</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 모달: 블로그 구독하기 -->
    <div v-if="showSubscribeModal" class="subscribe-modal">
      <div class="modal-content">
        <h2>블로그 구독하기</h2>
        <form @submit.prevent="handleSubscribeRequest">
          <label for="rssUri">블로그 URL:</label>
          <input type="text" id="rssUri" name="rssUri"/>
          <div class="button-group">
            <button type="submit" class="subscribe-button">구독하기</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 사이드 탭 영역 (마우스 감지용) -->
    <div v-if="showSideTab" class="side-tab" @mouseover="handleMouseOverSideTab"
         @mouseleave="handleMouseLeaveSideTab"></div>
  </div>
</template>

<script lang="ts">
import {defineComponent, onMounted, reactive, ref} from 'vue';
import {useRouter} from 'vue-router';
import {useAuthStore} from '@/stores';
import {fetchWrapper} from '@/utils/fetch-wrapper';
import {getAccessTokenFromCookie, deleteCookieFromBrowser} from '@/utils/cookie';

// Node 인터페이스 정의
interface Node {
  position: { x: number; y: number }; // 현재 위치
  velocity: { x: number; y: number }; // 현재 속도
  initialPosition: { x: number; y: number }; // 초기 위치
  bounds: { minX: number; maxX: number; minY: number; maxY: number }; // 이동 범위
  style: Record<string, string>; // 스타일
  isStopped: boolean; // 멈춤 상태 여부
  thumbnailUrl?: string; // 블로그 썸네일 URL
  nickName?: string; // 블로그 닉네임
}

export default defineComponent({
  name: 'App',
  setup() {
    const router = useRouter(); 
    const authStore = useAuthStore();

    const centerNode = reactive({x: window.innerWidth / 2, y: window.innerHeight / 2}); 
    const centerNodeSize = 60; 
    const nodeSize = 40; 
    const minDistance = 200; 
    const maxDistance = 250; 
    const visibleNodeCount = ref(20); 
    const range = 100; 

    const initialSpeed = 1; 

    const getRandomPosition = () => {
      const angle = Math.random() * Math.PI * 2;
      const r = minDistance + Math.random() * (maxDistance - minDistance);
      return {
        x: centerNode.x + r * Math.cos(angle),
        y: centerNode.y + r * Math.sin(angle),
      };
    };

    const getRandomVelocity = () => {
      return {
        x: (Math.random() * 2 - 1) * initialSpeed,
        y: (Math.random() * 2 - 1) * initialSpeed,
      };
    };

    const getInitialBounds = (initialPosition: { x: number; y: number }, range: number) => {
      return {
        minX: initialPosition.x - range,
        maxX: initialPosition.x + range,
        minY: initialPosition.y - range,
        maxY: initialPosition.y + range,
      };
    };

    const nodes = reactive<Node[]>([]); 
    const visibleNodes = ref<Node[]>([]); 

    const fetchBlogSubscriptions = async () => {
      try {
        const blogs = await fetchWrapper.get('/api/subscriptions/blogs/close', null);
        nodes.splice(0, nodes.length, ...blogs.map(createNodeFromBlog));
        visibleNodes.value = nodes.slice(0, visibleNodeCount.value);
      } catch (error) {
        console.error('Error fetching blog subscriptions:', error);
      }
    };

    const createNodeFromBlog = (blog: any): Node => {
      const initialPosition = getRandomPosition();
      const bounds = getInitialBounds(initialPosition, range);
      // 썸네일이 없을 경우 닉네임을 기반으로 한 아바타 이미지 사용
      const displayThumb = blog.thumbnailUrl || `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(blog.nickName || 'B')}`;
      
      return {
        position: initialPosition,
        velocity: getRandomVelocity(),
        initialPosition,
        bounds,
        style: { 
          backgroundImage: `url(${displayThumb})`,
          top: `${initialPosition.y}px`,
          left: `${initialPosition.x}px`
        },
        isStopped: false,
        thumbnailUrl: blog.thumbnailUrl,
        nickName: blog.nickName,
      };
    };

    let intervalId: number | null = null;

    const startMovement = () => {
      intervalId = setInterval(() => {
        nodes.forEach((node) => {
          if (!node.isStopped) {
            node.position.x += node.velocity.x;
            node.position.y += node.velocity.y;

            node.velocity.x += (Math.random() * 2 - 1) * 0.01;
            node.velocity.y += (Math.random() * 2 - 1) * 0.01;

            const maxSpeed = 1.3; 
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
      }, 100);
    };

    const handleMouseOver = (index: number) => {
      nodes.forEach((node, i) => {
        if (i !== index) {
          node.isStopped = true;
        }
      });
      nodes[index].style.transform = 'scale(1.5)';
      nodes[index].style.zIndex = '1';
    };

    const handleMouseLeave = (index: number) => {
      nodes.forEach((node) => {
        node.isStopped = false;
      });
      nodes[index].style.transform = 'scale(1)';
      nodes[index].style.zIndex = '0';
    };

    const showLoginModal = ref(false);
    const showSignupModal = ref(false);
    const showSubscribeModal = ref(false);
    const showSideTab = ref(false);

    const isLoggedIn = ref(!!getAccessTokenFromCookie());

    const isMouseOverSideTab = ref(false);

    const handleMouseOverSideTab = () => {
      isMouseOverSideTab.value = true;
    };

    const handleMouseLeaveSideTab = () => {
      isMouseOverSideTab.value = false;
      if (!showSideTab.value) {
        resetScreenPosition();
      }
    };

    let isMouseOverRightEdge = false; 
    let isMouseOverLeftEdge = false; 

    const edgeThreshold = 150; 
    let leftEdgeCounter = 0;
    let rightEdgeCounter = 0;

    const handleMouseMove = (event: MouseEvent) => {
      const {clientX} = event;

      if (clientX >= window.innerWidth - edgeThreshold) {
        if (!isMouseOverRightEdge) {
          moveScreen('left');
          showLoginModal.value = false;
          showSubscribeModal.value = true;
          isMouseOverRightEdge = true;
          rightEdgeCounter++;
          leftEdgeCounter = 0;
        }
      } else if (isMouseOverRightEdge) {
        resetScreenPosition();
        isMouseOverRightEdge = false;
      }

      if (clientX <= edgeThreshold) {
        if (!isMouseOverLeftEdge) {
          moveScreen('right');
          showSubscribeModal.value = false;
          isLoggedIn.value = !!getAccessTokenFromCookie();
          showLoginModal.value = true;
          isMouseOverLeftEdge = true;
          leftEdgeCounter++;
          rightEdgeCounter = 0;
        }
      } else if (isMouseOverLeftEdge) {
        resetScreenPosition();
        isMouseOverLeftEdge = false;
      }

      if (leftEdgeCounter >= 2) {
        showLoginModal.value = false;
        leftEdgeCounter = 0;
      }

      if (rightEdgeCounter >= 2) {
        showSubscribeModal.value = false;
        rightEdgeCounter = 0;
      }
    };

    const handleSigninRequest = async (event: Event) => {
      event.preventDefault();
      const email = (document.getElementById('userEmail') as HTMLInputElement).value;
      const password = (document.getElementById('password') as HTMLInputElement).value;

      return authStore.login(email, password)
          .then(() => {
            alert("로그인이 완료되었습니다.");
            isLoggedIn.value = true;
            showLoginModal.value = false;
          });
    };

    const handleLogout = () => {
      deleteCookieFromBrowser('accessToken');
      deleteCookieFromBrowser('refreshToken');
      isLoggedIn.value = false;
      showLoginModal.value = false;
      alert("로그아웃되었습니다.");
    };

    const handleOpenSignup = () => {
      showLoginModal.value = false;
      showSignupModal.value = true;
    };

    const handleSignupRequest = async (event: Event) => {
      event.preventDefault();
      const email = (document.getElementById('userEmail') as HTMLInputElement).value;
      const password = (document.getElementById('password') as HTMLInputElement).value;
      const confirmPassword = (document.getElementById('confirm-password') as HTMLInputElement).value;

      if (password !== confirmPassword) {
        alert("패스워드가 일치하지 않습니다.");
        return;
      }

      fetchWrapper.post("/api/member/auth/signup", {
        email: email,
        password: password,
        confirmPassword: confirmPassword
      })
          .then(() => {
            alert("회원가입이 완료되었습니다.");
            showSignupModal.value = false;
          });
    };

    const handleSubscribeRequest = async (event: Event) => {
      event.preventDefault();
      const rssUri = (document.getElementById('rssUri') as HTMLInputElement).value;

      fetchWrapper.post("/api/subscriptions", {
        rssUri: rssUri
      })
          .then(() => {
            alert("블로그 구독이 완료되었습니다.");
            showSubscribeModal.value = false;
            fetchBlogSubscriptions(); // 목록 갱신
          })
          .catch((error) => {
            console.log(error);
            alert(error);
          });
    };

    const moveScreen = (direction: 'left' | 'right') => {
      const app = document.getElementById('app');
      if (app) app.style.transform = direction === 'left' ? 'translateX(-100px)' : 'translateX(100px)';
    };

    const resetScreenPosition = () => {
      if (!isMouseOverSideTab.value) {
        const app = document.getElementById('app');
        if (app) app.style.transform = 'translateX(0)';
      }
    };

    onMounted(() => {
      fetchBlogSubscriptions();
      startMovement();
      window.addEventListener('mousemove', handleMouseMove);
    });

    return {
      centerNode,
      centerNodeSize,
      nodeSize,
      nodes,
      handleMouseOver,
      handleMouseLeave,
      handleOpenSignup,
      handleSignupRequest,
      handleSigninRequest,
      handleSubscribeRequest,
      handleLogout,
      showLoginModal,
      showSignupModal,
      showSubscribeModal,
      showSideTab,
      isLoggedIn,
      handleMouseOverSideTab,
      handleMouseLeaveSideTab,
      visibleNodes,
      visibleNodeCount,
    };
  },
});
</script>

<style lang="scss">
#app {
  position: relative;
  width: 100vw;
  height: 100vh;
  background-color: white;
  overflow: hidden;
  transition: transform 0.3s ease;
}

#svg {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none; /* SVG가 클릭을 방해하지 않도록 */
}

.node {
  position: absolute;
  background-color: #f0f0f0;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  border: 4px solid white;
  box-shadow: 0 4px 15px rgba(0,0,0,0.3);
  transition: transform 0.3s, z-index 0.3s, top 0.1s, left 0.1s, width 0.3s, height 0.3s;
  overflow: hidden;
}

.center-node {
  z-index: 10;
  box-shadow: 0 4px 20px rgba(0,0,0,0.4);
}

.sub-node {
  cursor: pointer;
}

.login-modal, .signup-modal, .subscribe-modal {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 320px;
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: none;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 30px;
}

.modal-content {
  width: 100%;
}

.modal-content h2 {
  margin-bottom: 25px;
  text-align: center;
  font-weight: 700;
  color: #333;
}

.modal-content form {
  display: flex;
  flex-direction: column;
}

.modal-content label {
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
}

.modal-content input {
  margin-bottom: 20px;
  padding: 12px 15px;
  border: 1px solid #eee;
  border-radius: 12px;
  background-color: #f9f9f9;
  font-size: 16px;
  transition: border-color 0.3s;
  
  &:focus {
    outline: none;
    border-color: #007bff;
  }
}

.button-group {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.signup-button,
.login-button,
.subscribe-button {
  flex: 1;
  padding: 12px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, background-color 0.3s;

  &:hover {
    background-color: #0056b3;
    transform: translateY(-2px);
  }

  &:active {
    transform: translateY(0);
  }
}

.signup-request-button {
  width: 100%;
}

.side-tab {
  position: fixed;
  top: 0;
  height: 100%;
  width: 20px;
  z-index: 500;
  /* background-color: rgba(0,0,0,0.05); 디버깅용 */
}
</style>
