'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Calculator, ArrowRight, ShieldCheck, CheckCircle2, RefreshCw, Cpu, Zap } from 'lucide-react';

export default function TradeInPage() {
  const [msrp, setMsrp] = useState<number>(20999000);
  const [ageYears, setAgeYears] = useState<number>(1);
  const [condition, setCondition] = useState<string>('EXCELLENT');
  const [functionality, setFunctionality] = useState<string>('FULLY_FUNCTIONAL');
  const [batteryHealth, setBatteryHealth] = useState<number>(98);
  const [hasAccessories, setHasAccessories] = useState<boolean>(true);
  const [repairEstimate, setRepairEstimate] = useState<number>(0);

  const conditionMultipliers: Record<string, number> = {
    EXCELLENT: 0.95,
    GOOD: 0.85,
    FAIR: 0.70,
    POOR: 0.50,
    DAMAGED: 0.30,
  };

  const functionalMultipliers: Record<string, number> = {
    FULLY_FUNCTIONAL: 1.00,
    MINOR_ISSUES: 0.80,
    MAJOR_ISSUES: 0.50,
    NOT_WORKING: 0.20,
  };

  const getBatteryMultiplier = (health: number) => {
    if (health >= 90) return 1.00;
    if (health >= 80) return 0.98;
    if (health >= 70) return 0.95;
    if (health >= 60) return 0.90;
    return 0.85;
  };

  // ReLoop Multiplicative Formula
  const baseValue = msrp * Math.pow(1 - 0.15, ageYears);
  const condFactor = conditionMultipliers[condition] || 0.70;
  const funcFactor = functionalMultipliers[functionality] || 1.00;
  const batteryFactor = getBatteryMultiplier(batteryHealth);
  const accessoriesFactor = hasAccessories ? 1.03 : 1.00;

  const adjustedValue = baseValue * condFactor * funcFactor * batteryFactor * accessoriesFactor;
  const netValue = Math.max(0, adjustedValue - repairEstimate);
  const finalOffer = Math.round(netValue * (1 - 0.15)); // 15% platform margin

  return (
    <div className="min-h-screen bg-background text-foreground py-12">
      <div className="container mx-auto px-6 max-w-5xl">
        <div className="text-center max-w-xl mx-auto mb-10">
          <div className="inline-flex items-center gap-2 rounded-full border border-border/80 bg-muted/30 px-3 py-1 text-[11px] font-medium text-foreground mb-3">
            <Calculator className="h-3.5 w-3.5 text-foreground" />
            Deterministic Algorithmic Pricing
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight">Instant Trade-In Valuation</h1>
          <p className="text-xs text-muted-foreground mt-2">
            Multiplicative mathematical valuation model calibrated against market depreciation curves and battery wear.
          </p>
        </div>

        <div className="grid lg:grid-cols-12 gap-8">
          {/* Controls */}
          <div className="lg:col-span-7 space-y-6 rounded-3xl border border-border/60 bg-card/40 p-6">
            <div>
              <label className="block text-xs font-semibold text-foreground mb-1.5">Original Retail Price (MSRP IDR)</label>
              <input
                type="number"
                value={msrp}
                onChange={(e) => setMsrp(Number(e.target.value))}
                className="w-full rounded-xl border border-border bg-background px-4 py-2.5 text-xs text-foreground font-mono focus:outline-none focus:ring-1 focus:ring-foreground"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-foreground mb-1.5">Device Age ({ageYears} Year)</label>
                <input
                  type="number"
                  min="0"
                  max="8"
                  value={ageYears}
                  onChange={(e) => setAgeYears(Number(e.target.value))}
                  className="w-full rounded-xl border border-border bg-background px-4 py-2 text-xs font-mono text-foreground focus:outline-none focus:ring-1 focus:ring-foreground"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-foreground mb-1.5">Battery Health ({batteryHealth}%)</label>
                <input
                  type="range"
                  min="50"
                  max="100"
                  value={batteryHealth}
                  onChange={(e) => setBatteryHealth(Number(e.target.value))}
                  className="w-full mt-2"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-foreground mb-1.5">Physical Condition</label>
                <select
                  value={condition}
                  onChange={(e) => setCondition(e.target.value)}
                  className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs text-foreground focus:outline-none focus:ring-1 focus:ring-foreground"
                >
                  <option value="EXCELLENT">Pristine / Flawless (95%)</option>
                  <option value="GOOD">Good / Minor Wear (85%)</option>
                  <option value="FAIR">Fair / Visible Marks (70%)</option>
                  <option value="POOR">Heavy Scratches (50%)</option>
                  <option value="DAMAGED">Cracked / Damaged (30%)</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-foreground mb-1.5">Internal Functionality</label>
                <select
                  value={functionality}
                  onChange={(e) => setFunctionality(e.target.value)}
                  className="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs text-foreground focus:outline-none focus:ring-1 focus:ring-foreground"
                >
                  <option value="FULLY_FUNCTIONAL">100% Fully Functional</option>
                  <option value="MINOR_ISSUES">Minor Speaker/Mic (80%)</option>
                  <option value="MAJOR_ISSUES">Camera/Display Fault (50%)</option>
                  <option value="NOT_WORKING">Dead / No Power (20%)</option>
                </select>
              </div>
            </div>

            <div className="flex items-center gap-2 pt-2">
              <input
                type="checkbox"
                id="acc"
                checked={hasAccessories}
                onChange={(e) => setHasAccessories(e.target.checked)}
                className="rounded border-border text-foreground"
              />
              <label htmlFor="acc" className="text-xs font-medium text-foreground cursor-pointer">
                Includes original manufacturer box & accessories (+3% Valuation Bonus)
              </label>
            </div>
          </div>

          {/* Offer Summary Card */}
          <div className="lg:col-span-5 rounded-3xl border border-border/60 bg-card p-6 flex flex-col justify-between shadow-sm">
            <div>
              <div className="text-[11px] font-mono uppercase tracking-wider text-muted-foreground">Deterministic Payout Offer</div>
              <div className="text-3xl font-extrabold text-foreground mt-2">
                Rp {finalOffer.toLocaleString('id-ID')}
              </div>
              <p className="text-xs text-muted-foreground mt-1">Direct wire payout upon 50-point technician verification</p>

              <div className="mt-6 space-y-2.5 text-xs text-muted-foreground border-t border-border/40 pt-4">
                <div className="flex justify-between">
                  <span>Base Residual:</span>
                  <span className="font-mono text-foreground">Rp {Math.round(baseValue).toLocaleString('id-ID')}</span>
                </div>
                <div className="flex justify-between">
                  <span>Condition Multiplier:</span>
                  <span className="font-mono text-foreground">{(condFactor * 100).toFixed(0)}%</span>
                </div>
                <div className="flex justify-between">
                  <span>Battery Multiplier:</span>
                  <span className="font-mono text-foreground">{(batteryFactor * 100).toFixed(0)}%</span>
                </div>
                <div className="flex justify-between">
                  <span>Platform Retained Fee:</span>
                  <span className="font-mono text-foreground">15% Standard</span>
                </div>
              </div>
            </div>

            <button className="w-full mt-6 inline-flex items-center justify-center gap-2 rounded-full bg-foreground py-3 px-4 text-xs font-bold text-background hover:opacity-90 transition-all shadow">
              Lock In Valuation & Generate Shipping Label
              <ArrowRight className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
