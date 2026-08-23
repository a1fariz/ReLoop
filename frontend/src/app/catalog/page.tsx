'use client';

import React from 'react';
import Link from 'next/link';
import { Award, ShieldCheck, CheckCircle2, ShoppingBag, ArrowLeft } from 'lucide-react';

export default function CatalogPage() {
  const mockListings = [
    {
      id: '1a2b3c-4d5e',
      title: 'iPhone 15 Pro 256GB Natural Titanium',
      askingPrice: 17500000,
      grade: 'A+',
      battery: 98,
      sellerName: 'Official Refurbish Store',
      sellerScore: 98.5,
      warranty: '6 Months Full Warranty',
      serialNumber: 'F2LZ90K8MD6M',
    },
    {
      id: '2b3c4d-5e6f',
      title: 'MacBook Air M2 16GB 512GB Midnight',
      askingPrice: 15800000,
      grade: 'A',
      battery: 94,
      sellerName: 'MacReLoop Certified',
      sellerScore: 96.0,
      warranty: '6 Months Full Warranty',
      serialNumber: 'C02G89A3MD6T',
    },
    {
      id: '3c4d5e-6f7g',
      title: 'ThinkPad X1 Carbon Gen 10 16GB 512GB',
      askingPrice: 12900000,
      grade: 'B+',
      battery: 89,
      sellerName: 'Enterprise ReUse',
      sellerScore: 92.4,
      warranty: '3 Months Certified Warranty',
      serialNumber: 'PF3A9812ZK',
    },
  ];

  return (
    <div className="min-h-screen bg-background text-foreground py-10">
      <div className="container mx-auto px-4 max-w-6xl">
        <div className="flex items-center justify-between mb-8">
          <div>
            <Link href="/" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-2">
              <ArrowLeft className="h-4 w-4" /> Back to Home
            </Link>
            <h1 className="text-3xl font-extrabold tracking-tight">Certified Pre-Owned Catalog</h1>
            <p className="text-sm text-muted-foreground">Each unit is serialized, inspected, and guaranteed with anti-hoarding checkout leases.</p>
          </div>
        </div>

        <div className="grid md:grid-cols-3 gap-6">
          {mockListings.map((listing) => (
            <div key={listing.id} className="bg-card rounded-xl border border-border overflow-hidden hover:shadow-lg transition-all flex flex-col justify-between">
              <div className="p-6">
                <div className="flex items-center justify-between mb-3">
                  <span className="inline-flex items-center gap-1 rounded-full bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300 px-2.5 py-0.5 text-xs font-bold">
                    <Award className="h-3.5 w-3.5" /> Grade {listing.grade}
                  </span>
                  <span className="text-xs font-mono text-muted-foreground">SN: {listing.serialNumber}</span>
                </div>

                <h3 className="text-lg font-bold mb-2 leading-snug">{listing.title}</h3>
                <div className="text-2xl font-black text-foreground mb-4">
                  Rp {listing.askingPrice.toLocaleString('id-ID')}
                </div>

                <div className="space-y-2 text-xs text-muted-foreground border-t border-border pt-4">
                  <div className="flex items-center justify-between">
                    <span>Battery Health:</span>
                    <span className="font-semibold text-foreground">{listing.battery}%</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span>Seller Reputation:</span>
                    <span className="font-semibold text-primary">{listing.sellerScore}/100</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span>Protection:</span>
                    <span className="font-semibold text-accent flex items-center gap-1">
                      <ShieldCheck className="h-3.5 w-3.5" /> {listing.warranty}
                    </span>
                  </div>
                </div>
              </div>

              <div className="p-6 bg-muted/30 border-t border-border">
                <Link
                  href={`/checkout/${listing.id}`}
                  className="w-full rounded-lg bg-primary py-2.5 px-4 text-center text-sm font-semibold text-white shadow hover:bg-primary-hover flex items-center justify-center gap-2"
                >
                  <ShoppingBag className="h-4 w-4" />
                  Acquire 15-Min Checkout Lease
                </Link>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
