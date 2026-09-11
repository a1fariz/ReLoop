'use client';

import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Layers, Cpu, Battery, Smartphone } from 'lucide-react';

export function HardwareExplodedView({ onSelectModule }: { onSelectModule?: (mod: string) => void }) {
  const [activeLayer, setActiveLayer] = useState<string>('all');

  const layers = [
    { id: 'chassis', name: 'Grade 5 Titanium Enclosure', score: '98/100', status: 'PASS', icon: Smartphone, desc: 'Zero structural warping, scratch-free aerospace perimeter.' },
    { id: 'display', name: 'Super Retina XDR 120Hz OLED', score: '100%', status: 'PASS', icon: Layers, desc: 'Zero dead sub-pixels, TrueTone & ambient calibration verified.' },
    { id: 'chipset', name: 'Apple A17 Pro Silicon (3nm)', score: '100%', status: 'PASS', icon: Cpu, desc: '6-core GPU peak load tested with 100% thermal stability.' },
    { id: 'battery', name: 'OEM Lithium Battery Telemetry', score: '98%', status: 'PASS', icon: Battery, desc: '184 full cycles logged. Peak voltage output guaranteed.' },
  ];

  const handleLayerClick = (layerId: string) => {
    setActiveLayer(layerId);
    if (onSelectModule) {
      onSelectModule(layerId);
    }
  };

  return (
    <div className="bg-white border border-zinc-200 rounded-3xl p-8 space-y-6 shadow-sm select-none">
      <div className="flex flex-col sm:flex-row sm:items-baseline justify-between border-b border-zinc-100 pb-4 gap-2">
        <div>
          <div className="text-[11px] font-mono uppercase tracking-widest text-sky-600 font-semibold">
            Hardware X-Ray [Exploded Module Architecture]
          </div>
          <h3 className="text-lg font-bold text-zinc-900 mt-1">Modular Component Breakdown</h3>
        </div>
        <span className="text-xs font-mono text-zinc-500 bg-zinc-50 px-2.5 py-1 rounded-full border border-zinc-200">Click component to isolate</span>
      </div>

      <div className="grid sm:grid-cols-2 gap-4">
        {layers.map((layer) => {
          const Icon = layer.icon;
          return (
            <motion.div
              key={layer.id}
              onClick={() => handleLayerClick(layer.id)}
              whileHover={{ scale: 1.015 }}
              whileTap={{ scale: 0.985 }}
              className={`p-5 border cursor-pointer transition-all rounded-2xl ${
                activeLayer === layer.id
                  ? 'border-sky-400 bg-sky-50/60 shadow-md'
                  : activeLayer === 'all'
                  ? 'border-zinc-200 bg-zinc-50 hover:border-zinc-300'
                  : 'border-zinc-200 bg-white hover:border-zinc-300'
              }`}
            >
              <div className="flex items-center justify-between mb-3 gap-2">
                <div className="flex items-center gap-2.5 min-w-0">
                  <div className={`p-2 rounded-xl flex-shrink-0 ${activeLayer === layer.id ? 'bg-sky-600 text-white' : 'bg-zinc-900 text-white'}`}>
                    <Icon className="h-4 w-4" />
                  </div>
                  <span className="font-bold text-sm text-zinc-900 truncate">{layer.name}</span>
                </div>
                <span className="font-mono text-xs text-zinc-700 bg-white border border-zinc-200 px-2 py-1 rounded-full whitespace-nowrap">
                  {layer.score}
                </span>
              </div>
              <p className="text-xs text-zinc-500 leading-relaxed">
                {layer.desc}
              </p>
            </motion.div>
          );
        })}
      </div>
    </div>
  );
}