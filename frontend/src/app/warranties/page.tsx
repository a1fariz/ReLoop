'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ShieldCheck, Scale, X } from 'lucide-react';
import { apiErrorMessage, createDispute, getMyDisputes, getMyWarranties } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import { useAuthStore } from '@/lib/auth';

export default function WarrantiesPage() {
  const t = useT();
  const { accessToken } = useAuthStore();
  const [page, setPage] = useState(0);

  const { data: warrantyPage, isPending: wPending } = useQuery({
    queryKey: queryKeys.warranties.mine(page, 20),
    queryFn: () => getMyWarranties(page, 20),
    enabled: accessToken !== null,
  });

  const { data: disputePage, isPending: dPending } = useQuery({
    queryKey: queryKeys.disputes.mine(page, 20),
    queryFn: () => getMyDisputes(page, 20),
    enabled: accessToken !== null,
  });

  const warranties = warrantyPage?.items ?? [];
  const totalPages = warrantyPage ? Math.max(1, Math.ceil(warrantyPage.total / warrantyPage.size)) : 1;

  if (!accessToken) {
    return (
      <div className="min-h-screen bg-[#fafafa] py-24 text-center">
        <ShieldCheck className="h-12 w-12 text-zinc-300 mx-auto mb-4" />
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
          <h1 className="text-4xl sm:text-5xl font-semibold tracking-tight">{t('war_title')}</h1>
          <p className="text-sm text-zinc-600 mt-3 max-w-2xl leading-relaxed">{t('war_desc')}</p>
        </div>

        {/* My warranties */}
        <section className="space-y-5">
          <h3 className="text-lg font-bold flex items-center gap-2"><ShieldCheck className="h-5 w-5 text-sky-600" /> {t('war_my')}</h3>
          {wPending && <div className="h-28 rounded-2xl bg-zinc-100 animate-pulse" />}
          {!wPending && (
            <div className="grid md:grid-cols-2 gap-4">
              {warranties.map((w) => (
                <div key={w.id} className="bg-white border border-zinc-200 rounded-2xl p-6 shadow-sm">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <div className="font-mono text-xs text-zinc-500">{t('war_unit')}: {w.unitId.slice(0, 13)}…</div>
                      <div className="text-sm font-bold mt-1">{w.policyTier}</div>
                      <div className="text-xs font-mono text-zinc-500 mt-1">
                        {t('war_period')}: {new Date(w.startsAt).toLocaleDateString('id-ID')} → {new Date(w.expiresAt).toLocaleDateString('id-ID')}
                      </div>
                    </div>
                    <span className={`font-mono text-[10px] font-bold px-2.5 py-1 rounded-full border whitespace-nowrap ${
                      w.isVoided ? 'text-red-700 bg-red-50 border-red-200' : 'text-emerald-700 bg-emerald-50 border-emerald-200'
                    }`}>
                      {w.isVoided ? t('war_voided') : t('war_valid')}
                    </span>
                  </div>
                </div>
              ))}
              {warranties.length === 0 && (
                <div className="md:col-span-2 p-10 rounded-2xl border border-zinc-200 bg-white text-center text-sm text-zinc-500">
                  {t('war_no_warranties')}
                </div>
              )}
            </div>
          )}

          {warrantyPage && warrantyPage.total > warrantyPage.size && (
            <div className="flex items-center justify-center gap-3 text-xs font-mono">
              <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="px-4 py-2 rounded-full border border-zinc-200 font-semibold disabled:opacity-40">{t('common_prev')}</button>
              <span className="text-zinc-500">{t('common_page')} {page + 1} / {totalPages}</span>
              <button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)} className="px-4 py-2 rounded-full border border-zinc-200 font-semibold disabled:opacity-40">{t('common_next')}</button>
            </div>
          )}
        </section>

        {/* My disputes */}
        <section className="space-y-5">
          <DisputesSection disputesPending={dPending} disputes={disputePage?.items ?? []} />
        </section>
      </div>
    </div>
  );
}

