'use client';

import { useEffect } from 'react';
import { useI18nStore } from '@/lib/i18n';

export function LocaleSync() {
  const lang = useI18nStore((s) => s.lang);
  useEffect(() => {
    document.documentElement.lang = lang === 'id' ? 'id' : 'en';
  }, [lang]);
  return null;
}