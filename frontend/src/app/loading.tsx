import { RefreshCw } from 'lucide-react';

export default function Loading() {
  return (
    <div className="min-h-[60vh] flex items-center justify-center bg-[#fafafa]">
      <div className="flex flex-col items-center gap-4">
        <div className="h-14 w-14 rounded-2xl bg-zinc-100 border border-zinc-200 flex items-center justify-center">
          <RefreshCw className="h-6 w-6 text-zinc-400 animate-spin" />
        </div>
        <span className="font-mono text-xs text-zinc-500">Loading…</span>
      </div>
    </div>
  );
}
