'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { ShieldCheck, Clock, CheckCircle2, ArrowRight, Lock } from 'lucide-react';

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
      <div className="container mx-auto px-4 max-w-3xl">
        {/* Anti-Hoarding Timer Banner */}
        <div className="mb-6 rounded-xl border border-primary/30 bg-primary/10 p-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Clock className="h-6 w-6 text-primary animate-pulse" />
            <div>
              <div className="text-sm font-bold text-foreground">Anti-Hoarding Unit Lease Active</div>
              <div className="text-xs text-muted-foreground">This serialized unit is locked exclusively for your session.</div>
            </div>
          </div>
          <div className="text-2xl font-black font-mono text-primary">
            {formatTime(secondsLeft)}
          </div>
        </div>

        {paymentDone ? (
          <div className="bg-card rounded-xl border border-border p-8 text-center space-y-4">
            <div className="h-16 w-16 bg-green-100 dark:bg-green-950 text-green-600 rounded-full flex items-center justify-center mx-auto">
              <CheckCircle2 className="h-8 w-8" />
            </div>
            <h2 className="text-2xl font-bold">Order Confirmed & Escrow Held!</h2>
            <p className="text-sm text-muted-foreground max-w-md mx-auto">
              Your payment of Rp 17.500.000 has been secured in the Double-Entry Platform Escrow. Seller will dispatch within 24 hours.
            </p>
            <div className="pt-4">
              <Link href="/catalog" className="inline-flex rounded-lg bg-primary px-6 py-2.5 text-sm font-semibold text-white">
                Back to Catalog
              </Link>
            </div>
          </div>
        ) : (
          <div className="grid md:grid-cols-3 gap-8">
            <div className="md:col-span-2 space-y-6">
              <div className="bg-card rounded-xl border border-border p-6">
                <h3 className="text-lg font-bold mb-4">Shipping Destination</h3>
                <div className="space-y-3 text-sm">
                  <input
                    type="text"
                    defaultValue="Muhammad Alfarizi"
                    className="w-full rounded-md border border-input bg-background px-3 py-2"
                  />
                  <textarea
                    defaultValue="Jl. Sudirman No. 45, Jakarta Pusat, DKI Jakarta 10220"
                    rows={2}
                    className="w-full rounded-md border border-input bg-background px-3 py-2"
                  />
                </div>
              </div>

              <div className="bg-card rounded-xl border border-border p-6">
                <h3 className="text-lg font-bold mb-4">Payment Method (Simulated Gateway)</h3>
                <div className="rounded-lg border border-primary/40 bg-primary/5 p-4 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Lock className="h-5 w-5 text-primary" />
                    <div>
                      <div className="text-sm font-bold">ReLoop Escrow Direct Clearing</div>
                      <div className="text-xs text-muted-foreground">Instant double-entry reconciliation</div>
                    </div>
                  </div>
                  <span className="text-xs font-bold text-primary">Active</span>
                </div>
              </div>
            </div>

            <div className="bg-card rounded-xl border border-border p-6 h-fit space-y-4">
              <h3 className="text-base font-bold">Order Summary</h3>
              <div className="space-y-2 text-xs text-muted-foreground">
                <div className="flex justify-between">
                  <span>Unit Price:</span>
                  <span>Rp 17.500.000</span>
                </div>
                <div className="flex justify-between">
                  <span>Shipping (Insured 3PL):</span>
                  <span>Rp 0</span>
                </div>
                <div className="flex justify-between">
                  <span>Escrow Guarantee Fee:</span>
                  <span>Included</span>
                </div>
                <div className="border-t border-border pt-2 flex justify-between font-bold text-sm text-foreground">
                  <span>Total Amount:</span>
                  <span>Rp 17.500.000</span>
                </div>
              </div>

              <button
                onClick={() => setPaymentDone(true)}
                className="w-full rounded-lg bg-accent py-3 text-center text-sm font-bold text-white shadow hover:bg-green-700 flex items-center justify-center gap-2"
              >
                Pay Rp 17.500.000
                <ArrowRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
