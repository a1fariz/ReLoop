'use client';

import React, { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { motion, useInView } from 'framer-motion';
import { ArrowRight } from 'lucide-react';
import { apiErrorMessage, searchListings } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import type { ListingDto } from '@/types/api';

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
  // Real live listings — replaces the hardcoded showcase
  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: queryKeys.listings.search({ page: 0, size: 6, sort: 'newest' }),
    queryFn: () => searchListings({ page: 0, size: 6, sort: 'newest' }),
  });
  const showcaseItems: ListingDto[] = data?.items ?? [];
  const [slowLoad, setSlowLoad] = useState(false);

  useEffect(() => {
    if (!isPending) {
      setSlowLoad(false);
      return;
    }
    const timer = window.setTimeout(() => setSlowLoad(true), 8000);
    return () => window.clearTimeout(timer);
  }, [isPending]);

  return (
    <div className="min-h-screen bg-[#fafaf9] text-stone-950 ambient-light-mesh">

      {/* Hero Section with Staggered Reveal */}
      <section className="mx-auto grid max-w-7xl items-center gap-12 px-5 pb-16 pt-16 sm:px-8 sm:pt-24 lg:grid-cols-[0.8fr_1.2fr] lg:gap-16 lg:pb-24">
        <div className="max-w-2xl text-center lg:text-left">
          <FadeInView>
            <h1 className="text-5xl font-semibold tracking-[-0.06em] text-stone-950 leading-[0.98] sm:text-7xl lg:text-[5.5rem]">
              {t('home_title_1')} <br />
              <span className="text-stone-500">{t('home_title_2')}</span>
            </h1>
          </FadeInView>
          <FadeInView delay={0.15}>
            <p className="mx-auto mt-7 max-w-xl text-base leading-7 text-stone-600 sm:text-lg lg:mx-0">{t('home_hero_desc')}</p>
          </FadeInView>
          <FadeInView delay={0.25}>
            <div className="mt-8 flex flex-col items-center gap-3 sm:flex-row lg:justify-start">
              <Link href="/catalog" className="btn-primary-dark w-full px-8 py-3.5 text-xs sm:w-auto">{t('home_browse')}</Link>
              <Link href="/trade-in" className="min-h-11 w-full rounded-full border border-stone-300 bg-white px-8 py-3.5 text-xs font-semibold text-stone-800 shadow-sm transition-all hover:-translate-y-0.5 hover:bg-stone-50 sm:w-auto">{t('home_valuation')}</Link>
            </div>
          </FadeInView>
        </div>
        <FadeInView delay={0.15} className="min-w-0">
          <div className="relative overflow-hidden rounded-[2rem] border border-stone-200 bg-white p-4 shadow-sm sm:p-6">
            <div className="aspect-[4/3] overflow-hidden rounded-[1.5rem] bg-stone-100">
              {showcaseItems[0] && parseImages(showcaseItems[0].images)[0] ? <img src={parseImages(showcaseItems[0].images)[0]} alt={showcaseItems[0].title} className="h-full w-full object-contain p-8" loading="lazy" decoding="async" /> : <div className="flex h-full items-center justify-center text-sm text-stone-500">{t('home_empty')}</div>}
            </div>
            {showcaseItems[0] && <div className="flex items-end justify-between gap-4 px-2 pt-5"><div><p className="text-xs font-mono uppercase tracking-wider text-stone-500">{t('common_grade')} {showcaseItems[0].gradeSnapshot}</p><h2 className="mt-1 line-clamp-2 text-lg font-bold text-stone-950">{showcaseItems[0].title}</h2></div><Link href={`/catalog/${showcaseItems[0].id}`} className="shrink-0 text-sm font-semibold text-sky-800">{t('catalog_inspect')} <ArrowRight className="inline h-4 w-4" /></Link></div>}
          </div>
        </FadeInView>
      </section>

      {/* Live listings grid */}
      <section className="container mx-auto px-6 max-w-7xl pb-28">
        <FadeInView>
          <div className="flex items-end justify-between gap-4 mb-10 pb-5 border-b border-stone-200">
            <div>
              <h2 className="text-3xl font-semibold tracking-tight text-stone-950 mt-2">{t('home_showcase')}</h2>
            </div>
            <Link href="/catalog" className="editorial-link text-sky-800 font-semibold">
              {t('home_view_all')} <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          </div>
        </FadeInView>

        {isPending && <div>{slowLoad && <p className="mb-4 rounded-2xl border border-sky-200 bg-sky-50 p-3 text-center text-xs font-medium text-sky-800">{t('home_waking')}</p>}<div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">{Array.from({ length: 3 }).map((_, index) => <div key={index} className="luxury-card animate-pulse rounded-3xl p-6"><div className="aspect-[4/3] rounded-2xl bg-stone-200" /><div className="mt-6 h-3 w-24 rounded bg-stone-200" /><div className="mt-3 h-6 w-3/4 rounded bg-stone-200" /><div className="mt-6 h-10 rounded bg-stone-200" /></div>)}</div></div>}

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
                          loading="lazy"
                          decoding="async"
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

      <section className="border-t border-stone-200 bg-stone-100/80 py-20 sm:py-24">
        <div className="mx-auto grid max-w-6xl gap-4 px-5 sm:px-8 md:grid-cols-3">
          {[
            ['01', 'Inspect before buying', 'See condition, grade, and available evidence before you commit.'],
            ['02', 'Reserve at checkout', 'The server confirms availability when you start checkout.'],
            ['03', 'Track after purchase', 'Orders, warranty, returns, and notifications stay in one place.'],
          ].map(([number, title, description], index) => <FadeInView key={number} delay={index * 0.08}><div className="h-full rounded-3xl border border-stone-200 bg-white p-6 shadow-sm"><div className="font-mono text-xs text-amber-700">[{number}]</div><h3 className="mt-8 text-lg font-bold text-stone-950">{title}</h3><p className="mt-2 text-sm leading-6 text-stone-600">{description}</p></div></FadeInView>)}
        </div>
      </section>
    </div>
  );
}
