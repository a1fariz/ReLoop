'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ClipboardCheck, Microscope, Search } from 'lucide-react';
import { useAuthStore } from '@/lib/auth';
import { apiErrorMessage, createInspection, getLatestInspection } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import type { InspectionResponse } from '@/types/api';

export default function InspectionsPage() {
  const { accessToken, role } = useAuthStore();
  const t = useT();

  if (!accessToken || (role !== 'TECHNICIAN' && role !== 'ADMIN')) {
    return (
      <div className="min-h-screen bg-[#fafafa] py-24 text-center">
        <Microscope className="h-12 w-12 text-zinc-300 mx-auto mb-4" />
        <h1 className="text-2xl font-bold mb-2">{t('insp_access_required')}</h1>
        <p className="text-sm text-zinc-500">{t('insp_access_desc')}</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 py-16 ambient-light-mesh">
      <div className="container mx-auto px-6 max-w-5xl space-y-12">
        <div className="pb-6 border-b border-zinc-200">
          <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold mb-2">
            {t('insp_lab')}
          </div>
          <h1 className="text-4xl sm:text-5xl font-semibold tracking-tight">{t('insp_title')}</h1>
        </div>

        <InspectionForm />
        <GradeLookup />
      </div>
    </div>
  );
}

function InspectionForm() {
  const t = useT();
  const queryClient = useQueryClient();
  const [unitId, setUnitId] = useState('');
  const [physicalScore, setPhysicalScore] = useState('80');
  const [hardwareScore, setHardwareScore] = useState('80');
  const [softwareScore, setSoftwareScore] = useState('80');
  const [hasCriticalFailure, setHasCriticalFailure] = useState(false);
  const [estimatedRepairCost, setEstimatedRepairCost] = useState('');
  const [technicianNotes, setTechnicianNotes] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: createInspection,
    onSuccess: (result: InspectionResponse) => {
      setError('');
      queryClient.invalidateQueries({ queryKey: ['inspections', 'unit'] });
      setSuccess(`${t('insp_recorded')}: ${result.finalCalculatedGrade}`);
    },
    onError: (err) => { setSuccess(null); setError(apiErrorMessage(err)); },
  });

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(''); setSuccess(null);
    mutation.mutate({
      unitId: unitId.trim(),
      physicalScore: parseInt(physicalScore, 10),
      hardwareScore: parseInt(hardwareScore, 10),
      softwareScore: parseInt(softwareScore, 10),
      hasCriticalFailure,
      estimatedRepairCost: estimatedRepairCost ? parseInt(estimatedRepairCost, 10) : undefined,
      technicianNotes: technicianNotes.trim() || undefined,
    });
  }

  return (
    <form onSubmit={submit} className="bg-white border border-zinc-200 rounded-3xl p-8 shadow-sm space-y-6">
      <div>
        <h3 className="text-lg font-bold flex items-center gap-2"><ClipboardCheck className="h-5 w-5 text-sky-600" /> {t('insp_record')}</h3>
        <p className="text-xs text-zinc-500 mt-0.5 font-mono">{t('insp_scores_desc')}</p>
      </div>

      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}
      {success && <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs text-emerald-700">{success}</div>}

      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('seller_unit_id')}</label>
        <input required value={unitId} onChange={(e) => setUnitId(e.target.value)} className={inputCls} placeholder={t('insp_unit_serial')} />
      </div>

      <div className="grid sm:grid-cols-3 gap-4">
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('insp_physical')}</label>
          <input required type="number" min="0" max="100" value={physicalScore} onChange={(e) => setPhysicalScore(e.target.value)} className={inputCls} />
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('insp_hardware')}</label>
          <input required type="number" min="0" max="100" value={hardwareScore} onChange={(e) => setHardwareScore(e.target.value)} className={inputCls} />
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('insp_software')}</label>
          <input required type="number" min="0" max="100" value={softwareScore} onChange={(e) => setSoftwareScore(e.target.value)} className={inputCls} />
        </div>
      </div>

      <div className="grid sm:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('insp_repair_cost')}</label>
          <input type="number" min="0" value={estimatedRepairCost} onChange={(e) => setEstimatedRepairCost(e.target.value)} className={inputCls} placeholder="0" />
        </div>
        <div className="flex items-end">
          <label className="flex items-center gap-3 text-xs font-semibold pb-3 cursor-pointer">
            <input type="checkbox" checked={hasCriticalFailure} onChange={(e) => setHasCriticalFailure(e.target.checked)} className="h-4 w-4 accent-sky-600" />
            {t('insp_critical')}
          </label>
        </div>
      </div>

      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('insp_tech_notes')}</label>
        <textarea value={technicianNotes} onChange={(e) => setTechnicianNotes(e.target.value)} rows={3} className={inputCls} placeholder="Observed defects, replaced parts…" />
      </div>

      <button type="submit" disabled={mutation.isPending} className="btn-blue px-6 py-3 text-xs disabled:opacity-60">
        {mutation.isPending ? t('insp_recording') : t('insp_submit')}
      </button>
    </form>
  );
}

