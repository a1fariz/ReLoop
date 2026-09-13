'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Award, Building2, CheckCircle, Clock, Package, Plus, Star, Truck, X } from 'lucide-react';
import { useAuthStore } from '@/lib/auth';
import {
  apiErrorMessage, createListing, getSellerFulfillments, getSellerMetrics,
  getMyListings, pauseListing, resumeListing, shipFulfillment, getSellerReviews,
} from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import type { FulfillmentOrderDto, ListingDto, ReviewResponse } from '@/types/api';

const STATUS_STYLES: Record<string, string> = {
  ACTIVE: 'text-emerald-700 bg-emerald-50 border-emerald-200',
  PAUSED: 'text-amber-700 bg-amber-50 border-amber-200',
  SOLD: 'text-zinc-700 bg-zinc-100 border-zinc-200',
};

const FULFILLMENT_STYLES: Record<string, string> = {
  PROCESSING: 'text-sky-700 bg-sky-50 border-sky-200',
  SHIPPED: 'text-indigo-700 bg-indigo-50 border-indigo-200',
  DELIVERED: 'text-cyan-700 bg-cyan-50 border-cyan-200',
  COMPLETED: 'text-emerald-700 bg-emerald-50 border-emerald-200',
  DISPUTED: 'text-red-700 bg-red-50 border-red-200',
  CANCELLED: 'text-zinc-500 bg-zinc-50 border-zinc-200',
};

