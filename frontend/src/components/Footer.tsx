import React from 'react';
import Link from 'next/link';
import { RefreshCw, ShieldCheck, Lock, Award, Heart } from 'lucide-react';

export function Footer() {
  return (
    <footer className="border-t border-border bg-card text-muted-foreground text-sm py-12">
      <div className="container mx-auto px-4 max-w-6xl">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-foreground font-bold">
              <RefreshCw className="h-5 w-5 text-primary" />
              <span className="text-lg">ReLoop Platform</span>
            </div>
            <p className="text-xs leading-relaxed">
              Authenticated circular commerce platform ensuring verified serialized electronics lifecycle provenance and double-entry escrow protection.
            </p>
          </div>

          <div>
            <h4 className="font-semibold text-foreground mb-3 text-xs uppercase tracking-wider">Marketplace</h4>
            <ul className="space-y-2 text-xs">
              <li><Link href="/catalog" className="hover:text-foreground">Certified Smart Devices</Link></li>
              <li><Link href="/trade-in" className="hover:text-foreground">Algorithmic Valuation</Link></li>
              <li><Link href="/catalog" className="hover:text-foreground">Grading Standards (A+ to D)</Link></li>
            </ul>
          </div>

          <div>
            <h4 className="font-semibold text-foreground mb-3 text-xs uppercase tracking-wider">Trust & Operations</h4>
            <ul className="space-y-2 text-xs">
              <li><Link href="/warranties" className="hover:text-foreground">Warranty Protection</Link></li>
              <li><Link href="/warranties" className="hover:text-foreground">Arbitrated Dispute Resolution</Link></li>
              <li><Link href="/seller" className="hover:text-foreground">Double-Entry Financial Ledger</Link></li>
            </ul>
          </div>

          <div>
            <h4 className="font-semibold text-foreground mb-3 text-xs uppercase tracking-wider">Enterprise Security</h4>
            <div className="space-y-2 text-xs">
              <div className="flex items-center gap-1.5 text-foreground font-medium">
                <Lock className="h-3.5 w-3.5 text-primary" /> 15-Minute Anti-Hoarding Leases
              </div>
              <div className="flex items-center gap-1.5 text-foreground font-medium">
                <ShieldCheck className="h-3.5 w-3.5 text-accent" /> Guaranteed Escrow Settlement
              </div>
            </div>
          </div>
        </div>

        <div className="border-t border-border pt-6 flex flex-col md:flex-row items-center justify-between text-xs">
          <p>© 2026 ReLoop Platform Inc. All rights reserved.</p>
          <p className="mt-2 md:mt-0 flex items-center gap-1">
            Engineered with Spring Modulith, PostgreSQL & Next.js
          </p>
        </div>
      </div>
    </footer>
  );
}
