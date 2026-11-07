import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Order } from '@/lib/types';

interface PlacedOrdersState {
  placed: Order[];
  add: (order: Order) => void;
  clear: () => void;
}

/**
 * Keeps orders placed in this browser so the demo confirmation flow is
 * reflected in the account area. With a live order service the server list
 * remains the source of truth and these entries are merged on top.
 */
export const usePlacedOrders = create<PlacedOrdersState>()(
  persist(
    (set) => ({
      placed: [],
      add: (order) => set((state) => ({ placed: [order, ...state.placed] })),
      clear: () => set({ placed: [] }),
    }),
    { name: 'northleaf-placed-orders' },
  ),
);
