// ─── API Response Models ────────────────────────────────────────

export interface SubscriptionResponse {
  subscriptionId: number;
  nickName: string;
  uri: string;
  thumbnailUrl: string | null;
  newPostsCnt: number;
  visitCnt: number;
  publishedDateTime: string | null;
  blogUrl?: string;
}

export interface Post {
  id: number;
  title: string;
  link: string;
  publishedAt: string;
}

export interface BlogInfo {
  subscriptionId?: number;
  blogUrl: string;
  nickName: string;
  thumbnailUrl?: string;
}

// ─── Auth Models ────────────────────────────────────────────────

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  confirmPassword: string;
}

export interface User {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  jwtToken: string;
}

// ─── Subscription Models ────────────────────────────────────────

export interface SubscribeRequest {
  rssUri: string;
}

export interface StatusRequest {
  message: string;
}

// ─── UI Models ──────────────────────────────────────────────────

export interface BlogNode {
  position: { x: number; y: number };
  velocity: { x: number; y: number };
  initialPosition: { x: number; y: number };
  bounds: { minX: number; maxX: number; minY: number; maxY: number };
  style: Record<string, string>;
  isStopped: boolean;
  subscriptionId?: number;
  thumbnailUrl?: string;
  nickName?: string;
  blogUrl?: string;
  newPostsCnt?: number;
  visitCnt?: number;
}
