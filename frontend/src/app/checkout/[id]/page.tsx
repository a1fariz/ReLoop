'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { Clock, CheckCircle2, ArrowRight, Lock, ArrowLeft, ShieldCheck } from 'lucide-react';

export default function CheckoutPage({ params }: { params: { id: string } }) {
  const [secondsLeft, setSecondsLeft] = useState<number>(900); // 15 Minutes (900s)
  const [paymentDone, setPaymentDone] = useState<boolean>(false);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const timer = setInterval(() => {
      setSecondsLeft((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [secondsLeft]);

  const formatTime = (totalSeconds: number) => {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="min-h-screen bg-background text-foreground py-12">
      <div className="container mx-auto px-6 max-w-4xl">
        <Link href="/catalog" className="inline-flex items-center gap-2 text-xs font-medium text-muted-foreground hover:text-foreground mb-6">
          <ArrowLeft className="h-3.5 w-3.5" /> Back to Catalog
        </Link>

        {/* Minimal Anti-Hoarding Lease Banner */}
        <div className="mb-8 rounded-2xl border border-border/80 bg-card/60 p-4 flex items-center justify-between shadow-sm">
          <div className="flex items-center gap-3">
            <div className="h-8 w-8 rounded-full bg-foreground text-background flex items-center justify-center">
              <Clock className="h-4 w-4" />
            </div>
            <div>
              <div className="text-xs font-bold text-foreground">Anti-Hoarding Unit Lease Active</div>
              <div className="text-[11px] text-muted-foreground">Serialized device locked exclusively in PostgreSQL for your session.</div>
            </div>
          </div>
          <div className="text-xl font-bold font-mono text-foreground bg-muted/60 px-3 py-1 rounded-lg border border-border/40">
            {formatTime(secondsLeft)}
          </div>
        </div>

        {paymentDone ? (
          <div className="bg-card rounded-3xl border border-border/60 p-10 text-center space-y-4 shadow-sm">
            <div className="h-14 w-14 bg-accent/15 text-accent rounded-full flex items-center justify-center mx-auto border border-accent/30">
              <CheckCircle2 className="h-7 w-7" />
            </div>
            <h2 className="text-2xl font-bold tracking-tight">Order Confirmed & Escrow Held</h2>
            <p className="text-xs text-muted-foreground max-w-md mx-auto leading-relaxed">
              Your payment of Rp 17.500.000 has been recorded in the platform Double-Entry Financial Ledger (DR: GATEWAY_CLEARING ↔ CR: ESCROW_HELD).
            </p>
            <div className="pt-4 flex justify-center gap-3">
              <Link href="/warranties" className="rounded-full bg-foreground px-6 py-2.5 text-xs font-semibold text-background hover:opacity-90">
                View Active Warranty & Status
              </Link>
              <Link href="/catalog" className="rounded-full border border-border px-6 py-2.5 text-xs font-medium hover:bg-muted">
                Back to Marketplace
              </Link>
            </div>
          </div>
        ) : (
          <div className="grid lg:grid-cols-12 gap-8">
            <div className="lg:col-span-7 space-y-6">
              {/* Shipping Box */}
              <div className="bg-card/40 rounded-3xl border border-border/60 p-6 space-y-4">
                <h3 className="text-sm font-bold text-foreground">1. Shipping & Delivery Address</h3>
                <div className="space-y-3 text-xs">
                  <div>
                    <label className="block text-muted-foreground mb-1">Full Recipient Name</label>
                    <input
                      type="text"
                      defaultValue="Muhammad Alfarizi"
                      className="w-full rounded-xl border border-border bg-background px-3 py-2 text-foreground focus:outline-none focus:ring-1 focus:ring-foreground"
                    />
                  </div>
                  <div>
                    <label className="block text-muted-foreground mb-1">Street Address</label>
                    <textarea
                      defaultValue="Jl. Jenderal Sudirman No. 45, Kavling 12, Jakarta Pusat, DKI Jakarta 10220"
                      rows={2}
                      className="w-full rounded-xl border border-border bg-background px-3 py-2 text-foreground focus:outline-none focus:ring-1 focus:ring-foreground"
                    />
                  </div>
                </div>
              </div>

              {/* Payment Box */}
              <div className="bg-card/40 rounded-3xl border border-border/60 p-6 space-y-4">
                <h3 className="text-sm font-bold text-foreground">2. Payment & Escrow Settlement</h3>
                <div className="rounded-2xl border border-border/80 bg-background/60 p-4 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Lock className="h-4 w-4 text-foreground" />
                    <div>
                      <div className="text-xs font-bold text-foreground">ReLoop Direct Escrow Clearing</div>
                      <div className="text-[11px] text-muted-foreground">Instant double-entry ledger lock</div>
                    </div>
                  </div>
                  <span className="text-[10px] font-mono font-bold bg-accent/15 text-accent px-2 py-0.5 rounded border border-accent/30">
                    VERIFIED
                  </span>
                </div>
              </div>
            </div>

            {/* Right Summary */}
            <div className="lg:col-span-5">
              <div className="bg-card rounded-3xl border border-border/60 p-6 space-y-5 shadow-sm">
                <h3 className="text-sm font-bold text-foreground">Order Breakdown</h3>

                <div className="space-y-2.5 text-xs text-muted-foreground border-b border-border/40 pb-4">
                  <div className="flex justify-between">
                    <span>iPhone 15 Pro 256GB (Grade A+):</span>
                    <span className="font-mono text-foreground">Rp 17.500.000</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Insured 3PL Logistics:</span>
                    <span className="font-mono text-foreground">Rp 0 (Free)</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Escrow Protection Fee:</span>
                    <span className="font-mono text-foreground">Included</span>
                  </div>
                </div>

                <div className="flex justify-between font-bold text-base text-foreground">
                  <span>Total Settlement:</span>
                  <span className="font-mono">Rp 17.500.000</span>
                </div>

                <button
                  onClick={() => setPaymentDone(true)}
                  className="w-full inline-flex items-center justify-center gap-2 rounded-full bg-foreground py-3 text-xs font-bold text-background hover:opacity-90 transition-all shadow"
                >
                  Confirm & Lock Escrow
                  <ArrowRight className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
