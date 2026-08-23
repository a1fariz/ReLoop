'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { RefreshCw, User, Mail, Lock, Phone } from 'lucide-react';

export default function RegisterPage() {
  const router = useRouter();
  const [fullName, setFullName] = useState('Muhammad Alfarizi');
  const [email, setEmail] = useState('alfarizi@example.com');
  const [password, setPassword] = useState('SecurePass123!');
  const [role, setRole] = useState('CUSTOMER');

  const handleRegister = (e: React.FormEvent) => {
    e.preventDefault();
    if (typeof window !== 'undefined') {
      localStorage.setItem('accessToken', 'mock-jwt-token-demo');
    }
    router.push('/catalog');
  };

  return (
    <div className="min-h-screen bg-background flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="inline-flex items-center justify-center h-12 w-12 rounded-xl bg-primary/10 text-primary mb-4">
          <RefreshCw className="h-6 w-6" />
        </div>
        <h2 className="text-3xl font-extrabold tracking-tight text-foreground">Create your ReLoop Account</h2>
        <p className="mt-2 text-sm text-muted-foreground">Join the next-generation authenticated circular economy.</p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-card py-8 px-4 shadow sm:rounded-xl sm:px-10 border border-border">
          <form className="space-y-4" onSubmit={handleRegister}>
            <div>
              <label className="block text-sm font-medium text-foreground">Full Name</label>
              <input
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className="mt-1 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-primary"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-foreground">Email address</label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-1 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-primary"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-foreground">Password</label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-1 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-primary"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-foreground">Account Type</label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value)}
                className="mt-1 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground"
              >
                <option value="CUSTOMER">Customer / Buyer / Individual Trader</option>
                <option value="SELLER">Verified Commercial Seller</option>
                <option value="TECHNICIAN">Certified Inspection Technician</option>
              </select>
            </div>

            <button
              type="submit"
              className="w-full mt-4 flex justify-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-semibold text-white bg-primary hover:bg-primary-hover focus:outline-none"
            >
              Create Account
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
