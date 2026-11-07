import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from './api';
import {
  orders as mockOrders,
  prescriptions as mockPrescriptions,
  products as mockProducts,
} from './mock';
import { usePlacedOrders } from '@/store/orders';
import type { Order, Prescription, Product } from './types';

const FIVE_MINUTES = 5 * 60 * 1000;

/**
 * Catalog data is served by the product service when `VITE_API_BASE_URL` is
 * configured. The mock shelf is used as the initial/fallback view so the
 * storefront still renders when the service is unavailable.
 */
export function useProducts(): Product[] {
  return useQuery({
    queryKey: ['products'],
    queryFn: api.listProducts,
    initialData: mockProducts,
    initialDataUpdatedAt: 0,
    staleTime: FIVE_MINUTES,
  }).data;
}

export function useProduct(slug: string | undefined): Product | undefined {
  const products = useProducts();
  return useMemo(() => products.find((product) => product.slug === slug), [products, slug]);
}

export function useOrders(): Order[] {
  const serverOrders = useQuery({
    queryKey: ['orders'],
    queryFn: api.listOrders,
    initialData: mockOrders,
    initialDataUpdatedAt: 0,
    staleTime: FIVE_MINUTES,
  }).data;
  const placedOrders = usePlacedOrders((state) => state.placed);

  return useMemo(() => [...placedOrders, ...serverOrders], [placedOrders, serverOrders]);
}

export function usePrescriptions(): Prescription[] {
  return useQuery({
    queryKey: ['prescriptions'],
    queryFn: api.listPrescriptions,
    initialData: mockPrescriptions,
    initialDataUpdatedAt: 0,
    staleTime: FIVE_MINUTES,
  }).data;
}
