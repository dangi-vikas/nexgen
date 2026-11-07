import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { CartLine, Product } from '@/lib/types';

interface CartState {
  lines: CartLine[];
  isOpen: boolean;
  promoCode: string | null;
  add: (product: Product, quantity?: number) => void;
  remove: (productId: string) => void;
  setQuantity: (productId: string, quantity: number) => void;
  applyPromo: (code: string) => boolean;
  removePromo: () => void;
  clear: () => void;
  open: () => void;
  close: () => void;
}

const PROMO_CODES: Record<string, number> = { WELCOME10: 0.1 };

export const useCartStore = create<CartState>()(
  persist(
    (set) => ({
      lines: [],
      isOpen: false,
      promoCode: null,
      add: (product, quantity = 1) =>
        set((state) => {
          const existing = state.lines.find((line) => line.product.id === product.id);
          if (existing) {
            return {
              lines: state.lines.map((line) =>
                line.product.id === product.id
                  ? {
                      ...line,
                      quantity: Math.min(line.quantity + quantity, line.product.stock),
                    }
                  : line,
              ),
            };
          }
          return { lines: [...state.lines, { product, quantity }] };
        }),
      remove: (productId) =>
        set((state) => ({ lines: state.lines.filter((line) => line.product.id !== productId) })),
      setQuantity: (productId, quantity) =>
        set((state) => ({
          lines:
            quantity <= 0
              ? state.lines.filter((line) => line.product.id !== productId)
              : state.lines.map((line) =>
                  line.product.id === productId
                    ? { ...line, quantity: Math.min(quantity, line.product.stock) }
                    : line,
                ),
        })),
      applyPromo: (code) => {
        const normalized = code.trim().toUpperCase();
        if (!PROMO_CODES[normalized]) return false;
        set({ promoCode: normalized });
        return true;
      },
      removePromo: () => set({ promoCode: null }),
      clear: () => set({ lines: [], promoCode: null }),
      open: () => set({ isOpen: true }),
      close: () => set({ isOpen: false }),
    }),
    {
      name: 'northleaf-cart',
      partialize: (state) => ({ lines: state.lines, promoCode: state.promoCode }),
    },
  ),
);

export const useCartCount = () =>
  useCartStore((state) => state.lines.reduce((total, line) => total + line.quantity, 0));

export const useCartSubtotal = () =>
  useCartStore((state) =>
    state.lines.reduce((total, line) => total + line.product.price * line.quantity, 0),
  );

export const FREE_SHIPPING_THRESHOLD = 35;
export const DELIVERY_FEE = 4.95;
export const TAX_RATE = 0.08;

export function cartTotals(subtotal: number, promoCode: string | null = null) {
  const shipping = subtotal >= FREE_SHIPPING_THRESHOLD || subtotal === 0 ? 0 : DELIVERY_FEE;
  const tax = Math.round(subtotal * TAX_RATE * 100) / 100;
  const discountRate = promoCode ? (PROMO_CODES[promoCode] ?? 0) : 0;
  const discount = Math.round(subtotal * discountRate * 100) / 100;
  return {
    subtotal,
    shipping,
    tax,
    discount,
    total: Math.max(0, Math.round((subtotal + shipping + tax - discount) * 100) / 100),
    remainingForFreeShipping: Math.max(0, FREE_SHIPPING_THRESHOLD - subtotal),
  };
}
