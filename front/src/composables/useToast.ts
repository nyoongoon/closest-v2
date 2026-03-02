import { reactive } from 'vue';

export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  message: string;
  type: ToastType;
}

// 전역 이벤트 버스 (fetch-wrapper 등 Vue 컨텍스트 외부에서도 사용 가능)
type ToastEventPayload = { message: string; type?: ToastType; duration?: number };
type ToastEventHandler = (payload: ToastEventPayload) => void;

class ToastBus {
  private handlers: ToastEventHandler[] = [];

  on(handler: ToastEventHandler) {
    this.handlers.push(handler);
    return () => {
      this.handlers = this.handlers.filter((h) => h !== handler);
    };
  }

  emit(event: 'show', payload: ToastEventPayload) {
    this.handlers.forEach((h) => h(payload));
  }
}

export const toastBus = new ToastBus();

// 앱 전역 상태 (싱글톤)
let _idCounter = 0;
const toasts = reactive<Toast[]>([]);

export function useToast() {
  const showToast = (
    message: string,
    type: ToastType = 'info',
    duration = 3000
  ) => {
    const id = ++_idCounter;
    toasts.push({ id, message, type });

    setTimeout(() => {
      removeToast(id);
    }, duration);
  };

  const removeToast = (id: number) => {
    const idx = toasts.findIndex((t) => t.id === id);
    if (idx !== -1) toasts.splice(idx, 1);
  };

  return { toasts, showToast, removeToast };
}

// fetch-wrapper에서 emit한 이벤트를 useToast로 연결하는 초기화 함수
// App.vue의 setup()에서 한 번 호출
export function initToastBus() {
  const { showToast } = useToast();
  return toastBus.on(({ message, type = 'error', duration }) => {
    showToast(message, type, duration);
  });
}