function GradeLookup() {
  const t = useT();
  const [unitId, setUnitId] = useState('');
  const [queryId, setQueryId] = useState<string | null>(null);

  const { data, isPending, isError, error } = useQuery({
    queryKey: queryKeys.inspections.unit(queryId ?? ''),
    queryFn: () => getLatestInspection(queryId as string),
    enabled: queryId !== null,
    retry: 1,
  });

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  return (
    <section className="bg-white border border-zinc-200 rounded-3xl p-8 shadow-sm space-y-6">
      <div>
        <h3 className="text-lg font-bold flex items-center gap-2"><Search className="h-5 w-5 text-sky-600" /> {t('insp_lookup')}</h3>
        <p className="text-xs text-zinc-500 mt-0.5 font-mono">{t('insp_lookup_desc')}</p>
      </div>

      <form
        onSubmit={(e) => { e.preventDefault(); setQueryId(unitId.trim() || null); }}
        className="flex flex-col sm:flex-row gap-3"
      >
        <input required value={unitId} onChange={(e) => setUnitId(e.target.value)} className={inputCls} placeholder={t('insp_unit_uuid')} />
        <button type="submit" className="btn-primary-dark px-6 py-3 text-xs whitespace-nowrap">{t('insp_look_up')}</button>
      </form>

      {isPending && queryId && <div className="h-28 rounded-2xl bg-zinc-100 animate-pulse" />}

      {isError && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700">
          {t('insp_not_found')}: {(error as Error).message}
        </div>
      )}

      {data && (
        <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-6 grid sm:grid-cols-3 gap-6 text-center">
          <div>
            <div className="text-[10px] uppercase font-mono text-zinc-500 tracking-widest">{t('insp_final_grade')}</div>
            <div className="text-4xl font-bold font-mono text-zinc-900 mt-1">{data.finalCalculatedGrade}</div>
          </div>
          <div>
            <div className="text-[10px] uppercase font-mono text-zinc-500 tracking-widest">{t('insp_scores')}</div>
            <div className="text-xl font-bold font-mono text-zinc-900 mt-2">{data.physicalScore} / {data.hardwareScore} / {data.softwareScore}</div>
          </div>
          <div>
            <div className="text-[10px] uppercase font-mono text-zinc-500 tracking-widest">{t('trade_repair_cost').replace(' (Rp)', '')}</div>
            <div className="text-xl font-bold font-mono text-zinc-900 mt-2">Rp {data.estimatedRepairCost.toLocaleString('id-ID')}</div>
          </div>
          {data.technicianNotes && (
            <div className="sm:col-span-3 text-left text-xs text-zinc-600 border-t border-zinc-200 pt-4">{data.technicianNotes}</div>
          )}
        </div>
      )}
    </section>
  );
}
