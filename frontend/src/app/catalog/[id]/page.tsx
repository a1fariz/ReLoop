'use client';

import React from 'react';
import Link from 'next/link';
import { Award, ShieldCheck, CheckCircle2, ShoppingBag, ArrowLeft, History, Cpu, BatteryCharging, Check, Lock, ChevronRight } from 'lucide-react';

export default function ListingDetailPage({ params }: { params: { id: string } }) {
  const item = {
    title: 'iPhone 15 Pro 256GB Natural Titanium',
    category: 'Smartphones',
    brand: 'Apple',
    serial: 'F2LZ90K8MD6M',
    price: 17500000,
    originalMsrp: 20999000,
    grade: 'A+',
    conditionTitle: 'Pristine Mint Certification',
    batteryHealth: 98,
    seller: 'Official iBox ReLoop',
    sellerRating: 98.5,
    sellerCompleted: 240,
    warranty: '6 Months Full Protection Guarantee',
    image: 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=1200&auto=format&fit=crop&q=80',
    diagnostics: [
      { name: 'Super Retina XDR 120Hz ProMotion', result: 'PASS', notes: 'Zero dead pixels, TrueTone active' },
      { name: 'Face ID & TrueDepth Biometrics', result: 'PASS', notes: 'Secure Enclave calibrated' },
      { name: '48MP Main / Ultra-Wide / 3x Telephoto', result: 'PASS', notes: 'Sensor-shift stabilization intact' },
      { name: 'Apple A17 Pro 6-Core GPU', result: 'PASS', notes: 'Stress test 100% thermal stability' },
      { name: 'Battery Diagnostic', result: '98% OEM', notes: '184 Cycles • Peak performance capable' },
      { name: 'Liquid Contact Indicators (LCI)', result: 'CLEAR', notes: 'No moisture presence detected' },
    ],
  };

  return (
    <div className="min-h-screen bg-background text-foreground py-10">
      <div className="container mx-auto px-6 max-w-6xl">
        <Link href="/catalog" className="inline-flex items-center gap-2 text-xs font-medium text-muted-foreground hover:text-foreground mb-8">
          <ArrowLeft className="h-3.5 w-3.5" /> Back to Verified Catalog
        </Link>

        <div className="grid lg:grid-cols-12 gap-10">
          {/* Left Column: Imagery & Diagnostic Report */}
          <div className="lg:col-span-7 space-y-8">
            <div className="rounded-3xl border border-border/60 bg-card/40 p-6 overflow-hidden">
              <div className="aspect-[4/3] w-full rounded-2xl overflow-hidden bg-muted/40 relative mb-6">
                <img src={item.image} alt={item.title} className="h-full w-full object-cover object-center" />
                <div className="absolute top-4 left-4 flex items-center gap-1.5 rounded-full bg-background/90 backdrop-blur-md px-3 py-1 text-xs font-semibold text-foreground border border-border/40">
                  <Award className="h-3.5 w-3.5 text-accent" /> Grade {item.grade} • {item.conditionTitle}
                </div>
              </div>

              <div className="flex items-center justify-between text-xs font-mono text-muted-foreground border-t border-border/40 pt-4">
                <span>SERIAL: {item.serial}</span>
                <span>ORIGINAL MSRP: Rp {item.originalMsrp.toLocaleString('id-ID')}</span>
              </div>
            </div>

            {/* 50-Point Technical Diagnostic Matrix */}
            <div className="rounded-3xl border border-border/60 bg-card/40 p-6 space-y-6">
              <div className="flex items-center justify-between border-b border-border/40 pb-4">
                <div>
                  <h3 className="text-base font-bold flex items-center gap-2">
                    <Cpu className="h-4 w-4 text-foreground" />
                    50-Point Technical Diagnostic Matrix
                  </h3>
                  <p className="text-xs text-muted-foreground mt-0.5">Automated hardware diagnostic verified by Certified Tech #3</p>
                </div>
                <span className="rounded-full bg-accent/15 px-2.5 py-1 text-[11px] font-bold text-accent border border-accent/30">
                  PASSED
                </span>
              </div>

              <div className="grid grid-cols-3 gap-3 text-center">
                <div className="p-3.5 rounded-xl border border-border/50 bg-background/60">
                  <div className="text-[11px] text-muted-foreground">Physical Body</div>
                  <div className="text-lg font-bold text-foreground mt-1">98 / 100</div>
                  <div className="text-[10px] text-accent mt-0.5 font-medium">Zero Flaws</div>
                </div>
                <div className="p-3.5 rounded-xl border border-border/50 bg-background/60">
                  <div className="text-[11px] text-muted-foreground">Logic & Chipset</div>
                  <div className="text-lg font-bold text-foreground mt-1">97 / 100</div>
                  <div className="text-[10px] text-accent mt-0.5 font-medium">A17 Pro Pass</div>
                </div>
                <div className="p-3.5 rounded-xl border border-border/50 bg-background/60">
                  <div className="text-[11px] text-muted-foreground">Battery Health</div>
                  <div className="text-lg font-bold text-foreground mt-1">98%</div>
                  <div className="text-[10px] text-primary mt-0.5 font-medium">Original Cell</div>
                </div>
              </div>

              <div className="space-y-2.5 pt-2">
                {item.diagnostics.map((d, i) => (
                  <div key={i} className="flex items-center justify-between p-2.5 rounded-lg bg-muted/20 border border-border/30 text-xs">
                    <div>
                      <div className="font-medium text-foreground flex items-center gap-1.5">
                        <Check className="h-3.5 w-3.5 text-accent" />
                        {d.name}
                      </div>
                      <div className="text-[11px] text-muted-foreground ml-5">{d.notes}</div>
                    </div>
                    <span className="font-mono text-xs font-bold text-foreground bg-background px-2 py-0.5 rounded border border-border/40">
                      {d.result}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            {/* Lifecycle Provenance */}
            <div className="rounded-3xl border border-border/60 bg-card/40 p-6 space-y-4">
              <h3 className="text-base font-bold flex items-center gap-2">
                <History className="h-4 w-4 text-foreground" />
                Immutable Serialized Provenance
              </h3>

              <div className="relative pl-6 space-y-4 border-l border-border/60 text-xs mt-4">
                <div className="relative">
                  <div className="absolute -left-[31px] top-1 h-2.5 w-2.5 rounded-full bg-foreground" />
                  <div className="font-semibold text-foreground">Verified Listing Active</div>
                  <div className="text-muted-foreground text-[11px]">August 2026 — Verified Partner: {item.seller}</div>
                </div>
                <div className="relative">
                  <div className="absolute -left-[31px] top-1 h-2.5 w-2.5 rounded-full bg-accent" />
                  <div className="font-semibold text-foreground">50-Point Technical Diagnostic Certified (Grade A+)</div>
                  <div className="text-muted-foreground text-[11px]">August 2026 — ReLoop Hub Jakarta Verification</div>
                </div>
                <div className="relative">
                  <div className="absolute -left-[31px] top-1 h-2.5 w-2.5 rounded-full bg-muted-foreground" />
                  <div className="font-semibold text-foreground">Authenticated Device Intake</div>
                  <div className="text-muted-foreground text-[11px]">July 2026 — Clean ESN & Ownership Verification</div>
                </div>
              </div>
            </div>
          </div>

          {/* Right Column: Sticky Purchasing & Lease Panel */}
          <div className="lg:col-span-5 space-y-6">
            <div className="sticky top-24 rounded-3xl border border-border/60 bg-card p-6 space-y-6 shadow-sm">
              <div>
                <div className="text-xs font-mono uppercase text-muted-foreground tracking-wider mb-1">
                  {item.category} • {item.brand}
                </div>
                <h1 className="text-2xl font-bold tracking-tight text-foreground mb-3">{item.title}</h1>
                <div className="text-3xl font-extrabold text-foreground">
                  Rp {item.price.toLocaleString('id-ID')}
                </div>
              </div>

              <div className="p-3.5 rounded-xl border border-primary/20 bg-primary/5 text-xs space-y-1">
                <div className="font-semibold text-foreground flex items-center gap-1.5">
                  <Lock className="h-3.5 w-3.5 text-primary" /> Anti-Hoarding Checkout Lease Protection
                </div>
                <p className="text-muted-foreground text-[11px] leading-relaxed">
                  Clicking below locks this unique unit with a 15-minute lease in PostgreSQL. No competitor can buy it while your session is active.
                </p>
              </div>

              <div className="space-y-2.5 text-xs text-muted-foreground border-y border-border/40 py-4">
                <div className="flex items-center justify-between">
                  <span>Seller Verification:</span>
                  <span className="font-semibold text-foreground">{item.seller}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span>Bayesian Reputation:</span>
                  <span className="font-semibold text-accent">{item.sellerRating} / 100 ({item.sellerCompleted} orders)</span>
                </div>
                <div className="flex items-center justify-between">
                  <span>Included Protection:</span>
                  <span className="font-semibold text-foreground flex items-center gap-1">
                    <ShieldCheck className="h-3.5 w-3.5 text-accent" /> {item.warranty}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span>Payment Settlement:</span>
                  <span className="font-semibold text-foreground">Double-Entry Escrow</span>
                </div>
              </div>

              <Link
                href={`/checkout/${params.id || 'demo'}`}
                className="w-full inline-flex items-center justify-center gap-2 rounded-full bg-foreground py-3 px-4 text-xs font-bold text-background hover:opacity-90 transition-all shadow"
              >
                <ShoppingBag className="h-4 w-4" />
                Acquire 15-Minute Lease & Checkout
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
