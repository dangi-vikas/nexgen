import { currentUser, orders, prescriptions, products } from './mock';
import type { Order, Prescription, Product, User } from './types';

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');
const MOCK_FLAG = import.meta.env.VITE_USE_MOCK;
const USE_MOCK = MOCK_FLAG === 'true' || (MOCK_FLAG !== 'false' && API_BASE.length === 0);

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

function authHeaders(): HeadersInit {
  const token = localStorage.getItem('northleaf-token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    Accept: 'application/json',
    ...(authHeaders() as Record<string, string>),
    ...(init.headers as Record<string, string> | undefined),
  };
  if (init.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
    signal: AbortSignal.timeout(4000),
  });

  if (!response.ok) {
    throw new ApiError(`Request failed: ${response.status}`, response.status);
  }
  return (await response.json()) as T;
}

/**
 * The storefront renders from a seeded catalogue when the gateway is not
 * reachable so the UI is always demoable. Set VITE_USE_MOCK=false to read
 * exclusively from the NexGen product service through Kong.
 */
async function withFallback<T>(action: () => Promise<T>, fallback: T): Promise<T> {
  if (USE_MOCK) return fallback;
  try {
    return await action();
  } catch {
    return fallback;
  }
}

interface BackendProduct {
  id?: string | number;
  sku?: string;
  skuCode?: string;
  name?: string;
  description?: string;
  price?: number | string;
  category?: string;
  stockQuantity?: number;
  quantity?: number;
  imageUrl?: string;
}

function normalizeProduct(raw: BackendProduct): Product | null {
  const sku = raw.skuCode ?? raw.sku;
  if (!sku || !raw.name) return null;
  const slug = raw.name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '');
  const price = Number(raw.price ?? 0);
  return {
    id: String(raw.id ?? sku),
    sku,
    slug,
    name: raw.name,
    subtitle: raw.category ?? 'Pharmacy essential',
    category: 'Everyday',
    concerns: [],
    price,
    rating: 4.8,
    reviews: 0,
    stock: raw.stockQuantity ?? raw.quantity ?? 0,
    image: raw.imageUrl ?? '/images/pharmacy-shelves.jpg',
    gallery: [raw.imageUrl ?? '/images/pharmacy-shelves.jpg'],
    size: 'Standard',
    description: raw.description ?? '',
    ingredients: [],
  };
}

function unwrapProducts(payload: unknown): BackendProduct[] {
  if (Array.isArray(payload)) return payload as BackendProduct[];
  if (payload && typeof payload === 'object') {
    const record = payload as Record<string, unknown>;
    if (Array.isArray(record.content)) return record.content as BackendProduct[];
    if (Array.isArray(record.items)) return record.items as BackendProduct[];
    if (Array.isArray(record.data)) return record.data as BackendProduct[];
  }
  return [];
}

export const api = {
  async listProducts(): Promise<Product[]> {
    return withFallback(async () => {
      const payload = await request<unknown>('/api/v1/products?size=48');
      return unwrapProducts(payload)
        .map(normalizeProduct)
        .filter((product): product is Product => product !== null);
    }, products);
  },

  async getProduct(slug: string): Promise<Product | undefined> {
    return withFallback(async () => {
      const list = await api.listProducts();
      return list.find((product) => product.slug === slug);
    }, products.find((product) => product.slug === slug));
  },

  async listOrders(): Promise<Order[]> {
    return withFallback(async () => {
      const payload = await request<unknown>('/api/v1/orders/me');
      return Array.isArray(payload) ? (payload as Order[]) : orders;
    }, orders);
  },

  async listPrescriptions(): Promise<Prescription[]> {
    return withFallback(async () => prescriptions, prescriptions);
  },

  async me(): Promise<User> {
    return withFallback(async () => {
      const payload = await request<Partial<User>>('/api/v1/auth/me');
      return { ...currentUser, ...payload };
    }, currentUser);
  },

  async login(email: string, password: string): Promise<User> {
    return withFallback(async () => {
      const payload = await request<{ accessToken: string; user: User }>('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      localStorage.setItem('northleaf-token', payload.accessToken);
      return payload.user;
    }, { ...currentUser, email });
  },

  async requestPasswordReset(email: string): Promise<boolean> {
    return withFallback(async () => {
      await request('/api/v1/auth/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email }),
      });
      return true;
    }, true);
  },

  async register(name: string, email: string, password: string): Promise<User> {
    return withFallback(async () => {
      const payload = await request<{ accessToken: string; user: User }>('/api/v1/auth/register', {
        method: 'POST',
        body: JSON.stringify({ name, email, password }),
      });
      localStorage.setItem('northleaf-token', payload.accessToken);
      return payload.user;
    }, { ...currentUser, name, email });
  },
};

export { USE_MOCK };