function DisputesSection({ disputesPending, disputes }: { disputesPending: boolean; disputes: import('@/types/api').DisputeResponse[] }) {
  const t = useT();
  const [showForm, setShowForm] = useState(false);

  if (disputesPending) {
    return <div className="h-28 rounded-2xl bg-zinc-100 animate-pulse" />;
  }

  return (
    <>
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-bold flex items-center gap-2"><Scale className="h-5 w-5 text-sky-600" /> {t('war_disputes')}</h3>
        {!showForm && (
          <button onClick={() => setShowForm(true)} className="btn-blue px-5 py-2.5 text-xs">
            {t('war_file_dispute')}
          </button>
        )}
      </div>

      {showForm && <DisputeForm onDone={() => setShowForm(false)} />}

      <div className="space-y-3 sm:hidden">
          {disputes.map((d) => <article key={d.id} className="rounded-2xl border border-zinc-200 bg-white p-5 shadow-sm"><div className="flex items-start justify-between gap-3"><div><p className="font-mono text-[10px] text-zinc-500">{d.id.slice(0, 8)} · {d.fulfillmentOrderId.slice(0, 8)}</p><h4 className="mt-1 font-bold">{d.reason}</h4></div><span className={`rounded-full border px-2.5 py-1 font-mono text-[10px] font-bold ${d.status === 'RESOLVED' ? 'text-emerald-700 bg-emerald-50 border-emerald-200' : 'text-amber-700 bg-amber-50 border-amber-200'}`}>{d.status}</span></div><p className="mt-3 line-clamp-2 text-xs text-zinc-500">{d.claimDescription}</p></article>)}
          {disputes.length === 0 && <div className="rounded-2xl border border-zinc-200 bg-white p-8 text-center text-sm text-zinc-500">{t('war_no_disputes')}</div>}
        </div>
      <div className="hidden overflow-x-auto bg-white border border-zinc-200 rounded-3xl p-6 shadow-sm sm:block">
        <table className="w-full text-left text-xs">
          <thead>
            <tr className="border-b border-zinc-200 text-[10px] text-zinc-500 uppercase tracking-wider font-mono font-semibold">
              <th className="pb-4">ID</th><th className="pb-4">{t('war_fulfillment_id')}</th><th className="pb-4">{t('war_reason')}</th><th className="pb-4">{t('common_status')}</th><th className="pb-4 text-right">{t('common_date')}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100">
            {disputes.map((d) => (
              <tr key={d.id} className="hover:bg-zinc-50 transition-colors">
                <td className="py-4 font-mono text-zinc-500">{d.id.slice(0, 8)}</td>
                <td className="py-4 font-mono">{d.fulfillmentOrderId.slice(0, 8)}</td>
                <td className="py-4 max-w-[280px]">
                  <span className="font-bold">{d.reason}</span>
                  <div className="text-[10px] text-zinc-400 line-clamp-1">{d.claimDescription}</div>
                </td>
                <td className="py-4">
                  <span className={`font-mono text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${
                    d.status === 'RESOLVED' ? 'text-emerald-700 bg-emerald-50 border-emerald-200' : 'text-amber-700 bg-amber-50 border-amber-200'
                  }`}>{d.status}</span>
                </td>
                <td className="py-4 text-right font-mono text-zinc-500">{new Date(d.createdAt).toLocaleDateString('id-ID')}</td>
              </tr>
            ))}
            {disputes.length === 0 && (
              <tr><td colSpan={5} className="py-10 text-center text-zinc-500">{t('war_no_disputes')}</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

function DisputeForm({ onDone }: { onDone: () => void }) {
  const t = useT();
  const queryClient = useQueryClient();
  const [fulfillmentOrderId, setFulfillmentOrderId] = useState('');
  const [sellerId, setSellerId] = useState('');
  const [reason, setReason] = useState('');
  const [claimDescription, setClaimDescription] = useState('');
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  const mutation = useMutation({
    mutationFn: createDispute,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['disputes', 'mine'] });
      setDone(true);
      onDone();
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        setError('');
        mutation.mutate({
          fulfillmentOrderId: fulfillmentOrderId.trim(),
          sellerId: parseInt(sellerId, 10),
          reason: reason.trim(),
          claimDescription: claimDescription.trim(),
        });
      }}
      className="bg-white border border-zinc-200 rounded-3xl p-8 space-y-5 shadow-sm"
    >
      <div>
        <h4 className="text-sm font-bold">{t('war_file_dispute')}</h4>
        <p className="text-xs text-zinc-500 mt-1">{t('war_dispute_desc')}</p>
      </div>
      {done && <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs text-emerald-700">{t('war_dispute_filed')}</div>}
      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}

      <div className="grid sm:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('war_fulfillment_id')}</label>
          <input required value={fulfillmentOrderId} onChange={(e) => setFulfillmentOrderId(e.target.value)} className={inputCls} placeholder="00000000-0000-…" />
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('war_seller_id')}</label>
          <input required type="number" min="1" value={sellerId} onChange={(e) => setSellerId(e.target.value)} className={inputCls} placeholder="20" />
        </div>
      </div>
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('war_reason')}</label>
        <input required value={reason} onChange={(e) => setReason(e.target.value)} className={inputCls} placeholder="DAMAGED_SCREEN / NOT_AS_DESCRIBED" maxLength={100} />
      </div>
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('war_claim')}</label>
        <textarea required value={claimDescription} onChange={(e) => setClaimDescription(e.target.value)} rows={3} className={inputCls} maxLength={2000} />
      </div>
      <div className="flex gap-3">
        <button type="submit" disabled={mutation.isPending} className="btn-blue px-6 py-3 text-xs disabled:opacity-60">
          {mutation.isPending ? t('common_loading') : t('war_submit_dispute')}
        </button>
        <button type="button" onClick={onDone} className="px-6 py-3 rounded-full border border-zinc-200 text-xs font-semibold text-zinc-600 hover:bg-zinc-50">
          {t('common_cancel')}
        </button>
      </div>
    </form>
  );
}
