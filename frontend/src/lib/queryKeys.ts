export const queryKeys = {
  auth: {
    me: ['auth', 'me'] as const,
  },
  products: {
    all: (filters?: Record<string, unknown>) => ['products', 'list', filters] as const,
    detail: (slug: string) => ['products', 'detail', slug] as const,
    categories: ['products', 'categories'] as const,
  },
  units: {
    detail: (unitId: string) => ['units', 'detail', unitId] as const,
    provenance: (unitId: string) => ['units', 'provenance', unitId] as const,
  },
  listings: {
    all: (filters?: Record<string, unknown>) => ['listings', 'list', filters] as const,
    detail: (id: string) => ['listings', 'detail', id] as const,
  },
  cart: {
    current: ['cart', 'current'] as const,
  },
  orders: {
    all: (filters?: Record<string, unknown>) => ['orders', 'list', filters] as const,
    detail: (id: string) => ['orders', 'detail', id] as const,
  },
  tradeIn: {
    estimate: (modelId: string) => ['trade-in', 'estimate', modelId] as const,
    requests: ['trade-in', 'requests'] as const,
  },
  seller: {
    dashboard: ['seller', 'dashboard'] as const,
    wallet: ['seller', 'wallet'] as const,
  },
};
