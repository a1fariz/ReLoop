# ReLoop Circular Commerce Platform ♻️

[![Live Demo](https://img.shields.io/badge/Live_Demo-reloop.biz.id-00C853?style=for-the-badge&logo=vercel)](https://reloop.biz.id)
[![CI](https://github.com/a1fariz/ReLoop/actions/workflows/ci.yml/badge.svg)](https://github.com/a1fariz/ReLoop/actions/workflows/ci.yml)
[![Quarkus 3.15](https://img.shields.io/badge/Quarkus-3.15%20LTS-4695EB.svg)](https://quarkus.io/)
[![Java 17](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Next.js 14](https://img.shields.io/badge/Next.js-14.2.15-black.svg)](https://nextjs.org/)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Redis 7](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **Live deployment:** frontend on [reloop.biz.id](https://reloop.biz.id) (Vercel) · backend on Render (Singapore) · PostgreSQL on Neon · Redis on Upstash. Flyway V1–V22 auto-applies on boot, including demo seed data.
>
> **Demo accounts** (password `SecurePass123!`): `customer@reloop.com` · `seller@reloop.com` · `tech@reloop.com` · `admin@reloop.com`
>
> **Google Sign-In** also available — click "Sign in with Google" on the login page to authenticate via Firebase Auth without creating a password.

ReLoop is an enterprise-grade circular commerce platform built for authenticated serialized electronics, 50-point technical grading certification, anti-hoarding checkout leases, and double-entry financial escrow accounting.

---

## 🏛️ Architecture Highlights

- **Modular Monolith Architecture:** Primary backend on **Quarkus 3.15** (JVM) with 25 bounded-context modules, ArchUnit-verified boundaries (`ModuleBoundaryArchitectureTest`), transactional outbox, and immutable audit trail. A **Spring Boot 3.3 / Spring Modulith** twin is kept as a legacy reference (migration story).
- **Double-Entry Financial Ledger:** Every monetary movement (Escrow Hold, Platform Commission, Seller Payout, Partial Dispute Refund) is recorded in balanced Debit/Credit (`DR`/`CR`) journal lines with zero financial discrepancies ($\sum \text{Debits} = \sum \text{Credits}$).
- **Anti-Hoarding Checkout Lease:** Two-stage reservation system where adding to cart does not block inventory, but initiating checkout acquires a pessimistic row lock (`SELECT ... FOR UPDATE`) with a 15-minute lease guarded by a PostgreSQL **Partial Unique Index**.
- **Algorithmic Math Engines:**
  - *Multiplicative Trade-In Valuation:* Dimensionless condition, battery, and accessory factors multiplied against depreciated MSRP before repair cost deduction.
  - *Bayesian Seller Reputation:* Volume-weighted confidence curve eliminating cold-start bias for new sellers.
  - *50-Point Technical Inspection:* Weighted physical (40%), hardware (40%), and software (20%) grading (A+ to D) with critical failure circuit breaker.
- **Transactional Outbox Worker:** Background scheduled event poller using `SELECT ... FOR UPDATE SKIP LOCKED` for reliable async side effects.

---

## 🛠️ Technology Stack

### Backend (primary — Quarkus)
- **Java 17 LTS · Quarkus 3.15** (RESTEasy Reactive, Hibernate ORM Panache, Flyway)
- **PostgreSQL 16** — Flyway Migrations V1 to V22 (schema + demo seed + Firebase auth)
- **Redis 7** — catalog cache, login rate limiting (Upstash TLS in cloud)
- **JWT (JJWT) + BCrypt cost-12** — refresh token rotation family
- **Firebase Auth (Google Sign-In)** — ID token verification via Google Identity Toolkit API, auto-register/link users
- **Transactional Outbox** — LOG / Kafka dispatch, email bridge
- **JUnit 5, AssertJ, Mockito, ArchUnit, Testcontainers** — 115 unit tests + full-stack IT

### Backend (legacy reference — Spring)
- **Spring Boot 3.3.4** (Spring Security, Spring Data JPA, Spring Modulith)

### Frontend
- **Next.js 14 (App Router)** & **TypeScript**
- **Tailwind CSS** (Design-MD & Stripe/Linear Tokens)
- **TanStack Query v5** + Zustand auth store with single-flight refresh rotation
- **Firebase SDK** — Google Sign-In popup with `signInWithPopup`, integrated across all auth-guarded pages
- **Lucide React Icons**

---

## 📁 Repository Structure

```text
reloop/
├── backend-quarkus/          # Quarkus 3.15 LTS Modular Monolith (primary backend)
│   ├── src/main/java/com/reloop/
│   │   ├── auth/             # JWT, Refresh Token Rotation, RBAC, Redis Login Rate-Limit, Firebase Google Sign-In
│   │   ├── catalog/          # Canonical ProductModels & Categories (Redis-cached)
│   │   ├── units/            # Serialized ProductUnits & Physical Custody
│   │   ├── listings/         # Verified Seller Listings & Pricing Snapshots
│   │   ├── checkout/         # 15-min Anti-Hoarding Leases & Checkout Saga
│   │   ├── cart/             # Read-Only Snapshot Cart (No Inventory Lock)
│   │   ├── orders/           # Master Orders & Sub-Fulfillment Orders
│   │   ├── payments/        # Payment Attempts, Mock Gateway & Webhook
│   │   ├── ledger/           # Double-Entry Financial Journal & Accounts
│   │   ├── escrow/           # Escrow Contract Views & Admin Force-Release
│   │   ├── ownership/        # Legal Ownership Provenance Chain
│   │   ├── tradein/          # Multiplicative Valuation Calculator
│   │   ├── inspections/      # 50-Point Technical Grading Engine
│   │   ├── refurbishment/    # Repair Tickets, Component Replacement & Re-Grading
│   │   ├── warranties/       # Warranty Claims & Protection Policies
│   │   ├── returns/          # Return Authorizations, Logistics & Refund Journal
│   │   ├── disputes/         # Arbitrated Dispute Resolution & Split Refunds
│   │   ├── users/            # User Profiles & KYC Verification Logs
│   │   ├── sellers/          # Bayesian Seller Reputation Metrics
│   │   ├── notifications/    # In-App Notification Center + Email Outbox Bridge
│   │   ├── outbox/           # Transactional Outbox → Kafka / Email Dispatch
│   │   └── audit/            # Immutable Append-Only Audit Trail
│   └── src/main/resources/db/migration/ # Flyway SQL Migrations (V1 to V22)
│
├── backend/                  # Spring Boot 3.3.4 Modular Monolith (legacy reference)
│   └── ...                   # Same module layout; kept until Quarkus parity is signed off
│
├── frontend/                 # Next.js 14 + Tailwind + TanStack Query
│   ├── src/app/
│   │   ├── page.tsx          # Certified Marketplace Landing Page
│   │   ├── catalog/          # Serialized Listing Catalog & 50-Pt Report
│   │   ├── checkout/[id]/    # Anti-Hoarding 15-min Countdown Lease Timer
│   │   ├── cart/             # Read-Only Snapshot Cart
│   │   ├── returns/          # Return Request Center
│   │   ├── notifications/    # In-App Notification Center
│   │   ├── trade-in/         # Real-time Algorithmic Valuation Calculator
│   │   ├── warranties/       # Customer Warranty & Dispute Center
│   │   ├── seller/           # Seller Dashboard & Double-Entry Ledger View
│   │   └── login/ & register/# Authentication Pages (Email/Password + Google Sign-In)
│   └── src/lib/              # queryKeys.ts & apiClient.ts & firebase.ts
│
├── docker-compose.yml        # PostgreSQL 16, Redis 7, Mailpit
├── .env.example              # Environment Configuration Template
├── .gitignore                # Clean Artifact Exclusions
└── README.md                 # Project Documentation
```

---

## 🚀 Getting Started

### 1. Configure Environment Variables
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

### 2. Start Infrastructure (Docker Compose)
```bash
docker compose up -d
```
Services started:
- **PostgreSQL 16:** `localhost:5433` (DB: `reloop_db`, User: `reloop_app`, Password: `reloop_secret_password`)
- **Redis 7:** `localhost:6379`
- **Mailpit:** `http://localhost:8025` (SMTP: `1025`)

### 3. Run Backend (Quarkus — recommended)
```bash
cd backend-quarkus
mvn quarkus:dev
```
Backend starts on `http://localhost:8080` and applies Flyway migrations `V1` to `V22` automatically.
Extras over the legacy stack: Swagger UI at [`/q/swagger-ui`](http://localhost:8080/q/swagger-ui),
health at `/q/health`, Prometheus metrics at `/q/metrics`, Redis-backed catalog cache &
login rate limiting, and real Kafka/email dispatch from the transactional outbox
(`OUTBOX_DISPATCH_MODE=KAFKA` with `docker-compose.kafka.yml`).

### 3b. Run Backend (Spring Boot 3 — legacy, deprecated)
```bash
cd backend
mvn spring-boot:run
```
Kept as a behavioral reference while `backend-quarkus/` is validated; both run
against the same schema (identical Flyway migrations and JWT format).

### 4. Run Backend Test Suite
```bash
# Quarkus backend: unit + ArchUnit boundary tests (no Docker needed)
cd backend-quarkus && mvn clean test

# Full suite including Testcontainers integration tests (requires Docker)
cd backend-quarkus && mvn clean verify

# Legacy backend
cd backend && mvn clean test
```

### 5. Run Frontend (Next.js 14)
```bash
cd frontend
npm install
npm run dev
```
Frontend runs at `http://localhost:3000`.

---

## 📡 REST API Summary

| Layanan | Port | URL / Akses | Keterangan |
|---|---|---|---|
| **API Gateway (Nginx)** | `8000` | http://localhost:8000 | Reverse proxy terpusat, rate limiter & auth routing |
| **Frontend (Next.js 14)** | `3000` | http://localhost:3000 | Web UI / Marketplace |
| **Backend (Spring Boot 3)** | `8080` | http://localhost:8080 | REST API & Actuator |
| **PostgreSQL 16 (Docker)** | `5433` | `localhost:5433/reloop_db` | Database Utama (User: `reloop_app`) |
| **Redis 7 (Docker)** | `6379` | `localhost:6379` | Cache & Redisson Locks |
| **Mailpit Web UI (Docker)** | `8025` | http://localhost:8025 | Dashboard Email Testing |
| **Mailpit SMTP (Docker)** | `1025` | `localhost:1025` | Server Pengiriman Email Lokal |

---

## ☁️ Live Deployment

| Layer | Provider | URL |
|---|---|---|
| Frontend | Vercel | https://reloop.biz.id |
| Backend (Quarkus JVM) | Render (Singapore, free) | https://reloop-backend-b5qx.onrender.com |
| PostgreSQL 18 | Neon (Singapore) | Flyway V1–V22 auto-migrated on boot |
| Redis (TLS) | Upstash (Singapore) | Catalog cache + login rate limiting |

Deploy configuration: [`render.yaml`](render.yaml) (backend service definition). The container is built from [`backend-quarkus/Dockerfile`](backend-quarkus/Dockerfile) directly from this repository.

**Demo accounts** (password `SecurePass123!`):

| Role | Email | What to try |
|---|---|---|
| Customer | `customer@reloop.com` | Catalog → checkout 15-min lease → orders → returns → notifications |
| Seller | `seller@reloop.com` | Seller hub: fulfillments, ship with tracking, ledger views |
| Technician | `tech@reloop.com` | 50-point grading form, repair bench with QC + re-grading |
| Admin | `admin@reloop.com` | Ops console: fulfillments pipeline, dispute arbitration, escrow stats, returns desk |

> Note: free-tier services sleep when idle — the first request after inactivity takes ~50 seconds to wake the backend.

## 🔒 Security Notes

- JWT HS256 with env-injected secret (`JWT_SECRET`), 15-min access tokens, 7-day rotating refresh families
- Firebase Auth integration: server-side ID token verification via Google Identity Toolkit API (no mock/bypass in production)
- IDOR guards: every resource access verifies ownership server-side (buyer/seller/technician/admin)
- PostgreSQL authority: `audit_logs` & `lifecycle_events` append-only; financial movements only via double-entry journals
- Rate limiting: Redis-backed login throttling + Nginx gateway rate zones
- No client-trusted money: all totals recomputed server-side from DB records
