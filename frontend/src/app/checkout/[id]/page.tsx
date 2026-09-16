'use client';

import React, { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, ArrowUpRight, AlertCircle, CheckCircle2, Lock, RefreshCw } from 'lucide-react';
import { getListing, reserveUnit, confirmPayment } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import { useAuthStore } from '@/lib/auth';
import FirebaseGoogleButton from '@/components/FirebaseGoogleButton';

type Reservation = { token: string; remainingSeconds: number };
type Order = { orderNumber: string; totalAmount: number; paymentStatus: string; escrowStatus: string };

export default function CheckoutPage({ params }: { params: { id: string } }) {
  const t = useT();
  const { accessToken } = useAuthStore();
  const [reservation, setReservation] = useState<Reservation>();
  const [order, setOrder] = useState<Order>();
  const [address, setAddress] = useState('');
  const [error, setError] = useState('');
  const [reserveError, setReserveError] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const { data: listing, isError: listingError, refetch: refetchListing } = useQuery({
    queryKey: queryKeys.listings.detail(params.id),
    queryFn: () => getListing(params.id),
    retry: 1,
  });

  const reserve = useCallback(async () => {
    if (!listing || !accessToken) return;
    setLoading(true);
    setReserveError('');
    setReservation(undefined);
    try {
      const res = await reserveUnit({ unitId: listing.unitId, listingId: listing.id });
      setReservation({ token: res.token, remainingSeconds: res.remainingSeconds });
    } catch (err: any) {
      setReserveError(err.response?.data?.message || t('checkout_reserve_fail'));
    } finally {
      setLoading(false);
    }
  }, [accessToken, listing, t]);

  useEffect(() => { void reserve(); }, [reserve]);

  useEffect(() => {
    if (!reservation || reservation.remainingSeconds <= 0) return;
    const timer = window.setInterval(() => setReservation((current) => current && {
      ...current,
      remainingSeconds: Math.max(0, current.remainingSeconds - 1),
    }), 1000);
    return () => window.clearInterval(timer);
  }, [reservation]);

  async function handleConfirmPayment(e: React.FormEvent) {
    e.preventDefault();
    if (!reservation || !address.trim()) { setError(t('checkout_enter_address')); return; }
    setError(''); setSubmitting(true);
    try {
      setOrder(await confirmPayment({ reservationToken: reservation.token, paymentMethod: 'ESCROW', shippingAddress: address.trim() }));
    } catch (err: any) {
      setError(err.response?.data?.message || t('checkout_confirm_fail'));
    } finally { setSubmitting(false); }
  }

  if (!accessToken) {
    return (
      <main className="min-h-screen bg-[#fafaf9] px-4 py-24 text-center ambient-light-mesh">
        <Lock className="mx-auto mb-4 h-10 w-10 text-stone-400" />
        <h1 className="text-2xl font-bold text-stone-950">{t('checkout_sign_in')}</h1>
        <p className="mx-auto mt-2 max-w-md text-sm text-stone-600">{t('checkout_sign_in_desc')}</p>
        <Link href={`/login?next=/checkout/${params.id}`} className="btn-blue mt-6 px-6 py-3 text-xs">{t('nav_sign_in')}</Link>
        <div className="mx-auto mt-4 max-w-xs">
          <div className="relative my-3"><div className="absolute inset-0 flex items-center"><div className="w-full border-t border-stone-200" /></div><div className="relative flex justify-center text-xs"><span className="bg-[#fafaf9] px-3 text-stone-400 uppercase">{t('auth_or')}</span></div></div>
          <FirebaseGoogleButton />
        </div>
      </main>
    );
  }

  const time = reservation ? `${String(Math.floor(reservation.remainingSeconds / 60)).padStart(2, '0')}:${String(reservation.remainingSeconds % 60).padStart(2, '0')}` : '--:--';
  const displayError = error || reserveError;

  return (
    <main className="min-h-screen bg-[#fafaf9] py-10 text-stone-950 sm:py-16 ambient-light-mesh">
      <div className="mx-auto max-w-5xl px-4 sm:px-6">
        <Link href="/catalog" className="editorial-link mb-8 inline-flex"><ArrowLeft className="h-3 w-3" />{t('checkout_return')}</Link>
        {order ? (
          <section aria-live="polite" className="mx-auto max-w-2xl rounded-3xl border border-stone-200 bg-white p-8 text-center shadow-sm sm:p-12">
            <CheckCircle2 className="mx-auto h-12 w-12 text-emerald-600" />
            <h1 className="mt-5 text-3xl font-bold">{t('checkout_confirmed')}</h1>
            <p className="mt-3 text-sm leading-6 text-stone-600"><strong>{order.orderNumber}</strong> {t('checkout_recorded')} {order.paymentStatus}. {t('checkout_escrow')}: {order.escrowStatus}.</p>
            <div className="flex flex-col justify-center gap-3 pt-6 sm:flex-row"><Link href="/orders" className="btn-blue px-6 py-3 text-xs">{t('checkout_view_orders')}</Link><Link href="/catalog" className="btn-primary-dark px-6 py-3 text-xs">{t('checkout_back_market')}</Link></div>
          </section>
        ) : !listing && !listingError ? (
          <div className="h-64 animate-pulse rounded-3xl bg-stone-200" />
        ) : listingError ? (
          <section role="alert" className="rounded-3xl border border-red-200 bg-red-50 p-8 text-center text-red-800"><AlertCircle className="mx-auto mb-3 h-8 w-8" /><p>{t('common_error_generic')}</p><button onClick={() => void refetchListing()} className="btn-primary-dark mt-5 px-5 py-3 text-xs">{t('common_try_again')}</button></section>
        ) : (
          <div className="grid gap-6 lg:grid-cols-[1fr_0.72fr]">
            <section className="space-y-6">
              <div className="flex flex-col justify-between gap-4 rounded-3xl border border-stone-200 bg-white p-6 shadow-sm sm:flex-row sm:items-center">
                <div><p className="text-xs font-mono uppercase tracking-widest text-sky-800 font-semibold">{t('checkout_reservation')}</p><h1 className="mt-1 text-xl font-bold">{loading ? t('checkout_reserving') : reservation ? t('checkout_active') : t('checkout_unavailable')}</h1><p className="mt-1 text-sm text-stone-600">{listing?.title} · {t('common_grade')} {listing?.gradeSnapshot}</p></div>
                <time aria-label={t('checkout_time_remaining')} className="rounded-2xl border border-stone-200 bg-stone-50 px-5 py-3 text-center font-mono text-3xl font-bold">{time}</time>
              </div>
              {displayError && <div role="alert" className="flex gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" /><span>{displayError}</span></div>}
              {reserveError ? <div className="flex flex-col gap-3 sm:flex-row"><button onClick={() => void reserve()} className="btn-blue px-5 py-3 text-xs"><RefreshCw className="mr-2 h-4 w-4" />{t('checkout_retry')}</button><Link href="/catalog" className="btn-primary-dark px-5 py-3 text-xs">{t('checkout_back_market')}</Link></div> : (
                <form onSubmit={handleConfirmPayment} className="rounded-3xl border border-stone-200 bg-white p-6 shadow-sm sm:p-8">
                  <p className="text-xs font-mono uppercase tracking-widest text-sky-800 font-semibold">{t('checkout_destination')}</p><label htmlFor="address" className="mt-5 mb-2 block text-sm font-semibold">{t('checkout_address')}</label><textarea id="address" required value={address} onChange={(e) => setAddress(e.target.value)} rows={4} className="w-full rounded-xl border border-stone-300 bg-stone-50 px-4 py-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-700" placeholder={t('checkout_address_placeholder')} />
                  <div className="mt-6 flex gap-3 rounded-2xl border border-sky-200 bg-sky-50 p-4 text-sm text-sky-950"><Lock className="h-5 w-5 shrink-0 text-sky-700" /><span>{t('checkout_escrow_note')}</span></div><button type="submit" disabled={!reservation || reservation.remainingSeconds === 0 || submitting} aria-busy={submitting} className="btn-blue mt-6 w-full py-4 text-sm">{submitting ? t('checkout_confirming') : t('checkout_confirm')} <ArrowUpRight className="ml-1 inline h-4 w-4" /></button>
                </form>
              )}
            </section>
            <aside className="h-fit rounded-3xl border border-stone-200 bg-white p-6 shadow-sm lg:sticky lg:top-24">
              <p className="text-xs font-mono uppercase tracking-widest text-amber-700">{t('checkout_summary')}</p><h2 className="mt-2 text-lg font-bold">{listing?.title}</h2><dl className="mt-6 space-y-3 text-sm"><div className="flex justify-between gap-4"><dt className="text-stone-600">{t('checkout_item')}</dt><dd className="font-mono">Rp {listing?.askingPrice.toLocaleString('id-ID')}</dd></div><div className="flex justify-between gap-4"><dt className="text-stone-600">{t('checkout_shipping')}</dt><dd>{t('checkout_shipping_free')}</dd></div><div className="flex justify-between gap-4 border-t border-stone-200 pt-3 font-bold"><dt>{t('checkout_total')}</dt><dd className="font-mono">Rp {listing?.askingPrice.toLocaleString('id-ID')}</dd></div></dl>
            </aside>
          </div>
        )}
      </div>
    </main>
  );
}
