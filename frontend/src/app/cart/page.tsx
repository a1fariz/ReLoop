'use client';

import React from 'react';
import Link from 'next/link';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ShoppingBag, Trash2, Lock } from 'lucide-react';
import { apiErrorMessage, getCart, removeCartItem, clearCart } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import { useAuthStore } from '@/lib/auth';
import FirebaseGoogleButton from '@/components/FirebaseGoogleButton';

export default function CartPage() {
  const t = useT();
  const { accessToken } = useAuthStore();
  const queryClient = useQueryClient();

  const { data: cart, isPending, isError, refetch } = useQuery({
    queryKey: queryKeys.cart.mine(),
    queryFn: () => getCart(),
    enabled: accessToken !== null,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['cart'] });

  const removeMutation = useMutation({
    mutationFn: removeCartItem,
    onSuccess: invalidate,
  });

  const clearMutation = useMutation({
    mutationFn: clearCart,
    onSuccess: invalidate,
  });

  if (!accessToken) {
    return (
      <div className="min-h-screen bg-[#fafafa] py-24 text-center">
        <ShoppingBag className="h-12 w-12 text-zinc-300 mx-auto mb-4" />
        <h1 className="text-2xl font-bold mb-2">{t('common_sign_in_required')}</h1>
        <div className="mx-auto mt-4 max-w-xs">
          <FirebaseGoogleButton />
        </div>
      </div>
    );
  }

  const items = cart?.items ?? [];
  const subtotal = items.reduce((sum, item) => sum + item.price * item.quantity, 0);

  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 py-16 ambient-light-mesh">
      <div className="container mx-auto px-6 max-w-6xl space-y-12">
        <div className="pb-6 border-b border-zinc-200">
          <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold mb-2">
            {t('nav_orders')}
          </div>
          <h1 className="text-4xl sm:text-5xl font-semibold tracking-tight">{t('cart_title')}</h1>
          <p className="text-sm text-zinc-600 mt-3 max-w-2xl leading-relaxed">{t('cart_desc')}</p>
        </div>

        {isPending && <div className="h-28 rounded-2xl bg-zinc-100 animate-pulse" />}

        {!isPending && isError && (
          <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-8 text-center text-sm text-red-800">
            <p>{t('common_error_generic')}</p>
            <button onClick={() => void refetch()} className="btn-primary-dark mt-4 px-5 py-3 text-xs">{t('common_try_again')}</button>
          </div>
        )}

        {!isPending && !isError && (
          <section className="space-y-5">
            {(removeMutation.isError || clearMutation.isError) && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-800">{t('common_error_generic')}</div>}
            {items.length === 0 ? (
              <div className="p-10 rounded-2xl border border-zinc-200 bg-white text-center">
                <p className="text-sm text-zinc-500">{t('cart_empty')}</p>
                <Link href="/catalog" className="btn-blue inline-block px-5 py-2.5 text-xs mt-4">{t('nav_browse')}</Link>
              </div>
            ) : (
              <>
                <div className="grid gap-4">
                  {items.map((item) => (
                    <div key={item.cartItemId} className="bg-white border border-zinc-200 rounded-2xl p-5 sm:p-6 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                      <div className="min-w-0">
                        <Link href={`/catalog/${item.listingId}`} className="font-bold hover:text-sky-700 transition-colors truncate block">
                          {item.listingTitle}
                        </Link>
                        <div className="text-xs font-mono text-zinc-500 mt-1">
                          Rp {item.price.toLocaleString('id-ID')} × {item.quantity} — {item.listingStatus}
                        </div>
                      </div>
                      <div className="flex w-full flex-wrap items-center justify-between gap-3 sm:w-auto sm:justify-end">
                        <span className="font-mono text-sm font-bold">
                          Rp {(item.price * item.quantity).toLocaleString('id-ID')}
                        </span>
                        {item.listingStatus === 'ACTIVE' && (
                          <Link
                            href={`/checkout/${item.listingId}`}
                            className="px-3 py-1.5 rounded-full bg-zinc-900 text-white text-[10px] font-bold hover:bg-zinc-700 transition-colors inline-flex items-center gap-1.5"
                          >
                            <Lock className="h-3 w-3" /> {t('cart_checkout')}
                          </Link>
                        )}
                        <button
                          onClick={() => removeMutation.mutate(item.listingId)}
                          disabled={removeMutation.isPending}
                          className="p-2 rounded-full text-zinc-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                          title={t('cart_remove')}
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>

<div className="bg-white border border-zinc-200 rounded-2xl p-5 sm:p-6 shadow-sm flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
                   <div className="flex items-start gap-3 text-xs font-mono text-zinc-500">
                    <Lock className="h-4 w-4 text-sky-600" />
                    {t('cart_check_contract')}
                  </div>
                  <div className="flex w-full items-center justify-between gap-4 sm:w-auto sm:justify-end">
                    <div className="text-right">
                      <div className="text-[10px] font-mono uppercase tracking-wider text-zinc-500">{t('cart_subtotal')}</div>
                      <div className="font-mono text-lg font-bold">Rp {subtotal.toLocaleString('id-ID')}</div>
                    </div>
                    <button
                      onClick={() => clearMutation.mutate()}
                      disabled={clearMutation.isPending}
                      className="px-4 py-2 rounded-full border border-zinc-200 text-xs font-semibold text-zinc-600 hover:bg-zinc-50 transition-colors"
                    >
                      {t('cart_clear')}
                    </button>
                  </div>
                </div>
              </>
            )}
          </section>
        )}
      </div>
    </div>
  );
}
