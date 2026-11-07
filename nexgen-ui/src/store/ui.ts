import { create } from 'zustand';
import type { User } from '@/lib/types';
import { currentUser } from '@/lib/mock';

interface Toast {
  id: number;
  title: string;
  detail?: string;
  image?: string;
}

interface UiState {
  searchOpen: boolean;
  toast: Toast | null;
  user: User | null;
  openSearch: () => void;
  closeSearch: () => void;
  showToast: (toast: Omit<Toast, 'id'>) => void;
  hideToast: () => void;
  signIn: (user: User) => void;
  signOut: () => void;
}

export const useUiStore = create<UiState>()((set) => ({
  searchOpen: false,
  toast: null,
  user: currentUser,
  openSearch: () => set({ searchOpen: true }),
  closeSearch: () => set({ searchOpen: false }),
  showToast: (toast) => set({ toast: { ...toast, id: Date.now() } }),
  hideToast: () => set({ toast: null }),
  signIn: (user) => set({ user }),
  signOut: () => set({ user: null }),
}));
