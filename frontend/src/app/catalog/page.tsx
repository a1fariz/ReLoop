'use client';

import React from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/apiClient';
import { queryKeys } from '@/lib/queryKeys';
import { Award, ShieldCheck, ShoppingBag, ArrowLeft, Loader2 } from 'lucide-react';

interface ListingItem {
  id: string;
  title: string;
  askingPrice: number;
  gradeSnapshot: string;
  sellerId: number;
  unitId: string;
}

export default function CatalogPage() {
  const fallbackListings = [
    {
      id: 'dddddddd-dddd-dddd-dddd-dddddddddddd',
      title: 'iPhone 15 Pro 256GB Natural Titanium (Grade A+)',
      askingPrice: 17500000,
      gradeSnapshot: 'A+',
      sellerId: 1,
      unitId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
      battery: 98,
      sellerName: 'Official iBox ReLoop',
      sellerScore: 98.5,
      warranty: '6 Months Full Warranty',
      serialNumber: 'F2LZ90K8MD6M',
    },
    {
      id: 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
      title: 'MacBook Air M2 16GB 512GB Midnight (Grade A)',
      askingPrice: 15800000,
      gradeSnapshot: 'A',
      sellerId: 1,
      unitId: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
      battery: 94,
      sellerName: 'Official iBox ReLoop',
      sellerScore: 98.5,
      warranty: '6 Months Full Warranty',
      serialNumber: 'C02G89A3MD6T',
    },
    {
      id: 'ffffffff-ffff-ffff-ffff-ffffffffffff',
      title: 'ThinkPad X1 Carbon Gen 10 16GB 512GB (Grade B+)',
      askingPrice: 12900000,
      gradeSnapshot: 'B+',
      sellerId: 1,
      unitId: 'cccccccc-cccc-cccc-cccc-cccccccccccc',
      battery: 89,
      sellerName: 'Enterprise ReUse',
      sellerScore: 92.4,
      warranty: '3 Months Certified Warranty',
      serialNumber: 'PF3A9812ZK09',
    },
  ];

  const { data: apiListings, isLoading } = useQuery({
    queryKey: queryKeys.listings.all(),
    queryFn: async () => {
      try {
        const res = await apiClient.get('/listings');
        return res.data?.data || [];
      } catch (e) {
        return fallbackListings;
      }
    },
    initialData: fallbackListings,
  });

  const listings = apiListings && apiListings.length > 0 ? apiListings : fallbackListings;

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

        {isLoading ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="h-8 w-8 text-primary animate-spin" />
          </div>
        ) : (
          <div className="grid md:grid-cols-3 gap-6">
            {listings.map((listing: any) => (
              <div key={listing.id} className="bg-card rounded-xl border border-border overflow-hidden hover:shadow-lg transition-all flex flex-col justify-between">
                <div className="p-6">
                  <div className="flex items-center justify-between mb-3">
                    <span className="inline-flex items-center gap-1 rounded-full bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300 px-2.5 py-0.5 text-xs font-bold">
                      <Award className="h-3.5 w-3.5" /> Grade {listing.gradeSnapshot || 'A+'}
                    </span>
                    <span className="text-xs font-mono text-muted-foreground">SN: {listing.serialNumber || 'CERTIFIED'}</span>
                  </div>

                  <h3 className="text-lg font-bold mb-2 leading-snug">{listing.title}</h3>
                  <div className="text-2xl font-black text-foreground mb-4">
                    Rp {Number(listing.askingPrice).toLocaleString('id-ID')}
                  </div>

                  <div className="space-y-2 text-xs text-muted-foreground border-t border-border pt-4">
                    <div className="flex items-center justify-between">
                      <span>Battery Diagnostic:</span>
                      <span className="font-semibold text-foreground">{listing.battery || 95}% Original</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span>Seller Reputation:</span>
                      <span className="font-semibold text-primary">{listing.sellerScore || 98.5}/100</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span>Protection:</span>
                      <span className="font-semibold text-accent flex items-center gap-1">
                        <ShieldCheck className="h-3.5 w-3.5" /> 6 Months Certified Warranty
                      </span>
                    </div>
                  </div>
                </div>

                <div className="p-6 bg-muted/30 border-t border-border">
                  <Link
                    href={`/catalog/${listing.id}`}
                    className="w-full rounded-lg bg-primary py-2.5 px-4 text-center text-sm font-semibold text-white shadow hover:bg-primary-hover flex items-center justify-center gap-2"
                  >
                    <ShoppingBag className="h-4 w-4" />
                    View 50-Pt Report & Reserve
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
