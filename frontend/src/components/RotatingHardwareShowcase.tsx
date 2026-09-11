'use client';

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Cpu, Battery, Eye, Sparkles, ShieldCheck, Check, RotateCcw, ArrowRight, Layers, Box, CheckCircle2, Sliders, Smartphone } from 'lucide-react';

const APPLE_FINISHES = {
  natural: {
    name: 'Natural Titanium',
    tag: 'Grade 5 Aerospace Alloy',
    colorHex: '#9c958c',
    heroImage: 'https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/iphone-15-pro-max-naturaltitanium-select?wid=940&hei=1112&fmt=p-jpg&qlt=90',
    studioBanner: 'https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/iphone-15-pro-finish-select-202309-6-7inch-naturaltitanium?wid=2560&hei=1440&fmt=p-jpg&qlt=85',
    specularGradient: 'radial-gradient(circle at 50% 50%, rgba(156, 149, 140, 0.15) 0%, transparent 70%)',
    desc: 'Micro-blasted natural titanium alloy finish with silver-beige luster. 98% battery health.',
    hotspots: [
      { id: 'titanium', title: 'Grade 5 Titanium Contoured Edge', top: '50%', left: '26%', desc: 'Precision brush-finished structural perimeter with smooth curvature.' },
      { id: 'camera', title: '48MP Pro Triple Sapphire Array', top: '24%', left: '38%', desc: '24mm, 28mm, 35mm focal lengths calibrated. 100% optical pass.' },
      { id: 'a17', title: 'Apple A17 Pro (3nm Node)', top: '44%', left: '48%', desc: 'Pro-class 6-Core GPU. Hardware ray tracing verified at 100% load.' },
      { id: 'battery', title: '98% OEM Battery Capacity', top: '64%', left: '48%', desc: '184 full cycles logged. Zero voltage sag under benchmark.' },
    ],
  },
  black: {
    name: 'Black Titanium',
    tag: 'PVD Vaporization Coated',
    colorHex: '#2b2a2c',
    heroImage: 'https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/iphone-15-pro-max-blacktitanium-select?wid=940&hei=1112&fmt=p-jpg&qlt=90',
    studioBanner: 'https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/iphone-15-pro-finish-select-202309-6-7inch-blacktitanium?wid=2560&hei=1440&fmt=p-jpg&qlt=85',
    specularGradient: 'radial-gradient(circle at 50% 50%, rgba(50, 50, 55, 0.15) 0%, transparent 70%)',
    desc: 'Deep obsidian matte rear glass paired with diamond-like carbon PVD coated frame.',
    hotspots: [
      { id: 'titanium', title: 'Black PVD Titanium Frame', top: '50%', left: '26%', desc: 'Sub-micron vaporization layer with extreme scratch resistance.' },
      { id: 'camera', title: '48MP Pro Triple Sapphire Array', top: '24%', left: '38%', desc: '24mm, 28mm, 35mm focal lengths calibrated. 100% optical pass.' },
      { id: 'a17', title: 'Apple A17 Pro (3nm Node)', top: '44%', left: '48%', desc: 'Pro-class 6-Core GPU. Hardware ray tracing verified at 100% load.' },
      { id: 'battery', title: '98% OEM Battery Capacity', top: '64%', left: '48%', desc: '184 full cycles logged. Zero voltage sag under benchmark.' },
    ],
  },
  white: {
    name: 'White Titanium',
    tag: 'Ceramic-Infused Glass',
    colorHex: '#e5e3de',
    heroImage: 'https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/iphone-15-pro-max-whitetitanium-select?wid=940&hei=1112&fmt=p-jpg&qlt=90',
    studioBanner: 'https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/iphone-15-pro-finish-select-202309-6-7inch-whitetitanium?wid=2560&hei=1440&fmt=p-jpg&qlt=85',
    specularGradient: 'radial-gradient(circle at 50% 50%, rgba(229, 227, 222, 0.15) 0%, transparent 70%)',
    desc: 'Brilliant white ceramic-doped glass rear with brushed metallic silver perimeter.',
    hotspots: [
      { id: 'titanium', title: 'Satin Silver Titanium Bezel', top: '50%', left: '26%', desc: 'Pure metallic luster paired with white ceramic shield.' },
      { id: 'camera', title: '48MP Pro Triple Sapphire Array', top: '24%', left: '38%', desc: '24mm, 28mm, 35mm focal lengths calibrated. 100% optical pass.' },
      { id: 'a17', title: 'Apple A17 Pro (3nm Node)', top: '44%', left: '48%', desc: 'Pro-class 6-Core GPU. Hardware ray tracing verified at 100% load.' },
      { id: 'battery', title: '98% OEM Battery Capacity', top: '64%', left: '48%', desc: '184 full cycles logged. Zero voltage sag under benchmark.' },
    ],
  },
  blue: {
    name: 'Blue Titanium',
    tag: 'Anodized Midnight Indigo',
    colorHex: '#3b4754',
    heroImage: 'https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/iphone-15-pro-max-bluetitanium-select?wid=940&hei=1112&fmt=p-jpg&qlt=90',
    studioBanner: 'https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/iphone-15-pro-finish-select-202309-6-7inch-bluetitanium?wid=2560&hei=1440&fmt=p-jpg&qlt=85',
    specularGradient: 'radial-gradient(circle at 50% 50%, rgba(59, 71, 84, 0.15) 0%, transparent 70%)',
    desc: 'Electrochemical deep midnight indigo hue with dark sapphire lens housing.',
    hotspots: [
      { id: 'titanium', title: 'Anodized Blue Titanium Frame', top: '50%', left: '26%', desc: 'Electrochemical color oxidation for deep, permanent tone.' },
      { id: 'camera', title: '48MP Pro Triple Sapphire Array', top: '24%', left: '38%', desc: '24mm, 28mm, 35mm focal lengths calibrated. 100% optical pass.' },
      { id: 'a17', title: 'Apple A17 Pro (3nm Node)', top: '44%', left: '48%', desc: 'Pro-class 6-Core GPU. Hardware ray tracing verified at 100% load.' },
      { id: 'battery', title: '98% OEM Battery Capacity', top: '64%', left: '48%', desc: '184 full cycles logged. Zero voltage sag under benchmark.' },
    ],
  },
};

