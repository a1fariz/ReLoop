'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { Search, ArrowRight } from 'lucide-react';
import { apiErrorMessage, searchListings } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import type { ListingDto, ListingSearchParams } from '@/types/api';

function parseImages(images: string | null): string[] {
  try {
    const parsed = JSON.parse(images ?? '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export default function CatalogPage() {
  const t = useT();
  const [params, setParams] = useState<ListingSearchParams>({ page: 0, size: 9, sort: 'newest' });
  const [minPriceInput, setMinPriceInput] = useState('');
  const [maxPriceInput, setMaxPriceInput] = useState('');

  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: queryKeys.listings.search(params),
    queryFn: () => searchListings(params),
    placeholderData: (prev) => prev,
  });

  const page = data?.page ?? 0;
  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  function applyPriceFilter() {
    const min = parseInt(minPriceInput, 10);
    const max = parseInt(maxPriceInput, 10);
    setParams((p) => ({
      ...p,
      page: 0,
      minPrice: Number.isFinite(min) && min > 0 ? min : undefined,
      maxPrice: Number.isFinite(max) && max > 0 ? max : undefined,
    }));
  }

  return (
    <div className="min-h-screen bg-[#ffffff] text-[#1d1d1f] py-16 light-ambient-glow">
      <div className="container mx-auto px-6 max-w-7xl">

        {/* Header Title */}
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-10 pb-6 border-b border-black/[0.08] gap-4">
          <div>
            <div className="text-xs font-mono uppercase text-[#0071e3] tracking-wider mb-2 font-semibold">
              {t('catalog_registry')} [{data?.total ?? '…'} {t('catalog_units_online')}]
            </div>
            <h1 className="text-4xl sm:text-5xl font-semibold tracking-tight text-[#1d1d1f]">
              {t('catalog_title')}
            </h1>
          </div>

          {/* Price Filter */}
          <div className="flex flex-wrap items-end gap-2 text-xs font-mono">
             <label className="sr-only" htmlFor="catalog-min-price">{t('catalog_min_price')}</label>
             <input
               id="catalog-min-price"
               type="number" min="0" placeholder={t('catalog_min_price')} value={minPriceInput}
              onChange={(e) => setMinPriceInput(e.target.value)}
              className="w-28 rounded-full border border-black/[0.1] bg-[#f5f5f7] px-4 py-2.5 text-xs focus:outline-none focus:border-[#0071e3] focus:bg-white transition-all"
            />
            <span className="text-[#86868b]">—</span>
             <label className="sr-only" htmlFor="catalog-max-price">{t('catalog_max_price')}</label>
             <input
               id="catalog-max-price"
               type="number" min="0" placeholder={t('catalog_max_price')} value={maxPriceInput}
              onChange={(e) => setMaxPriceInput(e.target.value)}
              className="w-28 rounded-full border border-black/[0.1] bg-[#f5f5f7] px-4 py-2.5 text-xs focus:outline-none focus:border-[#0071e3] focus:bg-white transition-all"
            />
            <button onClick={applyPriceFilter} className="px-4 py-2.5 rounded-full bg-[#0071e3] text-white font-semibold hover:bg-[#0062cc] transition-colors">
              {t('common_filter')}
            </button>
            {(params.minPrice || params.maxPrice) && (
              <button
                onClick={() => { setMinPriceInput(''); setMaxPriceInput(''); setParams((p) => ({ ...p, page: 0, minPrice: undefined, maxPrice: undefined })); }}
                className="px-3 py-2.5 rounded-full border border-black/[0.1] text-[#86868b] hover:text-[#1d1d1f] transition-colors"
              >
                {t('common_clear')}
              </button>
            )}
          </div>
        </div>

        {/* Filter Line */}
        <div className="flex flex-wrap items-center justify-between gap-6 mb-12 p-4 bg-[#f5f5f7] rounded-2xl border border-black/[0.06] text-xs">
          <div className="flex items-center gap-2">
            <Search className="h-3.5 w-3.5 text-[#86868b]" />
            <span className="font-mono text-[#86868b]">{t('common_grade')}:</span>
            {['', 'A', 'B', 'C'].map((g) => (
              <button
                key={g || 'ALL'}
                onClick={() => setParams((p) => ({ ...p, page: 0, grade: g || undefined }))}
                aria-pressed={(params.grade ?? '') === g}
                className={`px-3 py-1.5 rounded-full font-mono font-semibold transition-all ${
                  (params.grade ?? '') === g ? 'bg-[#1d1d1f] text-white' : 'bg-white text-[#1d1d1f] hover:bg-[#eaeaea]'
                }`}
              >
                {g || t('common_all')}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2 font-mono">
            <span className="text-[#86868b]">{t('common_sort')}:</span>
            {([['newest', t('common_newest')], ['priceAsc', t('common_price_asc')], ['priceDesc', t('common_price_desc')]] as const).map(([value, label]) => (
              <button
                key={value}
                 onClick={() => setParams((p) => ({ ...p, page: 0, sort: value }))}
                 aria-pressed={(params.sort ?? 'newest') === value}
                 className={`px-3 py-1.5 rounded-full transition-all ${
                  (params.sort ?? 'newest') === value ? 'bg-[#0071e3] text-white font-semibold' : 'bg-white text-[#1d1d1f] hover:bg-[#eaeaea]'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        {/* Error State */}
        {isError && (
          <div className="p-8 rounded-3xl border border-red-200 bg-red-50 text-sm text-red-700 text-center">
             <p>{apiErrorMessage(error)}</p>
             <button onClick={() => void refetch()} className="btn-primary-dark mt-4 px-5 py-3 text-xs">{t('common_try_again')}</button>
           </div>
        )}

        {/* Skeleton */}
        {isPending && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="luxury-card rounded-3xl p-8 animate-pulse">
                <div className="aspect-[4/3] rounded-2xl bg-zinc-100 mb-6" />
                <div className="h-3 w-24 bg-zinc-100 rounded mb-3" />
                <div className="h-5 w-3/4 bg-zinc-100 rounded mb-2" />
                <div className="h-3 w-1/2 bg-zinc-100 rounded" />
              </div>
            ))}
          </div>
        )}

        {/* Product Cards */}
        {!isPending && !isError && (
          <motion.div layout className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            <AnimatePresence mode="popLayout">
              {(data?.items ?? []).map((item: ListingDto, index: number) => {
                const image = parseImages(item.images)[0];
                return (
                  <motion.div
                    key={item.id}
                    layout
                    initial={{ opacity: 0, scale: 0.94, y: 20 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.94, y: -10 }}
                    transition={{ duration: 0.45, delay: index * 0.04, ease: [0.16, 1, 0.3, 1] }}
                    className="luxury-card rounded-3xl p-8 flex flex-col justify-between group overflow-hidden h-full"
                  >
                    <div>
                      <div className="aspect-[4/3] w-full rounded-2xl overflow-hidden bg-[#f5f5f7] relative mb-6 flex items-center justify-center p-4">
                        {image ? (
                          <img
                            src={image}
                            alt={item.title}
                            loading="lazy"
                            decoding="async"
                            className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-700 ease-out drop-shadow-md"
                          />
                        ) : (
                          <div className="text-4xl font-mono text-zinc-300">RL</div>
                        )}
                        <div className="absolute top-4 left-4 bg-white/90 backdrop-blur-md px-3 py-1 text-[11px] font-mono font-semibold rounded-full border border-black/[0.08] text-[#1d1d1f] shadow-sm">
                          Grade {item.gradeSnapshot} • {item.status}
                        </div>
                      </div>

                      <div className="text-xs font-mono text-[#86868b] uppercase tracking-wider mb-1">
                        {t('detail_seller')} #{item.sellerId} • {t('detail_unit')} {item.unitId.slice(0, 8)}
                      </div>
                      <Link href={`/catalog/${item.id}`}>
                        <h3 className="text-xl font-bold text-[#1d1d1f] group-hover:text-[#0071e3] transition-colors mb-2">
                          {item.title}
                        </h3>
                      </Link>
                      <p className="text-xs text-[#86868b] line-clamp-2 leading-relaxed">
                        {item.description ?? t('catalog_default_desc')}
                      </p>
                    </div>

                    <div className="pt-6 border-t border-black/[0.06] mt-6 space-y-4">
                      <div className="flex items-baseline justify-between">
                        <div>
                          <div className="text-[10px] uppercase font-mono text-[#86868b]">{t('catalog_certified_price')}</div>
                          <div className="text-xl font-bold text-[#1d1d1f] font-mono tnum mt-0.5">
                            Rp {item.askingPrice.toLocaleString('id-ID')}
                          </div>
                        </div>
                      </div>

                      <Link
                        href={`/catalog/${item.id}`}
                        className="w-full inline-flex items-center justify-center gap-2 btn-blue py-3 text-xs active:scale-95 transition-all shadow-sm"
                      >
                        {t('catalog_inspect')} <ArrowRight className="h-3.5 w-3.5" />
                      </Link>
                    </div>
                  </motion.div>
                );
              })}
            </AnimatePresence>

            {data && data.items.length === 0 && (
              <div className="col-span-full p-12 rounded-3xl border border-black/[0.06] bg-[#f5f5f7] text-center">
                <div className="text-2xl mb-2">{t('catalog_no_match')}</div>
                <button
                  onClick={() => setParams({ page: 0, size: 9, sort: 'newest' })}
                  className="mt-4 px-6 py-3 rounded-full bg-[#1d1d1f] text-white text-xs font-semibold"
                >
                  {t('common_reset')}
                </button>
              </div>
            )}
          </motion.div>
        )}

        {/* Pagination */}
        {data && data.total > data.size && (
          <div className="flex items-center justify-center gap-3 mt-12 text-xs font-mono">
            <button
              disabled={page === 0}
              onClick={() => setParams((p) => ({ ...p, page: Math.max(0, page - 1) }))}
              className="px-5 py-2.5 rounded-full border border-black/[0.1] font-semibold disabled:opacity-40 disabled:cursor-not-allowed hover:bg-[#f5f5f7] transition-all"
            >
              {t('common_prev')}
            </button>
            <span className="text-[#86868b]">{t('common_page')} {page + 1} / {totalPages}</span>
            <button
              disabled={page + 1 >= totalPages}
              onClick={() => setParams((p) => ({ ...p, page: page + 1 }))}
              className="px-5 py-2.5 rounded-full border border-black/[0.1] font-semibold disabled:opacity-40 disabled:cursor-not-allowed hover:bg-[#f5f5f7] transition-all"
            >
              {t('common_next')}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
