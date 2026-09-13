'use client';

import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Wallet } from 'lucide-react';
import { apiErrorMessage, getMyPayments } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import { useAuthStore } from '@/lib/auth';
import type { PaymentAttemptDto } from '@/types/api';

const PAYMENT_STATUS_STYLES: Record<string, string> = {
  SUCCEEDED: 'text-emerald-700 bg-emerald-50 border-emerald-200',
  FAILED: 'text-red-700 bg-red-50 border-red-200',
  EXPIRED: 'text-zinc-500 bg-zinc-50 border-zinc-200',
  PROCESSING: 'text-sky-700 bg-sky-50 border-sky-200',
  INITIATED: 'text-amber-700 bg-amber-50 border-amber-200',
};

export default function PaymentsPage() {
  const t = useT();
  const { accessToken } = useAuthStore();
  const [page, setPage] = useState(0);

  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: queryKeys.payments.mine(page, 20),
    queryFn: () => getMyPayments(page, 20),
    enabled: accessToken !== null,
  });

  if (!accessToken) {
    return (
      <div className="min-h-screen bg-[#fafafa] py-24 text-center">
        <Wallet className="h-12 w-12 text-zinc-300 mx-auto mb-4" />
        <h1 className="text-2xl font-bold mb-2">{t('common_sign_in_required')}</h1>
      </div>
    );
  }

  const attempts = data?.items ?? [];
  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 py-16 ambient-light-mesh">
      <div className="container mx-auto px-6 max-w-6xl space-y-10">
        <div className="pb-6 border-b border-zinc-200">
          <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold mb-2">
            {t('pay_title')}
          </div>
          <h1 className="text-4xl sm:text-5xl font-semibold tracking-tight">{t('pay_title')}</h1>
          <p className="text-sm text-zinc-600 mt-3 max-w-2xl leading-relaxed">{t('pay_desc')}</p>
        </div>

        {isError && <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-6 text-center text-sm text-red-800"><p>{apiErrorMessage(error)}</p><button onClick={() => void refetch()} className="btn-primary-dark mt-4 px-5 py-3 text-xs">{t('common_try_again')}</button></div>}

        {isPending && <div className="h-40 rounded-3xl bg-zinc-100 animate-pulse" />}

        {!isPending && !isError && (
          <div className="overflow-x-auto bg-white border border-zinc-200 rounded-3xl p-6 shadow-sm">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-zinc-200 text-[10px] text-zinc-500 uppercase tracking-wider font-mono font-semibold">
                  <th className="pb-4">{t('pay_attempt')}</th><th className="pb-4">{t('nav_orders')}</th><th className="pb-4">{t('pay_amount')}</th>
                  <th className="pb-4">{t('pay_status')}</th><th className="pb-4">{t('pay_gateway_ref')}</th><th className="pb-4 text-right">{t('common_page')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100">
                {attempts.map((p: PaymentAttemptDto) => (
                  <tr key={p.id} className="hover:bg-zinc-50 transition-colors">
                    <td className="py-4 font-mono text-zinc-500">{p.id.slice(0, 8)}</td>
                    <td className="py-4 font-mono">{p.masterOrderId.slice(0, 8)}</td>
                    <td className="py-4 font-mono tnum">Rp {p.amount.toLocaleString('id-ID')}</td>
                    <td className="py-4">
                      <span className={`font-mono text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${PAYMENT_STATUS_STYLES[p.status] ?? 'text-zinc-700 bg-zinc-50 border-zinc-200'}`}>
                        {p.status}
                      </span>
                    </td>
                    <td className="py-4 font-mono text-zinc-500 max-w-[140px] truncate">{p.gatewayReference.slice(0, 12)}</td>
                    <td className="py-4 text-right font-mono text-zinc-500">{new Date(p.createdAt).toLocaleDateString('id-ID')}</td>
                  </tr>
                ))}
                {attempts.length === 0 && (
                  <tr><td colSpan={6} className="py-10 text-center text-zinc-500">{t('pay_empty')}</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {data && data.total > data.size && (
          <div className="flex items-center justify-center gap-3 text-xs font-mono">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="px-4 py-2 rounded-full border border-zinc-200 font-semibold disabled:opacity-40">{t('common_prev')}</button>
            <span className="text-zinc-500">{t('common_page')} {page + 1} / {totalPages}</span>
            <button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)} className="px-4 py-2 rounded-full border border-zinc-200 font-semibold disabled:opacity-40">{t('common_next')}</button>
          </div>
        )}
      </div>
    </div>
  );
}
