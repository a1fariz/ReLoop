'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, ArrowUpRight, AlertCircle, CheckCircle2, Lock } from 'lucide-react';
import { getListing, reserveUnit, confirmPayment } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';

type Reservation = { token: string; remainingSeconds: number };
type Order = { orderNumber: string; totalAmount: number; paymentStatus: string; escrowStatus: string };

export default function CheckoutPage({ params }: { params: { id: string } }) {
  const t = useT();
  const [reservation, setReservation] = useState<Reservation>();
  const [order, setOrder] = useState<Order>();
  const [address, setAddress] = useState('');
  const [error, setError] = useState('');
  const [reserveError, setReserveError] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // Listing detail supplies the unitId required by POST /checkout/reserve
  const { data: listing } = useQuery({
    queryKey: queryKeys.listings.detail(params.id),
    queryFn: () => getListing(params.id),
    retry: 1,
  });

  useEffect(() => {
    if (!listing) return;
    let cancelled = false;
    (async () => {
      try {
        const res = await reserveUnit({ unitId: listing.unitId, listingId: listing.id });
        if (!cancelled) setReservation({ token: res.token, remainingSeconds: res.remainingSeconds });
      } catch (err: any) {
        if (!cancelled) setReserveError(err.response?.data?.message || t('checkout_reserve_fail'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [listing]);

  useEffect(() => {
    if (!reservation || reservation.remainingSeconds <= 0) return;
    const timer = window.setInterval(() => setReservation((current) => current && { ...current, remainingSeconds: Math.max(0, current.remainingSeconds - 1) }), 1000);
    return () => window.clearInterval(timer);
  }, [reservation]);

  async function handleConfirmPayment(e: React.FormEvent) {
    e.preventDefault();
    if (!reservation || !address.trim()) { setError(t('checkout_enter_address')); return; }
    setError(''); setSubmitting(true);
    try {
      const confirmation = await confirmPayment({
        reservationToken: reservation.token,
        paymentMethod: 'ESCROW',
        shippingAddress: address.trim(),
      });
      setOrder(confirmation);
    } catch (err: any) {
      setError(err.response?.data?.message || t('checkout_confirm_fail'));
    } finally { setSubmitting(false); }
  }

  const time = reservation ? `${String(Math.floor(reservation.remainingSeconds / 60)).padStart(2, '0')}:${String(reservation.remainingSeconds % 60).padStart(2, '0')}` : '--:--';
  const displayError = error || reserveError;
  return <div className="min-h-screen bg-[#fafafa] text-zinc-900 py-12 sm:py-16 ambient-light-mesh"><div className="container mx-auto px-4 sm:px-6 max-w-3xl">
    <Link href="/catalog" className="editorial-link mb-8 inline-flex"><ArrowLeft className="h-3 w-3" />{t('checkout_return')}</Link>
    {order ? <section aria-live="polite" className="bg-white border border-zinc-200 rounded-3xl p-8 sm:p-12 text-center space-y-5 shadow-sm"><CheckCircle2 className="h-12 w-12 text-emerald-600 mx-auto" /><h1 className="text-3xl font-bold">{t('checkout_confirmed')}</h1><p className="text-sm text-zinc-600"><strong>{order.orderNumber}</strong> {t('checkout_recorded')} {order.paymentStatus}. {t('checkout_escrow')}: {order.escrowStatus}.</p><div className="flex flex-wrap justify-center gap-3 pt-2"><Link href="/orders" className="inline-flex btn-blue px-6 py-3 text-xs">{t('checkout_view_orders')}</Link><Link href="/catalog" className="inline-flex btn-primary-dark px-6 py-3 text-xs">{t('checkout_back_market')}</Link></div></section> : <>
      <section className="bg-white border border-zinc-200 rounded-3xl p-6 mb-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-sm"><div><p className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold">{t('checkout_reservation')}</p><h1 className="text-xl font-bold mt-1">{loading ? t('checkout_reserving') : reservation ? t('checkout_active') : t('checkout_unavailable')}</h1>{listing && <p className="text-sm text-zinc-600 mt-1">{listing.title} • {t('common_grade')} {listing.gradeSnapshot} • Rp {listing.askingPrice.toLocaleString('id-ID')}</p>}</div><time className="font-mono text-3xl font-bold bg-zinc-50 border border-zinc-200 px-5 py-3 rounded-2xl">{time}</time></section>
      {displayError && <div role="alert" className="mb-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 flex gap-2"><AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />{displayError}</div>}
      <form onSubmit={handleConfirmPayment} className="bg-white border border-zinc-200 rounded-3xl p-6 sm:p-8 space-y-6 shadow-sm"><div><p className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold">{t('checkout_destination')}</p><label htmlFor="address" className="block mt-5 mb-2 text-sm font-semibold">{t('checkout_address')}</label><textarea id="address" required value={address} onChange={(e) => setAddress(e.target.value)} rows={4} className="w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500" placeholder={t('checkout_address_placeholder')} /></div><div className="rounded-2xl border border-sky-200 bg-sky-50 p-4 flex gap-3 text-sm"><Lock className="h-5 w-5 text-sky-600 shrink-0" /><span>{t('checkout_escrow_note')}</span></div><button type="submit" disabled={!reservation || reservation.remainingSeconds === 0 || submitting} aria-busy={submitting} className="w-full btn-blue py-4 text-sm disabled:opacity-60 disabled:cursor-not-allowed">{submitting ? t('checkout_confirming') : t('checkout_confirm')} <ArrowUpRight className="inline h-4 w-4 ml-1" /></button></form>
    </>}</div></div>;
}
