'use client';

import React from 'react';
import Link from 'next/link';
import { ArrowRight, ShieldCheck, Cpu, RefreshCw, CheckCircle2, Award, Zap, ChevronRight } from 'lucide-react';

export default function HomePage() {
  const showcaseItems = [
    {
      id: 'dddddddd-dddd-dddd-dddd-dddddddddddd',
      category: 'Flagship Smartphone',
      brand: 'Apple',
      title: 'iPhone 15 Pro 256GB',
      specs: 'Natural Titanium • A17 Pro • 98% Battery',
      price: 17500000,
      grade: 'A+',
      badge: 'Pristine Mint',
      serial: 'F2LZ90K8MD6M',
      image: 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
      category: 'Pro Laptop',
      brand: 'Apple',
      title: 'MacBook Air M2 16GB / 512GB',
      specs: 'Midnight • 8-Core GPU • 94% Battery',
      price: 15800000,
      grade: 'A',
      badge: 'Near Mint',
      serial: 'C02G89A3MD6T',
      image: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: 'ffffffff-ffff-ffff-ffff-ffffffffffff',
      category: 'Business Workstation',
      brand: 'Lenovo',
      title: 'ThinkPad X1 Carbon Gen 10',
      specs: 'Intel Core i7-1260P • 16GB RAM • 512GB SSD',
      price: 12900000,
      grade: 'B+',
      badge: 'Certified Clean',
      serial: 'PF3A9812ZK09',
      image: 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: '44444444-4444-4444-4444-444444444444',
      category: 'Flagship Tablet',
      brand: 'Apple',
      title: 'iPad Pro 11" M2 128GB Wi-Fi',
      specs: 'Space Gray • Liquid Retina • 99% Battery',
      price: 11200000,
      grade: 'A+',
      badge: 'Open Box',
      serial: 'DNPZ883K10',
      image: 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: '55555555-5555-5555-5555-555555555555',
      category: 'Pro Camera Phone',
      brand: 'Samsung',
      title: 'Galaxy S24 Ultra 512GB',
      specs: 'Titanium Gray • Snapdragon 8 Gen 3 • S-Pen',
      price: 16900000,
      grade: 'A+',
      badge: 'Like New',
      serial: 'R5CW109LMN',
      image: 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: '66666666-6666-6666-6666-666666666666',
      category: 'Creator Audio',
      brand: 'Sony',
      title: 'Sony WH-1000XM5 Wireless ANC',
      specs: 'Silver • Industry Leading Noise Canceling',
      price: 3850000,
      grade: 'A',
      badge: 'Verified Audio',
      serial: 'SN-WH5-9921',
      image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80',
    },
  ];

  return (
    <div className="min-h-screen bg-background text-foreground">
      {/* Clean Minimalist Hero Section */}
      <section className="relative pt-24 pb-20 border-b border-border/40 overflow-hidden">
        <div className="container mx-auto px-6 max-w-5xl text-center">
          <div className="inline-flex items-center gap-2 rounded-full border border-border/80 bg-muted/30 px-3.5 py-1 text-xs font-medium text-foreground mb-8">
            <span className="h-1.5 w-1.5 rounded-full bg-accent animate-pulse" />
            Serialized Circular Commerce Standard
          </div>

          <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-foreground max-w-3xl mx-auto leading-[1.15]">
            Certified pre-owned tech. <br />
            <span className="text-muted-foreground font-normal">Backed by immutable history.</span>
          </h1>

          <p className="mt-6 text-sm sm:text-base text-muted-foreground max-w-xl mx-auto leading-relaxed">
            Every single device undergoes a 50-point diagnostic inspection, unique serial lock, and double-entry escrow settlement.
          </p>

          <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-3">
            <Link
              href="/catalog"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-full bg-foreground px-6 py-2.5 text-xs font-semibold text-background hover:opacity-90 transition-all shadow-sm"
            >
              Explore Authenticated Inventory
              <ArrowRight className="h-3.5 w-3.5" />
            </Link>
            <Link
              href="/trade-in"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-full border border-border bg-card/60 px-6 py-2.5 text-xs font-medium hover:bg-muted transition-all"
            >
              Instant Trade-In Valuation
            </Link>
          </div>
        </div>
      </section>

      {/* Featured Showcase Grid */}
      <section className="py-20 container mx-auto px-6 max-w-7xl">
        <div className="flex flex-col sm:flex-row items-start sm:items-end justify-between mb-10 pb-4 border-b border-border/40 gap-4">
          <div>
            <div className="text-xs font-mono uppercase text-muted-foreground tracking-wider mb-1">Live Verified Inventory</div>
            <h2 className="text-2xl font-bold tracking-tight">Recent Certified Units</h2>
          </div>
          <Link href="/catalog" className="text-xs font-medium text-muted-foreground hover:text-foreground flex items-center gap-1">
            View all serialized devices <ChevronRight className="h-3.5 w-3.5" />
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {showcaseItems.map((item) => (
            <Link
              key={item.id}
              href={`/catalog/${item.id}`}
              className="group relative rounded-2xl border border-border/60 bg-card/40 p-5 hover:border-border transition-all duration-300 hover:shadow-sm flex flex-col justify-between"
            >
              <div>
                <div className="aspect-[4/3] w-full rounded-xl overflow-hidden bg-muted/40 mb-5 relative">
                  <img
                    src={item.image}
                    alt={item.title}
                    className="h-full w-full object-cover object-center group-hover:scale-105 transition-transform duration-500"
                  />
                  <div className="absolute top-3 left-3 flex items-center gap-1.5 rounded-full bg-background/90 backdrop-blur-md px-2.5 py-1 text-[11px] font-semibold text-foreground border border-border/40">
                    <Award className="h-3 w-3 text-accent" />
                    Grade {item.grade}
                  </div>
                  <div className="absolute top-3 right-3 rounded-md bg-background/80 backdrop-blur-md px-2 py-0.5 text-[10px] font-mono text-muted-foreground border border-border/40">
                    {item.badge}
                  </div>
                </div>

                <div className="text-[11px] font-mono text-muted-foreground uppercase tracking-wider mb-1.5">
                  {item.category} • {item.brand}
                </div>
                <h3 className="text-base font-semibold tracking-tight text-foreground group-hover:text-primary transition-colors mb-1.5">
                  {item.title}
                </h3>
                <p className="text-xs text-muted-foreground line-clamp-1 mb-4">
                  {item.specs}
                </p>
              </div>

              <div className="flex items-center justify-between pt-4 border-t border-border/40">
                <div>
                  <div className="text-[10px] text-muted-foreground">Certified Price</div>
                  <div className="text-lg font-bold text-foreground">
                    Rp {item.price.toLocaleString('id-ID')}
                  </div>
                </div>
                <span className="rounded-full bg-muted/80 px-3 py-1.5 text-xs font-medium text-foreground group-hover:bg-foreground group-hover:text-background transition-colors flex items-center gap-1">
                  Details <ArrowRight className="h-3 w-3" />
                </span>
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* Structural Invariants & Trust Pillar */}
      <section className="py-20 border-t border-border/40 bg-muted/20">
        <div className="container mx-auto px-6 max-w-6xl">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <div className="text-xs font-mono uppercase text-muted-foreground tracking-wider mb-2">Protocol Standards</div>
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight">How ReLoop Guarantees Truth</h2>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            <div className="p-6 rounded-2xl border border-border/60 bg-card/60 space-y-3">
              <div className="h-8 w-8 rounded-lg bg-foreground/5 flex items-center justify-center text-foreground mb-4">
                <Cpu className="h-4 w-4" />
              </div>
              <h3 className="text-sm font-semibold text-foreground">50-Point Technical Grading</h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Objective scoring across physical, component, battery, and logic gates resulting in definitive Grade A+ to D ratings.
              </p>
            </div>

            <div className="p-6 rounded-2xl border border-border/60 bg-card/60 space-y-3">
              <div className="h-8 w-8 rounded-lg bg-foreground/5 flex items-center justify-center text-foreground mb-4">
                <Zap className="h-4 w-4" />
              </div>
              <h3 className="text-sm font-semibold text-foreground">15-Min Anti-Hoarding Leases</h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Carting is non-blocking. Initiating checkout secures an exclusive, pessimistic row-locked lease with auto-reaping.
              </p>
            </div>

            <div className="p-6 rounded-2xl border border-border/60 bg-card/60 space-y-3">
              <div className="h-8 w-8 rounded-lg bg-foreground/5 flex items-center justify-center text-foreground mb-4">
                <ShieldCheck className="h-4 w-4" />
              </div>
              <h3 className="text-sm font-semibold text-foreground">Double-Entry Escrow Ledger</h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                All funds are locked in balanced accounting journals until delivery sign-off, protecting buyers and verified sellers.
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
