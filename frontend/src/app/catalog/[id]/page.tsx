'use client';

import React from 'react';
import Link from 'next/link';
import { Award, ShieldCheck, CheckCircle2, ShoppingBag, ArrowLeft, History, Cpu, BatteryCharging, Check } from 'lucide-react';

export default function ListingDetailPage({ params }: { params: { id: string } }) {
  return (
    <div className="min-h-screen bg-background text-foreground py-10">
      <div className="container mx-auto px-4 max-w-5xl">
        <Link href="/catalog" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-6">
          <ArrowLeft className="h-4 w-4" /> Back to Certified Catalog
        </Link>

        <div className="grid md:grid-cols-3 gap-8">
          {/* Main Unit Details & Diagnostic Breakdown */}
          <div className="md:col-span-2 space-y-6">
            <div className="bg-card rounded-xl border border-border p-6 space-y-4">
              <div className="flex items-center justify-between">
                <span className="inline-flex items-center gap-1 rounded-full bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300 px-3 py-1 text-sm font-bold">
                  <Award className="h-4 w-4" /> Certified Grade A+ (Pristine)
                </span>
                <span className="text-xs font-mono text-muted-foreground">Serial: F2LZ90K8MD6M</span>
              </div>

              <h1 className="text-2xl font-bold">iPhone 15 Pro 256GB Natural Titanium</h1>
              <div className="text-3xl font-black text-foreground">
                Rp 17.500.000
              </div>
              <p className="text-sm text-muted-foreground">
                Pristine condition with 98% battery health, original box, and comprehensive 50-point technical certification.
              </p>
            </div>

            {/* 50-Point Technical Inspection Diagnostic */}
            <div className="bg-card rounded-xl border border-border p-6 space-y-4">
              <div className="flex items-center justify-between border-b border-border pb-3">
                <h3 className="font-bold text-base flex items-center gap-2">
                  <Cpu className="h-5 w-5 text-primary" />
                  Certified Technical Diagnostic Report
                </h3>
                <span className="text-xs font-semibold text-accent flex items-center gap-1">
                  <CheckCircle2 className="h-3.5 w-3.5" /> Passed All Gates
                </span>
              </div>

              <div className="grid grid-cols-3 gap-4 text-center">
                <div className="p-3 rounded-lg bg-muted/40 border border-border">
                  <div className="text-xs text-muted-foreground">Physical Condition</div>
                  <div className="text-xl font-bold text-foreground mt-1">98 / 100</div>
                  <div className="text-[10px] text-green-600 mt-0.5">Zero Micro-Scratches</div>
                </div>
                <div className="p-3 rounded-lg bg-muted/40 border border-border">
                  <div className="text-xs text-muted-foreground">Hardware & Chipset</div>
                  <div className="text-xl font-bold text-foreground mt-1">97 / 100</div>
                  <div className="text-[10px] text-green-600 mt-0.5">A17 Pro Flawless</div>
                </div>
                <div className="p-3 rounded-lg bg-muted/40 border border-border">
                  <div className="text-xs text-muted-foreground">Battery Health</div>
                  <div className="text-xl font-bold text-primary mt-1">98%</div>
                  <div className="text-[10px] text-primary mt-0.5">Original OEM Cell</div>
                </div>
              </div>

              {/* Checklist Breakdown */}
              <div className="space-y-2 text-xs pt-2">
                <div className="flex items-center justify-between py-1.5 border-b border-border/50">
                  <span className="flex items-center gap-2"><Check className="h-3.5 w-3.5 text-accent" /> Super Retina XDR & 120Hz ProMotion</span>
                  <span className="font-bold text-accent">PASS</span>
                </div>
                <div className="flex items-center justify-between py-1.5 border-b border-border/50">
                  <span className="flex items-center gap-2"><Check className="h-3.5 w-3.5 text-accent" /> Face ID Biometrics & TrueDepth Camera</span>
                  <span className="font-bold text-accent">PASS</span>
                </div>
                <div className="flex items-center justify-between py-1.5 border-b border-border/50">
                  <span className="flex items-center gap-2"><Check className="h-3.5 w-3.5 text-accent" /> 48MP Main, Telephoto & Ultra-Wide Sensors</span>
                  <span className="font-bold text-accent">PASS</span>
                </div>
                <div className="flex items-center justify-between py-1.5">
                  <span className="flex items-center gap-2"><Check className="h-3.5 w-3.5 text-accent" /> Logic Board & Water Damage Indicators</span>
                  <span className="font-bold text-accent">CLEAR</span>
                </div>
              </div>
            </div>

            {/* Lifecycle Provenance Timeline */}
            <div className="bg-card rounded-xl border border-border p-6 space-y-4">
              <h3 className="font-bold text-base flex items-center gap-2">
                <History className="h-5 w-5 text-primary" />
                Immutable Unit Provenance History
              </h3>

              <div className="relative pl-6 space-y-4 border-l-2 border-primary/30 text-xs">
                <div className="relative">
                  <div className="absolute -left-[31px] top-0 h-3 w-3 rounded-full bg-primary" />
                  <div className="font-bold text-foreground">Verified Listing Created</div>
                  <div className="text-muted-foreground text-[11px]">August 2026 — Official iBox ReLoop</div>
                </div>
                <div className="relative">
                  <div className="absolute -left-[31px] top-0 h-3 w-3 rounded-full bg-accent" />
                  <div className="font-bold text-foreground">Technical 50-Point Inspection Certified (Grade A+)</div>
                  <div className="text-muted-foreground text-[11px]">August 2026 — Verified Technician #3</div>
                </div>
                <div className="relative">
                  <div className="absolute -left-[31px] top-0 h-3 w-3 rounded-full bg-muted-foreground" />
                  <div className="font-bold text-foreground">Device Trade-In Initial Intake</div>
                  <div className="text-muted-foreground text-[11px]">July 2026 — Authenticated Intake Center</div>
                </div>
              </div>
            </div>
          </div>

          {/* Action Sidebar */}
          <div className="space-y-6">
            <div className="bg-card rounded-xl border border-border p-6 space-y-4">
              <div className="text-xs uppercase font-bold text-muted-foreground">Certified Escrow Protection</div>
              <div className="text-2xl font-black text-foreground">Rp 17.500.000</div>

              <div className="space-y-2 text-xs text-muted-foreground border-t border-border pt-4">
                <div className="flex items-center justify-between">
                  <span>Seller Hub:</span>
                  <span className="font-bold text-foreground">Official iBox ReLoop</span>
                </div>
                <div className="flex items-center justify-between">
                  <span>Reputation Rating:</span>
                  <span className="font-bold text-primary">98.5 / 100</span>
                </div>
                <div className="flex items-center justify-between">
                  <span>Warranty:</span>
                  <span className="font-bold text-accent">6 Months Full Coverage</span>
                </div>
              </div>

              <Link
                href={`/checkout/${params.id || 'demo-unit'}`}
                className="w-full rounded-lg bg-primary py-3 px-4 text-center text-sm font-bold text-white shadow hover:bg-primary-hover flex items-center justify-center gap-2"
              >
                <ShoppingBag className="h-4 w-4" />
                Acquire 15-Min Checkout Lease
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