export default function SellerHubPage() {
  const t = useT();
  const { accessToken, userId, role } = useAuthStore();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<'listings' | 'orders'>('listings');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [shipTarget, setShipTarget] = useState<FulfillmentOrderDto | null>(null);

  const { data: metrics } = useQuery({
    queryKey: queryKeys.seller.metrics(userId ?? 0),
    queryFn: () => getSellerMetrics(userId as number),
    enabled: accessToken !== null && userId !== null,
  });

  const { data: listings, isPending: listingsPending } = useQuery({
    queryKey: queryKeys.listings.mine(),
    queryFn: getMyListings,
    enabled: accessToken !== null && tab === 'listings',
  });

  const { data: fulfillments, isPending: ordersPending } = useQuery({
    queryKey: queryKeys.orders.seller(0, 20),
    queryFn: () => getSellerFulfillments(0, 20),
    enabled: accessToken !== null && tab === 'orders',
  });

  const pauseMutation = useMutation({
    mutationFn: pauseListing,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.listings.mine() }),
  });
  const resumeMutation = useMutation({
    mutationFn: resumeListing,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.listings.mine() }),
  });

  if (!accessToken || (role !== 'SELLER' && role !== 'ADMIN')) {
    return (
      <div className="min-h-screen bg-[#fafafa] py-24 text-center">
        <Building2 className="h-12 w-12 text-zinc-300 mx-auto mb-4" />
        <h1 className="text-2xl font-bold mb-2">{t('seller_access_required')}</h1>
        <p className="text-sm text-zinc-500">{t('seller_access_desc')}</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 py-16 ambient-light-mesh">
      <div className="container mx-auto px-6 max-w-7xl">
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-12 pb-6 border-b border-zinc-200 gap-4">
          <div>
            <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold mb-2">
              {metrics ? `${metrics.storeName} • ${t('detail_seller')} #${metrics.sellerId}` : t('seller_hub')}
            </div>
            <h1 className="text-4xl sm:text-5xl font-semibold tracking-tight mt-1">{t('seller_hub')}</h1>
          </div>
          <div className="flex items-center gap-2 text-xs">
            {(['listings', 'orders'] as const).map((tb) => (
              <button
                key={tb}
                onClick={() => setTab(tb)}
                className={`px-5 py-2.5 rounded-full font-semibold transition-all ${
                  tab === tb ? 'bg-zinc-900 text-white shadow-sm' : 'bg-white border border-zinc-200 text-zinc-600 hover:border-zinc-300'
                }`}
              >
                {tb === 'listings' ? t('seller_my_listings') : t('nav_orders')}
              </button>
            ))}
          </div>
        </div>

        {/* Metrics */}
        {metrics ? (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-5 mb-12">
            <div className="luxury-card p-6 space-y-2">
              <div className="text-[11px] font-mono uppercase tracking-widest text-zinc-500">{t('seller_reputation')}</div>
              <div className="text-2xl sm:text-3xl font-bold text-zinc-900 font-mono tnum">{metrics.reputationScore.toFixed(1)} / 100</div>
              <div className="text-[11px] text-zinc-600 pt-1 flex items-center gap-1">
                <Award className="h-3 w-3 text-amber-600" /> KYC: {metrics.kycStatus}
              </div>
            </div>
            <div className="luxury-card p-6 space-y-2">
              <div className="text-[11px] font-mono uppercase tracking-widest text-zinc-500">{t('seller_completed')}</div>
              <div className="text-2xl sm:text-3xl font-bold text-zinc-900 font-mono tnum">{metrics.completedOrders}</div>
              <div className="text-[11px] text-zinc-600 pt-1 flex items-center gap-1">
                <CheckCircle className="h-3 w-3 text-emerald-600" /> {t('seller_return_rate')} {(metrics.returnRate * 100).toFixed(1)}%
              </div>
            </div>
            <div className="luxury-card p-6 space-y-2">
              <div className="text-[11px] font-mono uppercase tracking-widest text-zinc-500">{t('seller_active_disputes')}</div>
              <div className="text-2xl sm:text-3xl font-bold text-zinc-900 font-mono tnum">{metrics.activeDisputes}</div>
              <div className="text-[11px] text-zinc-600 pt-1 flex items-center gap-1">
                <Clock className="h-3 w-3 text-sky-600" /> {t('seller_dispute_rate')} {(metrics.disputeRate * 100).toFixed(1)}%
              </div>
            </div>
            <div className="luxury-card p-6 space-y-2">
              <div className="text-[11px] font-mono uppercase tracking-widest text-zinc-500">{t('seller_response_rate')}</div>
              <div className="text-2xl sm:text-3xl font-bold text-zinc-900 font-mono tnum">{(metrics.responseRate * 100).toFixed(0)}%</div>
              <div className="text-[11px] text-zinc-600 pt-1">{t('seller_store')}: {metrics.storeSlug}</div>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-5 mb-12">
            {Array.from({ length: 4 }).map((_, i) => <div key={i} className="luxury-card h-32 animate-pulse" />)}
          </div>
        )}

        {/* Tabs */}
        {tab === 'listings' ? (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold flex items-center gap-2"><Package className="h-5 w-5 text-sky-600" /> {t('seller_my_listings')}</h3>
              <button onClick={() => setShowCreateForm((v) => !v)} className="btn-blue px-5 py-2.5 text-xs inline-flex items-center gap-2">
                <Plus className="h-3.5 w-3.5" /> {showCreateForm ? t('common_cancel') : t('seller_create_listing')}
              </button>
            </div>

            {showCreateForm && <CreateListingForm onDone={() => setShowCreateForm(false)} />}

            {listingsPending && <div className="h-40 rounded-3xl bg-zinc-100 animate-pulse" />}

            {listings && (
              <>
              <div className="space-y-3 md:hidden">
                {listings.map((l: ListingDto) => (
                  <article key={l.id} className="rounded-2xl border border-zinc-200 bg-white p-5 shadow-sm">
                    <div className="flex items-start justify-between gap-3">
                      <div><h4 className="font-bold text-zinc-900">{l.title}</h4><p className="mt-1 font-mono text-[10px] text-zinc-500">{l.id.slice(0, 8)} · unit {l.unitId.slice(0, 8)}</p></div>
                      <span className={`rounded-full border px-2.5 py-1 font-mono text-[10px] font-bold ${STATUS_STYLES[l.status] ?? 'text-zinc-700 bg-zinc-50 border-zinc-200'}`}>{l.status}</span>
                    </div>
                    <div className="mt-4 flex items-center justify-between border-t border-zinc-100 pt-3">
                      <span className="font-mono text-sm font-bold">Rp {l.askingPrice.toLocaleString('id-ID')} · {t('common_grade')} {l.gradeSnapshot}</span>
                      {l.status === 'ACTIVE' ? (<button onClick={() => pauseMutation.mutate(l.id)} disabled={pauseMutation.isPending} aria-busy={pauseMutation.isPending} className="rounded-full border border-amber-200 px-3 py-1.5 text-[10px] font-bold text-amber-700">{t('seller_pause')}</button>) : l.status === 'PAUSED' ? (<button onClick={() => resumeMutation.mutate(l.id)} disabled={resumeMutation.isPending} aria-busy={resumeMutation.isPending} className="rounded-full border border-emerald-200 px-3 py-1.5 text-[10px] font-bold text-emerald-700">{t('seller_resume')}</button>) : (<span className="text-[10px] font-mono text-zinc-400">—</span>)}
                    </div>
                  </article>
                ))}
              </div>
              <div className="hidden overflow-x-auto rounded-3xl border border-zinc-200 bg-white p-6 shadow-sm md:block">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-zinc-200 text-[10px] text-zinc-500 uppercase tracking-wider font-mono font-semibold">
                      <th className="pb-4">{t('seller_title')}</th><th className="pb-4">{t('orders_total')}</th><th className="pb-4">{t('common_grade')}</th>
                      <th className="pb-4">{t('common_status')}</th><th className="pb-4 text-right">{t('common_actions')}</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-100">
                    {listings.map((l: ListingDto) => (
                      <tr key={l.id} className="hover:bg-zinc-50 transition-colors">
                        <td className="py-4 font-semibold text-zinc-900">
                          {l.title}
                          <div className="text-[10px] font-mono text-zinc-400 mt-0.5">{l.id.slice(0, 8)} • unit {l.unitId.slice(0, 8)}</div>
                        </td>
                        <td className="py-4 font-mono tnum">Rp {l.askingPrice.toLocaleString('id-ID')}</td>
                        <td className="py-4 font-mono">{l.gradeSnapshot}</td>
                        <td className="py-4">
                          <span className={`font-mono text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${STATUS_STYLES[l.status] ?? 'text-zinc-700 bg-zinc-50 border-zinc-200'}`}>{l.status}</span>
                        </td>
                        <td className="py-4 text-right">
                          {l.status === 'ACTIVE' ? (
                            <button
                              onClick={() => pauseMutation.mutate(l.id)}
                              disabled={pauseMutation.isPending} aria-busy={pauseMutation.isPending}
                              className="px-3 py-1.5 rounded-full border border-amber-200 text-amber-700 text-[10px] font-bold hover:bg-amber-50 transition-colors disabled:opacity-50"
                            >
                              {t('seller_pause')}
                            </button>
                          ) : l.status === 'PAUSED' ? (
                            <button
                              onClick={() => resumeMutation.mutate(l.id)}
                              disabled={resumeMutation.isPending} aria-busy={resumeMutation.isPending}
                              className="px-3 py-1.5 rounded-full border border-emerald-200 text-emerald-700 text-[10px] font-bold hover:bg-emerald-50 transition-colors disabled:opacity-50"
                            >
                              {t('seller_resume')}
                            </button>
                          ) : (
                            <span className="text-[10px] text-zinc-400 font-mono">—</span>
                          )}
                        </td>
                      </tr>
                    ))}
                    {listings.length === 0 && (
                      <tr><td colSpan={5} className="py-10 text-center text-zinc-500">{t('seller_no_listings')}</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
              </>
            )}

            <SellerReviewsPanel sellerId={userId as number} />
          </div>
        ) : (
          <div className="space-y-6">
            <h3 className="text-lg font-bold flex items-center gap-2"><Truck className="h-5 w-5 text-sky-600" /> {t('seller_fulfillments')}</h3>
            {ordersPending && <div className="h-40 rounded-3xl bg-zinc-100 animate-pulse" />}

            {fulfillments && (
              <>
              <div className="space-y-3 md:hidden">
                {fulfillments.items.map((f: FulfillmentOrderDto) => (
                  <article key={f.id} className="rounded-2xl border border-zinc-200 bg-white p-5 shadow-sm">
                    <div className="flex items-start justify-between gap-3">
                      <div><p className="font-mono text-[10px] text-zinc-500">{f.id.slice(0, 8)} · unit {f.unitId.slice(0, 8)}</p><p className="mt-1 font-mono text-sm font-bold text-emerald-600">Rp {f.sellerNetAmount.toLocaleString('id-ID')}</p></div>
                      <span className={`rounded-full border px-2.5 py-1 font-mono text-[10px] font-bold ${FULFILLMENT_STYLES[f.fulfillmentStatus] ?? 'text-zinc-700 bg-zinc-50 border-zinc-200'}`}>{f.fulfillmentStatus}</span>
                    </div>
                    <div className="mt-4 flex items-center justify-between border-t border-zinc-100 pt-3">
                      <span className="font-mono text-[10px] text-zinc-500">{t('seller_escrow')}: {f.escrowStatus}{f.trackingNumber ? ` · ${f.courierName} · ${f.trackingNumber}` : ''}</span>
                      {f.fulfillmentStatus === 'PROCESSING' ? (<button onClick={() => setShipTarget(f)} className="rounded-full bg-zinc-900 px-3 py-1.5 text-[10px] font-bold text-white">{t('seller_ship')}</button>) : <span className="text-[10px] font-mono text-zinc-400">—</span>}
                    </div>
                  </article>
                ))}
                {fulfillments.items.length === 0 && <div className="rounded-2xl border border-zinc-200 bg-white p-8 text-center text-sm text-zinc-500">{t('seller_no_fulfillments')}</div>}
              </div>
              <div className="hidden overflow-x-auto rounded-3xl border border-zinc-200 bg-white p-6 shadow-sm md:block">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-zinc-200 text-[10px] text-zinc-500 uppercase tracking-wider font-mono font-semibold">
                      <th className="pb-4">{t('seller_fulfillments')}</th><th className="pb-4">{t('seller_net')}</th><th className="pb-4">{t('seller_escrow')}</th>
                      <th className="pb-4">{t('common_status')}</th><th className="pb-4">{t('seller_tracking_col')}</th><th className="pb-4 text-right">{t('common_actions')}</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-100">
                    {fulfillments.items.map((f: FulfillmentOrderDto) => (
                      <tr key={f.id} className="hover:bg-zinc-50 transition-colors">
                        <td className="py-4 font-mono text-zinc-500">
                          {f.id.slice(0, 8)}
                          <div className="text-[10px] text-zinc-400 mt-0.5">unit {f.unitId.slice(0, 8)}</div>
                        </td>
                        <td className="py-4 font-mono tnum text-emerald-600 font-bold">Rp {f.sellerNetAmount.toLocaleString('id-ID')}</td>
                        <td className="py-4 font-mono text-zinc-600">{f.escrowStatus}</td>
                        <td className="py-4">
                          <span className={`font-mono text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${FULFILLMENT_STYLES[f.fulfillmentStatus] ?? 'text-zinc-700 bg-zinc-50 border-zinc-200'}`}>{f.fulfillmentStatus}</span>
                        </td>
                        <td className="py-4 font-mono text-zinc-500">{f.trackingNumber ? `${f.courierName} • ${f.trackingNumber}` : '—'}</td>
                        <td className="py-4 text-right">
                          {f.fulfillmentStatus === 'PROCESSING' ? (
                            <button onClick={() => setShipTarget(f)} className="px-3 py-1.5 rounded-full bg-zinc-900 text-white text-[10px] font-bold hover:bg-zinc-700 transition-colors">
                              {t('seller_ship')}
                            </button>
                          ) : <span className="text-[10px] text-zinc-400 font-mono">—</span>}
                        </td>
                      </tr>
                    ))}
                    {fulfillments.items.length === 0 && (
                      <tr><td colSpan={6} className="py-10 text-center text-zinc-500">{t('seller_no_fulfillments')}</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
              </>
            )}
          </div>
        )}
      </div>

      {shipTarget && <ShipModal fulfillment={shipTarget} onClose={() => setShipTarget(null)} />}
    </div>
  );
}

function SellerReviewsPanel({ sellerId }: { sellerId: number }) {
  const t = useT();
  const { data, isPending } = useQuery({
    queryKey: queryKeys.seller.reviews(sellerId),
    queryFn: () => getSellerReviews(sellerId),
  });

  return (
    <section className="bg-white border border-zinc-200 rounded-3xl p-8 shadow-sm space-y-5">
      <h3 className="text-lg font-bold flex items-center gap-2"><Star className="h-5 w-5 text-amber-400" /> {t('review_seller')}</h3>
      {isPending && <div className="h-24 rounded-2xl bg-zinc-100 animate-pulse" />}
      {data && data.length > 0 && (
        <div className="space-y-3">
          {data.map((r: ReviewResponse) => (
            <div key={r.id} className="rounded-2xl border border-zinc-100 bg-zinc-50 p-4">
              <div className="flex items-center gap-1 mb-1.5">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Star key={i} className={`h-3.5 w-3.5 ${i < r.rating ? 'text-amber-400 fill-amber-400' : 'text-zinc-300'}`} />
                ))}
                <span className="ml-2 text-[10px] font-mono text-zinc-400">{new Date(r.createdAt).toLocaleDateString('id-ID')}</span>
              </div>
              <p className="text-xs text-zinc-600">{r.comment ?? '—'}</p>
            </div>
          ))}
        </div>
      )}
      {data && data.length === 0 && (
        <p className="text-sm text-zinc-500 text-center py-6">{t('review_no_reviews')}</p>
      )}
    </section>
  );
}

function CreateListingForm({ onDone }: { onDone: () => void }) {
  const t = useT();
  const queryClient = useQueryClient();
  const [unitId, setUnitId] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [askingPrice, setAskingPrice] = useState('');
  const [images, setImages] = useState('');
  const [error, setError] = useState('');

  const mutation = useMutation({
    mutationFn: createListing,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.listings.mine() });
      onDone();
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  function submit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    const urls = images.split(/\r?\n/).map((url) => url.trim()).filter(Boolean);
    mutation.mutate({
      unitId: unitId.trim(),
      title: title.trim(),
      description: description.trim() || undefined,
      askingPrice: parseInt(askingPrice, 10),
      images: urls.length > 0 ? JSON.stringify(urls) : undefined,
    });
  }

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  return (
    <form onSubmit={submit} className="bg-white border border-zinc-200 rounded-3xl p-8 space-y-5 shadow-sm">
      <h4 className="text-sm font-bold">{t('seller_new_listing')}</h4>
      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}
      <div className="grid sm:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('seller_unit_id')}</label>
          <input required value={unitId} onChange={(e) => setUnitId(e.target.value)} className={inputCls} placeholder={t('insp_unit_serial')} />
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('seller_asking_price')}</label>
          <input required type="number" min="0" value={askingPrice} onChange={(e) => setAskingPrice(e.target.value)} className={inputCls} placeholder="12500000" />
        </div>
      </div>
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('seller_title')}</label>
        <input required value={title} onChange={(e) => setTitle(e.target.value)} className={inputCls} placeholder="iPhone 15 Pro 256GB" />
      </div>
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('seller_description')}</label>
        <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} className={inputCls} placeholder="Condition, included accessories…" />
      </div>
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('seller_images')}</label>
        <textarea value={images} onChange={(e) => setImages(e.target.value)} rows={3} className={inputCls} placeholder="https://example.com/photo-1.jpg&#10;https://example.com/photo-2.jpg" />
        <p className="mt-1 text-[11px] text-zinc-500">Satu URL per baris</p>
      </div>
      <button type="submit" disabled={mutation.isPending} className="btn-blue px-6 py-3 text-xs disabled:opacity-60">
        {mutation.isPending ? t('seller_creating') : t('seller_create_listing')}
      </button>
    </form>
  );
}

