'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { UserRound, IdCard, BadgeCheck } from 'lucide-react';
import { apiErrorMessage, getMyKycStatus, getMyProfile, submitMyKyc, updateMyProfile } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useT } from '@/lib/i18n';
import { useAuthStore } from '@/lib/auth';
import type { ProfileDto } from '@/types/api';

const KYC_STATUS_STYLES: Record<string, string> = {
  PENDING: 'text-amber-700 bg-amber-50 border-amber-200',
  SUBMITTED: 'text-sky-700 bg-sky-50 border-sky-200',
  VERIFIED: 'text-emerald-700 bg-emerald-50 border-emerald-200',
  REJECTED: 'text-red-700 bg-red-50 border-red-200',
};

export default function ProfilePage() {
  const t = useT();
  const { accessToken } = useAuthStore();

  const { data: profile, isPending } = useQuery({
    queryKey: queryKeys.profile.me(),
    queryFn: () => getMyProfile(),
    enabled: accessToken !== null,
  });

  const { data: kyc, isPending: kycPending } = useQuery({
    queryKey: queryKeys.profile.kyc(),
    queryFn: () => getMyKycStatus(),
    enabled: accessToken !== null,
  });

  if (!accessToken) {
    return (
      <div className="min-h-screen bg-[#fafafa] py-24 text-center">
        <UserRound className="h-12 w-12 text-zinc-300 mx-auto mb-4" />
        <h1 className="text-2xl font-bold mb-2">{t('common_sign_in_required')}</h1>
      </div>
    );
  }

  const kycStatus = kyc?.kycStatus ?? profile?.kycStatus ?? 'PENDING';

  return (
    <div className="min-h-screen bg-[#fafafa] text-zinc-900 py-16 ambient-light-mesh">
      <div className="container mx-auto px-6 max-w-4xl space-y-12">
        <div className="pb-6 border-b border-zinc-200">
          <div className="text-xs font-mono uppercase tracking-widest text-sky-600 font-semibold mb-2">
            {t('nav_profile')}
          </div>
          <h1 className="text-4xl sm:text-5xl font-semibold tracking-tight">{t('prof_title')}</h1>
          <p className="text-sm text-zinc-600 mt-3 max-w-2xl leading-relaxed">{t('prof_desc')}</p>
        </div>

        {/* Profile card */}
        <section className="space-y-5">
          {isPending && <div className="h-40 rounded-3xl bg-zinc-100 animate-pulse" />}
          {!isPending && profile && (
            <>
              <ProfileCard profile={profile} />
              <ProfileForm profile={profile} />
            </>
          )}
        </section>

        {/* KYC verification */}
        <section className="space-y-5">
          <div className="flex items-center justify-between gap-4 flex-wrap">
            <h3 className="text-lg font-bold flex items-center gap-2"><IdCard className="h-5 w-5 text-sky-600" /> {t('prof_kyc')}</h3>
            <span className={`font-mono text-[10px] font-bold px-2.5 py-1 rounded-full border whitespace-nowrap ${KYC_STATUS_STYLES[kycStatus] ?? 'text-zinc-700 bg-zinc-50 border-zinc-200'}`}>
              {kycStatus}
            </span>
          </div>
          <p className="text-xs text-zinc-500 max-w-2xl leading-relaxed">{t('prof_kyc_desc')}</p>

          {kycPending && <div className="h-28 rounded-3xl bg-zinc-100 animate-pulse" />}

          {!kycPending && kyc && (kycStatus === 'SUBMITTED') && (
            <div className="bg-white border border-sky-200 rounded-3xl p-6 shadow-sm text-sm text-sky-700 flex items-center gap-3">
              <BadgeCheck className="h-5 w-5 shrink-0" />
              {t('prof_kyc_under_review')}
              {kyc.kycSubmittedAt && (
                <span className="font-mono text-[10px] text-sky-500">
                  {t('prof_kyc_submitted_at')}: {new Date(kyc.kycSubmittedAt).toLocaleDateString('id-ID')}
                </span>
              )}
            </div>
          )}

          {!kycPending && kyc && kycStatus === 'VERIFIED' && (
            <div className="bg-white border border-emerald-200 rounded-3xl p-6 shadow-sm space-y-3">
              <div className="flex items-center gap-2 text-sm font-bold text-emerald-700">
                <BadgeCheck className="h-5 w-5" /> {t('prof_kyc_ok')}
              </div>
              <div className="grid sm:grid-cols-2 gap-3 text-xs font-mono text-zinc-600">
                <div>{t('prof_kyc_doc_type')}: <span className="font-bold text-zinc-900">{kyc.kycDocumentType}</span></div>
                <div>{t('prof_kyc_national_id')}: <span className="font-bold text-zinc-900">{maskNationalId(kyc.nationalId)}</span></div>
                <div>{t('prof_kyc_dob')}: <span className="font-bold text-zinc-900">{kyc.dateOfBirth}</span></div>
                {kyc.kycVerifiedAt && (
                  <div>{t('prof_kyc_verified_at')}: <span className="font-bold text-zinc-900">{new Date(kyc.kycVerifiedAt).toLocaleDateString('id-ID')}</span></div>
                )}
              </div>
            </div>
          )}

          {!kycPending && (kycStatus === 'PENDING' || kycStatus === 'REJECTED') && <KycForm />}
        </section>
      </div>
    </div>
  );
}

function maskNationalId(nationalId: string | null): string {
  if (!nationalId) return '—';
  if (nationalId.length <= 4) return nationalId;
  return `****${nationalId.slice(-4)}`;
}

