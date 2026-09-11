'use client';

import React from 'react';
import Link from 'next/link';
import { AlertCircle, RefreshCw } from 'lucide-react';

export default function GlobalError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <div className="min-h-[70vh] flex items-center justify-center bg-[#fafafa] px-6">
      <div className="max-w-md text-center space-y-6">
        <div className="flex justify-center">
          <div className="h-16 w-16 rounded-3xl bg-red-50 border border-red-100 flex items-center justify-center">
            <AlertCircle className="h-8 w-8 text-red-400" />
          </div>
        </div>
        <div className="space-y-2">
          <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold">Error</div>
          <h1 className="text-3xl font-semibold tracking-tight text-zinc-900">Something went wrong</h1>
          <p className="text-sm text-zinc-600 leading-relaxed">
            {error.message || 'An unexpected error occurred while loading this page.'}
          </p>
        </div>
        <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
          <button onClick={reset} className="btn-blue px-6 py-3 text-xs inline-flex items-center gap-2">
            <RefreshCw className="h-3.5 w-3.5" /> Try Again
          </button>
          <Link href="/" className="rounded-full border border-zinc-300 hover:border-zinc-400 px-6 py-3 text-xs font-semibold text-zinc-700 transition-colors">
            Back to Home
          </Link>
        </div>
      </div>
    </div>
  );
}
