'use client';

import React, { useState } from 'react';
import { ArrowLeftRight } from 'lucide-react';

export function GradeComparisonSlider() {
  const [sliderPosition, setSliderPosition] = useState(50);
  const [isDragging, setIsDragging] = useState(false);

  const updatePosition = (clientX: number, rect: DOMRect) => {
    const x = Math.max(0, Math.min(clientX - rect.left, rect.width));
    setSliderPosition((x / rect.width) * 100);
  };

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!isDragging) return;
    updatePosition(e.clientX, e.currentTarget.getBoundingClientRect());
  };

  const handleTouchMove = (e: React.TouchEvent<HTMLDivElement>) => {
    updatePosition(e.touches[0].clientX, e.currentTarget.getBoundingClientRect());
  };

  return (
    <div className="bg-white border border-zinc-200 rounded-3xl p-8 space-y-6 shadow-sm select-none">
      <div className="flex flex-col sm:flex-row sm:items-baseline justify-between border-b border-zinc-100 pb-4 gap-2">
        <div>
          <div className="text-[11px] font-mono uppercase tracking-widest text-sky-600 font-semibold">
            Visual Grading Calibration [Split-Screen Comparator]
          </div>
          <h3 className="text-lg font-bold text-zinc-900 mt-1">Grade A+ (Pristine) vs Grade B+ (Clean)</h3>
        </div>
        <div className="flex items-center gap-1.5 text-xs font-mono text-zinc-600 bg-zinc-50 px-3 py-1.5 rounded-full border border-zinc-200 w-fit">
          <ArrowLeftRight className="h-3.5 w-3.5" />
          <span>Drag to compare cosmetic contrast</span>
        </div>
      </div>

      {/* Interactive Split Viewport */}
      <div 
        className="w-full aspect-[16/9] rounded-2xl bg-zinc-100 relative overflow-hidden border border-zinc-200 cursor-ew-resize"
        onMouseDown={(e) => {
          setIsDragging(true);
          updatePosition(e.clientX, e.currentTarget.getBoundingClientRect());
        }}
        onMouseMove={handleMouseMove}
        onMouseUp={() => setIsDragging(false)}
        onMouseLeave={() => setIsDragging(false)}
        onTouchStart={handleTouchMove}
        onTouchMove={handleTouchMove}
      >
        {/* Right Background Image (Grade B+) */}
        <img
          src="https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=1200&auto=format&fit=crop&q=95"
          alt="Grade B+ Unit"
          className="absolute inset-0 w-full h-full object-cover"
        />
        <div className="absolute top-4 right-4 bg-black/70 backdrop-blur-md px-3 py-1 text-[11px] font-mono font-bold border border-white/10 rounded-full text-white shadow-md">
          Grade B+ (Minor Hairline Wear)
        </div>

        {/* Left Clipped Image (Grade A+) */}
        <div 
          className="absolute inset-0 overflow-hidden transition-none"
          style={{ width: `${sliderPosition}%` }}
        >
          <img
            src="https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/iphone-15-pro-max-naturaltitanium-select?wid=940&hei=1112&fmt=p-jpg&qlt=90"
            alt="Grade A+ Unit"
            className="absolute inset-0 h-full object-cover object-right drop-shadow-xl"
            style={{ width: `${10000 / sliderPosition}%`, maxWidth: 'none' }}
          />
          <div className="absolute top-4 left-4 bg-white/90 backdrop-blur-md px-3 py-1 text-[11px] font-mono font-bold border border-zinc-200 rounded-full text-zinc-900 shadow-md">
            Grade A+ (Pristine Mint)
          </div>
        </div>

        {/* Divider Line */}
        <div 
          className="absolute top-0 bottom-0 w-0.5 bg-sky-600 shadow-lg pointer-events-none z-20"
          style={{ left: `${sliderPosition}%` }}
        >
          <div className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 w-9 h-9 bg-white text-sky-600 rounded-full flex items-center justify-center shadow-2xl border border-zinc-200">
            <ArrowLeftRight className="h-3.5 w-3.5 stroke-[2.5]" />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 text-xs">
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-100">
          <span className="text-emerald-700 font-bold block mb-1">Grade A+ Specification:</span>
          <span className="text-zinc-600 leading-relaxed">Zero visible blemishes from 20cm distance. OEM screen & battery &gt;95%.</span>
        </div>
        <div className="p-4 rounded-2xl bg-amber-50 border border-amber-100">
          <span className="text-amber-700 font-bold block mb-1">Grade B+ Specification:</span>
          <span className="text-zinc-600 leading-relaxed">Micro-scratches on bezel only. Zero display cracks. 100% technical pass.</span>
        </div>
      </div>
    </div>
  );
}