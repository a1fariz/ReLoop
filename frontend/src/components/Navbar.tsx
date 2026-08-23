import React from 'react';
import Link from 'next/link';
import { RefreshCw, Search, ShieldCheck, ArrowRight, User } from 'lucide-react';

export function Navbar() {
  return (
    <header className="sticky top-0 z-50 w-full border-b border-border/60 bg-background/80 backdrop-blur-md">
      <div className="container mx-auto flex h-14 max-w-7xl items-center justify-between px-6">
        <div className="flex items-center gap-8">
          <Link href="/" className="flex items-center gap-2 font-semibold tracking-tight text-foreground">
            <div className="flex h-7 w-7 items-center justify-center rounded-md bg-foreground text-background">
              <RefreshCw className="h-4 w-4" />
            </div>
            <span className="text-base font-bold tracking-tight">ReLoop</span>
          </Link>

          <nav className="hidden md:flex items-center gap-6 text-xs font-medium text-muted-foreground">
            <Link href="/catalog" className="transition-colors hover:text-foreground">
              Marketplace
            </Link>
            <Link href="/trade-in" className="transition-colors hover:text-foreground">
              Valuation
            </Link>
            <Link href="/warranties" className="transition-colors hover:text-foreground">
              Guarantees
            </Link>
            <Link href="/seller" className="transition-colors hover:text-foreground">
              Seller Hub
            </Link>
          </nav>
        </div>

        <div className="flex items-center gap-3">
          <Link
            href="/catalog"
            className="hidden sm:flex items-center gap-2 rounded-full border border-border/80 bg-muted/40 px-3 py-1.5 text-xs text-muted-foreground hover:border-border hover:text-foreground transition-all"
          >
            <Search className="h-3.5 w-3.5" />
            <span>Search devices, serials...</span>
            <kbd className="rounded border border-border bg-background px-1.5 text-[10px] text-muted-foreground">⌘K</kbd>
          </Link>

          <Link
            href="/login"
            className="rounded-full border border-border/80 px-3.5 py-1 text-xs font-medium text-foreground hover:bg-muted transition-all"
          >
            Sign In
          </Link>
        </div>
      </div>
    </header>
  );
}