function ShipModal({ fulfillment, onClose }: { fulfillment: FulfillmentOrderDto; onClose: () => void }) {
  const t = useT();
  const queryClient = useQueryClient();
  const [trackingNumber, setTrackingNumber] = useState('');
  const [courierName, setCourierName] = useState('');
  const [error, setError] = useState('');

  const mutation = useMutation({
    mutationFn: (input: { trackingNumber: string; courierName: string }) => shipFulfillment(fulfillment.id, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.orders.seller(0, 20) });
      onClose();
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-900/40 backdrop-blur-sm p-4" onClick={onClose}>
      <form
        onSubmit={(e) => { e.preventDefault(); setError(''); mutation.mutate({ trackingNumber: trackingNumber.trim(), courierName: courierName.trim() }); }}
        className="w-full max-w-md rounded-3xl border border-zinc-200 bg-white p-8 space-y-5 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between">
          <h4 className="text-sm font-bold">{t('seller_ship_fulfillment')} {fulfillment.id.slice(0, 8)}</h4>
          <button type="button" onClick={onClose} className="p-1.5 rounded-lg hover:bg-zinc-100"><X className="h-4 w-4" /></button>
        </div>
        {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('seller_courier')}</label>
          <input required value={courierName} onChange={(e) => setCourierName(e.target.value)} className={inputCls} placeholder="JNE / SiCepat / J&T" />
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('seller_tracking')}</label>
          <input required value={trackingNumber} onChange={(e) => setTrackingNumber(e.target.value)} className={inputCls} placeholder="AWB number" />
        </div>
        <button type="submit" disabled={mutation.isPending} aria-busy={mutation.isPending} className="w-full btn-blue py-3 text-xs disabled:opacity-60">
          {mutation.isPending ? t('seller_marking_shipped') : t('seller_confirm_ship')}
        </button>
      </form>
    </div>
  );
}
