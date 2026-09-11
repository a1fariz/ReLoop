'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Package, ArrowRight, Star, X } from 'lucide-react';
import { useAuthStore } from '@/lib/auth';
import { getMyOrders, submitReview, apiErrorMessage } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import type { MasterOrderDto } from '@/types/api';

const PAYMENT_STYLES: Record<string, string> = {
  PAID: 'text-emerald-700 bg-emerald-50 border-emerald-200',
  HELD: 'text-sky-700 bg-sky-50 border-sky-200',
  REFUNDED: 'text-amber-700 bg-amber-50 border-amber-200',
};

export default function OrdersPage() {
  const t = useT();
  const { accessToken } = useAuthStore();
  const [page, setPage] = useState(0);

  const { data, isPending, isError, error } = useQuery({
    queryKey: queryKeys.orders.mine(page, 20),
    queryFn: () => getMyOrders(page, 20),
    enabled: accessToken !== null,
  });

  if (!accessToken) {
    return (
      <div className="min-h-screen bg-[#fafafa] py-24 text-center">
        <Package className="h-12 w-12 text-zinc-300 mx-auto mb-4" />
        <h1 className="text-2xl font-bold mb-2">{t('common_sign_in_required')}</h1>
        <Link href="/login" className="btn-blue px-6 py-3 text-xs inline-flex mt-2">{t('nav_sign_in')}</Link>
      </div>
    );
  }

  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 py-16 ambient-light-mesh">
      <div className="container mx-auto px-6 max-w-5xl">
        <div className="mb-12 pb-6 border-b border-zinc-200">
          <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold mb-2">
            {t('orders_buyer_account')}
          </div>
          <h1 className="text-4xl sm:text-5xl font-semibold tracking-tight">{t('orders_my')}</h1>
        </div>

        {isError && (
          <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 mb-8">
            {t('orders_load_fail')}: {(error as Error).message}
          </div>
        )}

        {isPending && (
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="h-24 rounded-2xl bg-zinc-100 animate-pulse" />
            ))}
          </div>
        )}

        {data && (
          <>
            <div className="space-y-4">
              {data.items.map((order: MasterOrderDto) => (
                <OrderCard key={order.id} order={order} />
              ))}

              {data.items.length === 0 && (
                <div className="p-12 rounded-3xl border border-zinc-200 bg-white text-center">
                  <Package className="h-10 w-10 text-zinc-300 mx-auto mb-3" />
                  <div className="font-bold mb-1">{t('orders_no_orders')}</div>
                  <p className="text-sm text-zinc-500 mb-5">{t('orders_no_orders_desc')}</p>
                  <Link href="/catalog" className="btn-blue px-6 py-3 text-xs inline-flex items-center gap-2">
                    {t('orders_browse')} <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                </div>
              )}
            </div>

            {data.total > data.size && (
              <div className="flex items-center justify-center gap-3 mt-10 text-xs font-mono">
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="px-5 py-2.5 rounded-full border border-zinc-200 font-semibold disabled:opacity-40 hover:bg-white transition-all"
                >
                  {t('common_prev')}
                </button>
                <span className="text-zinc-500">{t('common_page')} {page + 1} / {totalPages}</span>
                <button
                  disabled={page + 1 >= totalPages}
                  onClick={() => setPage((p) => p + 1)}
                  className="px-5 py-2.5 rounded-full border border-zinc-200 font-semibold disabled:opacity-40 hover:bg-white transition-all"
                >
                  {t('common_next')}
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function OrderCard({ order }: { order: MasterOrderDto }) {
  const t = useT();
  const [showReview, setShowReview] = useState(false);

  return (
    <>
      <div className="bg-white border border-zinc-200 rounded-2xl p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-sm hover:border-zinc-300 transition-colors">
        <div>
          <div className="font-mono text-xs text-zinc-500">{new Date(order.createdAt).toLocaleString('id-ID')}</div>
          <div className="text-lg font-bold mt-0.5">{order.orderNumber}</div>
        </div>
        <div className="flex items-center gap-6">
          <div className="text-right">
            <div className="text-[10px] uppercase font-mono text-zinc-400">{t('orders_total')}</div>
            <div className="font-mono font-bold text-zinc-900 tnum">Rp {order.totalAmount.toLocaleString('id-ID')}</div>
          </div>
          <span className={`font-mono text-[10px] font-bold px-3 py-1 rounded-full border ${PAYMENT_STYLES[order.paymentStatus] ?? 'text-zinc-700 bg-zinc-50 border-zinc-200'}`}>
            {order.paymentStatus}
          </span>
          <button
            onClick={() => setShowReview((v) => !v)}
            className="text-xs font-semibold text-sky-600 hover:text-sky-700"
            title={t('review_only_completed')}
          >
            {t('review_write')} →
          </button>
        </div>
      </div>
      {showReview && <ReviewModal onClose={() => setShowReview(false)} />}
    </>
  );
}

function ReviewModal({ onClose }: { onClose: () => void }) {
  const t = useT();
  const queryClient = useQueryClient();
  const [fulfillmentOrderId, setFulfillmentOrderId] = useState('');
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  const mutation = useMutation({
    mutationFn: submitReview,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['seller', 'reviews'] });
      setDone(true);
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-900/40 backdrop-blur-sm p-4" onClick={onClose}>
      <form
        onSubmit={(e) => {
          e.preventDefault(); setError('');
          if (!fulfillmentOrderId.trim()) { setError(t('war_fulfillment_id')); return; }
          mutation.mutate({ fulfillmentOrderId: fulfillmentOrderId.trim(), rating, comment: comment.trim() || undefined });
        }}
        className="w-full max-w-md rounded-3xl border border-zinc-200 bg-white p-8 space-y-5 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between">
          <h4 className="text-sm font-bold">{t('review_title')}</h4>
          <button type="button" onClick={onClose} className="p-1.5 rounded-lg hover:bg-zinc-100"><X className="h-4 w-4" /></button>
        </div>
        {done && <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs text-emerald-700">{t('review_done')}</div>}
        {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('war_fulfillment_id')}</label>
          <input required value={fulfillmentOrderId} onChange={(e) => setFulfillmentOrderId(e.target.value)} className={inputCls} placeholder="00000000-0000-…" />
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('review_rating')}</label>
          <div className="flex gap-2">
            {[1, 2, 3, 4, 5].map((n) => (
              <button
                key={n} type="button" onClick={() => setRating(n)}
                className={`p-2 rounded-xl border transition-colors ${n <= rating ? 'border-amber-300 bg-amber-50' : 'border-zinc-200 hover:bg-zinc-50'}`}
              >
                <Star className={`h-5 w-5 ${n <= rating ? 'text-amber-400 fill-amber-400' : 'text-zinc-300'}`} />
              </button>
            ))}
          </div>
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('review_comment')}</label>
          <textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={3} className={inputCls} maxLength={2000} placeholder={t('review_comment_placeholder')} />
        </div>
        <button type="submit" disabled={mutation.isPending} className="w-full btn-blue py-3 text-xs disabled:opacity-60">
          {mutation.isPending ? t('review_submitting') : t('review_submit')}
        </button>
      </form>
    </div>
  );
}