type ViewMode = 'studio-duo' | 'single' | 'camera-zoom' | 'chipset-zoom';

export function RotatingHardwareShowcase() {
  const [selectedColor, setSelectedColor] = useState<keyof typeof APPLE_FINISHES>('natural');
  const [viewMode, setViewMode] = useState<ViewMode>('studio-duo');
  const [activeHotspot, setActiveHotspot] = useState<any>(null);

  const current = APPLE_FINISHES[selectedColor];

  return (
    <div className="w-full bg-[#fbfbfd] border border-black/[0.08] rounded-3xl text-[#1d1d1f] select-none overflow-hidden font-sans shadow-sm">
      
      {/* Editorial Header */}
      <div className="p-8 sm:p-12 pb-0 flex flex-col lg:flex-row lg:items-end justify-between gap-6 border-b border-black/[0.06] pb-8">
        <div>
          <div className="text-xs font-mono tracking-widest text-[#86868b] uppercase mb-2 flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-[#0071e3] animate-pulse" />
            Apple Hardware Laboratory [Grade A+ Inspection]
          </div>
          <h2 className="text-3xl sm:text-5xl font-semibold tracking-tight text-[#1d1d1f] leading-[1.08]">
            iPhone 15 Pro Max. <br />
            <span className="text-[#86868b]">Forged in Titanium.</span>
          </h2>
          <p className="text-xs sm:text-sm text-[#86868b] mt-3 max-w-xl leading-relaxed">
            {current.desc}
          </p>
        </div>

        {/* Finish Selector & Mode Controls */}
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-1.5 border border-black/[0.08] p-1 bg-[#f5f5f7] rounded-full text-xs font-medium">
            <button
              onClick={() => setViewMode('studio-duo')}
              className={`px-4 py-1.5 rounded-full transition-all ${
                viewMode === 'studio-duo' ? 'bg-white text-black font-semibold shadow-sm' : 'text-[#86868b] hover:text-black'
              }`}
            >
              Duo Showcase
            </button>
            <button
              onClick={() => setViewMode('single')}
              className={`px-4 py-1.5 rounded-full transition-all ${
                viewMode === 'single' ? 'bg-white text-black font-semibold shadow-sm' : 'text-[#86868b] hover:text-black'
              }`}
            >
              Portrait View
            </button>
          </div>

          {/* Color Switcher */}
          <div className="flex items-center gap-3 p-1.5 bg-[#f5f5f7] border border-black/[0.08] rounded-full">
            {Object.entries(APPLE_FINISHES).map(([key, item]) => (
              <button
                key={key}
                onClick={() => {
                  setSelectedColor(key as any);
                  setActiveHotspot(null);
                }}
                className={`w-6 h-6 rounded-full transition-all ${
                  selectedColor === key
                    ? 'ring-2 ring-[#0071e3] ring-offset-2 ring-offset-white scale-110 shadow-sm'
                    : 'opacity-70 hover:opacity-100'
                }`}
                style={{ backgroundColor: item.colorHex }}
                title={item.name}
              />
            ))}
          </div>
        </div>
      </div>

      {/* Main Studio Viewport */}
      <div className="relative w-full aspect-[4/3] sm:aspect-[21/9] flex items-center justify-center p-6 sm:p-12 overflow-hidden bg-gradient-to-b from-[#ffffff] via-[#fafafc] to-[#f5f5f7]">
        
        {/* Specular Glow */}
        <div 
          className="absolute inset-0 pointer-events-none transition-all duration-700"
          style={{ background: current.specularGradient }}
        />

        {/* Left Telemetry Action Nodes */}
        <div className="absolute top-8 left-8 space-y-2 z-20">
          <button
            onClick={() => setViewMode(viewMode === 'camera-zoom' ? 'studio-duo' : 'camera-zoom')}
            className={`block px-3.5 py-1.5 text-xs font-mono border rounded-full backdrop-blur-md transition-all ${
              viewMode === 'camera-zoom'
                ? 'border-black bg-black text-white font-bold'
                : 'border-black/[0.08] bg-white/80 text-[#1d1d1f] hover:border-black/30'
            }`}
          >
            [01] 48MP Pro Triple Optics
          </button>
          <button
            onClick={() => setViewMode(viewMode === 'chipset-zoom' ? 'studio-duo' : 'chipset-zoom')}
            className={`block px-3.5 py-1.5 text-xs font-mono border rounded-full backdrop-blur-md transition-all ${
              viewMode === 'chipset-zoom'
                ? 'border-black bg-black text-white font-bold'
                : 'border-black/[0.08] bg-white/80 text-[#1d1d1f] hover:border-black/30'
            }`}
          >
            [02] Apple A17 Pro (3nm Node)
          </button>
          <button
            onClick={() => {
              setActiveHotspot(current.hotspots[3]);
              setViewMode('studio-duo');
            }}
            className={`block px-3.5 py-1.5 text-xs font-mono border rounded-full backdrop-blur-md transition-all ${
              activeHotspot?.id === 'battery'
                ? 'border-black bg-black text-white font-bold'
                : 'border-black/[0.08] bg-white/80 text-[#1d1d1f] hover:border-black/30'
            }`}
          >
            [03] 98% OEM Battery Health
          </button>
        </div>

        {/* Real Apple Hardware Media Stage with Fluid Zoom Transitions */}
        <AnimatePresence mode="wait">
          <motion.div
            key={selectedColor + viewMode}
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ 
              opacity: 1, 
              scale: viewMode === 'camera-zoom' ? 1.55 : viewMode === 'chipset-zoom' ? 1.35 : 1,
              x: viewMode === 'camera-zoom' ? 100 : viewMode === 'chipset-zoom' ? -40 : 0,
              y: viewMode === 'camera-zoom' ? 70 : 0,
            }}
            exit={{ opacity: 0, scale: 1.02 }}
            transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
            className="relative w-full max-w-4xl h-full flex items-center justify-center"
          >
            {viewMode === 'single' ? (
              <div className="relative h-full flex items-center justify-center">
                <img
                  src={current.heroImage}
                  alt={current.name}
                  className="max-h-full max-w-full object-contain pointer-events-none drop-shadow-[0_20px_40px_rgba(0,0,0,0.12)]"
                />
              </div>
            ) : (
              <div className="relative w-full h-full flex items-center justify-center">
                <img
                  src={current.studioBanner}
                  alt={current.name}
                  className="max-h-full max-w-full object-contain pointer-events-none drop-shadow-[0_25px_50px_rgba(0,0,0,0.15)]"
                />

                {/* Hotspot 1: Camera */}
                <button
                  onClick={(e) => { e.stopPropagation(); setActiveHotspot(current.hotspots[1]); }}
                  className="absolute top-[26%] left-[34%] z-20 group p-2 transition-transform hover:scale-125"
                >
                  <span className="relative flex h-6 w-6">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#0071e3] opacity-40"></span>
                    <span className="relative inline-flex rounded-full h-6 w-6 bg-[#0071e3] text-white text-[10px] font-mono font-bold items-center justify-center shadow-lg">
                      1
                    </span>
                  </span>
                </button>

                {/* Hotspot 2: Titanium Band */}
                <button
                  onClick={(e) => { e.stopPropagation(); setActiveHotspot(current.hotspots[0]); }}
                  className="absolute top-[48%] left-[22%] z-20 group p-2 transition-transform hover:scale-125"
                >
                  <span className="relative flex h-6 w-6">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#0071e3] opacity-40"></span>
                    <span className="relative inline-flex rounded-full h-6 w-6 bg-[#0071e3] text-white text-[10px] font-mono font-bold items-center justify-center shadow-lg">
                      2
                    </span>
                  </span>
                </button>

                {/* Hotspot 3: Silicon Core */}
                <button
                  onClick={(e) => { e.stopPropagation(); setActiveHotspot(current.hotspots[2]); }}
                  className="absolute top-[46%] left-[45%] z-20 group p-2 transition-transform hover:scale-125"
                >
                  <span className="relative flex h-6 w-6">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#0071e3] opacity-40"></span>
                    <span className="relative inline-flex rounded-full h-6 w-6 bg-[#0071e3] text-white text-[10px] font-mono font-bold items-center justify-center shadow-lg">
                      3
                    </span>
                  </span>
                </button>

                {/* Hotspot 4: Battery */}
                <button
                  onClick={(e) => { e.stopPropagation(); setActiveHotspot(current.hotspots[3]); }}
                  className="absolute bottom-[24%] left-[45%] z-20 group p-2 transition-transform hover:scale-125"
                >
                  <span className="relative flex h-6 w-6">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#34c759] opacity-40"></span>
                    <span className="relative inline-flex rounded-full h-6 w-6 bg-[#34c759] text-white text-[10px] font-mono font-bold items-center justify-center shadow-lg">
                      4
                    </span>
                  </span>
                </button>
              </div>
            )}
          </motion.div>
        </AnimatePresence>

        {/* Detail Telemetry Flyout */}
        <div className="absolute bottom-8 right-8 max-w-sm bg-white/95 border border-black/[0.08] p-6 rounded-2xl backdrop-blur-2xl text-xs space-y-2.5 z-30 shadow-xl text-[#1d1d1f]">
          <div className="flex justify-between items-center font-mono text-[10px] text-[#86868b] uppercase border-b border-black/[0.06] pb-2">
            <span>Hardware Diagnostic Matrix</span>
            <span className="text-[#34c759] font-bold">100% PASSED</span>
          </div>

          {activeHotspot ? (
            <div className="space-y-1.5 animate-in fade-in">
              <div className="font-bold text-[#1d1d1f] text-sm flex items-center gap-2">
                <CheckCircle2 className="h-4 w-4 text-[#34c759]" /> {activeHotspot.title}
              </div>
              <p className="text-[#86868b] leading-relaxed">
                {activeHotspot.desc}
              </p>
            </div>
          ) : (
            <div className="space-y-1.5 animate-in fade-in">
              <div className="font-bold text-[#1d1d1f] text-sm flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-[#0071e3]" /> Grade A+ Pristine Mint Certification
              </div>
              <p className="text-[#86868b] leading-relaxed">
                Unit Serial <strong className="text-[#1d1d1f] font-mono">F2LZ90K8MD6M</strong> passed all 50 physical, logic, and battery diagnostic gates.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Specification Bar */}
      <div className="p-6 sm:p-8 border-t border-black/[0.06] grid md:grid-cols-12 gap-6 items-center text-xs font-mono text-[#86868b] bg-[#fbfbfd]">
        <div className="md:col-span-6 flex items-center gap-4">
          <span>Model: <strong className="text-[#1d1d1f]">iPhone 15 Pro Max (6.7")</strong></span>
          <span>Finish: <strong className="text-[#1d1d1f]">{current.name}</strong></span>
        </div>

        <div className="md:col-span-6 flex items-center justify-between md:justify-end gap-6 text-[#86868b]">
          <span>Serial: <strong className="text-[#1d1d1f]">F2LZ90K8MD6M</strong></span>
          <span>Grade: <strong className="text-[#34c759] font-bold">A+ Pristine</strong></span>
          <span>Escrow: <strong className="text-[#1d1d1f]">Locked</strong></span>
        </div>
      </div>
    </div>
  );
}