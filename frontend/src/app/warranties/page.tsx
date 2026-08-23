'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { ShieldCheck, AlertCircle, CheckCircle2, Clock, FileText, ArrowRight } from 'lucide-react';

export default function WarrantiesPage() {
  const [activeTab, setActiveTab] = useState<'warranties' | 'disputes'>('warranties');

  return (
    <div className="min-h-screen bg-background text-foreground py-10">
      <div className="container mx-auto px-4 max-w-5xl">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight">Warranties & Protection Center</h1>
            <p className="text-sm text-muted-foreground">Manage active device guarantees, claim coverage, and arbitrated dispute resolutions.</p>
          </div>
        </div>

        {/* Tab Selector */}
        <div className="flex border-b border-border mb-6">
          <button
            onClick={() => setActiveTab('warranties')}
            className={`pb-3 px-4 text-sm font-semibold border-b-2 transition-all ${
              activeTab === 'warranties'
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
          >
            Active Warranties (1)
          </button>
          <button
            onClick={() => setActiveTab('disputes')}
            className={`pb-3 px-4 text-sm font-semibold border-b-2 transition-all ${
              activeTab === 'disputes'
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
          >
            Arbitrated Disputes (0)
          </button>
        </div>

        {activeTab === 'warranties' ? (
          <div className="space-y-4">
            <div className="bg-card rounded-xl border border-border p-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
              <div className="flex items-start gap-4">
                <div className="h-12 w-12 rounded-xl bg-accent/10 flex items-center justify-center text-accent">
                  <ShieldCheck className="h-6 w-6" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-base">iPhone 15 Pro 256GB Natural Titanium</h3>
                    <span className="rounded-full bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300 px-2.5 py-0.5 text-xs font-bold">
                      ACTIVE
                    </span>
                  </div>
                  <div className="text-xs text-muted-foreground mt-1">Serial Number: F2LZ90K8MD6M • Policy Tier: STANDARD_6_MONTHS</div>
                  <div className="text-xs text-muted-foreground mt-0.5">Expires: February 23, 2027 (180 Days Remaining)</div>
                </div>
              </div>

              <button className="rounded-lg border border-border px-4 py-2 text-xs font-semibold hover:bg-muted transition-all">
                File Technical Claim
              </button>
            </div>
          </div>
        ) : (
          <div className="bg-card rounded-xl border border-border p-12 text-center space-y-3">
            <div className="h-12 w-12 rounded-full bg-primary/10 text-primary flex items-center justify-center mx-auto">
              <CheckCircle2 className="h-6 w-6" />
            </div>
            <h3 className="text-lg font-bold">No Active Disputes</h3>
            <p className="text-xs text-muted-foreground max-w-sm mx-auto">
              All transactions have been fulfilled successfully. ReLoop Escrow guarantees 100% resolution for any delivery or condition discrepancy.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
