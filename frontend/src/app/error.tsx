'use client';

import React from 'react';
import Link from 'next/link';
import { AlertCircle, RefreshCw } from 'lucide-react';
import { useT } from '@/lib/i18n';

export default function GlobalError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const t = useT();
  return (
    <div role="alert" className="flex min-h-[70vh] items-center justify-center bg-[#fafaf9] px-6">
      <div className="max-w-md space-y-6 text-center">
        <div className="flex justify-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-3xl border border-red-100 bg-red-50">
            <AlertCircle className="h-8 w-8 text-red-400" />
          </div>
        </div>
        <div className="space-y-2">
          <div className="text-xs font-mono uppercase tracking-widest text-sky-800 font-semibold">{t('error_title')}</div>
          <h1 className="text-3xl font-semibold tracking-tight text-stone-900">{t('error_heading')}</h1>
          <p className="text-sm leading-6 text-stone-600">{t('error_body')}</p>
          {error.digest && <p className="text-xs font-mono text-stone-400">ID: {error.digest}</p>}
        </div>
        <div className="flex flex-col items-center justify-center gap-3 sm:flex-row">
          <button onClick={reset} className="btn-blue inline-flex items-center gap-2 px-6 py-3 text-xs">
            <RefreshCw className="h-3.5 w-3.5" /> {t('common_try_again')}
          </button>
          <Link href="/" className="rounded-full border border-stone-300 px-6 py-3 text-xs font-semibold text-stone-700 transition-colors hover:border-stone-400">
            {t('error_home')}
          </Link>
        </div>
      </div>
    </div>
  );
}