'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Wrench, X } from 'lucide-react';
import { useAuthStore } from '@/lib/auth';
import {
  apiErrorMessage, createRepairTicket, getMyRepairTickets,
  repairCancel, repairComplete, repairStartDiagnosis, repairStartRepair, repairSubmitQc,
} from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import type { CompleteRepairRequest, ComponentReplacementDto, RepairTicketDto } from '@/types/api';

const STATUS_STYLES: Record<string, string> = {
  OPEN: 'text-zinc-700 bg-zinc-50 border-zinc-200',
  DIAGNOSING: 'text-sky-700 bg-sky-50 border-sky-200',
  IN_PROGRESS: 'text-indigo-700 bg-indigo-50 border-indigo-200',
  QC_PENDING: 'text-amber-700 bg-amber-50 border-amber-200',
  COMPLETED: 'text-emerald-700 bg-emerald-50 border-emerald-200',
  CANCELLED: 'text-zinc-500 bg-zinc-50 border-zinc-200',
};

export default function RepairsPage() {
  const { accessToken, role } = useAuthStore();
  const t = useT();

  if (!accessToken || (role !== 'TECHNICIAN' && role !== 'ADMIN')) {
    return (
      <div className="min-h-screen bg-[#fafafa] py-24 text-center">
        <Wrench className="h-12 w-12 text-zinc-300 mx-auto mb-4" />
        <h1 className="text-2xl font-bold mb-2">{t('insp_access_required')}</h1>
        <p className="text-sm text-zinc-500">{t('insp_access_desc')}</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 py-16 ambient-light-mesh">
      <div className="container mx-auto px-6 max-w-6xl space-y-12">
        <div className="pb-6 border-b border-zinc-200">
          <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold mb-2">
            {t('insp_lab')}
          </div>
          <h1 className="text-4xl sm:text-5xl font-semibold tracking-tight">{t('rep_title')}</h1>
          <p className="text-sm text-zinc-600 mt-3 max-w-2xl leading-relaxed">{t('rep_desc')}</p>
        </div>

        <NewTicketForm />
        <TicketsTable />
      </div>
    </div>
  );
}

function NewTicketForm() {
  const t = useT();
  const queryClient = useQueryClient();
  const [unitId, setUnitId] = useState('');
  const [issueDescription, setIssueDescription] = useState('');
  const [initialPartsCost, setInitialPartsCost] = useState('');
  const [error, setError] = useState('');

  const mutation = useMutation({
    mutationFn: createRepairTicket,
    onSuccess: () => {
      setError('');
      queryClient.invalidateQueries({ queryKey: ['repairs'] });
      setUnitId('');
      setIssueDescription('');
      setInitialPartsCost('');
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
          unitId: unitId.trim(),
          issueDescription: issueDescription.trim(),
          initialPartsCost: initialPartsCost ? parseInt(initialPartsCost, 10) : undefined,
        });
      }}
      className="bg-white border border-zinc-200 rounded-3xl p-8 shadow-sm space-y-6"
    >
      <div>
        <h3 className="text-lg font-bold flex items-center gap-2"><Wrench className="h-5 w-5 text-sky-600" /> {t('rep_new_ticket')}</h3>
      </div>

      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}

      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('rep_unit_id')}</label>
        <input required value={unitId} onChange={(e) => setUnitId(e.target.value)} className={inputCls} placeholder="00000000-0000-…" />
      </div>
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('rep_issue')}</label>
        <textarea required value={issueDescription} onChange={(e) => setIssueDescription(e.target.value)} rows={3} className={inputCls} />
      </div>
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('rep_parts_cost')}</label>
        <input type="number" min="0" value={initialPartsCost} onChange={(e) => setInitialPartsCost(e.target.value)} className={inputCls} placeholder="0" />
      </div>

      <button type="submit" disabled={mutation.isPending} className="btn-blue px-6 py-3 text-xs disabled:opacity-60">
        {mutation.isPending ? t('common_loading') : t('rep_new_ticket')}
      </button>
    </form>
  );
}

