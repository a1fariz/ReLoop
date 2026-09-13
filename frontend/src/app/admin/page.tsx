'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ShieldCheck, Truck, CheckCircle, Banknote, Scale, Unlock, X, Undo2, Landmark, CreditCard } from 'lucide-react';
import { useAuthStore } from '@/lib/auth';
import {
  apiErrorMessage, closeReturn, completeFulfillment, deliverFulfillment, disbursePayout,
  finalizeReturnRefund, getAdminDisputes, getAdminFulfillments, getAdminPayments, getAdminReturns,
  getEscrowStats, inspectReturn, markReturnReceived, recordReturnShipment, resolveDispute,
  reviewReturn, unlockUser,
} from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import type { DisputeResponse, FulfillmentOrderDto, PaymentAttemptDto, ResolveDisputeRequest, ReturnResponse } from '@/types/api';

const FULFILLMENT_STYLES: Record<string, string> = {
  PROCESSING: 'text-sky-700 bg-sky-50 border-sky-200',
  SHIPPED: 'text-indigo-700 bg-indigo-50 border-indigo-200',
  DELIVERED: 'text-cyan-700 bg-cyan-50 border-cyan-200',
  COMPLETED: 'text-emerald-700 bg-emerald-50 border-emerald-200',
  DISPUTED: 'text-red-700 bg-red-50 border-red-200',
  CANCELLED: 'text-zinc-500 bg-zinc-50 border-zinc-200',
};

