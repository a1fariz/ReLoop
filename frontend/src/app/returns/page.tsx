'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Undo2, PackageCheck } from 'lucide-react';
import { apiErrorMessage, getMyReturns, requestReturn } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import { useAuthStore } from '@/lib/auth';

export default function ReturnsPage() {
  const t = useT();
  const { accessToken } = useAuthStore();
  const [page, setPage] = useState(0);

  const { data: returnPage, isPending } = useQuery({
    queryKey: queryKeys.returns.mine(page, 20),
    queryFn: () => getMyReturns(page, 20),
    enabled: accessToken !== null,
  });

  const returns = returnPage?.items ?? [];
  const totalPages = returnPage ? Math.max(1, Math.ceil(returnPage.total / returnPage.size)) : 1;

  if (!accessToken) {
    return (
      <div className="min-h-screen bg-[#fafafa] py-24 text-center">
        <Undo2 className="h-12 w-12 text-zinc-300 mx-auto mb-4" />
        <h1 className="text-2xl font-bold mb-2">{t('common_sign_in_required')}</h1>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 py-16 ambient-light-mesh">
      <div className="container mx-auto px-6 max-w-6xl space-y-12">
        <div className="pb-6 border-b border-zinc-200">
          <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold mb-2">
            {t('footer_trust')}
          </div>
          <h1 className="text-4xl sm:text-5xl font-semibold tracking-tight">{t('ret_title')}</h1>
          <p className="text-sm text-zinc-600 mt-3 max-w-2xl leading-relaxed">{t('ret_desc')}</p>
        </div>

        <ReturnRequestForm />

        <section className="space-y-5">
          <h3 className="text-lg font-bold flex items-center gap-2"><Undo2 className="h-5 w-5 text-sky-600" /> {t('ret_my')}</h3>
          {isPending && <div className="h-28 rounded-2xl bg-zinc-100 animate-pulse" />}
          {!isPending && (
            <div className="overflow-x-auto bg-white border border-zinc-200 rounded-3xl p-6 shadow-sm">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-zinc-200 text-[10px] text-zinc-500 uppercase tracking-wider font-mono font-semibold">
                    <th className="pb-4">ID</th><th className="pb-4">{t('war_fulfillment_id')}</th><th className="pb-4">{t('ret_reason')}</th><th className="pb-4">{t('common_status')}</th><th className="pb-4 text-right">{t('ret_refund')}</th><th className="pb-4 text-right">{t('common_date')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100">
                  {returns.map((r) => (
                    <tr key={r.id} className="hover:bg-zinc-50 transition-colors">
                      <td className="py-4 font-mono text-zinc-500">{r.id.slice(0, 8)}</td>
                      <td className="py-4 font-mono">{r.fulfillmentOrderId.slice(0, 8)}</td>
                      <td className="py-4 max-w-[280px]">
                        <span className="font-bold">{r.reason}</span>
                        <div className="text-[10px] text-zinc-400 line-clamp-1">{r.description}</div>
                      </td>
                      <td className="py-4">
                        <span className={`font-mono text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${
                          r.status === 'REFUNDED' || r.status === 'CLOSED'
                            ? 'text-emerald-700 bg-emerald-50 border-emerald-200'
                            : r.status === 'REJECTED'
                              ? 'text-red-700 bg-red-50 border-red-200'
                              : 'text-amber-700 bg-amber-50 border-amber-200'
                        }`}>{r.status}</span>
                      </td>
                      <td className="py-4 text-right font-mono">{r.refundAmount > 0 ? `Rp ${r.refundAmount.toLocaleString('id-ID')}` : '—'}</td>
                      <td className="py-4 text-right font-mono text-zinc-500">{new Date(r.createdAt).toLocaleDateString('id-ID')}</td>
                    </tr>
                  ))}
                  {returns.length === 0 && (
                    <tr><td colSpan={6} className="py-10 text-center text-zinc-500">{t('ret_no_returns')}</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}

          {returnPage && returnPage.total > returnPage.size && (
            <div className="flex items-center justify-center gap-3 text-xs font-mono">
              <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="px-4 py-2 rounded-full border border-zinc-200 font-semibold disabled:opacity-40">{t('common_prev')}</button>
              <span className="text-zinc-500">{t('common_page')} {page + 1} / {totalPages}</span>
              <button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)} className="px-4 py-2 rounded-full border border-zinc-200 font-semibold disabled:opacity-40">{t('common_next')}</button>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function ReturnRequestForm() {
  const t = useT();
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [fulfillmentOrderId, setFulfillmentOrderId] = useState('');
  const [reason, setReason] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  const mutation = useMutation({
    mutationFn: requestReturn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['returns', 'mine'] });
      setDone(true);
      setShowForm(false);
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  if (!showForm) {
    return (
      <div className="flex items-center justify-between bg-white border border-zinc-200 rounded-2xl p-6 shadow-sm">
        <div className="flex items-center gap-3 text-xs text-zinc-600">
          <PackageCheck className="h-4 w-4 text-sky-600" />
          {t('ret_request_desc')}
        </div>
        <button onClick={() => { setShowForm(true); setDone(false); setError(''); }} className="btn-blue px-5 py-2.5 text-xs">
          {t('ret_request')}
        </button>
      </div>
    );
  }

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        setError('');
        mutation.mutate({
          fulfillmentOrderId: fulfillmentOrderId.trim(),
          reason: reason.trim(),
          description: description.trim(),
        });
      }}
      className="bg-white border border-zinc-200 rounded-3xl p-8 space-y-5 shadow-sm"
    >
      <div>
        <h4 className="text-sm font-bold">{t('ret_request')}</h4>
        <p className="text-xs text-zinc-500 mt-1">{t('ret_request_desc')}</p>
      </div>
      {done && <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs text-emerald-700">{t('ret_filed')}</div>}
      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}

      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('war_fulfillment_id')}</label>
        <input required value={fulfillmentOrderId} onChange={(e) => setFulfillmentOrderId(e.target.value)} className={inputCls} placeholder="00000000-0000-…" />
      </div>
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('ret_reason')}</label>
        <input required value={reason} onChange={(e) => setReason(e.target.value)} className={inputCls} placeholder="NOT_AS_DESCRIBED / DAMAGED_IN_TRANSIT" maxLength={100} />
      </div>
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('ret_claim')}</label>
        <textarea required value={description} onChange={(e) => setDescription(e.target.value)} rows={3} className={inputCls} maxLength={2000} />
      </div>
      <div className="flex gap-3">
        <button type="submit" disabled={mutation.isPending} className="btn-blue px-6 py-3 text-xs disabled:opacity-60">
          {mutation.isPending ? t('common_loading') : t('ret_submit')}
        </button>
        <button type="button" onClick={() => setShowForm(false)} className="px-6 py-3 rounded-full border border-zinc-200 text-xs font-semibold text-zinc-600 hover:bg-zinc-50">
          {t('common_cancel')}
        </button>
      </div>
    </form>
  );
}
