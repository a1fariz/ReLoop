'use client';

import React from 'react';
import { DollarSign, ArrowUpRight, TrendingUp, ShieldCheck, Clock, CheckCircle } from 'lucide-react';

export default function SellerHubPage() {
  return (
    <div className="min-h-screen bg-background text-foreground py-10">
      <div className="container mx-auto px-4 max-w-5xl">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight">Seller Operations & Financial Hub</h1>
            <p className="text-sm text-muted-foreground">Double-entry ledger balance, payout requests, and real-time Bayesian reputation metrics.</p>
          </div>
        </div>

        {/* Metrics Grid */}
        <div className="grid md:grid-cols-4 gap-6 mb-8">
          <div className="bg-card p-6 rounded-xl border border-border">
            <div className="text-xs font-semibold text-muted-foreground uppercase">Available Payout Balance</div>
            <div className="text-2xl font-black text-foreground mt-2">Rp 48.500.000</div>
            <div className="text-xs text-green-600 mt-1 flex items-center gap-1">
              <CheckCircle className="h-3 w-3" /> Ready for withdrawal
            </div>
          </div>

          <div className="bg-card p-6 rounded-xl border border-border">
            <div className="text-xs font-semibold text-muted-foreground uppercase">Pending Escrow Held</div>
            <div className="text-2xl font-black text-foreground mt-2">Rp 17.500.000</div>
            <div className="text-xs text-muted-foreground mt-1 flex items-center gap-1">
              <Clock className="h-3 w-3" /> 1 unit in transit
            </div>
          </div>

          <div className="bg-card p-6 rounded-xl border border-border">
            <div className="text-xs font-semibold text-muted-foreground uppercase">Bayesian Reputation</div>
            <div className="text-2xl font-black text-primary mt-2">98.5 / 100</div>
            <div className="text-xs text-primary mt-1">Tier 1 Verified Seller</div>
          </div>

          <div className="bg-card p-6 rounded-xl border border-border">
            <div className="text-xs font-semibold text-muted-foreground uppercase">Completed Orders</div>
            <div className="text-2xl font-black text-foreground mt-2">128 Orders</div>
            <div className="text-xs text-muted-foreground mt-1">Return rate: 0.8%</div>
          </div>
        </div>

        {/* Financial Ledger Journal Table */}
        <div className="bg-card rounded-xl border border-border p-6">
          <h3 className="text-lg font-bold mb-4">Recent Double-Entry Ledger Transactions</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-border text-xs text-muted-foreground uppercase">
                  <th className="pb-3">Transaction ID</th>
                  <th className="pb-3">Reference / Order</th>
                  <th className="pb-3">Type</th>
                  <th className="pb-3">Amount</th>
                  <th className="pb-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                <tr>
                  <td className="py-3 font-mono text-xs">TX-8912A-90</td>
                  <td className="py-3">Fulfillment iPhone 15 Pro</td>
                  <td className="py-3 font-semibold text-green-600">ESCROW_RELEASE (CR)</td>
                  <td className="py-3 font-bold">Rp +14.875.000</td>
                  <td className="py-3"><span className="rounded-full bg-green-100 dark:bg-green-950 text-green-700 px-2 py-0.5 text-xs font-bold">SETTLED</span></td>
                </tr>
                <tr>
                  <td className="py-3 font-mono text-xs">TX-7718B-12</td>
                  <td className="py-3">Platform Fee (15% Commission)</td>
                  <td className="py-3 font-semibold text-red-600">FEE_DEDUCTION (DR)</td>
                  <td className="py-3 font-bold text-red-600">Rp -2.625.000</td>
                  <td className="py-3"><span className="rounded-full bg-slate-100 dark:bg-slate-800 text-slate-700 px-2 py-0.5 text-xs font-bold">PROCESSED</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
