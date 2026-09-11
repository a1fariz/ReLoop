'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { z } from 'zod';
import { AlertCircle } from 'lucide-react';
import { register } from '@/lib/api';
import { useT } from '@/lib/i18n';

const registerSchema = z.object({
  fullName: z.string().min(3, 'Full name must be at least 3 characters'),
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
});



export default function RegisterPage() {
  const router = useRouter();
  const t = useT();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  async function handleRegister(e: React.FormEvent) {
    e.preventDefault();
    setErrors({});
    const result = registerSchema.safeParse({ fullName, email, password });
    if (!result.success) {
      setErrors(Object.fromEntries(result.error.errors.map(({ path, message }) => [path[0], message])));
      return;
    }

    setLoading(true);
    try {
      await register({ fullName: result.data.fullName, email: result.data.email, password: result.data.password });
      router.push('/catalog');
    } catch (error: any) {
      setErrors({ form: error.response?.data?.message || t('auth_register_fail') });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 flex flex-col justify-center py-12 px-4 sm:px-6 ambient-light-mesh">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center space-y-3 mb-8"><div className="text-xs font-mono tracking-widest text-sky-600 uppercase font-semibold">{t('auth_identity_authority')}</div><h1 className="text-4xl font-semibold tracking-tight text-zinc-900">{t('auth_register_title')}</h1><p className="text-sm text-zinc-600 leading-relaxed max-w-sm mx-auto">{t('auth_register_desc')}</p></div>
      <div className="sm:mx-auto sm:w-full sm:max-w-md"><div className="luxury-card p-6 sm:p-8 space-y-6 shadow-md"><form className="space-y-5" onSubmit={handleRegister} noValidate>
        {errors.form && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700 flex gap-2"><AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />{errors.form}</div>}
        <Field id="fullName" label={t('auth_full_name')} value={fullName} onChange={setFullName} error={errors.fullName} autoComplete="name" />
        <Field id="email" label={t('auth_email')} value={email} onChange={setEmail} error={errors.email} type="email" autoComplete="email" />
        <Field id="password" label={t('auth_password')} value={password} onChange={setPassword} error={errors.password} type="password" autoComplete="new-password" />
        <button type="submit" disabled={loading} aria-busy={loading} className="w-full btn-blue py-3.5 text-sm shadow-md disabled:cursor-not-allowed disabled:opacity-60">{loading ? t('auth_registering') : t('auth_register')}</button>
      </form><div className="pt-5 border-t border-zinc-100 text-center text-xs text-zinc-500">{t('auth_have_account')} <Link href="/login" className="text-sky-600 hover:text-sky-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 rounded font-semibold transition-colors">{t('auth_login')}</Link></div></div></div>
    </div>
  );
}

function Field({ id, label, value, onChange, error, type = 'text', autoComplete }: { id: string; label: string; value: string; onChange: (value: string) => void; error?: string; type?: string; autoComplete: string }) {
  return <div><label htmlFor={id} className="block text-zinc-700 font-semibold mb-1.5 text-sm">{label}</label><input id={id} type={type} autoComplete={autoComplete} value={value} onChange={(e) => onChange(e.target.value)} aria-invalid={!!error} aria-describedby={error ? `${id}-error` : undefined} className="w-full rounded-xl border border-zinc-200 bg-zinc-50/80 px-4 py-3 text-sm text-zinc-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 focus-visible:ring-offset-2 focus:border-sky-400 focus:bg-white transition-all" />{error && <p id={`${id}-error`} className="mt-1.5 text-red-600 text-xs flex items-center gap-1.5"><AlertCircle className="h-3.5 w-3.5" />{error}</p>}</div>;
}
