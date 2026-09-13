'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { motion, useScroll, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { Search, X, RefreshCw, Menu, ChevronRight, LogOut, Package, ShieldCheck, Globe } from 'lucide-react';
import { useAuthStore } from '@/lib/auth';
import { logout, searchListings } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useI18nStore, useT } from '@/lib/i18n';
import type { ListingDto } from '@/types/api';

function formatPrice(value: number): string {
  return `Rp ${value.toLocaleString('id-ID')}`;
}

function parseImages(images: string | null): string[] {
  try {
    const parsed = JSON.parse(images ?? '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function Navbar() {
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const { scrollY } = useScroll();
  const [isScrolled, setIsScrolled] = useState(false);
  const { accessToken, email, role } = useAuthStore();
  const t = useT();
  const { lang, toggleLang } = useI18nStore();

  useEffect(() => {
    return scrollY.on('change', (latest) => {
      setIsScrolled(latest > 50);
    });
  }, [scrollY]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setIsSearchOpen((prev) => !prev);
      }
      if (e.key === 'Escape') {
        setIsSearchOpen(false);
        setIsMobileMenuOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const navLinks = [
    { href: '/catalog', label: t('nav_inventory') },
    { href: '/trade-in', label: t('nav_valuation') },
    { href: '/cart', label: t('nav_cart'), auth: true },
    { href: '/orders', label: t('nav_orders'), auth: true },
    { href: '/profile', label: t('nav_profile'), auth: true },
    { href: '/returns', label: t('nav_returns'), auth: true },
    { href: '/notifications', label: t('nav_notifications'), auth: true },
    { href: '/seller', label: t('nav_seller'), roles: ['SELLER', 'ADMIN'] },
    { href: '/admin', label: t('nav_admin'), roles: ['ADMIN'] },
    { href: '/inspections', label: t('nav_inspections'), roles: ['TECHNICIAN', 'ADMIN'] },
    { href: '/repairs', label: t('nav_repairs'), roles: ['TECHNICIAN', 'ADMIN'] },
  ].filter((link) => {
    if (link.auth && !accessToken) return false;
    if (link.roles && !accessToken) return false;
    if (link.roles && accessToken && !link.roles.includes(role ?? '')) return false;
    return true;
  });

  // Latest listings for the command dialog (backend search filters by price/grade; no text search yet)
  const { data: latest } = useQuery({
    queryKey: queryKeys.listings.search({ page: 0, size: 5, sort: 'newest', q: searchTerm.trim() || undefined }),
    queryFn: () => searchListings({ page: 0, size: 5, sort: 'newest', q: searchTerm.trim() || undefined }),
    enabled: isSearchOpen,
  });

  return (
    <>
      <motion.header
        className="sticky top-0 z-50 w-full bg-white/80 backdrop-blur-xl border-b border-black/[0.06] transition-colors"
        animate={{ height: isScrolled ? 56 : 64 }}
        transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
        style={{ boxShadow: isScrolled ? '0 1px 3px 0 rgba(0,0,0,0.04)' : 'none' }}
      >
        <div className="container mx-auto flex h-full max-w-7xl items-center justify-between px-6">
          <div className="flex items-center gap-10">
            <Link href="/" className="flex items-center gap-2.5 font-bold tracking-tight text-zinc-900 group">
              <motion.div
                className="flex items-center justify-center rounded-xl bg-zinc-900 text-white shadow-sm"
                animate={{ width: isScrolled ? 28 : 32, height: isScrolled ? 28 : 32 }}
                transition={{ duration: 0.3 }}
              >
                <RefreshCw className="stroke-[2.5]" style={{ width: isScrolled ? 14 : 16, height: isScrolled ? 14 : 16 }} />
              </motion.div>
              <span className="text-base font-extrabold tracking-tight leading-none hidden sm:inline">ReLoop</span>
            </Link>

            {/* Desktop Nav */}
            <nav className="hidden md:flex items-center gap-7 text-xs font-semibold tracking-wide text-zinc-600">
              {navLinks.slice(0, 5).map((link) => (
                <Link key={link.href} href={link.href} className="hover:text-zinc-900 transition-colors">
                  {link.label}
                </Link>
              ))}
            </nav>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={toggleLang}
              className="hidden sm:flex items-center gap-1.5 rounded-full border border-zinc-200 bg-white px-3 py-2 text-[11px] font-mono font-bold text-zinc-700 hover:border-zinc-300 transition-all shadow-sm"
              title={t('lang_toggle')}
            >
              <Globe className="h-3.5 w-3.5 text-sky-600" />
              {lang.toUpperCase()}
            </button>
            <button
              onClick={() => setIsSearchOpen(true)}
              className="hidden sm:flex items-center gap-3 rounded-full border border-zinc-200 bg-zinc-50/80 px-4 py-2 text-xs text-zinc-600 hover:text-zinc-900 hover:border-zinc-300 hover:bg-white transition-all shadow-sm"
            >
              <Search className="h-3.5 w-3.5" />
              <span className="font-normal">{t('nav_search')}</span>
              <kbd className="rounded-md border border-zinc-200 bg-white px-1.5 py-0.5 text-[10px] font-mono text-zinc-600 shadow-xs">⌘K</kbd>
            </button>

            {accessToken ? (
              <div className="hidden sm:flex items-center gap-2">
                <Link
                  href="/orders"
                  className="flex items-center gap-2 rounded-full border border-zinc-200 bg-white px-3 py-2 text-xs text-zinc-700 hover:border-zinc-300 transition-all shadow-sm"
                  title={email ?? ''}
                >
                  <span className="max-w-[120px] truncate font-semibold">{email}</span>
                  {role && (
                    <span className="rounded-full bg-sky-50 border border-sky-200 px-2 py-0.5 text-[10px] font-mono font-bold text-sky-700">
                      {role}
                    </span>
                  )}
                </Link>
                <button
                  onClick={() => logout()}
                  className="p-2 rounded-full text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 transition-colors"
                  title="Sign out"
                >
                  <LogOut className="h-4 w-4" />
                </button>
              </div>
            ) : (
              <Link href="/login" className="btn-primary-dark px-5 py-2 text-xs shadow-sm hidden sm:inline-flex">
                {t('nav_sign_in')}
              </Link>
            )}

            {/* Mobile Hamburger */}
            <button
              type="button"
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="md:hidden min-h-11 min-w-11 rounded-xl p-2 hover:bg-zinc-100 transition-colors"
              aria-label={isMobileMenuOpen ? t('nav_close_menu') : t('nav_open_menu')}
              aria-expanded={isMobileMenuOpen}
              aria-controls="mobile-navigation"
            >
              {isMobileMenuOpen ? <X className="h-5 w-5 text-zinc-900" /> : <Menu className="h-5 w-5 text-zinc-900" />}
            </button>
          </div>
        </div>
      </motion.header>

      {/* Mobile Menu Overlay */}
      <AnimatePresence>
        {isMobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 z-40 md:hidden"
          >
            <div className="absolute inset-0 bg-zinc-900/30 backdrop-blur-sm" onClick={() => setIsMobileMenuOpen(false)} />
            <motion.div
              id="mobile-navigation"
              role="dialog"
              aria-modal="true"
              aria-label={t('nav_mobile_menu')}
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
              className="absolute right-0 top-0 h-full w-80 max-w-[85vw] bg-white shadow-2xl"
            >
              <div className="p-6 space-y-6">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 font-bold text-zinc-900">
                    <div className="h-8 w-8 rounded-xl bg-zinc-900 flex items-center justify-center">
                      <RefreshCw className="h-4 w-4 text-white stroke-[2.5]" />
                    </div>
                    <span className="text-sm">ReLoop Bureau</span>                  </div>
                   <button type="button" onClick={() => setIsMobileMenuOpen(false)} className="min-h-11 min-w-11 rounded-xl p-2 hover:bg-zinc-100" aria-label={t('nav_close_menu')}>
                    <X className="h-5 w-5 text-zinc-900" />
                  </button>
                </div>

                <div className="border-t border-zinc-100 pt-4 space-y-1">
                  {(accessToken ? [...navLinks, { href: '/orders', label: t('nav_my_orders') }] : navLinks).map((link) => (
                    <Link
                      key={link.href}
                      href={link.href}
                      onClick={() => setIsMobileMenuOpen(false)}
                      className="flex items-center justify-between p-3 rounded-xl hover:bg-zinc-50 transition-colors text-sm font-medium text-zinc-900"
                    >
                      {link.label}
                      <ChevronRight className="h-4 w-4 text-zinc-400" />
                    </Link>
                  ))}
                </div>

                <div className="border-t border-zinc-100 pt-4 space-y-3">
                  <button
                    onClick={toggleLang}
                    className="w-full flex items-center gap-2 p-3 rounded-xl bg-zinc-50 border border-zinc-200 text-xs font-mono font-bold text-zinc-700"
                  >
                    <Globe className="h-4 w-4 text-sky-600" />
                    {lang === 'id' ? 'English' : 'Bahasa Indonesia'}
                  </button>
                  <button
                    onClick={() => { setIsSearchOpen(true); setIsMobileMenuOpen(false); }}
                    className="w-full flex items-center gap-3 p-3 rounded-xl bg-zinc-50 border border-zinc-200 text-sm text-zinc-600"
                  >
                    <Search className="h-4 w-4" />
                    {t('nav_search')}
                    <kbd className="ml-auto rounded border border-zinc-200 bg-white px-1.5 text-[10px]">⌘K</kbd>
                  </button>
                  {accessToken ? (
                    <button
                      onClick={() => { logout(); setIsMobileMenuOpen(false); }}
                      className="w-full flex items-center justify-center gap-2 rounded-xl border border-zinc-200 py-3 text-xs font-semibold text-zinc-700 hover:bg-zinc-50 transition-colors"
                    >
                      <LogOut className="h-4 w-4" /> {t('nav_sign_out')}
                    </button>
                  ) : (
                    <Link href="/login" onClick={() => setIsMobileMenuOpen(false)} className="w-full btn-primary-dark py-3 text-xs text-center">
                      {t('nav_sign_in')}
                    </Link>
                  )}
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Command Search Dialog — latest live listings */}
      <AnimatePresence>
        {isSearchOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 z-50 flex items-start justify-center pt-24 px-4 bg-zinc-900/40 backdrop-blur-md"
            onClick={() => setIsSearchOpen(false)}
          >
            <motion.div
              initial={{ opacity: 0, y: -20, scale: 0.96 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -10, scale: 0.98 }}
              transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
              className="w-full max-w-xl rounded-3xl border border-zinc-200 bg-white p-6 shadow-2xl space-y-4"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between border-b border-zinc-100 pb-4">
                <div className="flex items-center gap-3 text-zinc-900 flex-1">
                  <Search className="h-4 w-4 text-sky-600" />
                   <input
                     type="search"
                     value={searchTerm}
                     onChange={(e) => setSearchTerm(e.target.value)}
                     placeholder={t('nav_search_placeholder')}
                     aria-label={t('nav_search')}
                     className="w-full bg-transparent text-sm text-zinc-900 placeholder:text-zinc-400 focus:outline-none"
                   />
                </div>
                <button
                  onClick={() => setIsSearchOpen(false)}
                  className="text-zinc-400 hover:text-zinc-900 p-1.5 rounded-lg hover:bg-zinc-100 transition-colors"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <div className="space-y-2 pt-1 text-xs">
                <div className="text-[11px] font-mono uppercase text-zinc-600 font-semibold tracking-wider">{t('nav_latest')}</div>
                {(latest?.items ?? []).filter((listing: ListingDto) => !searchTerm.trim() || listing.title.toLowerCase().includes(searchTerm.toLowerCase())).map((listing: ListingDto) => (
                  <Link key={listing.id} href={`/catalog/${listing.id}`} onClick={() => setIsSearchOpen(false)}
                    className="flex items-center justify-between p-3.5 rounded-2xl bg-zinc-50 hover:bg-zinc-100 transition-colors border border-zinc-200/60">
                    <div>
                      <div className="font-bold text-zinc-900">{listing.title}</div>
                      <div className="font-mono text-zinc-600 text-[11px]">Grade {listing.gradeSnapshot} • {listing.status}</div>
                    </div>
                    <span className="font-mono text-sky-700 font-bold bg-sky-50 border border-sky-200 px-2.5 py-1 rounded-full">
                      {formatPrice(listing.askingPrice)}
                    </span>
                  </Link>
                ))}
                {latest && latest.items.length === 0 && (
                  <div className="p-4 text-center text-zinc-500">{t('nav_no_listings')}</div>
                )}
                <Link href="/catalog" onClick={() => setIsSearchOpen(false)}
                  className="flex items-center justify-between p-3.5 rounded-2xl hover:bg-zinc-100 transition-colors text-zinc-700">
                  <span className="flex items-center gap-2"><Package className="h-4 w-4 text-sky-600" /> {t('nav_browse')}</span>
                  <ChevronRight className="h-4 w-4 text-zinc-400" />
                </Link>
                {role === 'ADMIN' && (
                  <Link href="/admin" onClick={() => setIsSearchOpen(false)}
                    className="flex items-center justify-between p-3.5 rounded-2xl hover:bg-zinc-100 transition-colors text-zinc-700">
                    <span className="flex items-center gap-2"><ShieldCheck className="h-4 w-4 text-sky-600" /> {t('nav_admin_ops')}</span>
                    <ChevronRight className="h-4 w-4 text-zinc-400" />
                  </Link>
                )}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
