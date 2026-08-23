'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/apiClient';
import { queryKeys } from '@/lib/queryKeys';
import { Award, Search, Filter, ArrowRight, ShieldCheck, CheckCircle2, ChevronRight, SlidersHorizontal } from 'lucide-react';

export default function CatalogPage() {
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [selectedGrade, setSelectedGrade] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const inventory = [
    {
      id: 'dddddddd-dddd-dddd-dddd-dddddddddddd',
      category: 'Smartphones',
      brand: 'Apple',
      title: 'iPhone 15 Pro 256GB Natural Titanium',
      specs: 'A17 Pro • 98% Battery • Full Box • Dual eSIM',
      price: 17500000,
      grade: 'A+',
      badge: 'Pristine Mint',
      serial: 'F2LZ90K8MD6M',
      seller: 'Official iBox ReLoop',
      sellerRating: 98.5,
      image: 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
      category: 'Laptops',
      brand: 'Apple',
      title: 'MacBook Air M2 16GB / 512GB Midnight',
      specs: '13.6" Liquid Retina • 94% Battery • 35W Dual Charger',
      price: 15800000,
      grade: 'A',
      badge: 'Near Mint',
      serial: 'C02G89A3MD6T',
      seller: 'Official iBox ReLoop',
      sellerRating: 98.5,
      image: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: 'ffffffff-ffff-ffff-ffff-ffffffffffff',
      category: 'Laptops',
      brand: 'Lenovo',
      title: 'ThinkPad X1 Carbon Gen 10 16GB / 512GB',
      specs: 'Intel Core i7-1260P • 14" WUXGA Anti-Glare • 89% Battery',
      price: 12900000,
      grade: 'B+',
      badge: 'Certified Clean',
      serial: 'PF3A9812ZK09',
      seller: 'Enterprise ReUse',
      sellerRating: 92.4,
      image: 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: '44444444-4444-4444-4444-444444444444',
      category: 'Tablets',
      brand: 'Apple',
      title: 'iPad Pro 11" M2 128GB Wi-Fi Space Gray',
      specs: 'Apple M2 • ProMotion 120Hz • 99% Battery',
      price: 11200000,
      grade: 'A+',
      badge: 'Open Box',
      serial: 'DNPZ883K10',
      seller: 'Official iBox ReLoop',
      sellerRating: 98.5,
      image: 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: '55555555-5555-5555-5555-555555555555',
      category: 'Smartphones',
      brand: 'Samsung',
      title: 'Galaxy S24 Ultra 512GB Titanium Gray',
      specs: 'Snapdragon 8 Gen 3 • 200MP Quad Cam • S-Pen',
      price: 16900000,
      grade: 'A+',
      badge: 'Like New',
      serial: 'R5CW109LMN',
      seller: 'Samsung Certified Refurb',
      sellerRating: 97.8,
      image: 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: '66666666-6666-6666-6666-666666666666',
      category: 'Audio',
      brand: 'Sony',
      title: 'Sony WH-1000XM5 Wireless Headphones Silver',
      specs: 'Hi-Res Audio Wireless • 30h Battery • Auto NC Optimizer',
      price: 3850000,
      grade: 'A',
      badge: 'Verified Audio',
      serial: 'SN-WH5-9921',
      seller: 'AudioTech Pro',
      sellerRating: 95.0,
      image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: '77777777-7777-7777-7777-777777777777',
      category: 'Smartphones',
      brand: 'Apple',
      title: 'iPhone 13 128GB Midnight (Grade B+)',
      specs: 'A15 Bionic • 88% Battery • Original Screen Clear',
      price: 7800000,
      grade: 'B+',
      badge: 'Great Value',
      serial: 'G6TZ88019X',
      seller: 'Cellular Exchange Hub',
      sellerRating: 94.2,
      image: 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: '88888888-8888-8888-8888-888888888888',
      category: 'Laptops',
      brand: 'Dell',
      title: 'Dell XPS 13 Plus 9320 OLED Touch',
      specs: 'Intel Core i7-1360P • 32GB RAM • 1TB NVMe • 3.5K OLED',
      price: 18500000,
      grade: 'A+',
      badge: 'Pristine Mint',
      serial: 'DELL-XPS-9932',
      seller: 'Premium Workstations',
      sellerRating: 99.1,
      image: 'https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=800&auto=format&fit=crop&q=80',
    },
    {
      id: '99999999-9999-9999-9999-999999999999',
      category: 'Audio',
      brand: 'Apple',
      title: 'AirPods Pro 2nd Gen USB-C MagSafe',
      specs: 'H2 Chip • Active Noise Cancellation • Adaptive Audio',
      price: 2900000,
      grade: 'A+',
      badge: 'Sanitized & Tested',
      serial: 'H7QL098199',
      seller: 'Official iBox ReLoop',
      sellerRating: 98.5,
      image: 'https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=800&auto=format&fit=crop&q=80',
    },
  ];

  const filteredItems = inventory.filter((item) => {
    const matchesCategory = selectedCategory === 'ALL' || item.category === selectedCategory;
    const matchesGrade = selectedGrade === 'ALL' || item.grade === selectedGrade;
    const matchesSearch =
      item.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.specs.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.serial.toLowerCase().includes(searchQuery.toLowerCase());

    return matchesCategory && matchesGrade && matchesSearch;
  });

  return (
    <div className="min-h-screen bg-background text-foreground py-10">
      <div className="container mx-auto px-6 max-w-7xl">
        {/* Header Title */}
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-8 pb-6 border-b border-border/40 gap-4">
          <div>
            <div className="text-xs font-mono uppercase text-muted-foreground tracking-wider mb-1">
              Verified Serialized Database ({filteredItems.length} Available)
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight">Marketplace Catalog</h1>
          </div>

          {/* Search Input */}
          <div className="relative w-full md:w-80">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
            <input
              type="text"
              placeholder="Search model, chipset, serial..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-full border border-border bg-card/60 pl-9 pr-4 py-2 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-foreground"
            />
          </div>
        </div>

        {/* Filter Bar */}
        <div className="flex flex-wrap items-center justify-between gap-4 mb-8">
          <div className="flex items-center gap-1.5 overflow-x-auto pb-2 sm:pb-0">
            {['ALL', 'Smartphones', 'Laptops', 'Tablets', 'Audio'].map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`rounded-full px-4 py-1.5 text-xs font-medium transition-all ${
                  selectedCategory === cat
                    ? 'bg-foreground text-background font-semibold'
                    : 'border border-border/80 bg-card/40 text-muted-foreground hover:text-foreground hover:bg-muted'
                }`}
              >
                {cat === 'ALL' ? 'All Hardware' : cat}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground">Grade:</span>
            {['ALL', 'A+', 'A', 'B+'].map((g) => (
              <button
                key={g}
                onClick={() => setSelectedGrade(g)}
                className={`rounded-md px-2.5 py-1 text-xs font-mono font-medium transition-all ${
                  selectedGrade === g
                    ? 'bg-accent/20 text-accent border border-accent/40 font-bold'
                    : 'border border-border/80 text-muted-foreground hover:text-foreground'
                }`}
              >
                {g}
              </button>
            ))}
          </div>
        </div>

        {/* Product Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredItems.map((item) => (
            <Link
              key={item.id}
              href={`/catalog/${item.id}`}
              className="group relative rounded-2xl border border-border/60 bg-card/40 p-5 hover:border-border transition-all duration-300 hover:shadow-sm flex flex-col justify-between"
            >
              <div>
                <div className="aspect-[4/3] w-full rounded-xl overflow-hidden bg-muted/30 mb-5 relative">
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
                    SN: {item.serial}
                  </div>
                </div>

                <div className="text-[10px] font-mono uppercase text-muted-foreground tracking-wider mb-1">
                  {item.category} • {item.seller} ({item.sellerRating})
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
                  Inspect & Reserve <ArrowRight className="h-3 w-3" />
                </span>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
