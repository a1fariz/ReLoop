'use client';

import React, { useState } from 'react';
import { RefreshCw, Calculator, ArrowRight, ShieldCheck, CheckCircle2 } from 'lucide-react';

export default function TradeInPage() {
  const [msrp, setMsrp] = useState<number>(12000000);
  const [ageYears, setAgeYears] = useState<number>(1);
  const [condition, setCondition] = useState<string>('EXCELLENT');
  const [functionality, setFunctionality] = useState<string>('FULLY_FUNCTIONAL');
  const [batteryHealth, setBatteryHealth] = useState<number>(92);
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
      <div className="container mx-auto px-4 max-w-4xl">
        <div className="flex items-center gap-3 mb-6">
          <div className="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center text-primary">
            <Calculator className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Multiplicative Trade-In Valuation Engine</h1>
            <p className="text-sm text-muted-foreground">Deterministic algorithmic pricing based on ReLoop Master Spec §23</p>
          </div>
        </div>

        <div className="grid md:grid-cols-3 gap-8">
          {/* Controls */}
          <div className="md:col-span-2 space-y-6 bg-card p-6 rounded-lg border border-border">
            <div>
              <label className="block text-sm font-medium mb-1">MSRP (Original Price IDR)</label>
              <input
                type="number"
                value={msrp}
                onChange={(e) => setMsrp(Number(e.target.value))}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1">Device Age (Years)</label>
                <input
                  type="number"
                  min="0"
                  max="10"
                  value={ageYears}
                  onChange={(e) => setAgeYears(Number(e.target.value))}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Battery Health ({batteryHealth}%)</label>
                <input
                  type="range"
                  min="40"
                  max="100"
                  value={batteryHealth}
                  onChange={(e) => setBatteryHealth(Number(e.target.value))}
                  className="w-full"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1">Physical Condition</label>
                <select
                  value={condition}
                  onChange={(e) => setCondition(e.target.value)}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                >
                  <option value="EXCELLENT">Pristine / Excellent (95%)</option>
                  <option value="GOOD">Good / Minor Wear (85%)</option>
                  <option value="FAIR">Fair / Scratched (70%)</option>
                  <option value="POOR">Heavy Scratches (50%)</option>
                  <option value="DAMAGED">Cracked / Damaged (30%)</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Functionality</label>
                <select
                  value={functionality}
                  onChange={(e) => setFunctionality(e.target.value)}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                >
                  <option value="FULLY_FUNCTIONAL">100% Fully Functional</option>
                  <option value="MINOR_ISSUES">Minor Issues (80%)</option>
                  <option value="MAJOR_ISSUES">Major Hardware Faults (50%)</option>
                  <option value="NOT_WORKING">Not Powering On (20%)</option>
                </select>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="acc"
                checked={hasAccessories}
                onChange={(e) => setHasAccessories(e.target.checked)}
                className="rounded border-input text-primary"
              />
              <label htmlFor="acc" className="text-sm font-medium">Includes Original Box & Accessories (+3% Multiplier)</label>
            </div>
          </div>

          {/* Offer Summary */}
          <div className="bg-card p-6 rounded-lg border border-border flex flex-col justify-between">
            <div>
              <span className="text-xs font-semibold text-primary uppercase tracking-wider">Instant Final Offer</span>
              <div className="text-3xl font-extrabold text-foreground mt-2">
                Rp {finalOffer.toLocaleString('id-ID')}
              </div>
              <p className="text-xs text-muted-foreground mt-1">Guaranteed minimum trade-in payout</p>

              <div className="mt-6 space-y-2 text-xs text-muted-foreground border-t border-border pt-4">
                <div className="flex justify-between">
                  <span>Base Depreciated:</span>
                  <span>Rp {Math.round(baseValue).toLocaleString('id-ID')}</span>
                </div>
                <div className="flex justify-between">
                  <span>Condition Multiplier:</span>
                  <span>{(condFactor * 100).toFixed(0)}%</span>
                </div>
                <div className="flex justify-between">
                  <span>Battery Multiplier:</span>
                  <span>{(batteryFactor * 100).toFixed(0)}%</span>
                </div>
              </div>
            </div>

            <button className="w-full mt-6 rounded-md bg-accent px-4 py-2.5 text-sm font-semibold text-white shadow hover:bg-green-700 flex items-center justify-center gap-2">
              Lock In Trade-In Offer
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
