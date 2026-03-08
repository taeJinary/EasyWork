import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthUser } from '@/types';

type LegacyAuthUser = {
  id?: number;
  email?: string;
  name?: string;
  profileImageUrl?: string | null;
  createdAt?: string;
};

type PersistedAuthState = {
  accessToken?: string | null;
  user?: AuthUser | LegacyAuthUser | null;
  isAuthenticated?: boolean;
};

interface AuthState {
  accessToken: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  setToken: (token: string) => void;
  login: (token: string, user: AuthUser) => void;
  logout: () => void;
  setUser: (user: AuthUser) => void;
}

function normalizePersistedUser(user: PersistedAuthState['user']): AuthUser | null {
  if (!user || typeof user !== 'object') {
    return null;
  }

  if ('userId' in user && 'nickname' in user && 'profileImg' in user && 'role' in user) {
    return user as AuthUser;
  }

  const legacyUser = user as LegacyAuthUser;
  if (!legacyUser.email || !legacyUser.name) {
    return null;
  }

  return {
    userId: legacyUser.id ?? 0,
    email: legacyUser.email,
    nickname: legacyUser.name,
    profileImg: legacyUser.profileImageUrl ?? null,
    role: 'USER',
  };
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      user: null,
      isAuthenticated: false,
      setToken: (token) => set({ accessToken: token }),
      login: (token, user) =>
        set({ accessToken: token, user, isAuthenticated: true }),
      logout: () =>
        set({ accessToken: null, user: null, isAuthenticated: false }),
      setUser: (user) => set({ user }),
    }),
    {
      name: 'easywork-auth',
      version: 2,
      partialize: (state) => ({
        accessToken: state.accessToken,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
      migrate: (persistedState) => {
        const state = persistedState as PersistedAuthState | undefined;
        const user = normalizePersistedUser(state?.user);

        return {
          accessToken: state?.accessToken ?? null,
          user,
          isAuthenticated: Boolean(state?.accessToken && user && state?.isAuthenticated),
        };
      },
    }
  )
);
