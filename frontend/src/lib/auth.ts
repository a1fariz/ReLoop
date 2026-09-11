'use client';

import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthData } from '@/types/api';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  userId: number | null;
  email: string | null;
  fullName: string | null;
  role: AuthData['role'] | null;
  setSession: (data: AuthData) => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  clearSession: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      userId: null,
      email: null,
      fullName: null,
      role: null,
      setSession: (data) =>
        set({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          userId: data.userId,
          email: data.email,
          fullName: data.fullName,
          role: data.role,
        }),
      setTokens: (accessToken, refreshToken) => set({ accessToken, refreshToken }),
      clearSession: () =>
        set({
          accessToken: null,
          refreshToken: null,
          userId: null,
          email: null,
          fullName: null,
          role: null,
        }),
    }),
    { name: 'reloop-auth' }
  )
);

export function isAuthenticated(): boolean {
  return useAuthStore.getState().accessToken !== null;
}
