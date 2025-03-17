To achieve this, you need to add a new modal for "블로그 구독하기" and modify the `handleMouseMove` function to handle the right edge for showing this new modal. Here is the updated code:

```vue
<template>
  <div id="app">
    <svg id="svg">
      <!-- 중앙 노드를 원 형태로 표시 -->
      <circle :cx="centerNode.x" :cy="centerNode.y" :r="centerNodeSize / 2" fill="black"/>
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
    <div
        v-for="(node, index) in visibleNodes"
        :key="index"
        class="node"
        :style="{ ...node.style, width: nodeSize + 'px', height: nodeSize + 'px' }"
        @mouseover="handleMouseOver(index)"
        @mouseleave="handleMouseLeave(index)"
    ></div>
    <div v-if="showLoginModal" class="login-modal">
      <div class="modal-content">
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
    </div>
    <div v-if="showSignupModal" class="signup-modal">
      <div class="modal-content">
        <h2>Login</h2>
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
    <div v-if="showSubscribeModal" class="subscribe-modal">
      <div class="modal-content">
        <h2>블로그 구독하기</h2>
        <form @submit.prevent="handleSubscribeRequest">
          <label for="blogUrl">블로그 URL:</label>
          <input type="text" id="blogUrl" name="blogUrl"/>
          <div class="button-group">
            <button type="submit" class="subscribe-button">구독하기</button>
          </div>
        </form>
      </div>
    </div>
    <div v-if="showSideTab" class="side-tab" @mouseover="handleMouseOverSideTab"
         @mouseleave="handleMouseLeaveSideTab"></div>
  </div>
</template>

<script lang="ts">
import { defineComponent, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores';
import { fetchWrapper } from '@/utils/fetch-wrapper';

// Node 인터페이스 정의
interface Node {
  position: { x: number; y: number }; // 현재 위치
  velocity: { x: number; y: number }; // 현재 속도
  initialPosition: { x: number; y: number }; // 초기 위치
  bounds: { minX: number; maxX: number; minY: number; maxY: number }; // 이동 범위
  style: Record<string, string>; // 스타일
  isStopped: boolean; // 멈춤 상태 여부
}

export default defineComponent({
  name: 'App',
  setup() {
    const router = useRouter(); // 라우터
    const authStore = useAuthStore();

    const centerNode = reactive({ x: window.innerWidth / 2, y: window.innerHeight / 2 }); // 중앙 노드의 위치
    const centerNodeSize = 60; // 중앙 노드의 크기
    const nodeSize = 40; // 서브노드의 크기
    const minDistance = 200; // 중앙에서 최소 거리
    const maxDistance = 250; // 중앙에서 최대 거리
    const visibleNodeCount = ref(20); // 화면에 보이는 노드의 개수
    const range = 100; // 서브노드의 이동 범위

    // 초기 속도 범위 설정
    const initialSpeed = 1; // 초기 속도 범위 값 조절

    // 랜덤 위치 생성 함수
    const getRandomPosition = () => {
      const angle = Math.random() * Math.PI * 2;
      const r = minDistance + Math.random() * (maxDistance - minDistance);
      return {
        x: centerNode.x + r * Math.cos(angle),
        y: centerNode.y + r * Math.sin(angle),
      };
    };

    // 랜덤 속도 생성 함수
    const getRandomVelocity = () => {
      return {
        x: (Math.random() * 2 - 1) * initialSpeed,
        y: (Math.random() * 2 - 1) * initialSpeed,
      };
    };

    // 초기 위치를 기준으로 이동 범위 생성 함수
    const getInitialBounds = (initialPosition: { x: number; y: number }, range: number) => {
      return {
        minX: initialPosition.x - range,
        maxX: initialPosition.x + range,
        minY: initialPosition.y - range,
        maxY: initialPosition.y + range,
      };
    };

    // 서브노드 생성 함수
    const createNode = () => {
      const initialPosition = getRandomPosition();
      const bounds = getInitialBounds(initialPosition, range);
      return {
        position: initialPosition,
        velocity: getRandomVelocity(),
        initialPosition,
        bounds,
        style: {},
        isStopped: false,
      };
    };

    const nodes = reactive<Node[]>(Array.from({ length: 0 }, createNode)); // 서브노드 배열 생성
    const visibleNodes = ref<Node[]>([]); // 화면에 보이는 노드 배열

    const fetchBlogSubscriptions = async () => {
      try {
        const blogs = await fetchWrapper.get('/api/subscriptions/blogs/close');

        // Update nodes and visibleNodes with the fetched data
        nodes.splice(0, nodes.length, ...blogs.map(createNodeFromBlog));
        visibleNodes.value = nodes.slice(0, visibleNodeCount.value);
      } catch (error) {
        console.error('Error fetching blog subscriptions:', error);
      }
    };

    const createNodeFromBlog = (blog: any): Node => {
      const initialPosition = getRandomPosition();
      const bounds = getInitialBounds(initialPosition, range);
      return {
        position: initialPosition,
        velocity: getRandomVelocity(),
        initialPosition,
        bounds,
        style: {},
        isStopped: false,
      };
    };

    let intervalId: number | null = null;

    // 서브노드 움직임 함수
    const startMovement = () => {
      intervalId = setInterval(() => {
        nodes.forEach((node) => {
          if (!node.isStopped) {
            node.position.x += node.velocity.x;
            node.position.y += node.velocity.y;

            // 자연스러운 움직임을 위한 속도 변화
            node.velocity.x += (Math.random() * 2 - 1) * 0.01;
            node.velocity.y += (Math.random() * 2 - 1) * 0.01;

            // 최대 속도 제한 설정
            const maxSpeed = 1.3; // 최대 속도 값 조절
            node.velocity.x = Math.max(Math.min(node.velocity.x, maxSpeed), -maxSpeed);
            node.velocity.y = Math.max(Math.min(node.velocity.y, maxSpeed), -maxSpeed);

            // 노드가 이동 범위를 벗어나면 반전
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

        // 화면에 보이는 노드 업데이트
        visibleNodes.value = nodes.slice(0, visibleNodeCount.value);
      }, 100);
    };

    // 노드 마우스 오버 이벤트 핸들러
    const handleMouseOver = (index: number) => {
      nodes.forEach((node, i) => {
        if (i !== index) {
          node.isStopped = true;
        }
      });
      nodes[index].style.transform = 'scale(1.5)';
      nodes[index].style.zIndex = '1';
    };

    // 노드 마우스 리브 이벤트 핸들러
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

    const isMouseOverSideTab = ref(false);

    // 사이드탭 마우스 오버 핸들러
    const handleMouseOverSideTab = () => {
      isMouseOverSideTab.value = true;
    };

    // 사이드탭 마우스 리브 핸들러
    const handleMouseLeaveSideTab = () => {
      isMouseOverSideTab.value = false;
      if (!showSideTab.value) {
        resetScreenPosition();
      }
    };

    // 마우스 이동 핸들러
    let isMouseOverRightEdge = false; // 오른쪽 끝 임계값에 마우스가 있는지 여부
    let isMouseOverLeftEdge = false; // 오른쪽 끝 임계값에 마우스가 있는지 여부

    const edgeThreshold = 150; // 좌우 끝으로 인식할 임계값
    let leftEdgeCounter = 0;
    let rightEdgeCounter = 0;

    const handleMouseMove = (event: MouseEvent) => {
      const { clientX } = event;

      if (clientX >= window.innerWidth - edgeThreshold) {
        // 오른쪽 끝으로 마우스를 이동했을 때
        if (!isMouseOverRightEdge) {
          moveScreen('left'); // 화면 왼쪽으로 이동
          showLoginModal.value = false; // 로그인 모달 닫기
          showSubscribeModal.value = true; // 블로그 구독 모달 열기
          isMouseOverRightEdge = true; // 마우스 위치 업데이트
          rightEdgeCounter++;
          leftEdgeCounter = 0;
        }
      } else if (isMouseOverRightEdge) {
        // 오른쪽 끝 임계값을 벗어났을 때
        resetScreenPosition(); // 화면 위치 초기화
        isMouseOverRightEdge = false; // 마우스 위치 업데이트
      }

      if (clientX <= edgeThreshold) {
        // 왼쪽 끝으로 마우스를 이동했을 때
        if (!isMouseOverLeftEdge) {
          moveScreen('right'); // 화면 오른쪽으로 이동
          showSubscribeModal.value = false; // 블로그 구독 모달 닫기
          showLoginModal.value = true; // 로그인 모달 열기
          isMouseOverLeftEdge = true; // 마우스 위치 업데이트
          leftEdgeCounter++;
          rightEdgeCounter = 0;
        }
      } else if (isMouseOverLeftEdge) {
        // 왼쪽 끝 임계값을 벗어났을 때
        resetScreenPosition(); // 화면 위치 초기화
        isMouseOverLeftEdge = false; // 마우스 위치 업데이트
      }

      // 모달이 두 번 열리면 닫기
      if (leftEdgeCounter >= 2) {
        showLoginModal.value = false;
        leftEdgeCounter = 0;
      }

      if (rightEdgeCounter >= 2) {
        showSubscribeModal.value = false;
        rightEdgeCounter = 0;
      }
    };

    // 로그인 요청
    const handleSigninRequest = async (event: Event) => {
      event.preventDefault();
      const email = (document.getElementById('userEmail') as HTMLInputElement).value;
      const password = (document.getElementById('password') as HTMLInputElement).value;

      return authStore.login(email, password)
          .then(() => {
            alert("로그인이 완료되었습니다.");
            showLoginModal.value = false;
          });
    };

    // 회원가입 버튼 클릭 핸들러
    const handleOpenSignup = () => {
      // 회원가입 모달로 변경
      showLoginModal.value = false;
      showSignupModal.value = true;
    };

    // 회원가입 요청
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

    // 블로그 구독 요청
    const handleSubscribeRequest = async (event: Event) => {
      event.preventDefault();
      const blogUrl = (document.getElementById('blogUrl') as HTMLInputElement).value;


      fetchWrapper.post("/api/subscriptions", {
        url: blogUrl
      })
          .then(() => {
            alert("블로그 구독이 완료되었습니다.");
            showSubscribeModal.value = false;
          })
          .catch((error) => {
            console.log(error);
            alert(error);
          });
    };

    // 화면 이동 함수
    const moveScreen = (direction: 'left' | 'right') => {
      document.getElementById('app')!.style.transform = direction === 'left' ? 'translateX(-100px)' : 'translateX(100px)';
    };

    // 화면 위치 초기화 함수
    const resetScreenPosition = () => {
      if (!isMouseOverSideTab.value) {
        document.getElementById('app')!.style.transform = 'translateX(0)';
      }
    };

    // 컴포넌트 마운트 시 실행되는 함수
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
      showLoginModal,
      showSignupModal,
      showSubscribeModal,
      showSideTab,
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
}

.node {
  position: absolute;
  background-color: black;
  border-radius: 50%;
  transition: transform 0.3s, z-index 0.3s, top 0.1s, left 0.1s, width 0.3s, height 0.3s; /* 변환, z-index, 상단, 왼쪽, 너비, 높이에 대한 트랜지션 효과 */
}

/* 중앙 노드 스타일 */
#svg circle {
  fill: black;
}

.login-modal, .signup-modal, .subscribe-modal {
  /* 로그인, 회원가입, 블로그 구독 모달 스타일 */
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 300px;
  background-color: white;
  border: 1px solid #ccc;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.modal-content {
  width: 100%;
}

.modal-content h2 {
  margin-bottom: 20px;
  text-align: center;
}

.modal-content form {
  display: flex;
  flex-direction: column;
}

.modal-content label {
  margin-bottom: 5px;
}

.modal-content input {
  margin-bottom: 15px;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
}

.button-group {
  display: flex;
  justify-content: space-between;
}

.signup-button,
.login-button,
.subscribe-button {
  width: 48%;
  padding: 10px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.signup-request-button {
  width: 100%; /* 버튼을 부모 요소의 너비에 맞춤 */
  display: flex;
  justify-content: center; /* 버튼 내용물을 수평 가운데 정렬 */
}

.signup-button:hover,
.login-button:hover {
  background-color: #0056b3;
}

.side-tab {
  /* 사이드 탭 스타일 */
}
</style>

