'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { ShieldCheck, AlertCircle, CheckCircle2, Clock, FileText, ArrowRight, Shield, Cpu, RefreshCw } from 'lucide-react';

export default function WarrantiesPage() {
  const [activeTab, setActiveTab] = useState<'warranties' | 'disputes'>('warranties');

  const warranties = [
    {
      id: 'w-1001',
      device: 'iPhone 15 Pro 256GB Natural Titanium',
      serial: 'F2LZ90K8MD6M',
      grade: 'A+',
      tier: 'STANDARD_6_MONTHS',
      starts: 'Aug 23, 2026',
      expires: 'Feb 23, 2027',
      remainingDays: 180,
      status: 'ACTIVE',
    },
    {
      id: 'w-1002',
      device: 'MacBook Air M2 16GB / 512GB Midnight',
      serial: 'C02G89A3MD6T',
      grade: 'A',
      tier: 'EXTENDED_12_MONTHS',
      starts: 'Aug 20, 2026',
      expires: 'Aug 20, 2027',
      remainingDays: 362,
      status: 'ACTIVE',
    },
  ];

  return (
    <div className="min-h-screen bg-background text-foreground py-10">
      <div className="container mx-auto px-6 max-w-5xl">
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-8 pb-6 border-b border-border/40 gap-4">
          <div>
            <div className="text-xs font-mono uppercase text-muted-foreground tracking-wider mb-1">
              Customer Protection & Arbitration Portal
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight">Active Guarantees & Disputes</h1>
          </div>
        </div>

        {/* Tab Controls */}
        <div className="flex gap-2 border-b border-border/40 mb-8 pb-3">
          <button
            onClick={() => setActiveTab('warranties')}
            className={`rounded-full px-4 py-1.5 text-xs font-semibold transition-all ${
              activeTab === 'warranties'
                ? 'bg-foreground text-background'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            Guaranteed Devices ({warranties.length})
          </button>
          <button
            onClick={() => setActiveTab('disputes')}
            className={`rounded-full px-4 py-1.5 text-xs font-semibold transition-all ${
              activeTab === 'disputes'
                ? 'bg-foreground text-background'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            Arbitrated Disputes (0)
          </button>
        </div>

        {activeTab === 'warranties' ? (
          <div className="space-y-4">
            {warranties.map((w) => (
              <div
                key={w.id}
                className="rounded-3xl border border-border/60 bg-card/40 p-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-6 hover:border-border transition-all"
              >
                <div className="flex items-start gap-4">
                  <div className="h-10 w-10 rounded-2xl bg-accent/15 text-accent flex items-center justify-center border border-accent/30 flex-shrink-0">
                    <ShieldCheck className="h-5 w-5" />
                  </div>
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-sm text-foreground">{w.device}</h3>
                      <span className="rounded-full bg-accent/15 text-accent px-2 py-0.5 text-[10px] font-bold border border-accent/30">
                        {w.status}
                      </span>
                    </div>
                    <div className="text-xs text-muted-foreground font-mono">
                      SERIAL: {w.serial} • TIER: {w.tier}
                    </div>
                    <div className="text-xs text-muted-foreground">
                      Valid through: <span className="font-medium text-foreground">{w.expires}</span> ({w.remainingDays} days remaining)
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-3 w-full md:w-auto">
                  <button className="flex-1 md:flex-initial rounded-full border border-border/80 bg-background/60 px-4 py-2 text-xs font-medium text-foreground hover:bg-muted transition-all">
                    View Coverage Terms
                  </button>
                  <button className="flex-1 md:flex-initial rounded-full bg-foreground px-4 py-2 text-xs font-semibold text-background hover:opacity-90 transition-all">
                    File Technical Claim
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="rounded-3xl border border-border/60 bg-card/40 p-12 text-center space-y-3">
            <div className="h-10 w-10 rounded-full bg-muted/60 text-muted-foreground flex items-center justify-center mx-auto">
              <CheckCircle2 className="h-5 w-5 text-accent" />
            </div>
            <h3 className="text-base font-bold text-foreground">Zero Active Disputes</h3>
            <p className="text-xs text-muted-foreground max-w-sm mx-auto leading-relaxed">
              Every completed order is audited by the Double-Entry Escrow ledger. In the event of condition mismatch, arbitrated split settlements are guaranteed.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
