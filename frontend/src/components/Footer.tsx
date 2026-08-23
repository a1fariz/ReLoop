import React from 'react';
import Link from 'next/link';

export function Footer() {
  return (
    <footer className="border-t border-border/40 bg-background text-muted-foreground text-xs py-12">
      <div className="container mx-auto px-6 max-w-7xl">
        <div className="grid grid-cols-2 md:grid-cols-5 gap-8 mb-12">
          <div className="col-span-2 space-y-3">
            <span className="font-semibold text-foreground text-sm tracking-tight">ReLoop Circular Platform</span>
            <p className="text-muted-foreground text-xs max-w-sm leading-relaxed">
              Authenticated circular commerce platform ensuring verified serialized electronics lifecycle provenance and double-entry escrow protection.
            </p>
          </div>

          <div>
            <div className="font-medium text-foreground mb-3 text-xs">Marketplace</div>
            <ul className="space-y-2">
              <li><Link href="/catalog" className="hover:text-foreground">Phones & Laptops</Link></li>
              <li><Link href="/trade-in" className="hover:text-foreground">Algorithmic Valuation</Link></li>
              <li><Link href="/catalog" className="hover:text-foreground">Grading Matrix</Link></li>
            </ul>
          </div>

          <div>
            <div className="font-medium text-foreground mb-3 text-xs">Trust & Escrow</div>
            <ul className="space-y-2">
              <li><Link href="/warranties" className="hover:text-foreground">Warranty Policies</Link></li>
              <li><Link href="/warranties" className="hover:text-foreground">Dispute Arbitration</Link></li>
              <li><Link href="/seller" className="hover:text-foreground">Ledger Accounting</Link></li>
            </ul>
          </div>

          <div>
            <div className="font-medium text-foreground mb-3 text-xs">Architecture</div>
            <ul className="space-y-2">
              <li><a href="https://github.com/a1fariz/ReLoop" target="_blank" rel="noreferrer" className="hover:text-foreground">Spring Modulith</a></li>
              <li><a href="https://github.com/a1fariz/ReLoop" target="_blank" rel="noreferrer" className="hover:text-foreground">PostgreSQL 16</a></li>
              <li><a href="https://github.com/a1fariz/ReLoop" target="_blank" rel="noreferrer" className="hover:text-foreground">Next.js 14</a></li>
            </ul>
          </div>
        </div>

        <div className="border-t border-border/40 pt-6 flex flex-col sm:flex-row items-center justify-between text-muted-foreground">
          <p>© 2026 ReLoop Platform. Production-grade circular commerce standard.</p>
          <p className="mt-2 sm:mt-0 font-mono text-[11px]">System Status: Operational • All Services UP</p>
        </div>
      </div>
    </footer>
  );
}