export default function AdminPage() {
  const { accessToken, role } = useAuthStore();
  const t = useT();

  if (!accessToken || role !== 'ADMIN') {
    return (
      <div className="min-h-screen bg-[#fafafa] py-24 text-center">
        <ShieldCheck className="h-12 w-12 text-zinc-300 mx-auto mb-4" />
        <h1 className="text-2xl font-bold mb-2">{t('admin_access_required')}</h1>
        <p className="text-sm text-zinc-500">{t('admin_access_desc')}</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 py-16 ambient-light-mesh">
      <div className="container mx-auto px-6 max-w-7xl space-y-14">
        <div className="pb-6 border-b border-zinc-200">
          <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold mb-2">
            {t('admin_console')}
          </div>
          <h1 className="text-4xl sm:text-5xl font-semibold tracking-tight">{t('admin_bureau')}</h1>
        </div>

        <FulfillmentsTable />
        <DisputesTable />
        <ReturnsTable />
        <EscrowPaymentsSection />
        <UnlockForm />
      </div>
    </div>
  );
}

function FulfillmentsTable() {
  const t = useT();
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();
  const [error, setError] = useState('');

  const { data, isPending } = useQuery({
    queryKey: queryKeys.orders.admin(page, 20),
    queryFn: () => getAdminFulfillments(page, 20),
  });

  const action = useMutation({
    mutationFn: ({ id, fn }: { id: string; fn: (id: string) => Promise<unknown> }) => fn(id),
    onSuccess: () => {
      setError('');
      queryClient.invalidateQueries({ queryKey: ['orders', 'admin'] });
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const nextActions: Record<string, { label: string; fn: (id: string) => Promise<unknown>; icon: React.ReactNode } | null> = {
    PROCESSING: null, // waits on seller ship
    SHIPPED: { label: t('admin_mark_delivered'), fn: deliverFulfillment, icon: <Truck className="h-3 w-3" /> },
    DELIVERED: { label: t('admin_complete_settle'), fn: completeFulfillment, icon: <CheckCircle className="h-3 w-3" /> },
    COMPLETED: { label: t('admin_disburse'), fn: disbursePayout, icon: <Banknote className="h-3 w-3" /> },
  };

  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  return (
    <section className="bg-white border border-zinc-200 rounded-3xl p-8 shadow-sm space-y-6">
      <div className="flex items-baseline justify-between border-b border-zinc-100 pb-4">
        <div>
          <h3 className="text-lg font-bold flex items-center gap-2"><Truck className="h-5 w-5 text-sky-600" /> {t('admin_pipeline')}</h3>
          <p className="text-xs text-zinc-500 mt-0.5 font-mono">{t('admin_pipeline_desc')}</p>
        </div>
        <span className="font-mono text-xs text-zinc-500">{data?.total ?? '…'} {t('common_total')}</span>
      </div>

      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}

      {isPending ? (
        <div className="h-40 rounded-2xl bg-zinc-100 animate-pulse" />
      ) : (
        <>
        <div className="space-y-3 md:hidden">
          {(data?.items ?? []).map((f: FulfillmentOrderDto) => {
            const next = nextActions[f.fulfillmentStatus];
            return (
              <article key={f.id} className="rounded-2xl border border-zinc-200 bg-white p-5 shadow-sm">
                <div className="flex items-start justify-between gap-3">
                  <div><p className="font-mono text-[10px] text-zinc-500">{f.id.slice(0, 8)} · unit {f.unitId.slice(0, 8)} · seller #{f.sellerId}</p><p className="mt-1 font-mono text-sm font-bold">Rp {f.sellerNetAmount.toLocaleString('id-ID')}</p><p className="text-[10px] text-zinc-400">fee {f.platformFeeAmount.toLocaleString('id-ID')}</p></div>
                  <span className={`rounded-full border px-2.5 py-1 font-mono text-[10px] font-bold ${FULFILLMENT_STYLES[f.fulfillmentStatus] ?? 'text-zinc-700 bg-zinc-50 border-zinc-200'}`}>{f.fulfillmentStatus}</span>
                </div>
                <div className="mt-4 flex items-center justify-between border-t border-zinc-100 pt-3">
                  <span className="font-mono text-[10px] text-zinc-500">{t('seller_escrow')}: {f.escrowStatus}{f.trackingNumber ? ` · ${f.courierName} · ${f.trackingNumber}` : ''}</span>
                  {next ? (action.isPending ? <span className="text-[10px] font-mono text-zinc-400">{t('admin_working')}</span> : <button onClick={() => action.mutate({ id: f.id, fn: next.fn })} className="inline-flex items-center gap-1.5 rounded-full bg-zinc-900 px-3 py-1.5 text-[10px] font-bold text-white">{next.icon} {next.label}</button>) : <span className="text-[10px] font-mono text-zinc-400">—</span>}
                </div>
              </article>
            );
          })}
          {data && data.items.length === 0 && <div className="rounded-2xl border border-zinc-200 bg-white p-8 text-center text-sm text-zinc-500">{t('admin_no_fulfillments')}</div>}
        </div>
        <div className="hidden overflow-x-auto md:block">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-zinc-200 text-[10px] text-zinc-500 uppercase tracking-wider font-mono font-semibold">
                <th className="pb-4">{t('seller_fulfillments')}</th><th className="pb-4">{t('detail_seller')}</th><th className="pb-4">{t('seller_net')} / Fee</th>
                <th className="pb-4">{t('seller_escrow')}</th><th className="pb-4">{t('common_status')}</th><th className="pb-4">{t('seller_tracking_col')}</th><th className="pb-4 text-right">{t('common_actions')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100">
              {(data?.items ?? []).map((f: FulfillmentOrderDto) => {
                const next = nextActions[f.fulfillmentStatus];
                return (
                  <tr key={f.id} className="hover:bg-zinc-50 transition-colors">
                    <td className="py-4 font-mono text-zinc-500">
                      {f.id.slice(0, 8)}
                      <div className="text-[10px] text-zinc-400 mt-0.5">unit {f.unitId.slice(0, 8)}</div>
                    </td>
                    <td className="py-4 font-mono">#{f.sellerId}</td>
                    <td className="py-4 font-mono tnum">
                      Rp {f.sellerNetAmount.toLocaleString('id-ID')}
                      <div className="text-[10px] text-zinc-400">fee {f.platformFeeAmount.toLocaleString('id-ID')}</div>
                    </td>
                    <td className="py-4 font-mono text-zinc-600">{f.escrowStatus}</td>
                    <td className="py-4">
                      <span className={`font-mono text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${FULFILLMENT_STYLES[f.fulfillmentStatus] ?? 'text-zinc-700 bg-zinc-50 border-zinc-200'}`}>
                        {f.fulfillmentStatus}
                      </span>
                    </td>
                    <td className="py-4 font-mono text-zinc-500">{f.trackingNumber ? `${f.courierName} • ${f.trackingNumber}` : '—'}</td>
                    <td className="py-4 text-right">
                      {next && action.isPending ? (
                        <span className="text-[10px] font-mono text-zinc-400">{t('admin_working')}</span>
                      ) : next ? (
                        <button
                          onClick={() => action.mutate({ id: f.id, fn: next.fn })}
                          aria-busy={action.isPending}
                          className="px-3 py-1.5 rounded-full bg-zinc-900 text-white text-[10px] font-bold hover:bg-zinc-700 transition-colors inline-flex items-center gap-1.5"
                        >
                          {next.icon} {next.label}
                        </button>
                      ) : (
                        <span className="text-[10px] text-zinc-400 font-mono">—</span>
                      )}
                    </td>
                  </tr>
                );
              })}
              {data && data.items.length === 0 && (
                <tr><td colSpan={7} className="py-10 text-center text-zinc-500">{t('admin_no_fulfillments')}</td></tr>
              )}
            </tbody>
          </table>
        </div>
        </>
      )}

      {data && data.total > data.size && (
        <div className="flex items-center justify-center gap-3 text-xs font-mono">
          <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="px-4 py-2 rounded-full border border-zinc-200 font-semibold disabled:opacity-40">{t('common_prev')}</button>
          <span className="text-zinc-500">{t('common_page')} {page + 1} / {totalPages}</span>
          <button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)} className="px-4 py-2 rounded-full border border-zinc-200 font-semibold disabled:opacity-40">{t('common_next')}</button>
        </div>
      )}
    </section>
  );
}

function DisputesTable() {
  const t = useT();
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();
  const [resolveTarget, setResolveTarget] = useState<DisputeResponse | null>(null);

  const { data, isPending } = useQuery({
    queryKey: queryKeys.disputes.admin(page, 20),
    queryFn: () => getAdminDisputes(page, 20),
  });

  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  return (
    <section className="bg-white border border-zinc-200 rounded-3xl p-8 shadow-sm space-y-6">
      <div className="flex items-baseline justify-between border-b border-zinc-100 pb-4">
        <div>
          <h3 className="text-lg font-bold flex items-center gap-2"><Scale className="h-5 w-5 text-sky-600" /> {t('admin_disputes')}</h3>
          <p className="text-xs text-zinc-500 mt-0.5 font-mono">{t('admin_disputes_desc')}</p>
        </div>
        <span className="font-mono text-xs text-zinc-500">{data?.total ?? '…'} {t('common_total')}</span>
      </div>

      {isPending ? (
        <div className="h-40 rounded-2xl bg-zinc-100 animate-pulse" />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-zinc-200 text-[10px] text-zinc-500 uppercase tracking-wider font-mono font-semibold">
                <th className="pb-4">{t('war_disputes')}</th><th className="pb-4">Buyer → Seller</th><th className="pb-4">{t('war_reason')}</th>
                <th className="pb-4">{t('common_status')}</th><th className="pb-4 text-right">{t('common_actions')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100">
              {(data?.items ?? []).map((d: DisputeResponse) => (
                <tr key={d.id} className="hover:bg-zinc-50 transition-colors">
                  <td className="py-4 font-mono text-zinc-500">
                    {d.id.slice(0, 8)}
                    <div className="text-[10px] text-zinc-400 mt-0.5">{new Date(d.createdAt).toLocaleDateString('id-ID')}</div>
                  </td>
                  <td className="py-4 font-mono">#{d.buyerId} → #{d.sellerId}</td>
                  <td className="py-4 max-w-[280px]">
                    <span className="font-bold">{d.reason}</span>
                    <div className="text-[10px] text-zinc-400 line-clamp-1">{d.claimDescription}</div>
                  </td>
                  <td className="py-4">
                    <span className={`font-mono text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${
                      d.status === 'RESOLVED' ? 'text-emerald-700 bg-emerald-50 border-emerald-200' : 'text-amber-700 bg-amber-50 border-amber-200'
                    }`}>{d.status}</span>
                  </td>
                  <td className="py-4 text-right">
                    {d.status === 'OPEN' ? (
                      <button onClick={() => setResolveTarget(d)} className="px-3 py-1.5 rounded-full bg-zinc-900 text-white text-[10px] font-bold hover:bg-zinc-700 transition-colors">
                        {t('admin_resolve')}
                      </button>
                    ) : (
                      <span className="text-[10px] font-mono text-zinc-400">{d.resolutionType ?? '—'}</span>
                    )}
                  </td>
                </tr>
              ))}
              {data && data.items.length === 0 && (
                <tr><td colSpan={5} className="py-10 text-center text-zinc-500">{t('admin_no_disputes')}</td></tr>
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

      {resolveTarget && <ResolveModal dispute={resolveTarget} onClose={() => setResolveTarget(null)} />}
    </section>
  );
}

function ResolveModal({ dispute, onClose }: { dispute: DisputeResponse; onClose: () => void }) {
  const t = useT();
  const queryClient = useQueryClient();
  const [resolutionType, setResolutionType] = useState<ResolveDisputeRequest['resolutionType']>('PARTIAL_REFUND');
  const [buyerRefundAmount, setBuyerRefundAmount] = useState('');
  const [sellerPayoutAmount, setSellerPayoutAmount] = useState('');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [error, setError] = useState('');

  const mutation = useMutation({
    mutationFn: (input: ResolveDisputeRequest) => resolveDispute(dispute.id, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['disputes', 'admin'] });
      queryClient.invalidateQueries({ queryKey: ['orders', 'admin'] });
      onClose();
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  function submit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    mutation.mutate({
      resolutionType,
      buyerRefundAmount: buyerRefundAmount ? parseInt(buyerRefundAmount, 10) : undefined,
      sellerPayoutAmount: sellerPayoutAmount ? parseInt(sellerPayoutAmount, 10) : undefined,
      resolutionNotes: resolutionNotes.trim() || undefined,
    });
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-900/40 backdrop-blur-sm p-4" onClick={onClose}>
      <form onSubmit={submit} className="w-full max-w-md rounded-3xl border border-zinc-200 bg-white p-8 space-y-5 shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <h4 className="text-sm font-bold">{t('admin_resolve_dispute')} {dispute.id.slice(0, 8)}</h4>
          <button type="button" onClick={onClose} className="p-1.5 rounded-lg hover:bg-zinc-100"><X className="h-4 w-4" /></button>
        </div>
        <p className="text-xs text-zinc-500 font-mono">{dispute.reason}: {dispute.claimDescription}</p>
        {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('admin_resolution_type')}</label>
          <select value={resolutionType} onChange={(e) => setResolutionType(e.target.value as ResolveDisputeRequest['resolutionType'])} className={inputCls}>
            <option value="FULL_REFUND">{t('admin_full_refund')}</option>
            <option value="PARTIAL_REFUND">{t('admin_partial_refund')}</option>
            <option value="RELEASE_PAYMENT">{t('admin_release_payment')}</option>
            <option value="REPAIR">{t('admin_repair')}</option>
            <option value="REPLACEMENT">{t('admin_replacement')}</option>
          </select>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold mb-1.5">{t('admin_buyer_refund')}</label>
            <input type="number" min="0" value={buyerRefundAmount} onChange={(e) => setBuyerRefundAmount(e.target.value)} className={inputCls} placeholder="0" />
          </div>
          <div>
            <label className="block text-xs font-semibold mb-1.5">{t('admin_seller_payout')}</label>
            <input type="number" min="0" value={sellerPayoutAmount} onChange={(e) => setSellerPayoutAmount(e.target.value)} className={inputCls} placeholder="0" />
          </div>
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('admin_notes')}</label>
          <textarea value={resolutionNotes} onChange={(e) => setResolutionNotes(e.target.value)} rows={2} className={inputCls} placeholder="Arbitration rationale…" />
        </div>
        <button type="submit" disabled={mutation.isPending} aria-busy={mutation.isPending} className="w-full btn-blue py-3 text-xs disabled:opacity-60">
          {mutation.isPending ? t('admin_settling') : t('admin_resolve_post')}
        </button>
      </form>
    </div>
  );
}
type ReturnAction =
  | { kind: 'REVIEW'; ret: ReturnResponse }
  | { kind: 'SHIPMENT'; ret: ReturnResponse }
  | { kind: 'INSPECTION'; ret: ReturnResponse };

function ReturnsTable() {
  const t = useT();
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();
  const [action, setAction] = useState<ReturnAction | null>(null);

  const { data, isPending } = useQuery({
    queryKey: queryKeys.returns.admin(page, 20),
    queryFn: () => getAdminReturns(undefined, page, 20),
  });

  const direct = useMutation({
    mutationFn: ({ id, fn }: { id: string; fn: (id: string) => Promise<unknown> }) => fn(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['returns', 'admin'] }),
  });

  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  return (
    <section className="bg-white border border-zinc-200 rounded-3xl p-8 shadow-sm space-y-6">
      <div className="flex items-baseline justify-between border-b border-zinc-100 pb-4">
        <div>
          <h3 className="text-lg font-bold flex items-center gap-2"><Undo2 className="h-5 w-5 text-sky-600" /> {t('adm_returns_title')}</h3>
          <p className="text-xs text-zinc-500 mt-0.5 font-mono">{t('adm_returns_desc')}</p>
        </div>
        <span className="font-mono text-xs text-zinc-500">{data?.total ?? '…'} {t('common_total')}</span>
      </div>

      {isPending ? (
        <div className="h-40 rounded-2xl bg-zinc-100 animate-pulse" />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-zinc-200 text-[10px] text-zinc-500 uppercase tracking-wider font-mono font-semibold">
                <th className="pb-4">ID</th><th className="pb-4">Buyer</th><th className="pb-4">Fulfillment</th><th className="pb-4">{t('ret_reason')}</th>
                <th className="pb-4">{t('ret_refund')}</th><th className="pb-4">{t('common_status')}</th><th className="pb-4 text-right">{t('common_actions')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100">
              {(data?.items ?? []).map((r: ReturnResponse) => (
                <tr key={r.id} className="hover:bg-zinc-50 transition-colors">
                  <td className="py-4 font-mono text-zinc-500">
                    {r.id.slice(0, 8)}
                    <div className="text-[10px] text-zinc-400 mt-0.5">{new Date(r.createdAt).toLocaleDateString('id-ID')}</div>
                  </td>
                  <td className="py-4 font-mono">#{r.buyerId}</td>
                  <td className="py-4 font-mono text-zinc-500">{r.fulfillmentOrderId.slice(0, 8)}</td>
                  <td className="py-4 max-w-[240px]">
                    <span className="font-bold">{r.reason}</span>
                    <div className="text-[10px] text-zinc-400 line-clamp-1">{r.description}</div>
                  </td>
                  <td className="py-4 font-mono tnum">{r.refundAmount > 0 ? `Rp ${r.refundAmount.toLocaleString('id-ID')}` : '—'}</td>
                  <td className="py-4">
                    <span className={`font-mono text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${
                      r.status === 'REFUNDED' || r.status === 'CLOSED'
                        ? 'text-emerald-700 bg-emerald-50 border-emerald-200'
                        : r.status === 'REJECTED'
                          ? 'text-red-700 bg-red-50 border-red-200'
                          : 'text-amber-700 bg-amber-50 border-amber-200'
                    }`}>{r.status}</span>
                  </td>
                  <td className="py-4 text-right">
                    <div className="flex items-center justify-end gap-2 flex-wrap">
                      {r.status === 'REQUESTED' && (
                        <button onClick={() => setAction({ kind: 'REVIEW', ret: r })} className="px-3 py-1.5 rounded-full bg-zinc-900 text-white text-[10px] font-bold hover:bg-zinc-700 transition-colors">
                          {t('adm_review')}
                        </button>
                      )}
                      {r.status === 'APPROVED' && (
                        <button onClick={() => setAction({ kind: 'SHIPMENT', ret: r })} className="px-3 py-1.5 rounded-full bg-zinc-900 text-white text-[10px] font-bold hover:bg-zinc-700 transition-colors">
                          {t('adm_shipment')}
                        </button>
                      )}
                      {r.status === 'IN_TRANSIT' && (
                        <button onClick={() => direct.mutate({ id: r.id, fn: markReturnReceived })} disabled={direct.isPending} className="px-3 py-1.5 rounded-full bg-zinc-900 text-white text-[10px] font-bold hover:bg-zinc-700 transition-colors disabled:opacity-60">
                          {direct.isPending ? t('admin_working') : t('adm_received')}
                        </button>
                      )}
                      {r.status === 'RECEIVED' && (
                        <button onClick={() => setAction({ kind: 'INSPECTION', ret: r })} className="px-3 py-1.5 rounded-full bg-zinc-900 text-white text-[10px] font-bold hover:bg-zinc-700 transition-colors">
                          {t('adm_inspect')}
                        </button>
                      )}
                      {r.status === 'INSPECTED' && (
                        <button onClick={() => direct.mutate({ id: r.id, fn: finalizeReturnRefund })} disabled={direct.isPending} className="px-3 py-1.5 rounded-full bg-emerald-600 text-white text-[10px] font-bold hover:bg-emerald-700 transition-colors disabled:opacity-60">
                          {direct.isPending ? t('admin_working') : t('adm_finalize_refund')}
                        </button>
                      )}
                      {(r.status === 'REFUNDED' || r.status === 'REJECTED') && (
                        <button onClick={() => direct.mutate({ id: r.id, fn: closeReturn })} disabled={direct.isPending} className="px-3 py-1.5 rounded-full border border-zinc-200 bg-white text-zinc-600 text-[10px] font-bold hover:bg-zinc-50 transition-colors disabled:opacity-40">
                          {direct.isPending ? t('admin_working') : t('adm_close_return')}
                        </button>
                      )}
                      {r.status === 'CLOSED' && <span className="text-[10px] text-zinc-400 font-mono">—</span>}
                    </div>
                  </td>
                </tr>
              ))}
              {data && data.items.length === 0 && (
                <tr><td colSpan={7} className="py-10 text-center text-zinc-500">{t('adm_returns_empty')}</td></tr>
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

      {action && <ReturnActionModal action={action} onClose={() => setAction(null)} />}
    </section>
  );
}

function ReturnActionModal({ action, onClose }: { action: ReturnAction; onClose: () => void }) {
  const t = useT();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const [courierName, setCourierName] = useState('');
  const [trackingNumber, setTrackingNumber] = useState('');
  const [notes, setNotes] = useState('');
  const [refundAmount, setRefundAmount] = useState('');
  const [error, setError] = useState('');

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['returns', 'admin'] });

  const reviewMutation = useMutation({
    mutationFn: (approved: boolean) => reviewReturn(action.ret.id, { approved, reason: reason.trim() || undefined }),
    onSuccess: () => { invalidate(); onClose(); },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  const titles: Record<ReturnAction['kind'], string> = {
    REVIEW: t('adm_review'),
    SHIPMENT: t('adm_shipment'),
    INSPECTION: t('adm_inspect'),
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-900/40 backdrop-blur-sm p-4" onClick={onClose}>
      <div className="w-full max-w-md rounded-3xl border border-zinc-200 bg-white p-8 space-y-5 shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <h4 className="text-sm font-bold">{titles[action.kind]} {action.ret.id.slice(0, 8)}</h4>
          <button type="button" onClick={onClose} className="p-1.5 rounded-lg hover:bg-zinc-100"><X className="h-4 w-4" /></button>
        </div>
        <p className="text-xs text-zinc-500 font-mono line-clamp-2">{action.ret.reason}: {action.ret.description}</p>
        {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}

        {action.kind === 'REVIEW' && (
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-semibold mb-1.5">{t('adm_reject_reason')}</label>
              <textarea value={reason} onChange={(e) => setReason(e.target.value)} rows={2} className={inputCls} />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <button disabled={reviewMutation.isPending} onClick={() => reviewMutation.mutate(true)} className="py-3 rounded-full bg-zinc-900 text-white text-xs font-bold hover:bg-zinc-700 transition-colors disabled:opacity-60">
                {t('adm_approve')}
              </button>
              <button disabled={reviewMutation.isPending} onClick={() => { if (!reason.trim()) { setError(t('adm_reject_reason')); return; } reviewMutation.mutate(false); }} className="py-3 rounded-full bg-red-600 text-white text-xs font-bold hover:bg-red-700 transition-colors disabled:opacity-60">
                {t('adm_reject')}
              </button>
            </div>
          </div>
        )}

        {action.kind === 'SHIPMENT' && (
          <form onSubmit={(e) => {
            e.preventDefault();
            setError('');
            recordReturnShipment(action.ret.id, { courierName: courierName.trim(), trackingNumber: trackingNumber.trim() }).then(invalidate).then(onClose).catch((err) => setError(apiErrorMessage(err)));
          }} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold mb-1.5">{t('adm_courier')}</label>
              <input required value={courierName} onChange={(e) => setCourierName(e.target.value)} className={inputCls} />
            </div>
            <div>
              <label className="block text-xs font-semibold mb-1.5">{t('adm_tracking')}</label>
              <input required value={trackingNumber} onChange={(e) => setTrackingNumber(e.target.value)} className={inputCls} />
            </div>
            <button type="submit" className="w-full btn-blue py-3 text-xs">{t('adm_shipment')}</button>
          </form>
        )}

        {action.kind === 'INSPECTION' && (
          <form onSubmit={(e) => {
            e.preventDefault();
            setError('');
            inspectReturn(action.ret.id, { notes: notes.trim() || undefined, refundAmount: parseInt(refundAmount, 10) }).then(invalidate).then(onClose).catch((err) => setError(apiErrorMessage(err)));
          }} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold mb-1.5">{t('adm_inspect_notes')}</label>
              <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} className={inputCls} />
            </div>
            <div>
              <label className="block text-xs font-semibold mb-1.5">{t('adm_refund_amount')}</label>
              <input required type="number" min="0" value={refundAmount} onChange={(e) => setRefundAmount(e.target.value)} className={inputCls} placeholder="0" />
            </div>
            <button type="submit" className="w-full btn-blue py-3 text-xs">{t('adm_inspect')}</button>
          </form>
        )}
      </div>
    </div>
  );
}

function EscrowPaymentsSection() {
  const t = useT();

  const { data: stats, isPending: statsPending } = useQuery({
    queryKey: queryKeys.escrow.stats(),
    queryFn: getEscrowStats,
  });

  const { data: payments, isPending: payPending } = useQuery({
    queryKey: queryKeys.payments.admin(0, 5),
    queryFn: () => getAdminPayments(undefined, 0, 5),
  });

  return (
    <div className="grid md:grid-cols-2 gap-6">
      <section className="bg-white border border-zinc-200 rounded-3xl p-8 shadow-sm space-y-6">
        <div className="border-b border-zinc-100 pb-4">
          <h3 className="text-lg font-bold flex items-center gap-2"><Landmark className="h-5 w-5 text-sky-600" /> {t('adm_escrow_stats')}</h3>
        </div>
        {statsPending ? (
          <div className="h-24 rounded-2xl bg-zinc-100 animate-pulse" />
        ) : (
          <>
            <div>
              <div className="text-[10px] uppercase font-mono text-zinc-500 tracking-widest">{t('adm_total_held')}</div>
              <div className="text-4xl font-bold font-mono tnum mt-2">Rp {(stats?.totalHeldAmount ?? 0).toLocaleString('id-ID')}</div>
            </div>
            <div className="flex flex-wrap gap-2">
              {Object.entries(stats?.escrowStatusCounts ?? {}).map(([status, count]) => (
                <span key={status} className="font-mono text-[10px] font-bold px-2.5 py-0.5 rounded-full border text-zinc-700 bg-zinc-50 border-zinc-200">
                  {status} · {count}
                </span>
              ))}
            </div>
          </>
        )}
      </section>

      <section className="bg-white border border-zinc-200 rounded-3xl p-8 shadow-sm space-y-6">
        <div className="flex items-baseline justify-between border-b border-zinc-100 pb-4">
          <div>
            <h3 className="text-lg font-bold flex items-center gap-2"><CreditCard className="h-5 w-5 text-sky-600" /> {t('adm_payments')}</h3>
          </div>
          <span className="font-mono text-xs text-zinc-500">{payments?.total ?? '…'} {t('common_total')}</span>
        </div>
        {payPending ? (
          <div className="h-40 rounded-2xl bg-zinc-100 animate-pulse" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-zinc-200 text-[10px] text-zinc-500 uppercase tracking-wider font-mono font-semibold">
                  <th className="pb-4">Attempt</th><th className="pb-4">Buyer</th><th className="pb-4">{t('orders_total')}</th><th className="pb-4">{t('common_status')}</th><th className="pb-4">Ref</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100">
                {(payments?.items ?? []).map((p: PaymentAttemptDto) => (
                  <tr key={p.id} className="hover:bg-zinc-50 transition-colors">
                    <td className="py-3 font-mono text-zinc-500">{p.id.slice(0, 8)}</td>
                    <td className="py-3 font-mono">#{p.buyerId}</td>
                    <td className="py-3 font-mono tnum">Rp {p.amount.toLocaleString('id-ID')}</td>
                    <td className="py-3">
                      <span className={`font-mono text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${
                        p.status === 'SUCCEEDED' ? 'text-emerald-700 bg-emerald-50 border-emerald-200'
                        : p.status === 'FAILED' ? 'text-red-700 bg-red-50 border-red-200'
                        : 'text-amber-700 bg-amber-50 border-amber-200'
                      }`}>{p.status}</span>
                    </td>
                    <td className="py-3 font-mono text-zinc-500 truncate max-w-[120px]">{p.gatewayReference ?? '—'}</td>
                  </tr>
                ))}
                {payments && payments.items.length === 0 && (
                  <tr><td colSpan={5} className="py-8 text-center text-zinc-500">{t('adm_payments_empty')}</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function UnlockForm() {
  const t = useT();
  const [userId, setUserId] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const mutation = useMutation({
    mutationFn: (id: number) => unlockUser(id),
    onSuccess: () => {
      setError('');
      setMessage(t('admin_user_unlocked'));
      setUserId('');
    },
    onError: (err) => { setMessage(''); setError(apiErrorMessage(err)); },
  });

  const inputCls = 'rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  return (
    <section className="bg-white border border-zinc-200 rounded-3xl p-8 shadow-sm">
      <h3 className="text-lg font-bold flex items-center gap-2 mb-1"><Unlock className="h-5 w-5 text-sky-600" /> {t('admin_unlock')}</h3>
      <p className="text-xs text-zinc-500 mb-5 font-mono">{t('admin_unlock_desc')}</p>
      <form
        onSubmit={(e) => { e.preventDefault(); setError(''); setMessage(''); mutation.mutate(parseInt(userId, 10)); }}
        className="flex flex-col sm:flex-row gap-3"
      >
        <input
          required type="number" min="1" value={userId} onChange={(e) => setUserId(e.target.value)}
          className={`${inputCls} sm:w-48`} placeholder={t('admin_user_id')}
        />
        <button type="submit" disabled={mutation.isPending || !userId} className="btn-primary-dark px-6 py-3 text-xs disabled:opacity-60">
          {mutation.isPending ? t('admin_unlocking') : t('admin_unlock_btn')}
        </button>
      </form>
      {message && <p className="mt-3 text-xs text-emerald-600 font-semibold">{message}</p>}
      {error && <p className="mt-3 text-xs text-red-600 font-semibold">{error}</p>}
    </section>
  );
}
