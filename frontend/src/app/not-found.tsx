'use client';

import React from 'react';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { ArrowLeft, Search, RefreshCw } from 'lucide-react';
import { useT } from '@/lib/i18n';

export default function NotFound() {
  const t = useT();
  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 flex items-center justify-center px-6">
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
        className="max-w-md text-center space-y-6"
      >
        <div className="flex justify-center">
          <div className="w-16 h-16 rounded-3xl bg-zinc-100 border border-zinc-200 flex items-center justify-center">
            <RefreshCw className="h-8 w-8 text-zinc-400" />
          </div>
        </div>

        <div className="space-y-2">
          <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold">404 — {t('notfound_code')}</div>
          <h1 className="text-3xl sm:text-4xl font-semibold tracking-tight text-zinc-900">
            {t('notfound_title')}
          </h1>
          <p className="text-sm text-zinc-600 leading-relaxed max-w-sm mx-auto">
            {t('notfound_desc')}
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
          <Link href="/" className="btn-primary-dark px-6 py-3 text-xs shadow-md">
            <ArrowLeft className="h-3.5 w-3.5 inline mr-1.5" />
            {t('notfound_back')}
          </Link>
          <Link href="/catalog" className="rounded-full border border-zinc-300 hover:border-zinc-400 px-6 py-3 text-xs font-semibold transition-colors text-zinc-700">
            {t('nav_browse')}
          </Link>
        </div>
      </motion.div>
    </div>
  );
}