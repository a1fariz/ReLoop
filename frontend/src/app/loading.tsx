import { RefreshCw } from 'lucide-react';

export default function Loading() {
  return (
    <div role="status" aria-live="polite" className="flex min-h-[60vh] items-center justify-center bg-[#fafaf9]">
      <div className="flex flex-col items-center gap-4">
        <div className="flex h-14 w-14 items-center justify-center rounded-2xl border border-stone-200 bg-stone-100">
          <RefreshCw className="h-6 w-6 animate-spin text-stone-400" />
        </div>
        <span className="font-mono text-xs text-stone-500">Loading…</span>
      </div>
    </div>
  );
}