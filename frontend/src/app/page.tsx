'use client';

import React, { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { motion, useInView } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { ArrowRight } from 'lucide-react';
import { RotatingHardwareShowcase } from '@/components/RotatingHardwareShowcase';
import { TelemetryStreamHUD } from '@/components/TelemetryStreamHUD';
import { apiErrorMessage, searchListings } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import type { ListingDto } from '@/types/api';

function useCounter(end: number, duration = 2000, startOnView = true) {
  const [count, setCount] = useState(0);
  const ref = useRef<HTMLDivElement>(null);
  const isInView = useInView(ref, { once: true, margin: '-50px' });

  useEffect(() => {
    if (!startOnView || !isInView) return;
    let startTime = 0;
    const animate = (currentTime: number) => {
      if (!startTime) startTime = currentTime;
      const progress = Math.min((currentTime - startTime) / duration, 1);
      setCount(Math.floor(progress * end));
      if (progress < 1) requestAnimationFrame(animate);
    };
    requestAnimationFrame(animate);
  }, [isInView, end, duration, startOnView]);

  return { count, ref };
}

function FadeInView({ children, delay = 0, className = '' }: { children: React.ReactNode; delay?: number; className?: string }) {
  const ref = useRef(null);
  const isInView = useInView(ref, { once: true, margin: '-60px' });

  return (
    <motion.div
      ref={ref}
      initial={{ opacity: 0, y: 30 }}
      animate={isInView ? { opacity: 1, y: 0 } : { opacity: 0, y: 30 }}
      transition={{ duration: 0.7, delay, ease: [0.16, 1, 0.3, 1] }}
      className={className}
    >
      {children}
    </motion.div>
  );
}

function parseImages(images: string | null): string[] {
  try {
    const parsed = JSON.parse(images ?? '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export default function HomePage() {
  const t = useT();
  const stat1 = useCounter(50, 1500);
  const stat2 = useCounter(15, 1200);
  const stat3 = useCounter(100, 1800);
  const stat4 = useCounter(0, 800);

  // Real live listings — replaces the hardcoded showcase
  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: queryKeys.listings.search({ page: 0, size: 6, sort: 'newest' }),
    queryFn: () => searchListings({ page: 0, size: 6, sort: 'newest' }),
  });
  const showcaseItems: ListingDto[] = data?.items ?? [];

  return (
    <div className="min-h-screen bg-[#fafaf9] text-stone-950 ambient-light-mesh">

      {/* Hero Section with Staggered Reveal */}
      <section className="container mx-auto px-6 max-w-7xl pt-20 sm:pt-28 pb-16 text-center">
        <div className="space-y-6 max-w-4xl mx-auto">
          <FadeInView>
            <div className="inline-flex items-center gap-2 rounded-full border border-amber-200 bg-amber-50/80 px-4 py-2 text-[11px] font-bold uppercase tracking-[0.14em] text-amber-800">
              <span className="h-2 w-2 rounded-full bg-amber-600 animate-pulse" aria-hidden="true" />
              {t('home_badge')}
            </div>
          </FadeInView>

          <FadeInView delay={0.1}>
            <h1 className="text-5xl sm:text-7xl font-semibold tracking-[-0.055em] text-stone-950 leading-[1.02]">
              {t('home_title_1')} <br />
              <span className="bg-gradient-to-r from-stone-500 via-amber-700 to-stone-500 bg-clip-text text-transparent">{t('home_title_2')}</span>
            </h1>
          </FadeInView>

          <FadeInView delay={0.2}>
            <p className="text-base sm:text-lg text-stone-600 max-w-2xl mx-auto leading-relaxed">
              {t('home_hero_desc')}
            </p>
          </FadeInView>

          <FadeInView delay={0.3}>
            <div className="pt-4 flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link
                href="/catalog"
                className="btn-primary-dark px-8 py-3.5 text-xs font-semibold shadow-md"
              >
                {t('home_browse')}
              </Link>
              <Link
                href="/trade-in"
                className="min-h-11 rounded-full border border-stone-300 bg-white px-8 py-3.5 text-xs font-semibold text-stone-800 shadow-sm transition-all hover:-translate-y-0.5 hover:border-stone-400 hover:bg-stone-50"
              >
                {t('home_valuation')}
              </Link>
            </div>
          </FadeInView>
        </div>
      </section>

      {/* Hardware Showcase */}
      <FadeInView>
        <section className="container mx-auto px-6 max-w-7xl pb-20">
          <RotatingHardwareShowcase />
        </section>
      </FadeInView>

      {/* Live Telemetry */}
      <FadeInView>
        <section className="container mx-auto px-6 max-w-7xl pb-24">
          <TelemetryStreamHUD />
        </section>
      </FadeInView>

      {/* Live listings grid */}
      <section className="container mx-auto px-6 max-w-7xl pb-28">
        <FadeInView>
          <div className="flex items-end justify-between gap-4 mb-10 pb-5 border-b border-stone-200">
            <div>
              <div className="text-[11px] font-mono uppercase tracking-[0.18em] text-amber-700">{t('home_badge')}</div>
              <h2 className="text-3xl font-semibold tracking-tight text-stone-950 mt-2">{t('home_showcase')}</h2>
            </div>
            <Link href="/catalog" className="editorial-link text-sky-800 font-semibold">
              {t('home_view_all')} <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          </div>
        </FadeInView>

        {isPending && <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">{Array.from({ length: 3 }).map((_, index) => <div key={index} className="luxury-card animate-pulse rounded-3xl p-6"><div className="aspect-[4/3] rounded-2xl bg-stone-200" /><div className="mt-6 h-3 w-24 rounded bg-stone-200" /><div className="mt-3 h-6 w-3/4 rounded bg-stone-200" /><div className="mt-6 h-10 rounded bg-stone-200" /></div>)}</div>}

        {isError && <div role="alert" className="rounded-3xl border border-red-200 bg-red-50 p-8 text-center text-sm text-red-800"><p>{apiErrorMessage(error)}</p><button onClick={() => void refetch()} className="btn-primary-dark mt-4 px-5 py-3 text-xs">{t('common_try_again')}</button></div>}

        {!isPending && !isError && <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
          {showcaseItems.map((item, index) => {
            const image = parseImages(item.images)[0];
            return (
              <FadeInView key={item.id} delay={index * 0.08}>
                <Link
                  href={`/catalog/${item.id}`}
                  className="luxury-card rounded-3xl p-8 flex flex-col justify-between group overflow-hidden h-full"
                >
                  <div>
                    <div className="aspect-[4/3] w-full rounded-2xl overflow-hidden bg-stone-100 relative mb-6 flex items-center justify-center p-4">
                      {image ? (
                        <img
                          src={image}
                          alt={item.title}
                          className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-700 ease-out drop-shadow-md"
                        />
                      ) : (
                        <div className="text-4xl font-mono text-zinc-300">RL</div>
                      )}
                      <div className="absolute top-4 left-4 bg-white/90 backdrop-blur-md px-3 py-1.5 text-[11px] font-mono font-semibold rounded-full border border-stone-200 text-stone-800 shadow-sm">
                        {t('common_grade')} {item.gradeSnapshot}
                      </div>
                    </div>

                    <div className="text-[11px] font-mono text-stone-500 uppercase tracking-[0.12em] mb-1">
                      {t('detail_seller')} #{item.sellerId} • {t('detail_unit')} {item.unitId.slice(0, 8)}
                    </div>
                    <h3 className="text-xl font-bold text-stone-950 group-hover:text-sky-800 transition-colors duration-200 mb-2">
                      {item.title}
                    </h3>
                    <p className="text-sm text-stone-600 line-clamp-2 leading-relaxed">
                      {item.description ?? t('catalog_default_desc')}
                    </p>
                  </div>

                  <div className="pt-6 border-t border-stone-200 mt-6 flex items-baseline justify-between">
                    <div>
                      <div className="text-[10px] uppercase font-mono tracking-[0.12em] text-stone-500">{t('catalog_certified_price')}</div>
                      <div className="text-xl font-bold text-stone-950 font-mono tnum mt-0.5">
                        Rp {item.askingPrice.toLocaleString('id-ID')}
                      </div>
                    </div>
                    <span className="text-xs font-semibold text-sky-800 group-hover:translate-x-1 transition-transform duration-200 flex items-center gap-1">
                      Inspect <ArrowRight className="h-3.5 w-3.5" />
                    </span>
                  </div>
                </Link>
              </FadeInView>
            );
          })}
        </div>}

        {data && showcaseItems.length === 0 && (
          <div className="p-12 rounded-3xl border border-black/[0.06] bg-[#f5f5f7] text-center text-sm text-[#86868b]">
            {t('home_empty')}
          </div>
        )}
      </section>

      {/* Animated KPI Protocol Pillars */}
      <section className="bg-stone-100/80 border-t border-stone-200 py-24">
        <div className="container mx-auto px-6 max-w-6xl">
          <div className="grid md:grid-cols-4 gap-6">
            <FadeInView delay={0}>
              <div ref={stat1.ref} className="p-6 rounded-3xl bg-white border border-stone-200 space-y-2 shadow-sm text-center">
                <div className="text-3xl sm:text-4xl font-bold text-zinc-900 font-mono tnum">{stat1.count}</div>
                <div className="text-xs text-[#86868b] font-semibold">Point Diagnostic Gates</div>
              </div>
            </FadeInView>

            <FadeInView delay={0.1}>
              <div ref={stat2.ref} className="p-6 rounded-3xl bg-white border border-stone-200 space-y-2 shadow-sm text-center">
                <div className="text-3xl sm:text-4xl font-bold text-zinc-900 font-mono tnum">{stat2.count} <span className="text-lg text-[#86868b]">min</span></div>
                <div className="text-xs text-[#86868b] font-semibold">Row-Locked Lease</div>
              </div>
            </FadeInView>

            <FadeInView delay={0.2}>
              <div ref={stat3.ref} className="p-6 rounded-3xl bg-white border border-stone-200 space-y-2 shadow-sm text-center">
                <div className="text-3xl sm:text-4xl font-bold text-zinc-900 font-mono tnum">{stat3.count}%</div>
                <div className="text-xs text-[#86868b] font-semibold">Double-Entry Escrow</div>
              </div>
            </FadeInView>

            <FadeInView delay={0.3}>
              <div ref={stat4.ref} className="p-6 rounded-3xl bg-white border border-stone-200 space-y-2 shadow-sm text-center">
                <div className="text-3xl sm:text-4xl font-bold text-emerald-600 font-mono tnum">{stat4.count}.00%</div>
                <div className="text-xs text-[#86868b] font-semibold">Unresolved Disputes</div>
              </div>
            </FadeInView>
          </div>
        </div>
      </section>
    </div>
  );
}