function TicketsTable() {
  const t = useT();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [actionError, setActionError] = useState('');
  const [modal, setModal] = useState<
    | { kind: 'startRepair'; ticket: RepairTicketDto }
    | { kind: 'submitQc'; ticket: RepairTicketDto }
    | { kind: 'complete'; ticket: RepairTicketDto }
    | { kind: 'cancel'; ticket: RepairTicketDto }
    | null
  >(null);

  const { data, isPending } = useQuery({
    queryKey: queryKeys.repairs.mine(page, 20),
    queryFn: () => getMyRepairTickets(page, 20),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['repairs'] });

  const action = useMutation({
    mutationFn: ({ id, fn }: { id: string; fn: (id: string) => Promise<unknown> }) => fn(id),
    onSuccess: () => { setActionError(''); invalidate(); },
    onError: (err) => setActionError(apiErrorMessage(err)),
  });

  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  return (
    <section className="bg-white border border-zinc-200 rounded-3xl p-8 shadow-sm space-y-6">
      <div className="flex items-baseline justify-between border-b border-zinc-100 pb-4">
        <div>
          <h3 className="text-lg font-bold flex items-center gap-2"><Wrench className="h-5 w-5 text-sky-600" /> {t('rep_title')}</h3>
        </div>
        <span className="font-mono text-xs text-zinc-500">{data?.total ?? '…'} {t('common_total')}</span>
      </div>

      {actionError && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{actionError}</div>}

      {isPending ? (
        <div className="h-40 rounded-2xl bg-zinc-100 animate-pulse" />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-zinc-200 text-[10px] text-zinc-500 uppercase tracking-wider font-mono font-semibold">
                <th className="pb-4">ID</th><th className="pb-4">{t('detail_unit')}</th><th className="pb-4">{t('rep_issue')}</th>
                <th className="pb-4">{t('rep_parts_cost')}</th><th className="pb-4">{t('common_status')}</th><th className="pb-4 text-right">{t('common_actions')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100">
              {(data?.items ?? []).map((r: RepairTicketDto) => {
                return (
                  <tr key={r.id} className="hover:bg-zinc-50 transition-colors">
                    <td className="py-4 font-mono text-zinc-500">
                      {r.id.slice(0, 8)}
                      <div className="text-[10px] text-zinc-400 mt-0.5">{new Date(r.createdAt).toLocaleDateString('id-ID')}</div>
                    </td>
                    <td className="py-4 font-mono text-zinc-500">{r.unitId.slice(0, 8)}</td>
                    <td className="py-4 max-w-[280px]">
                      <span className="line-clamp-1">{r.issueDescription}</span>
                    </td>
                    <td className="py-4 font-mono tnum">{r.partsCost > 0 ? `Rp ${r.partsCost.toLocaleString('id-ID')}` : '—'}</td>
                    <td className="py-4">
                      <span className={`font-mono text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${STATUS_STYLES[r.status] ?? 'text-zinc-700 bg-zinc-50 border-zinc-200'}`}>
                        {r.status}
                      </span>
                    </td>
                    <td className="py-4 text-right">
                      <div className="flex items-center justify-end gap-2 flex-wrap">
                        {action.isPending ? (
                          <span className="text-[10px] font-mono text-zinc-400">{t('admin_working')}</span>
                        ) : (
                          <>
                            {r.status === 'OPEN' && (
                              <button onClick={() => action.mutate({ id: r.id, fn: repairStartDiagnosis })} className="px-3 py-1.5 rounded-full bg-zinc-900 text-white text-[10px] font-bold hover:bg-zinc-700 transition-colors">
                                {t('rep_start_diagnosis')}
                              </button>
                            )}
                            {r.status === 'DIAGNOSING' && (
                              <button onClick={() => setModal({ kind: 'startRepair', ticket: r })} className="px-3 py-1.5 rounded-full bg-zinc-900 text-white text-[10px] font-bold hover:bg-zinc-700 transition-colors">
                                {t('rep_start_repair')}
                              </button>
                            )}
                            {r.status === 'IN_PROGRESS' && (
                              <button onClick={() => setModal({ kind: 'submitQc', ticket: r })} className="px-3 py-1.5 rounded-full bg-zinc-900 text-white text-[10px] font-bold hover:bg-zinc-700 transition-colors">
                                {t('rep_submit_qc')}
                              </button>
                            )}
                            {r.status === 'QC_PENDING' && (
                              <button onClick={() => setModal({ kind: 'complete', ticket: r })} className="px-3 py-1.5 rounded-full bg-zinc-900 text-white text-[10px] font-bold hover:bg-zinc-700 transition-colors">
                                {t('rep_complete')}
                              </button>
                            )}
                            {(r.status === 'OPEN' || r.status === 'DIAGNOSING' || r.status === 'IN_PROGRESS') && (
                              <button onClick={() => setModal({ kind: 'cancel', ticket: r })} className="px-3 py-1.5 rounded-full border border-red-200 text-red-700 bg-white text-[10px] font-bold hover:bg-red-50 transition-colors">
                                {t('rep_cancel_ticket')}
                              </button>
                            )}
                            {(r.status === 'COMPLETED' || r.status === 'CANCELLED') && (
                              <span className="text-[10px] text-zinc-400 font-mono">—</span>
                            )}
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
              {data && data.items.length === 0 && (
                <tr><td colSpan={6} className="py-10 text-center text-zinc-500">{t('rep_empty')}</td></tr>
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

      {modal?.kind === 'startRepair' && <StartRepairModal ticket={modal.ticket} onClose={() => setModal(null)} />}
      {modal?.kind === 'submitQc' && <SubmitQcModal ticket={modal.ticket} onClose={() => setModal(null)} />}
      {modal?.kind === 'complete' && <CompleteModal ticket={modal.ticket} onClose={() => setModal(null)} />}
      {modal?.kind === 'cancel' && <CancelModal ticket={modal.ticket} onClose={() => setModal(null)} />}
    </section>
  );
}

function modalShell(onClose: () => void, children: React.ReactNode) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-900/40 backdrop-blur-sm p-4" onClick={onClose}>
      <div className="w-full max-w-md rounded-3xl border border-zinc-200 bg-white p-8 shadow-2xl" onClick={(e) => e.stopPropagation()}>
        {children}
      </div>
    </div>
  );
}

function modalHead(title: string, onClose: () => void) {
  return (
    <div className="flex items-center justify-between">
      <h4 className="text-sm font-bold">{title}</h4>
      <button type="button" onClick={onClose} className="p-1.5 rounded-lg hover:bg-zinc-100"><X className="h-4 w-4" /></button>
    </div>
  );
}

function StartRepairModal({ ticket, onClose }: { ticket: RepairTicketDto; onClose: () => void }) {
  const t = useT();
  const queryClient = useQueryClient();
  const [estimatedPartsCost, setEstimatedPartsCost] = useState('');
  const [error, setError] = useState('');

  const mutation = useMutation({
    mutationFn: () => repairStartRepair(ticket.id, estimatedPartsCost ? parseInt(estimatedPartsCost, 10) : undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repairs'] });
      onClose();
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  return modalShell(onClose, (
    <form onSubmit={(e) => { e.preventDefault(); setError(''); mutation.mutate(); }} className="space-y-5">
      {modalHead(`${t('rep_start_repair')} ${ticket.id.slice(0, 8)}`, onClose)}
      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('rep_est_parts_cost')}</label>
        <input type="number" min="0" value={estimatedPartsCost} onChange={(e) => setEstimatedPartsCost(e.target.value)} className={inputCls} placeholder="0" />
      </div>
      <button type="submit" disabled={mutation.isPending} className="w-full btn-blue py-3 text-xs disabled:opacity-60">
        {mutation.isPending ? t('common_loading') : t('rep_start_repair')}
      </button>
    </form>
  ));
}

function SubmitQcModal({ ticket, onClose }: { ticket: RepairTicketDto; onClose: () => void }) {
  const t = useT();
  const queryClient = useQueryClient();
  const [components, setComponents] = useState<ComponentReplacementDto[]>([{ componentName: '' }]);
  const [totalPartsCost, setTotalPartsCost] = useState('');
  const [error, setError] = useState('');

  const mutation = useMutation({
    mutationFn: (input: { components: ComponentReplacementDto[]; totalPartsCost?: number }) => repairSubmitQc(ticket.id, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repairs'] });
      onClose();
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  function updateComponent(i: number, field: keyof ComponentReplacementDto, value: string) {
    setComponents((prev) => prev.map((c, idx) => idx === i ? { ...c, [field]: field === 'cost' ? (value ? parseInt(value, 10) : undefined) : value } : c));
  }

  function submit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    mutation.mutate({
      components: components
        .filter((c) => c.componentName.trim())
        .map((c) => ({
          componentName: c.componentName.trim(),
          serialNumber: c.serialNumber?.trim() || undefined,
          cost: c.cost,
        })),
      totalPartsCost: totalPartsCost ? parseInt(totalPartsCost, 10) : undefined,
    });
  }

  return modalShell(onClose, (
    <form onSubmit={submit} className="space-y-5">
      {modalHead(`${t('rep_submit_qc')} ${ticket.id.slice(0, 8)}`, onClose)}
      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}
      <div className="space-y-3">
        <label className="block text-xs font-semibold">{t('rep_components')}</label>
        {components.map((c, i) => (
          <div key={i} className="space-y-2 rounded-2xl border border-zinc-200 p-4">
            <div>
              <label className="block text-[10px] font-semibold mb-1 text-zinc-500">{t('rep_component_name')}</label>
              <input value={c.componentName} onChange={(e) => updateComponent(i, 'componentName', e.target.value)} className={inputCls} />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[10px] font-semibold mb-1 text-zinc-500">{t('rep_serial')}</label>
                <input value={c.serialNumber ?? ''} onChange={(e) => updateComponent(i, 'serialNumber', e.target.value)} className={inputCls} />
              </div>
              <div>
                <label className="block text-[10px] font-semibold mb-1 text-zinc-500">{t('rep_cost')}</label>
                <input type="number" min="0" value={c.cost ?? ''} onChange={(e) => updateComponent(i, 'cost', e.target.value)} className={inputCls} placeholder="0" />
              </div>
            </div>
            {components.length > 1 && (
              <button type="button" onClick={() => setComponents((prev) => prev.filter((_, idx) => idx !== i))} className="text-[10px] font-bold text-red-600 hover:text-red-700">
                {t('cart_remove')}
              </button>
            )}
          </div>
        ))}
        <button type="button" onClick={() => setComponents((prev) => [...prev, { componentName: '' }])} className="text-xs font-bold text-sky-600 hover:text-sky-700">
          {t('rep_add_component')}
        </button>
      </div>
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('rep_total_cost')}</label>
        <input type="number" min="0" value={totalPartsCost} onChange={(e) => setTotalPartsCost(e.target.value)} className={inputCls} placeholder="0" />
      </div>
      <button type="submit" disabled={mutation.isPending} className="w-full btn-blue py-3 text-xs disabled:opacity-60">
        {mutation.isPending ? t('common_loading') : t('rep_submit_qc')}
      </button>
    </form>
  ));
}

function CompleteModal({ ticket, onClose }: { ticket: RepairTicketDto; onClose: () => void }) {
  const t = useT();
  const queryClient = useQueryClient();
  const [regraded, setRegraded] = useState(false);
  const [newPhysicalScore, setNewPhysicalScore] = useState('');
  const [newHardwareScore, setNewHardwareScore] = useState('');
  const [newSoftwareScore, setNewSoftwareScore] = useState('');
  const [technicianNotes, setTechnicianNotes] = useState('');
  const [error, setError] = useState('');

  const mutation = useMutation({
    mutationFn: (input: CompleteRepairRequest) => repairComplete(ticket.id, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repairs'] });
      onClose();
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  function submit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    mutation.mutate({
      regraded,
      newPhysicalScore: regraded && newPhysicalScore ? parseInt(newPhysicalScore, 10) : undefined,
      newHardwareScore: regraded && newHardwareScore ? parseInt(newHardwareScore, 10) : undefined,
      newSoftwareScore: regraded && newSoftwareScore ? parseInt(newSoftwareScore, 10) : undefined,
      technicianNotes: technicianNotes.trim() || undefined,
    });
  }

  return modalShell(onClose, (
    <form onSubmit={submit} className="space-y-5">
      {modalHead(`${t('rep_complete')} ${ticket.id.slice(0, 8)}`, onClose)}
      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}
      <label className="flex items-center gap-3 text-xs font-semibold cursor-pointer">
        <input type="checkbox" checked={regraded} onChange={(e) => setRegraded(e.target.checked)} className="h-4 w-4 accent-sky-600" />
        {t('reg_regraded')}
      </label>
      {regraded && (
        <div className="space-y-3">
          <p className="text-[10px] text-zinc-500 font-mono">{t('rep_scores_note')}</p>
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-[10px] font-semibold mb-1 text-zinc-500">{t('rep_phys_score')}</label>
              <input required type="number" min="0" max="100" value={newPhysicalScore} onChange={(e) => setNewPhysicalScore(e.target.value)} className={inputCls} />
            </div>
            <div>
              <label className="block text-[10px] font-semibold mb-1 text-zinc-500">{t('rep_hw_score')}</label>
              <input required type="number" min="0" max="100" value={newHardwareScore} onChange={(e) => setNewHardwareScore(e.target.value)} className={inputCls} />
            </div>
            <div>
              <label className="block text-[10px] font-semibold mb-1 text-zinc-500">{t('rep_sw_score')}</label>
              <input required type="number" min="0" max="100" value={newSoftwareScore} onChange={(e) => setNewSoftwareScore(e.target.value)} className={inputCls} />
            </div>
          </div>
        </div>
      )}
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('rep_notes')}</label>
        <textarea value={technicianNotes} onChange={(e) => setTechnicianNotes(e.target.value)} rows={3} className={inputCls} />
      </div>
      <button type="submit" disabled={mutation.isPending} className="w-full btn-blue py-3 text-xs disabled:opacity-60">
        {mutation.isPending ? t('common_loading') : t('rep_complete')}
      </button>
    </form>
  ));
}

function CancelModal({ ticket, onClose }: { ticket: RepairTicketDto; onClose: () => void }) {
  const t = useT();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');

  const mutation = useMutation({
    mutationFn: () => repairCancel(ticket.id, reason.trim()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repairs'] });
      onClose();
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  return modalShell(onClose, (
    <form onSubmit={(e) => { e.preventDefault(); setError(''); mutation.mutate(); }} className="space-y-5">
      {modalHead(`${t('rep_cancel_ticket')} ${ticket.id.slice(0, 8)}`, onClose)}
      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('rep_cancel_reason')}</label>
        <textarea required value={reason} onChange={(e) => setReason(e.target.value)} rows={3} className={inputCls} />
      </div>
      <button type="submit" disabled={mutation.isPending || !reason.trim()} className="w-full py-3 text-xs rounded-full bg-red-600 text-white font-bold hover:bg-red-700 transition-colors disabled:opacity-60">
        {mutation.isPending ? t('common_loading') : t('rep_cancel_ticket')}
      </button>
    </form>
  ));
}
