'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Calculator, ArrowUpRight, History } from 'lucide-react';
import { calculateTradeIn, getMyTradeInRequests, getProductModels, submitTradeInRequest, apiErrorMessage } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import { useAuthStore } from '@/lib/auth';
import type { TradeInCalculationRequest, TradeInOfferResponse } from '@/types/api';

const CONDITION_OPTIONS: { value: TradeInCalculationRequest['condition']; label: string }[] = [
  { value: 'EXCELLENT', label: 'Excellent (0.95×)' },
  { value: 'GOOD', label: 'Good (0.85×)' },
  { value: 'FAIR', label: 'Fair (0.70×)' },
  { value: 'POOR', label: 'Poor (0.50×)' },
  { value: 'DAMAGED', label: 'Damaged (0.30×)' },
];

const FUNCTIONALITY_OPTIONS: { value: TradeInCalculationRequest['functionality']; label: string }[] = [
  { value: 'FULLY_FUNCTIONAL', label: 'Fully functional (1.00×)' },
  { value: 'MINOR_ISSUES', label: 'Minor issues (0.80×)' },
  { value: 'MAJOR_ISSUES', label: 'Major issues (0.50×)' },
  { value: 'NOT_WORKING', label: 'Not working (0.20×)' },
];

export default function TradeInPage() {
  const t = useT();
  const { accessToken } = useAuthStore();
  const queryClient = useQueryClient();

  const [msrp, setMsrp] = useState('20999000');
  const [annualDepreciationRate, setAnnualDepreciationRate] = useState('15');
  const [releaseDate, setReleaseDate] = useState('2023-09-22');
  const [condition, setCondition] = useState<TradeInCalculationRequest['condition']>('EXCELLENT');
  const [functionality, setFunctionality] = useState<TradeInCalculationRequest['functionality']>('FULLY_FUNCTIONAL');
  const [batteryHealth, setBatteryHealth] = useState('98');
  const [hasCompleteAccessories, setHasCompleteAccessories] = useState(true);
  const [estimatedRepairCost, setEstimatedRepairCost] = useState('');
  const [productModelId, setProductModelId] = useState('');
  const [error, setError] = useState('');
  const [offer, setOffer] = useState<TradeInOfferResponse | null>(null);

  const { data: models } = useQuery({
    queryKey: queryKeys.catalog.models(),
    queryFn: getProductModels,
  });

  const { data: myRequests } = useQuery({
    queryKey: queryKeys.tradein.requests(),
    queryFn: getMyTradeInRequests,
    enabled: accessToken !== null,
  });

  const calcMutation = useMutation({
    mutationFn: calculateTradeIn,
    onSuccess: (result) => { setError(''); setOffer(result); },
    onError: (err) => { setOffer(null); setError(apiErrorMessage(err)); },
  });

  const submitMutation = useMutation({
    mutationFn: submitTradeInRequest,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.tradein.requests() }),
    onError: (err) => setError(apiErrorMessage(err)),
  });

  function buildInput() {
    const msrpNum = parseInt(msrp, 10) || 0;
    const rateNum = (parseFloat(annualDepreciationRate) || 0) / 100;
    const repair = estimatedRepairCost ? parseInt(estimatedRepairCost, 10) : undefined;
    return {
      msrp: msrpNum,
      annualDepreciationRate: rateNum,
      releaseDate,
      condition,
      functionality,
      batteryHealthPercentage: parseInt(batteryHealth, 10) || 0,
      hasCompleteAccessories,
      estimatedRepairCost: repair,
    };
  }

  function handleCalculate(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    calcMutation.mutate(buildInput());
  }

  function handleSubmitRequest() {
    if (!offer || !productModelId) { setError(t('trade_need_calc')); return; }
    setError('');
    const input = buildInput();
    submitMutation.mutate({
      productModelId,
      msrp: input.msrp,
      annualDepreciationRate: input.annualDepreciationRate,
      releaseDate: input.releaseDate,
      declaredCondition: input.condition,
      declaredFunctionality: input.functionality,
      batteryHealthPercentage: input.batteryHealthPercentage,
      hasCompleteAccessories: input.hasCompleteAccessories,
      estimatedRepairCost: input.estimatedRepairCost,
    });
  }

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 py-16 ambient-light-mesh">
      <div className="container mx-auto px-6 max-w-6xl">
        {/* Header */}
        <div className="mb-12 pb-6 border-b border-zinc-200 flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold mb-2">{t('trade_engine')}</div>
            <h1 className="text-4xl sm:text-5xl font-semibold tracking-tight text-zinc-900 mt-1">{t('trade_title')}</h1>
            <p className="text-sm text-zinc-600 mt-3 max-w-xl leading-relaxed">{t('trade_desc')}</p>
          </div>
        </div>

        <div className="grid lg:grid-cols-2 gap-8">
          {/* Calculator form */}
          <form onSubmit={handleCalculate} className="bg-white border border-zinc-200 rounded-3xl p-8 space-y-5 shadow-sm">
            {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}

            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold mb-1.5">{t('trade_msrp')}</label>
                <input required type="number" min="0" value={msrp} onChange={(e) => setMsrp(e.target.value)} className={inputCls} />
              </div>
              <div>
                <label className="block text-xs font-semibold mb-1.5">{t('trade_dep_rate')}</label>
                <input required type="number" min="0" max="100" step="0.5" value={annualDepreciationRate} onChange={(e) => setAnnualDepreciationRate(e.target.value)} className={inputCls} />
              </div>
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold mb-1.5">{t('trade_release_date')}</label>
                <input required type="date" value={releaseDate} onChange={(e) => setReleaseDate(e.target.value)} className={inputCls} />
              </div>
              <div>
                <label className="block text-xs font-semibold mb-1.5">{t('trade_battery')}</label>
                <input required type="number" min="0" max="100" value={batteryHealth} onChange={(e) => setBatteryHealth(e.target.value)} className={inputCls} />
              </div>
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold mb-1.5">{t('trade_condition')}</label>
                <select value={condition} onChange={(e) => setCondition(e.target.value as TradeInCalculationRequest['condition'])} className={inputCls}>
                  {CONDITION_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold mb-1.5">{t('trade_functionality')}</label>
                <select value={functionality} onChange={(e) => setFunctionality(e.target.value as TradeInCalculationRequest['functionality'])} className={inputCls}>
                  {FUNCTIONALITY_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select>
              </div>
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold mb-1.5">{t('trade_repair_cost')}</label>
                <input type="number" min="0" value={estimatedRepairCost} onChange={(e) => setEstimatedRepairCost(e.target.value)} className={inputCls} placeholder="0" />
              </div>
              <label className="flex items-center gap-3 text-xs font-semibold self-end pb-3 cursor-pointer">
                <input type="checkbox" checked={hasCompleteAccessories} onChange={(e) => setHasCompleteAccessories(e.target.checked)} className="h-4 w-4 accent-sky-600" />
                {t('trade_accessories')}
              </label>
            </div>

            <button type="submit" disabled={calcMutation.isPending} className="btn-blue px-6 py-3 text-xs inline-flex items-center gap-2 disabled:opacity-60">
              <Calculator className="h-3.5 w-3.5" />
              {calcMutation.isPending ? t('trade_calculating') : t('trade_calculate')} <ArrowUpRight className="h-3.5 w-3.5" />
            </button>
          </form>

          {/* Result panel */}
          <div className="space-y-6">
            {offer ? (
              <div className="luxury-card p-8 space-y-6">
                <div>
                  <div className="text-[11px] font-mono uppercase tracking-widest text-zinc-500">{t('trade_result')}</div>
                  <div className="text-4xl font-extrabold text-zinc-900 font-mono tnum mt-1">
                    Rp {offer.estimatedOffer.toLocaleString('id-ID')}
                  </div>
                  <div className="text-xs font-mono text-zinc-500 mt-1">{t('trade_offer')}</div>
                </div>

                <div className="grid grid-cols-2 gap-4 border-t border-zinc-100 pt-5 text-xs">
                  <div>
                    <div className="font-mono text-zinc-500 uppercase">{t('trade_base_value')}</div>
                    <div className="font-mono font-bold text-zinc-900 mt-0.5">Rp {offer.baseDepreciatedValue.toLocaleString('id-ID')}</div>
                  </div>
                  <div>
                    <div className="font-mono text-zinc-500 uppercase">{t('trade_margin')}</div>
                    <div className="font-mono font-bold text-zinc-900 mt-0.5">{(offer.platformMarginRate * 100).toFixed(0)}%</div>
                  </div>
                </div>

                <div className="border-t border-zinc-100 pt-5">
                  <div className="font-mono text-zinc-500 uppercase text-[10px] mb-2">{t('trade_multipliers')}</div>
                  <div className="flex flex-wrap gap-2 text-[11px] font-mono">
                    <span className="px-2.5 py-1 rounded-full border border-zinc-200 bg-zinc-50">Condition {offer.conditionMultiplier}×</span>
                    <span className="px-2.5 py-1 rounded-full border border-zinc-200 bg-zinc-50">Battery {offer.batteryMultiplier}×</span>
                    <span className="px-2.5 py-1 rounded-full border border-zinc-200 bg-zinc-50">Accessories {offer.accessoriesMultiplier}×</span>
                  </div>
                </div>

                {/* Submit official request */}
                {accessToken && (
                  <div className="border-t border-zinc-100 pt-5 space-y-3">
                    <label className="block text-xs font-semibold">{t('trade_model')} <span className="font-normal text-zinc-400">— {t('trade_model_optional')}</span></label>
                    <select value={productModelId} onChange={(e) => setProductModelId(e.target.value)} className={inputCls}>
                      <option value="">{t('trade_select_model')}</option>
                      {(models ?? []).map((m) => (
                        <option key={m.id} value={m.id}>{m.brand} {m.modelName}</option>
                      ))}
                    </select>
                    <button
                      type="button"
                      onClick={handleSubmitRequest}
                      disabled={submitMutation.isPending || !productModelId}
                      className="btn-primary-dark px-6 py-3 text-xs disabled:opacity-60"
                    >
                      {submitMutation.isPending ? t('trade_submitting') : t('trade_submit_request')}
                    </button>
                    {submitMutation.isSuccess && <p className="text-xs text-emerald-600 font-semibold">{t('trade_submitted')}</p>}
                  </div>
                )}
              </div>
            ) : (
              <div className="luxury-card p-8 text-center text-zinc-500 text-sm">
                {t('trade_need_calc')}
              </div>
            )}
          </div>
        </div>

        {/* My requests */}
        {accessToken && (
          <section className="mt-12 bg-white border border-zinc-200 rounded-3xl p-8 shadow-sm space-y-5">
            <h3 className="text-lg font-bold flex items-center gap-2"><History className="h-5 w-5 text-sky-600" /> {t('trade_my_requests')}</h3>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-zinc-200 text-[10px] text-zinc-500 uppercase tracking-wider font-mono font-semibold">
                    <th className="pb-4">ID</th><th className="pb-4">{t('trade_condition')}</th><th className="pb-4">{t('trade_battery')}</th><th className="pb-4">{t('trade_offer')}</th><th className="pb-4">{t('common_status')}</th><th className="pb-4 text-right">{t('common_page')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100">
                  {(myRequests ?? []).map((r) => (
                    <tr key={r.id} className="hover:bg-zinc-50 transition-colors">
                      <td className="py-4 font-mono text-zinc-500">{r.id.slice(0, 8)}</td>
                      <td className="py-4 font-mono">{r.declaredCondition}</td>
                      <td className="py-4 font-mono">{r.declaredBatteryHealth}%</td>
                      <td className="py-4 font-mono font-bold text-emerald-600 tnum">Rp {r.estimatedOffer.toLocaleString('id-ID')}</td>
                      <td className="py-4">
                        <span className="font-mono text-[10px] font-bold px-2.5 py-0.5 rounded-full border text-zinc-700 bg-zinc-50 border-zinc-200">{r.status}</span>
                      </td>
                      <td className="py-4 text-right font-mono text-zinc-500">{new Date(r.createdAt).toLocaleDateString('id-ID')}</td>
                    </tr>
                  ))}
                  {myRequests && myRequests.length === 0 && (
                    <tr><td colSpan={6} className="py-10 text-center text-zinc-500">{t('trade_no_requests')}</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
