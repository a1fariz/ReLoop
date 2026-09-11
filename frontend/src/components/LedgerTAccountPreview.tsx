'use client';

import React from 'react';
import { Scale, Lock, ShieldCheck, CheckCircle2 } from 'lucide-react';

export function LedgerTAccountPreview({
  amount = 17500000,
  sellerName = 'Official iBox ReLoop',
}: {
  amount?: number;
  sellerName?: string;
}) {
  const takeRate = Math.round(amount * 0.15);
  const sellerPayout = amount - takeRate;

  return (
    <div className="w-full bg-white border border-zinc-200 rounded-3xl p-8 space-y-6 shadow-sm select-none">
      <div className="flex flex-col sm:flex-row sm:items-baseline justify-between border-b border-zinc-100 pb-4 gap-2">
        <div>
          <div className="text-[11px] uppercase tracking-widest text-sky-600 font-semibold font-mono">
            Double-Entry Accounting [T-Account Journal]
          </div>
          <h3 className="text-base font-bold text-zinc-900 mt-1">Escrow Settlement Ledger</h3>
        </div>
        <div className="flex items-center gap-1.5 text-xs text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-100 font-semibold w-fit">
          <Scale className="h-3.5 w-3.5" />
          <span>Zero-Sum Balance</span>
        </div>
      </div>

      {/* T-Account Visualizer Table */}
      <div className="grid md:grid-cols-2 gap-4">
        {/* Left: Debit Account */}
        <div className="border border-zinc-200 rounded-2xl bg-zinc-50/60 p-5 space-y-3">
          <div className="flex justify-between items-center border-b border-zinc-200 pb-2">
            <span className="font-bold text-zinc-900 uppercase text-[11px] font-mono">DEBIT (DR)</span>
            <span className="text-zinc-500 text-[10px]">Asset / Inflow</span>
          </div>
          <div className="space-y-2">
            <div className="flex justify-between items-baseline gap-2">
              <div>
                <div className="text-sm font-bold text-zinc-900">1010 - Gateway Clearing</div>
                <div className="text-[10px] text-zinc-500 mt-0.5">Incoming buyer wire transfer</div>
              </div>
              <span className="text-zinc-900 font-bold tnum text-xs whitespace-nowrap">Rp {amount.toLocaleString('id-ID')}</span>
            </div>
          </div>
        </div>

        {/* Right: Credit Accounts */}
        <div className="border border-zinc-200 rounded-2xl bg-zinc-50/60 p-5 space-y-3">
          <div className="flex justify-between items-center border-b border-zinc-200 pb-2">
            <span className="font-bold text-zinc-900 uppercase text-[11px] font-mono">CREDIT (CR)</span>
            <span className="text-zinc-500 text-[10px]">Liability & Revenue</span>
          </div>
          <div className="space-y-2.5">
            <div className="flex justify-between items-baseline gap-2">
              <div>
                <div className="text-sm font-bold text-zinc-900">2010 - Escrow Vault</div>
                <div className="text-[10px] text-zinc-500 mt-0.5">Payable to {sellerName}</div>
              </div>
              <span className="text-zinc-900 font-bold tnum text-xs whitespace-nowrap">Rp {sellerPayout.toLocaleString('id-ID')}</span>
            </div>

            <div className="flex justify-between items-baseline gap-2 border-t border-zinc-200 pt-2.5">
              <div>
                <div className="text-sm font-bold text-zinc-900">4010 - Platform Commission</div>
                <div className="text-[10px] text-zinc-500 mt-0.5">15% Take-Rate Fee</div>
              </div>
              <span className="text-zinc-900 font-bold tnum text-xs whitespace-nowrap">Rp {takeRate.toLocaleString('id-ID')}</span>
            </div>
          </div>
        </div>
      </div>

      <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-100 flex flex-col sm:flex-row sm:items-center justify-between text-xs gap-2">
        <div className="flex items-center gap-2 text-emerald-800 font-semibold">
          <Lock className="h-3.5 w-3.5" />
          <span>DoubleEntryLedgerService auto-allocated</span>
        </div>
        <span className="text-emerald-700 font-bold font-mono">Sum DR = Sum CR ✓</span>
      </div>
    </div>
  );
}
