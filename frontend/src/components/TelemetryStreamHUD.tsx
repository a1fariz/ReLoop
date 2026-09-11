'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Activity, ShieldCheck, CheckCircle2, Sparkles, Server } from 'lucide-react';

const mockStreams = [
  { time: '14:22:01', serial: 'F2LZ90K8MD6M', model: 'iPhone 15 Pro Max', test: '50-Point Hardware Pass', grade: 'Grade A+', status: 'CERTIFIED' },
  { time: '14:21:44', serial: 'C02G89A3MD6T', model: 'MacBook Air M2 15"', test: 'Battery Telemetry (94%)', grade: 'Grade A', status: 'CERTIFIED' },
  { time: '14:21:12', serial: 'PF3A9812ZK09', model: 'ThinkPad X1 Carbon', test: 'Thermal Stress 100%', grade: 'Grade B+', status: 'PASSED' },
  { time: '14:20:50', serial: 'DNPZ883K10', model: 'iPad Pro 11" M2', test: 'Liquid Indicator Clear', grade: 'Grade A+', status: 'CERTIFIED' },
  { time: '14:20:15', serial: 'R5CW109LMN', model: 'Galaxy S24 Ultra', test: 'Display Sub-pixel Test', grade: 'Grade A+', status: 'CERTIFIED' },
];

export function TelemetryStreamHUD() {
  const [logs, setLogs] = useState(mockStreams);

  useEffect(() => {
    const interval = setInterval(() => {
      const randomTime = new Date().toTimeString().split(' ')[0];
      const newEntry = {
        time: randomTime,
        serial: 'SN-' + Math.random().toString(36).substring(2, 8).toUpperCase(),
        model: 'Certified Intake Unit',
        test: 'Diagnostic Gate Verified',
        grade: 'Grade A+',
        status: 'PASSED',
      };
      setLogs((prev) => [newEntry, ...prev.slice(0, 4)]);
    }, 4500);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="w-full bg-white border border-zinc-200 rounded-3xl p-6 sm:p-8 space-y-4 shadow-sm select-none">
      <div className="flex items-center justify-between border-b border-zinc-100 pb-4">
        <div className="flex items-center gap-2.5 text-zinc-900 font-bold text-sm">
          <div className="relative flex h-2.5 w-2.5">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
          </div>
          <span className="tracking-tight">Live Diagnostic Intake Stream [Jakarta Central Hub]</span>
        </div>
        <div className="flex items-center gap-2 text-xs font-mono text-zinc-500">
          <Server className="h-3.5 w-3.5 text-sky-600" />
          <span className="hidden sm:inline">Rate: 20 req/s • Automated</span>
        </div>
      </div>

      <div className="space-y-2.5 overflow-hidden font-mono text-xs">
        <AnimatePresence initial={false}>
          {logs.map((log) => (
            <motion.div
              key={log.serial + log.time}
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 8 }}
              transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
              className="flex items-center justify-between p-3.5 bg-zinc-50/80 hover:bg-zinc-100/80 rounded-2xl border border-zinc-200/70 text-xs transition-colors"
            >
              <div className="flex items-center gap-3">
                <span className="text-zinc-400 font-mono text-[11px]">{log.time}</span>
                <span className="text-zinc-900 font-semibold">{log.serial}</span>
                <span className="text-zinc-600 hidden sm:inline">{log.model}</span>
                <span className="text-zinc-400 hidden md:inline">• {log.test}</span>
              </div>
              <div className="flex items-center gap-2.5">
                <span className="text-emerald-700 font-bold bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-full text-[10px]">
                  {log.grade}
                </span>
                <span className="font-semibold text-zinc-700 bg-white border border-zinc-200 px-2 py-0.5 rounded-md text-[10px]">
                  {log.status}
                </span>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </div>
  );
}
