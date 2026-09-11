// React Query keys aligned with the real backend endpoints.
import type { ListingSearchParams } from '@/types/api';

export const queryKeys = {
  listings: {
    search: (params: ListingSearchParams) => ['listings', 'search', params] as const,
    detail: (id: string) => ['listings', 'detail', id] as const,
    mine: () => ['listings', 'mine'] as const,
  },
  orders: {
    mine: (page: number, size: number) => ['orders', 'mine', page, size] as const,
    seller: (page: number, size: number) => ['orders', 'seller', page, size] as const,
    admin: (page: number, size: number) => ['orders', 'admin', page, size] as const,
  },
  seller: {
    metrics: (sellerId: number) => ['seller', 'metrics', sellerId] as const,
    reviews: (sellerId: number) => ['seller', 'reviews', sellerId] as const,
  },
  disputes: {
    admin: (page: number, size: number) => ['disputes', 'admin', page, size] as const,
    mine: (page: number, size: number) => ['disputes', 'mine', page, size] as const,
  },
  inspections: {
    unit: (unitId: string) => ['inspections', 'unit', unitId] as const,
  },
  tradein: {
    requests: () => ['tradein', 'requests'] as const,
  },
  warranties: {
    mine: (page: number, size: number) => ['warranties', 'mine', page, size] as const,
  },
  catalog: {
    models: () => ['catalog', 'models'] as const,
  },
  cart: {
    mine: () => ['cart', 'mine'] as const,
  },
  returns: {
    mine: (page: number, size: number) => ['returns', 'mine', page, size] as const,
    admin: (page: number, size: number) => ['returns', 'admin', page, size] as const,
  },
  notifications: {
    mine: (unreadOnly: boolean, page: number, size: number) => ['notifications', 'mine', unreadOnly, page, size] as const,
  },
  profile: {
    me: () => ['profile', 'me'] as const,
    kyc: () => ['profile', 'kyc'] as const,
  },
  payments: {
    mine: (page: number, size: number) => ['payments', 'mine', page, size] as const,
    admin: (page: number, size: number) => ['payments', 'admin', page, size] as const,
  },
  repairs: {
    mine: (page: number, size: number) => ['repairs', 'mine', page, size] as const,
  },
  escrow: {
    stats: () => ['escrow', 'stats'] as const,
  },
};
