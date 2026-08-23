'use client';

import React from 'react';
import Link from 'next/link';
import { DollarSign, ArrowUpRight, TrendingUp, ShieldCheck, Clock, CheckCircle, ArrowRight, Award } from 'lucide-react';

export default function SellerHubPage() {
  const transactions = [
    {
      id: 'TX-8912A-90',
      reference: 'Fulfillment #ORD-2026-9912 (iPhone 15 Pro)',
      type: 'ESCROW_RELEASE (CR)',
      amount: 'Rp +14.875.000',
      fee: 'Rp 2.625.000 (15% Take-Rate)',
      status: 'SETTLED',
      date: 'Aug 23, 2026',
    },
    {
      id: 'TX-7718B-12',
      reference: 'Fulfillment #ORD-2026-8831 (MacBook Air M2)',
      type: 'ESCROW_RELEASE (CR)',
      amount: 'Rp +13.430.000',
      fee: 'Rp 2.370.000 (15% Take-Rate)',
      status: 'SETTLED',
      date: 'Aug 22, 2026',
    },
    {
      id: 'TX-6610C-44',
      reference: 'Disbursement to BCA Bank (Account **8812)',
      type: 'PAYOUT_WITHDRAWAL (DR)',
      amount: 'Rp -25.000.000',
      fee: 'Rp 0',
      status: 'PROCESSED',
      date: 'Aug 21, 2026',
    },
  ];

  return (
    <div className="min-h-screen bg-background text-foreground py-10">
      <div className="container mx-auto px-6 max-w-6xl">
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-8 pb-6 border-b border-border/40 gap-4">
          <div>
            <div className="text-xs font-mono uppercase text-muted-foreground tracking-wider mb-1">
              Store: Official iBox ReLoop (ID #1)
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight">Seller Financial & Operations Hub</h1>
          </div>

          <button className="rounded-full bg-foreground px-5 py-2 text-xs font-semibold text-background hover:opacity-90 transition-all">
            Request Payout Withdrawal
          </button>
        </div>

        {/* Metrics Overview */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-5 mb-10">
          <div className="rounded-2xl border border-border/60 bg-card/40 p-5 space-y-2">
            <div className="text-[11px] font-mono uppercase tracking-wider text-muted-foreground">Available Payout Balance</div>
            <div className="text-2xl font-bold text-foreground">Rp 48.500.000</div>
            <div className="text-[11px] text-accent flex items-center gap-1 font-medium">
              <CheckCircle className="h-3 w-3" /> Reconciled & Ready
            </div>
          </div>

          <div className="rounded-2xl border border-border/60 bg-card/40 p-5 space-y-2">
            <div className="text-[11px] font-mono uppercase tracking-wider text-muted-foreground">Escrow In-Transit</div>
            <div className="text-2xl font-bold text-foreground">Rp 17.500.000</div>
            <div className="text-[11px] text-muted-foreground flex items-center gap-1">
              <Clock className="h-3 w-3" /> 1 Unit Awaiting Delivery
            </div>
          </div>

          <div className="rounded-2xl border border-border/60 bg-card/40 p-5 space-y-2">
            <div className="text-[11px] font-mono uppercase tracking-wider text-muted-foreground">Bayesian Reputation</div>
            <div className="text-2xl font-bold text-foreground">98.5 / 100</div>
            <div className="text-[11px] text-accent font-medium">
              Tier 1 Top Certified Merchant
            </div>
          </div>

          <div className="rounded-2xl border border-border/60 bg-card/40 p-5 space-y-2">
            <div className="text-[11px] font-mono uppercase tracking-wider text-muted-foreground">Completed Sales</div>
            <div className="text-2xl font-bold text-foreground">240 Units</div>
            <div className="text-[11px] text-muted-foreground">Dispute rate: 0.00%</div>
          </div>
        </div>

        {/* Double-Entry Journal Table */}
        <div className="rounded-3xl border border-border/60 bg-card/40 p-6 space-y-4">
          <div className="flex items-center justify-between border-b border-border/40 pb-4">
            <div>
              <h3 className="text-base font-bold text-foreground">Double-Entry Financial Ledger Journal</h3>
              <p className="text-xs text-muted-foreground mt-0.5">Immutable credit/debit audit trail recorded via DoubleEntryLedgerService</p>
            </div>
            <span className="text-xs font-mono text-muted-foreground">Currency: IDR (Bank Rounded)</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-border/40 text-[11px] font-mono text-muted-foreground uppercase">
                  <th className="pb-3">Journal Ref</th>
                  <th className="pb-3">Transaction Purpose</th>
                  <th className="pb-3">Type</th>
                  <th className="pb-3">Net Amount</th>
                  <th className="pb-3">Status</th>
                  <th className="pb-3 text-right">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/30">
                {transactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-muted/20 transition-colors">
                    <td className="py-3.5 font-mono text-muted-foreground">{tx.id}</td>
                    <td className="py-3.5 font-medium text-foreground">
                      {tx.reference}
                      <div className="text-[10px] text-muted-foreground font-normal">{tx.fee}</div>
                    </td>
                    <td className="py-3.5 font-mono font-semibold text-foreground">{tx.type}</td>
                    <td className="py-3.5 font-mono font-bold text-foreground">{tx.amount}</td>
                    <td className="py-3.5">
                      <span className="rounded-full bg-accent/15 text-accent px-2 py-0.5 text-[10px] font-bold border border-accent/30">
                        {tx.status}
                      </span>
                    </td>
                    <td className="py-3.5 text-right font-mono text-muted-foreground">{tx.date}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
