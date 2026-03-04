import { fetchWrapper } from '@/utils/fetch-wrapper';
import type {
  LoginRequest,
  SignupRequest,
  SubscribeRequest,
  SubscriptionResponse,
  Post,
  RecentPost,
  StatusRequest,
} from '@/types';

const AUTH_BASE = '/api/member/auth';
const SUBS_BASE = '/api/subscriptions';

// ─── Auth API ───────────────────────────────────────────────────

export const authApi = {
  signin(data: LoginRequest) {
    return fetchWrapper.post(`${AUTH_BASE}/signin`, data, { credentials: 'include' });
  },

  signup(data: SignupRequest) {
    return fetchWrapper.post(`${AUTH_BASE}/signup`, data);
  },

  refreshToken() {
    return fetchWrapper.post(`${AUTH_BASE}/refresh-token`, {}, { credentials: 'include' });
  },

  revokeToken() {
    return fetchWrapper.post(`${AUTH_BASE}/revoke-token`, {}, { credentials: 'include' });
  },
};

// ─── Subscription API ───────────────────────────────────────────

export const subscriptionApi = {
  getCloseBlogs(): Promise<SubscriptionResponse[]> {
    return fetchWrapper.get(`${SUBS_BASE}/blogs/close`, null, { silent: true });
  },

  getBlogs(page = 0, size = 20): Promise<SubscriptionResponse[]> {
    return fetchWrapper.get(`${SUBS_BASE}/blogs?page=${page}&size=${size}`, null, { silent: true });
  },

  subscribe(data: SubscribeRequest) {
    return fetchWrapper.post(SUBS_BASE, data);
  },

  unsubscribe(subscriptionId: number) {
    return fetchWrapper.delete(`${SUBS_BASE}/${subscriptionId}`, null);
  },

  visit(subscriptionId: number) {
    return fetchWrapper.get(`${SUBS_BASE}/${subscriptionId}/visit`, null);
  },

  visitPost(subscriptionId: number, postUrl: string) {
    return fetchWrapper.get(
      `${SUBS_BASE}/${subscriptionId}/visit/${encodeURIComponent(postUrl)}`,
      null,
    );
  },

  getPosts(subscriptionId: number): Promise<Post[]> {
    return fetchWrapper.get(`${SUBS_BASE}/${subscriptionId}/posts`, null).then((data) =>
      Array.isArray(data) ? data : data?.content ?? [],
    );
  },

  getPostsByBlogUrl(blogUrl: string): Promise<Post[]> {
    return fetchWrapper
      .get(`${SUBS_BASE}/blogs/${encodeURIComponent(blogUrl)}/posts`, null)
      .then((data) => (Array.isArray(data) ? data : data?.content ?? []));
  },
};

// ─── Blog API ───────────────────────────────────────────────────

export const blogApi = {
  updateStatus(data: StatusRequest) {
    return fetchWrapper.put('/api/my-blog/status', data);
  },

  getAuthMessage(rssUri: string) {
    return fetchWrapper.get(`/api/blog/auth/message?rssUri=${encodeURIComponent(rssUri)}`, null);
  },

  verify() {
    return fetchWrapper.post('/api/blog/auth/verification', null);
  },
};

// ─── Post API ───────────────────────────────────────────────────

export const postApi = {
  like(postUri: string) {
    return fetchWrapper.post('/api/posts/like', { postUri });
  },

  getRecentPosts(limit = 30): Promise<RecentPost[]> {
    return fetchWrapper.get(`/api/posts/recent?limit=${limit}`, null, { silent: true });
  },

  getMixedFeed(limit = 100): Promise<RecentPost[]> {
    return fetchWrapper.get(`/api/posts/feed?limit=${limit}`, null, { silent: true });
  },
};

// ─── Discover API ────────────────────────────────────────────────

export interface DiscoverCategory {
  id: number;
  name: string;
  slug: string;
  icon: string;
  sortOrder: number;
  blogCount: number;
}

export interface DiscoverBlog {
  blogId: number;
  rssUrl: string;
  blogUrl: string;
  blogTitle: string;
  author?: string;
  thumbnailUrl?: string;
  platform: string;
  score: number;
  postCount: number;
  tags: string[];
  categories: string[];
}

export interface DiscoverBlogsResponse {
  blogs: DiscoverBlog[];
  hasMore: boolean;
  page: number;
}

export const discoverApi = {
  getCategories(): Promise<DiscoverCategory[]> {
    return fetchWrapper.get('/api/discover/categories', null, { silent: true });
  },

  getBlogs(params: { category?: string; tag?: string; q?: string; page?: number; size?: number } = {}): Promise<DiscoverBlogsResponse> {
    const qs = new URLSearchParams();
    if (params.category) qs.set('category', params.category);
    if (params.tag) qs.set('tag', params.tag);
    if (params.q) qs.set('q', params.q);
    if (params.page !== undefined) qs.set('page', String(params.page));
    if (params.size) qs.set('size', String(params.size));
    return fetchWrapper.get(`/api/discover/blogs?${qs.toString()}`, null, { silent: true });
  },

  getTags(limit = 30): Promise<{ id: number; name: string; blogCount: number }[]> {
    return fetchWrapper.get(`/api/discover/tags?limit=${limit}`, null, { silent: true });
  },
};
