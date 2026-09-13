import axios from 'axios';
import { randomUUID } from './uuid';
import { useAuthStore } from '@/lib/auth';

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 120000,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = useAuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

/**
 * Single-flight refresh: on the first 401, rotate the refresh token once and
 * retry the original request; concurrent 401s await the same rotation.
 * Login/register/refresh endpoints never trigger rotation.
 */
let refreshPromise: Promise<string | null> | null = null;

async function doRefresh(): Promise<string | null> {
  const { refreshToken, setTokens } = useAuthStore.getState();
  if (!refreshToken) return null;
  try {
    // Bare axios (not apiClient) so a failing refresh cannot recurse
    const { data } = await axios.post(
      `${API_BASE_URL}/auth/refresh`,
      { refreshToken },
      { headers: { 'Content-Type': 'application/json' } }
    );
    const auth = data.data as { accessToken: string; refreshToken: string };
    setTokens(auth.accessToken, auth.refreshToken);
    return auth.accessToken;
  } catch {
    return null;
  }
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    const url: string = original?.url ?? '';

    const isAuthPath = url.startsWith('/auth/login') || url.startsWith('/auth/register') || url.startsWith('/auth/refresh');

    if (status === 401 && typeof window !== 'undefined' && original && !original._retried && !isAuthPath) {
      original._retried = true;
      refreshPromise = refreshPromise ?? doRefresh();
      const newToken = await refreshPromise;
      refreshPromise = null;

      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(original);
      }

      useAuthStore.getState().clearSession();
      window.location.href = '/login';
    } else if (status === 401 && !isAuthPath && typeof window !== 'undefined') {
      useAuthStore.getState().clearSession();
    }
    return Promise.reject(error);
  }
);

/** Adds a unique Idempotency-Key header (required by POST /checkout/confirm-payment). */
export function withIdempotencyKey(): { 'Idempotency-Key': string } {
  return { 'Idempotency-Key': randomUUID() };
}

export { randomUUID };
