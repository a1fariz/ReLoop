'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { Package, ArrowRight } from 'lucide-react';
import { useAuthStore } from '@/lib/auth';
import { getMyOrders, apiErrorMessage } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import FirebaseGoogleButton from '@/components/FirebaseGoogleButton';
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
  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: queryKeys.orders.mine(page, 20),
    queryFn: () => getMyOrders(page, 20),
    enabled: accessToken !== null,
  });

  if (!accessToken) return <div className="min-h-screen bg-[#fafaf9] py-24 text-center"><Package className="mx-auto mb-4 h-12 w-12 text-stone-300" /><h1 className="mb-2 text-2xl font-bold">{t('common_sign_in_required')}</h1><Link href="/login" className="btn-blue mt-2 inline-flex px-6 py-3 text-xs">{t('nav_sign_in')}</Link><div className="mx-auto mt-4 max-w-xs"><div className="relative my-3"><div className="absolute inset-0 flex items-center"><div className="w-full border-t border-stone-200" /></div><div className="relative flex justify-center text-xs"><span className="bg-[#fafaf9] px-3 text-stone-400 uppercase">{t('auth_or')}</span></div></div><FirebaseGoogleButton /></div></div>;
  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  return <main className="min-h-screen bg-[#fafaf9] py-12 text-stone-950 sm:py-16 ambient-light-mesh"><div className="mx-auto max-w-5xl px-4 sm:px-6">
    <div className="mb-10 border-b border-stone-200 pb-6"><div className="mb-2 text-xs font-mono uppercase tracking-widest text-sky-800 font-semibold">{t('orders_buyer_account')}</div><h1 className="text-4xl font-semibold tracking-tight sm:text-5xl">{t('orders_my')}</h1></div>
    {isError && <div className="mb-8 rounded-2xl border border-red-200 bg-red-50 p-5 text-center text-sm text-red-800" role="alert"><p>{apiErrorMessage(error)}</p><button onClick={() => void refetch()} className="btn-primary-dark mt-4 px-5 py-3 text-xs">{t('common_try_again')}</button></div>}
    {isPending && <div className="space-y-4">{Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-28 animate-pulse rounded-2xl bg-stone-200" />)}</div>}
    {data && <><div className="space-y-4">{data.items.map((order) => <OrderCard key={order.id} order={order} />)}{data.items.length === 0 && <div className="rounded-3xl border border-stone-200 bg-white p-12 text-center"><Package className="mx-auto mb-3 h-10 w-10 text-stone-300" /><div className="mb-1 font-bold">{t('orders_no_orders')}</div><p className="mb-5 text-sm text-stone-500">{t('orders_no_orders_desc')}</p><Link href="/catalog" className="btn-blue inline-flex items-center gap-2 px-6 py-3 text-xs">{t('orders_browse')} <ArrowRight className="h-3.5 w-3.5" /></Link></div>}</div>{data.total > data.size && <div className="mt-10 flex items-center justify-center gap-3 text-xs font-mono"><button disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))} className="rounded-full border border-stone-200 px-5 py-2.5 font-semibold disabled:opacity-40 hover:bg-white">{t('common_prev')}</button><span className="text-stone-500">{t('common_page')} {page + 1} / {totalPages}</span><button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)} className="rounded-full border border-stone-200 px-5 py-2.5 font-semibold disabled:opacity-40 hover:bg-white">{t('common_next')}</button></div>}</>}
  </div></main>;
}

function OrderCard({ order }: { order: MasterOrderDto }) {
  const t = useT();
  return <article className="flex flex-col justify-between gap-4 rounded-2xl border border-stone-200 bg-white p-5 shadow-sm transition-colors hover:border-stone-300 sm:flex-row sm:items-center sm:p-6"><div><div className="text-xs font-mono text-stone-500">{new Date(order.createdAt).toLocaleString('id-ID')}</div><div className="mt-0.5 text-lg font-bold">{order.orderNumber}</div></div><div className="flex w-full flex-wrap items-center justify-between gap-4 sm:w-auto sm:justify-end"><div className="text-left sm:text-right"><div className="text-[10px] uppercase font-mono text-stone-500">{t('orders_total')}</div><div className="font-mono font-bold text-stone-950 tnum">Rp {order.totalAmount.toLocaleString('id-ID')}</div></div><span className={`rounded-full border px-3 py-1 font-mono text-[10px] font-bold ${PAYMENT_STYLES[order.paymentStatus] ?? 'text-stone-700 bg-stone-50 border-stone-200'}`}>{order.paymentStatus}</span></div></article>;
}
