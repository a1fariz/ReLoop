'use client';

import React from 'react';
import { TrendingDown, Calendar, ShieldCheck, ArrowUpRight } from 'lucide-react';

export function DepreciationCurveChart({
  msrp = 20999000,
  ageYears = 1,
  currentOffer = 14500000,
}: {
  msrp?: number;
  ageYears?: number;
  currentOffer?: number;
}) {
  // Generate 5-year curve points
  const points = [0, 1, 2, 3, 4, 5].map((year) => {
    const value = msrp * Math.pow(1 - 0.15, year) * 0.85;
    return { year, value: Math.round(value) };
  });

  const maxValue = msrp;
  const svgWidth = 600;
  const svgHeight = 220;
  const padding = 35;

  const getX = (year: number) => padding + (year / 5) * (svgWidth - padding * 2);
  const getY = (val: number) => svgHeight - padding - (val / maxValue) * (svgHeight - padding * 2);

  const pathD = points.reduce((acc, pt, i) => {
    const x = getX(pt.year);
    const y = getY(pt.value);
    return i === 0 ? `M ${x} ${y}` : `${acc} L ${x} ${y}`;
  }, '');

  return (
    <div className="w-full bg-white border border-zinc-200 rounded-3xl p-8 space-y-6 shadow-sm select-none">
      <div className="flex flex-col sm:flex-row sm:items-baseline justify-between border-b border-zinc-100 pb-4 gap-2">
        <div>
          <div className="text-[11px] font-mono uppercase tracking-widest text-sky-600 font-semibold">
            Predictive Model [Depreciation Trajectory]
          </div>
          <h3 className="text-lg font-bold text-zinc-900 mt-1">Multiplicative Curve Projection</h3>
        </div>
        <div className="flex items-center gap-1.5 text-xs font-mono text-zinc-600 bg-zinc-50 px-3 py-1.5 rounded-full border border-zinc-200">
          <TrendingDown className="h-3.5 w-3.5 text-sky-600" />
          <span>Annual decay: 15.0% deterministic rate</span>
        </div>
      </div>

      {/* SVG Interactive Curve Chart */}
      <div className="w-full bg-zinc-50/60 p-4 rounded-2xl border border-zinc-100 overflow-x-auto">
        <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} className="w-full h-48">
          {/* Grid lines */}
          <line x1={padding} y1={padding} x2={padding} y2={svgHeight - padding} stroke="#e4e4e7" strokeWidth="1" />
          <line x1={padding} y1={svgHeight - padding} x2={svgWidth - padding} y2={svgHeight - padding} stroke="#e4e4e7" strokeWidth="1" />

          {/* Area fill under curve */}
          <path
            d={`${pathD} L ${getX(5)} ${svgHeight - padding} L ${getX(0)} ${svgHeight - padding} Z`}
            fill="rgba(2, 132, 199, 0.06)"
          />

          {/* Curve stroke */}
          <path d={pathD} fill="none" stroke="#0284c7" strokeWidth="2.5" strokeLinecap="round" />

          {/* Year dots */}
          {points.map((pt) => {
            const isCurrent = pt.year === ageYears;
            const x = getX(pt.year);
            const y = getY(pt.value);
            return (
              <g key={pt.year}>
                <circle
                  cx={x}
                  cy={y}
                  r={isCurrent ? 6 : 4}
                  fill={isCurrent ? '#09090b' : '#ffffff'}
                  stroke={isCurrent ? '#09090b' : '#0284c7'}
                  strokeWidth="2"
                />
                <text
                  x={x}
                  y={svgHeight - 12}
                  textAnchor="middle"
                  fill="#71717a"
                  fontSize="10"
                  fontFamily="monospace"
                  fontWeight="600"
                >
                  Yr {pt.year}
                </text>
              </g>
            );
          })}

          {/* Current Year Indicator Pin */}
          <line
            x1={getX(ageYears)}
            y1={padding}
            x2={getX(ageYears)}
            y2={svgHeight - padding}
            stroke="#d4d4d8"
            strokeDasharray="4 4"
          />
        </svg>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 text-xs font-mono text-zinc-600">
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-100">
          <span className="text-emerald-700 block mb-1 font-medium">Current Payout:</span>
          <span className="text-zinc-900 font-bold tnum">Rp {currentOffer.toLocaleString('id-ID')}</span>
        </div>
        <div className="p-4 rounded-2xl bg-sky-50 border border-sky-100">
          <span className="text-sky-700 block mb-1 font-medium">Next Year Projected:</span>
          <span className="text-zinc-900 font-bold tnum">Rp {Math.round(currentOffer * 0.85).toLocaleString('id-ID')}</span>
        </div>
        <div className="p-4 rounded-2xl bg-zinc-50 border border-zinc-100 col-span-2 sm:col-span-1">
          <span className="text-zinc-500 block mb-1 font-medium">Optimal Action:</span>
          <span className="text-zinc-900 font-bold">Lock Valuation Now</span>
        </div>
      </div>
    </div>
  );
}
