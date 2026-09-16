'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Bell, CheckCheck } from 'lucide-react';
import { getMyNotifications, markNotificationRead, markAllNotificationsRead, apiErrorMessage } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import { useAuthStore } from '@/lib/auth';
import FirebaseGoogleButton from '@/components/FirebaseGoogleButton';

export default function NotificationsPage() {
  const t = useT();
  const { accessToken } = useAuthStore();
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();

  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: queryKeys.notifications.mine(unreadOnly, page, 20),
    queryFn: () => getMyNotifications(unreadOnly, page, 20),
    enabled: accessToken !== null,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['notifications'] });

  const readMutation = useMutation({ mutationFn: markNotificationRead, onSuccess: invalidate });
  const readAllMutation = useMutation({ mutationFn: markAllNotificationsRead, onSuccess: invalidate });

  if (!accessToken) {
    return (
      <div className="min-h-screen bg-[#fafafa] py-24 text-center">
        <Bell className="h-12 w-12 text-zinc-300 mx-auto mb-4" />
        <h1 className="text-2xl font-bold mb-2">{t('common_sign_in_required')}</h1>
        <div className="mx-auto mt-4 max-w-xs">
          <FirebaseGoogleButton />
        </div>
      </div>
    );
  }

  const notifications = data?.items ?? [];
  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 py-16 ambient-light-mesh">
      <div className="container mx-auto px-6 max-w-4xl space-y-10">
        <div className="pb-6 border-b border-zinc-200">
          <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold mb-2">
            {t('nav_notifications')}
          </div>
          <h1 className="text-4xl sm:text-5xl font-semibold tracking-tight">{t('ntf_title')}</h1>
          <p className="text-sm text-zinc-600 mt-3 max-w-2xl leading-relaxed">{t('ntf_desc')}</p>
        </div>

        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div className="flex items-center gap-2">
            <button
              onClick={() => { setUnreadOnly(false); setPage(0); }}
              className={`px-4 py-2 rounded-full text-xs font-semibold border transition-colors ${
                !unreadOnly ? 'bg-zinc-900 text-white border-zinc-900' : 'bg-white text-zinc-600 border-zinc-200 hover:bg-zinc-50'
              }`}
            >
              {t('ntf_all')}
            </button>
            <button
              onClick={() => { setUnreadOnly(true); setPage(0); }}
              className={`px-4 py-2 rounded-full text-xs font-semibold border transition-colors ${
                unreadOnly ? 'bg-zinc-900 text-white border-zinc-900' : 'bg-white text-zinc-600 border-zinc-200 hover:bg-zinc-50'
              }`}
            >
              {t('ntf_unread')} {data ? `(${data.unreadCount})` : ''}
            </button>
          </div>
          <button
            onClick={() => readAllMutation.mutate()}
            disabled={readAllMutation.isPending || !data?.unreadCount}
            className="flex items-center gap-2 px-4 py-2 rounded-full border border-zinc-200 bg-white text-xs font-semibold text-zinc-600 hover:bg-zinc-50 transition-colors disabled:opacity-40"
          >
            <CheckCheck className="h-4 w-4" /> {t('ntf_read_all')}
          </button>
        </div>

        {isError && <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-6 text-center text-sm text-red-800"><p>{apiErrorMessage(error)}</p><button onClick={() => void refetch()} className="btn-primary-dark mt-4 px-5 py-3 text-xs">{t('common_try_again')}</button></div>}

        {isPending && <div className="h-28 rounded-2xl bg-zinc-100 animate-pulse" />}

        {!isPending && !isError && (
          <div className="space-y-3">
            {notifications.map((n) => (
              <button
                key={n.id}
                onClick={() => !n.isRead && readMutation.mutate(n.id)}
                className={`w-full text-left bg-white border rounded-2xl p-5 shadow-sm transition-all ${
                  n.isRead ? 'border-zinc-200 opacity-60' : 'border-sky-200 hover:border-sky-300'
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      {!n.isRead && <span className="h-2 w-2 rounded-full bg-sky-500 shrink-0" />}
                      <span className="font-bold text-sm">{n.title}</span>
                      <span className="font-mono text-[10px] font-bold px-2 py-0.5 rounded-full border border-zinc-200 text-zinc-500">
                        {n.category}
                      </span>
                    </div>
                    <p className="text-xs text-zinc-600 mt-1.5 leading-relaxed">{n.body}</p>
                  </div>
                  <span className="font-mono text-[10px] text-zinc-400 shrink-0">
                    {new Date(n.createdAt).toLocaleString('id-ID', { dateStyle: 'medium', timeStyle: 'short' })}
                  </span>
                </div>
              </button>
            ))}
            {notifications.length === 0 && (
              <div className="p-10 rounded-2xl border border-zinc-200 bg-white text-center text-sm text-zinc-500">
                {t('ntf_empty')}
              </div>
            )}
          </div>
        )}

        {data && data.total > data.size && (
          <div className="flex items-center justify-center gap-3 text-xs font-mono">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="px-4 py-2 rounded-full border border-zinc-200 font-semibold disabled:opacity-40">{t('common_prev')}</button>
            <span className="text-zinc-500">{t('common_page')} {page + 1} / {totalPages}</span>
            <button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)} className="px-4 py-2 rounded-full border border-zinc-200 font-semibold disabled:opacity-40">{t('common_next')}</button>
          </div>
        )}
      </div>
    </div>
  );
}
