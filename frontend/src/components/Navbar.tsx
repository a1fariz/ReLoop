import React from 'react';
import Link from 'next/link';
import { RefreshCw, ShoppingBag, ShieldCheck, User, Store, Calculator, Award } from 'lucide-react';

export function Navbar() {
  return (
    <header className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <Link href="/" className="flex items-center gap-2.5 font-bold tracking-tight text-foreground hover:opacity-90">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-white shadow-sm">
            <RefreshCw className="h-5 w-5" />
          </div>
          <span className="text-xl font-black bg-gradient-to-r from-primary to-blue-400 bg-clip-text text-transparent">
            ReLoop
          </span>
        </Link>

        <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-muted-foreground">
          <Link href="/catalog" className="transition-colors hover:text-foreground">
            Marketplace
          </Link>
          <Link href="/trade-in" className="transition-colors hover:text-foreground flex items-center gap-1.5">
            <Calculator className="h-4 w-4 text-primary" />
            Trade-In Valuation
          </Link>
          <Link href="/warranties" className="transition-colors hover:text-foreground flex items-center gap-1.5">
            <ShieldCheck className="h-4 w-4 text-accent" />
            Warranty & Disputes
          </Link>
          <Link href="/seller" className="transition-colors hover:text-foreground flex items-center gap-1.5">
            <Store className="h-4 w-4 text-blue-400" />
            Seller Hub
          </Link>
        </nav>

        <div className="flex items-center gap-3">
          <Link
            href="/login"
            className="rounded-lg border border-border px-3.5 py-1.5 text-sm font-semibold text-foreground hover:bg-muted transition-all"
          >
            Sign In
          </Link>
          <Link
            href="/register"
            className="rounded-lg bg-primary px-3.5 py-1.5 text-sm font-semibold text-primary-foreground shadow-sm hover:bg-primary-hover transition-all"
          >
            Join ReLoop
          </Link>
        </div>
      </div>
    </header>
  );
}
