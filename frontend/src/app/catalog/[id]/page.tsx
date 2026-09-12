'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, ArrowUpRight, Lock, ShieldCheck, ShoppingBag, Check } from 'lucide-react';
import { addCartItem, apiErrorMessage, getListing } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import { useAuthStore } from '@/lib/auth';

function parseImages(images: string | null): string[] {
  try {
    const parsed = JSON.parse(images ?? '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export default function ListingDetailPage({ params }: { params: { id: string } }) {
  const t = useT();
  const { accessToken } = useAuthStore();
  const queryClient = useQueryClient();
  const [cartError, setCartError] = useState('');
  const { data: listing, isPending, isError, error } = useQuery({
    queryKey: queryKeys.listings.detail(params.id),
    queryFn: () => getListing(params.id),
    retry: 1,
  });

  const addToCart = useMutation({
    mutationFn: () => addCartItem({ listingId: params.id }),
    onSuccess: () => {
      setCartError('');
      queryClient.invalidateQueries({ queryKey: ['cart'] });
    },
    onError: (err) => setCartError(apiErrorMessage(err)),
  });



  if (isPending) {
    return (
      <div className="min-h-screen bg-[#fafafa] py-16">
        <div className="container mx-auto px-6 max-w-7xl animate-pulse space-y-8">
          <div className="h-4 w-40 bg-zinc-100 rounded" />
          <div className="h-[480px] bg-zinc-100 rounded-3xl" />
        </div>
      </div>
    );
  }

  if (isError || !listing) {
    return (
      <div className="min-h-screen bg-[#fafafa] py-24 text-center">
        <h1 className="text-2xl font-bold mb-3">{t('detail_unavailable')}</h1>
        <p className="text-sm text-zinc-500 mb-6">{(error as Error)?.message ?? t('detail_unavailable')}</p>
        <Link href="/catalog" className="btn-blue px-6 py-3 text-xs inline-flex">{t('detail_back_catalog')}</Link>
      </div>
    );
  }

  const image = parseImages(listing.images)[0];

  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 py-16 ambient-light-mesh">
      <div className="container mx-auto px-6 max-w-7xl">
        <Link href="/catalog" className="editorial-link mb-10 inline-flex">
          <ArrowLeft className="h-3 w-3" /> {t('detail_back')}
        </Link>

        <div className="grid lg:grid-cols-12 gap-10">
          {/* Left: Imagery & Diagnostics */}
          <div className="lg:col-span-7 space-y-10">
            <div className="luxury-card p-7 overflow-hidden">
              <div className="aspect-[4/3] w-full rounded-2xl bg-gradient-to-br from-zinc-50 to-zinc-100 border border-zinc-200 relative flex items-center justify-center p-8 overflow-hidden">
                {image ? (
                  <img src={image} alt={listing.title} className="max-h-full max-w-full object-contain drop-shadow-xl" />
                ) : (
                  <div className="text-6xl font-mono text-zinc-300">RL</div>
                )}
                <div className="absolute top-4 left-4 bg-white/95 backdrop-blur-md px-4 py-1.5 text-xs font-bold rounded-full border border-zinc-200 text-zinc-900 shadow-md flex items-center gap-1.5">
                  <ShieldCheck className="h-3 w-3 text-sky-600" />
                  Grade {listing.gradeSnapshot} • {listing.status}
                </div>
              </div>
              <div className="p-5 flex items-center justify-between text-xs font-mono text-zinc-500 border-t border-zinc-100 mt-5">
                <span>{t('detail_unit').toUpperCase()}: <strong className="text-zinc-900">{listing.unitId.slice(0, 13)}…</strong></span>
                <span>{t('detail_seller')} #{listing.sellerId}</span>
              </div>
            </div>

            <section className="rounded-3xl border border-stone-200 bg-white p-6 shadow-sm">
              <p className="text-[11px] font-mono uppercase tracking-[0.16em] text-amber-700">{t('detail_evidence_label')}</p>
              <h2 className="mt-2 text-xl font-bold text-stone-950">{t('detail_evidence_title')}</h2>
              <p className="mt-2 text-sm leading-6 text-stone-600">{t('detail_evidence_desc')}</p>
              <div className="mt-5 grid gap-3 sm:grid-cols-3">
                {[t('detail_evidence_grade'), t('detail_evidence_listing'), t('detail_evidence_reservation')].map((item) => <div key={item} className="rounded-2xl border border-stone-200 bg-stone-50 p-4 text-xs font-semibold text-stone-700">{item}</div>)}
              </div>
            </section>
          </div>

          {/* Right: Checkout & Lease Panel */}
          <div className="lg:col-span-5 space-y-8">
            <div className="sticky top-20 luxury-card p-8 space-y-7">
              <div>
                <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-bold mb-2">
                  {listing.status} • {t('catalog_registry')}
                </div>
                <h1 className="text-3xl font-bold tracking-tight text-zinc-900 leading-tight mb-3">{listing.title}</h1>
                <div className="text-3xl font-extrabold text-zinc-900 font-mono tnum">
                  Rp {listing.askingPrice.toLocaleString('id-ID')}
                </div>
              </div>

              {listing.description && (
                <p className="text-sm text-zinc-600 leading-relaxed border-y border-zinc-100 py-5">{listing.description}</p>
              )}

              <div className="p-5 rounded-2xl bg-sky-50 border border-sky-100 text-xs space-y-1.5">
                <div className="font-bold text-sky-700 flex items-center gap-2 font-mono uppercase text-[11px]">
                  <Lock className="h-3.5 w-3.5" /> {t('detail_lease_lock')}
                </div>
                <p className="text-zinc-600 text-[12px] leading-relaxed">
                  {t('detail_lease_desc')}
                </p>
              </div>

              <div className="space-y-3.5 text-xs text-zinc-600 border-y border-zinc-100 py-6">
                <div className="flex items-center justify-between">
                  <span>{t('detail_certified_grade')}:</span>
                  <span className="font-mono font-bold text-zinc-900">{listing.gradeSnapshot}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span>{t('detail_serialized_unit')}:</span>
                  <span className="font-mono font-bold text-zinc-900">{listing.unitId.slice(0, 8)}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span>{t('detail_settlement')}:</span>
                  <span className="font-bold text-zinc-900">{t('detail_escrow_vault')}</span>
                </div>
              </div>

              <Link
                href={`/checkout/${listing.id}`}
                className="w-full inline-flex items-center justify-center gap-2 btn-blue py-4 px-6 text-xs active:scale-[0.98] shadow-md"
              >
                {t('detail_acquire')} <ArrowUpRight className="h-4 w-4" />
              </Link>

              {accessToken && listing.status === 'ACTIVE' && (
                <div className="space-y-2">
                  <button
                    onClick={() => addToCart.mutate()}
                    disabled={addToCart.isPending || addToCart.isSuccess}
                    className={`w-full inline-flex items-center justify-center gap-2 py-4 px-6 text-xs font-bold rounded-full border transition-all active:scale-[0.98] ${
                      addToCart.isSuccess
                        ? 'border-emerald-300 bg-emerald-50 text-emerald-700'
                        : 'border-zinc-200 bg-white text-zinc-700 hover:border-zinc-300 hover:bg-zinc-50'
                    }`}
                  >
                    {addToCart.isSuccess ? (
                      <><Check className="h-4 w-4" /> {t('cart_added')}</>
                    ) : (
                      <><ShoppingBag className="h-4 w-4" /> {t('cart_add')}</>
                    )}
                  </button>
                  {cartError && <p className="text-xs text-red-600 font-semibold">{cartError}</p>}
                </div>
              )}
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}