function ProfileCard({ profile }: { profile: ProfileDto }) {
  const t = useT();

  return (
    <div className="bg-white border border-zinc-200 rounded-3xl p-8 shadow-sm space-y-4">
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div className="flex items-center gap-4">
          <div className="h-12 w-12 rounded-2xl bg-zinc-900 text-white flex items-center justify-center shrink-0">
            <UserRound className="h-6 w-6" />
          </div>
          <div>
            <div className="font-bold text-lg">{profile.fullName}</div>
            <div className="text-sm text-zinc-500">{profile.email}</div>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <span className="font-mono text-[10px] font-bold px-2.5 py-1 rounded-full border text-sky-700 bg-sky-50 border-sky-200">
            {profile.role}
          </span>
          <span className={`font-mono text-[10px] font-bold px-2.5 py-1 rounded-full border ${
            profile.verified ? 'text-emerald-700 bg-emerald-50 border-emerald-200' : 'text-zinc-700 bg-zinc-50 border-zinc-200'
          }`}>
            {profile.verified ? t('prof_verified') : t('prof_unverified')}
          </span>
        </div>
      </div>
      <div className="grid sm:grid-cols-2 gap-4 pt-2 border-t border-zinc-100 text-xs font-mono">
        <div>
          <div className="text-[10px] uppercase tracking-wider text-zinc-400">{t('prof_phone')}</div>
          <div className="text-zinc-700 mt-1">{profile.phoneNumber ?? '—'}</div>
        </div>
        <div>
          <div className="text-[10px] uppercase tracking-wider text-zinc-400">{t('prof_address')}</div>
          <div className="text-zinc-700 mt-1 break-words">{profile.address || '—'}</div>
        </div>
      </div>
    </div>
  );
}

function ProfileForm({ profile }: { profile: ProfileDto }) {
  const t = useT();
  const queryClient = useQueryClient();
  const [fullName, setFullName] = useState(profile.fullName);
  const [phoneNumber, setPhoneNumber] = useState(profile.phoneNumber ?? '');
  const [address, setAddress] = useState(profile.address ?? '');
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  const mutation = useMutation({
    mutationFn: updateMyProfile,
    onSuccess: () => {
      setDone(true);
      queryClient.invalidateQueries({ queryKey: ['profile'] });
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        setError('');
        setDone(false);
        mutation.mutate({
          fullName: fullName.trim(),
          phoneNumber: phoneNumber.trim() || undefined,
          address: address.trim() || undefined,
        });
      }}
      className="bg-white border border-zinc-200 rounded-3xl p-8 space-y-5 shadow-sm"
    >
      <h4 className="text-sm font-bold">{t('prof_edit')}</h4>
      {done && <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs text-emerald-700">{t('prof_saved')}</div>}
      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}

      <div className="grid sm:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('prof_full_name')}</label>
          <input required value={fullName} onChange={(e) => setFullName(e.target.value)} className={inputCls} minLength={3} />
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('prof_phone')}</label>
          <input value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} className={inputCls} placeholder="+62…" />
        </div>
      </div>
      <div>
        <label className="block text-xs font-semibold mb-1.5">{t('prof_address')}</label>
        <textarea value={address} onChange={(e) => setAddress(e.target.value)} rows={3} className={inputCls} />
      </div>
      <button type="submit" disabled={mutation.isPending} className="btn-blue px-6 py-3 text-xs disabled:opacity-60">
        {mutation.isPending ? t('common_loading') : t('prof_save')}
      </button>
    </form>
  );
}

function KycForm() {
  const t = useT();
  const queryClient = useQueryClient();
  const [documentType, setDocumentType] = useState('KTP');
  const [documentReference, setDocumentReference] = useState('');
  const [nationalId, setNationalId] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  const mutation = useMutation({
    mutationFn: submitMyKyc,
    onSuccess: () => {
      setDone(true);
      queryClient.invalidateQueries({ queryKey: ['profile', 'kyc'] });
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const inputCls = 'w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500';

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        setError('');
        mutation.mutate({
          documentType,
          documentReference: documentReference.trim(),
          nationalId: nationalId.trim(),
          dateOfBirth,
        });
      }}
      className="bg-white border border-zinc-200 rounded-3xl p-8 space-y-5 shadow-sm"
    >
      <h4 className="text-sm font-bold">{t('prof_kyc_submit')}</h4>
      {done && <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs text-emerald-700">{t('prof_kyc_under_review')}</div>}
      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">{error}</div>}

      <div className="grid sm:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('prof_kyc_doc_type')}</label>
          <select value={documentType} onChange={(e) => setDocumentType(e.target.value)} className={inputCls}>
            <option value="KTP">KTP</option>
            <option value="PASSPORT">PASSPORT</option>
            <option value="SIM">SIM</option>
          </select>
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('prof_kyc_doc_ref')}</label>
          <input required value={documentReference} onChange={(e) => setDocumentReference(e.target.value)} className={inputCls} />
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('prof_kyc_national_id')}</label>
          <input required value={nationalId} onChange={(e) => setNationalId(e.target.value)} className={inputCls} />
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1.5">{t('prof_kyc_dob')}</label>
          <input required type="date" value={dateOfBirth} onChange={(e) => setDateOfBirth(e.target.value)} className={inputCls} />
        </div>
      </div>
      <button type="submit" disabled={mutation.isPending} className="btn-blue px-6 py-3 text-xs disabled:opacity-60">
        {mutation.isPending ? t('common_loading') : t('prof_kyc_submit')}
      </button>
    </form>
  );
}
