'use client';

import React from 'react';
import Link from 'next/link';
import { useT } from '@/lib/i18n';

export function Footer() {
  const t = useT();
  return (
    <footer className="border-t border-black/[0.08] bg-[#f5f5f7] text-[#86868b] text-xs py-16">
      <div className="container mx-auto px-4 sm:px-6 max-w-7xl">
         <div className="grid grid-cols-2 md:grid-cols-5 gap-8 lg:gap-10 mb-14">
          <div className="col-span-2 space-y-4">
            <Link href="/" className="flex items-center gap-2 font-bold text-sm tracking-tight text-[#1d1d1f]">
              <span className="font-mono text-xs uppercase tracking-widest text-[#0071e3]">[01]</span>
              <span>ReLoop Circular Platform</span>
            </Link>
            <p className="text-[#86868b] text-xs max-w-sm leading-relaxed">
              {t('footer_desc')}
            </p>
          </div>

          <div>
            <div className="font-bold text-[#1d1d1f] mb-4 text-xs uppercase tracking-widest font-mono">{t('footer_marketplace')}</div>
            <ul className="space-y-2.5">
              <li><Link href="/catalog" className="hover:text-[#1d1d1f] transition-colors">{t('footer_catalog')}</Link></li>
              <li><Link href="/trade-in" className="hover:text-[#1d1d1f] transition-colors">{t('nav_valuation')}</Link></li>
              <li><Link href="/catalog" className="hover:text-[#1d1d1f] transition-colors">{t('footer_grading')}</Link></li>
            </ul>
          </div>

          <div>
            <div className="font-bold text-[#1d1d1f] mb-4 text-xs uppercase tracking-widest font-mono">{t('footer_trust')}</div>
            <ul className="space-y-2.5">
              <li><Link href="/warranties" className="hover:text-[#1d1d1f] transition-colors">{t('footer_protection')}</Link></li>
              <li><Link href="/warranties" className="hover:text-[#1d1d1f] transition-colors">{t('footer_dispute')}</Link></li>
              <li><Link href="/seller" className="hover:text-[#1d1d1f] transition-colors">{t('footer_ledger')}</Link></li>
            </ul>
          </div>

          <div>
             <div className="font-bold text-[#1d1d1f] mb-4 text-xs uppercase tracking-widest font-mono">{t('footer_help')}</div>
             <ul className="space-y-2.5 text-sm">
               <li><Link href="/returns" className="hover:text-[#1d1d1f] transition-colors">{t('nav_returns')}</Link></li>
               <li><Link href="/warranties" className="hover:text-[#1d1d1f] transition-colors">{t('footer_protection')}</Link></li>
               <li><a href="mailto:alfarizi.developer@gmail.com" className="hover:text-[#1d1d1f] transition-colors">{t('footer_contact')}</a></li>
             </ul>
           </div>
        </div>

        <div className="border-t border-black/[0.08] pt-8 flex flex-col sm:flex-row items-center justify-between text-[#86868b] text-[11px] font-mono">
          <p>{t('footer_rights')}</p>
          <div className="mt-3 sm:mt-0 flex items-center gap-2 text-[#1d1d1f]">
            <span className="h-2 w-2 rounded-full bg-[#34c759]" />
            {t('footer_ops')}
          </div>
        </div>
      </div>
    </footer>
  );
}