import { ref, onMounted, onUnmounted, watch, nextTick, type Ref } from 'vue';

interface DraggableOptions {
  storageKey: string;
  /** Default distance from right edge (default 16) */
  defaultRight?: number;
  /** Default distance from bottom edge (default 16) */
  defaultBottom?: number;
  /** Boundary element ref — positions are relative to this element */
  boundary?: Ref<HTMLElement | null>;
}

/**
 * Makes an element draggable via touch/mouse.
 * Default position: bottom-right corner.
 * Saves position to localStorage so it persists across reloads.
 */
export function useDraggable(
  elRef: Ref<HTMLElement | null>,
  options: DraggableOptions
) {
  const x = ref(0);
  const y = ref(0);

  let isDragging = false;
  let startX = 0;
  let startY = 0;
  let startElX = 0;
  let startElY = 0;
  let hasMoved = false;
  let attached = false;

  const DRAG_THRESHOLD = 5;

  function clamp(val: number, mn: number, mx: number) {
    return Math.max(mn, Math.min(mx, val));
  }

  function getContainerSize() {
    const parent = options.boundary?.value;
    return {
      w: parent ? parent.clientWidth : window.innerWidth,
      h: parent ? parent.clientHeight : window.innerHeight,
    };
  }

  function getElSize() {
    const el = elRef.value;
    if (!el) return { elW: 0, elH: 0 };
    return { elW: el.offsetWidth, elH: el.offsetHeight };
  }

  function loadPosition() {
    const { elW, elH } = getElSize();
    const { w, h } = getContainerSize();

    try {
      const saved = localStorage.getItem(options.storageKey);
      if (saved) {
        const pos = JSON.parse(saved);
        x.value = clamp(pos.x, 0, Math.max(0, w - elW));
        y.value = clamp(pos.y, 0, Math.max(0, h - elH));
        return;
      }
    } catch {}

    // Default: bottom-right
    const dr = options.defaultRight ?? 16;
    const db = options.defaultBottom ?? 16;
    x.value = Math.max(0, w - elW - dr);
    y.value = Math.max(0, h - elH - db);
  }

  function savePosition() {
    try {
      localStorage.setItem(options.storageKey, JSON.stringify({ x: x.value, y: y.value }));
    } catch {}
  }

  // ── Mouse ──
  function onMouseDown(e: MouseEvent) {
    if ((e.target as HTMLElement).closest('button')) return;
    isDragging = true;
    hasMoved = false;
    startX = e.clientX;
    startY = e.clientY;
    startElX = x.value;
    startElY = y.value;
    e.preventDefault();
    e.stopPropagation();
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  }

  function onMouseMove(e: MouseEvent) {
    if (!isDragging) return;
    const dx = e.clientX - startX;
    const dy = e.clientY - startY;
    if (!hasMoved && Math.abs(dx) < DRAG_THRESHOLD && Math.abs(dy) < DRAG_THRESHOLD) return;
    hasMoved = true;
    const { w, h } = getContainerSize();
    const { elW, elH } = getElSize();
    x.value = clamp(startElX + dx, 0, Math.max(0, w - elW));
    y.value = clamp(startElY + dy, 0, Math.max(0, h - elH));
  }

  function onMouseUp() {
    isDragging = false;
    document.removeEventListener('mousemove', onMouseMove);
    document.removeEventListener('mouseup', onMouseUp);
    if (hasMoved) savePosition();
  }

  // ── Touch ──
  function onTouchStart(e: TouchEvent) {
    if ((e.target as HTMLElement).closest('button')) return;
    isDragging = true;
    hasMoved = false;
    const t = e.touches[0];
    startX = t.clientX;
    startY = t.clientY;
    startElX = x.value;
    startElY = y.value;
  }

  function onTouchMove(e: TouchEvent) {
    if (!isDragging) return;
    const t = e.touches[0];
    const dx = t.clientX - startX;
    const dy = t.clientY - startY;
    if (!hasMoved && Math.abs(dx) < DRAG_THRESHOLD && Math.abs(dy) < DRAG_THRESHOLD) return;
    hasMoved = true;
    e.preventDefault();
    e.stopPropagation();
    const { w, h } = getContainerSize();
    const { elW, elH } = getElSize();
    x.value = clamp(startElX + dx, 0, Math.max(0, w - elW));
    y.value = clamp(startElY + dy, 0, Math.max(0, h - elH));
  }

  function onTouchEnd(e: TouchEvent) {
    if (!isDragging) return;
    isDragging = false;
    if (hasMoved) {
      e.preventDefault();
      e.stopPropagation();
      savePosition();
    }
  }

  function attach() {
    const el = elRef.value;
    if (!el || attached) return;
    attached = true;
    el.addEventListener('mousedown', onMouseDown);
    el.addEventListener('touchstart', onTouchStart, { passive: false });
    el.addEventListener('touchmove', onTouchMove, { passive: false });
    el.addEventListener('touchend', onTouchEnd, { passive: false });
  }

  function detach() {
    const el = elRef.value;
    if (!el) return;
    attached = false;
    el.removeEventListener('mousedown', onMouseDown);
    el.removeEventListener('touchstart', onTouchStart);
    el.removeEventListener('touchmove', onTouchMove);
    el.removeEventListener('touchend', onTouchEnd);
    document.removeEventListener('mousemove', onMouseMove);
    document.removeEventListener('mouseup', onMouseUp);
  }

  watch(elRef, (el) => {
    if (el && !attached) {
      nextTick(() => { loadPosition(); attach(); });
    }
  });

  onMounted(() => {
    nextTick(() => {
      if (elRef.value) { loadPosition(); attach(); }
    });
  });

  onUnmounted(() => { detach(); });

  function wasDrag() { return hasMoved; }

  return { x, y, wasDrag, loadPosition };
}
