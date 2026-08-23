import React from "react";
import Link from "next/link";
import { ArrowRight, ShieldCheck, RefreshCw, Smartphone, Award } from "lucide-react";

export default function HomePage() {
  return (
    <div className="min-h-screen bg-background text-foreground">
      {/* Header */}
      <header className="border-b border-border bg-card">
        <div className="container mx-auto flex h-16 items-center justify-between px-4">
          <div className="flex items-center gap-2">
            <RefreshCw className="h-6 w-6 text-primary" />
            <span className="text-xl font-bold tracking-tight">ReLoop</span>
          </div>
          <nav className="flex items-center gap-6 text-sm font-medium">
            <Link href="/catalog" className="text-muted-foreground hover:text-foreground">
              Marketplace
            </Link>
            <Link href="/trade-in" className="text-muted-foreground hover:text-foreground">
              Trade-In Calculator
            </Link>
            <Link href="/seller" className="text-muted-foreground hover:text-foreground">
              Seller Hub
            </Link>
          </nav>
          <div className="flex items-center gap-3">
            <Link
              href="/login"
              className="rounded-md border border-border px-4 py-2 text-sm font-medium hover:bg-muted"
            >
              Sign In
            </Link>
            <Link
              href="/register"
              className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary-hover"
            >
              Get Started
            </Link>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="py-20 md:py-28 bg-gradient-to-b from-background to-muted/40 border-b border-border">
        <div className="container mx-auto px-4 text-center max-w-4xl">
          <div className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/5 px-3 py-1 text-xs font-semibold text-primary mb-6">
            <ShieldCheck className="h-4 w-4" />
            Certified Serialized Circular Marketplace
          </div>
          <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight mb-6">
            Buy, Verify, and Trade-In <br className="hidden sm:inline" />
            <span className="text-primary">Authenticated Electronics</span>
          </h1>
          <p className="text-lg md:text-xl text-muted-foreground mb-8">
            Every device has an immutable provenance history, verified 50-point technical inspection grade, and guaranteed escrow protection.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link
              href="/catalog"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-md bg-primary px-6 py-3 text-base font-medium text-primary-foreground shadow hover:bg-primary-hover"
            >
              Browse Certified Listings
              <ArrowRight className="h-4 w-4" />
            </Link>
            <Link
              href="/trade-in"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-md border border-border bg-card px-6 py-3 text-base font-medium hover:bg-muted"
            >
              Instant Valuation
            </Link>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section className="py-16 container mx-auto px-4">
        <div className="grid md:grid-cols-3 gap-8">
          <div className="p-6 rounded-lg border border-border bg-card">
            <div className="h-12 w-12 rounded-md bg-primary/10 flex items-center justify-center mb-4 text-primary">
              <Award className="h-6 w-6" />
            </div>
            <h3 className="text-lg font-bold mb-2">Grade A+ to D Certification</h3>
            <p className="text-sm text-muted-foreground">
              Transparent hardware and battery health grading calculated by certified technicians.
            </p>
          </div>
          <div className="p-6 rounded-lg border border-border bg-card">
            <div className="h-12 w-12 rounded-md bg-accent/10 flex items-center justify-center mb-4 text-accent">
              <ShieldCheck className="h-6 w-6" />
            </div>
            <h3 className="text-lg font-bold mb-2">Double-Entry Escrow</h3>
            <p className="text-sm text-muted-foreground">
              Funds are secured in balanced accounting ledgers until delivery verification and inspection sign-off.
            </p>
          </div>
          <div className="p-6 rounded-lg border border-border bg-card">
            <div className="h-12 w-12 rounded-md bg-primary/10 flex items-center justify-center mb-4 text-primary">
              <Smartphone className="h-6 w-6" />
            </div>
            <h3 className="text-lg font-bold mb-2">Anti-Hoarding Checkout</h3>
            <p className="text-sm text-muted-foreground">
              Pessimistic concurrency reservation locks ensure fair 15-minute lease purchasing without inventory hoarding.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
