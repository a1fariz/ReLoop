# ReLoop Circular Commerce Platform ♻️

[![Java 17](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Modulith](https://img.shields.io/badge/Spring%20Modulith-1.2.4-blue.svg)](https://spring.io/projects/spring-modulith)
[![Next.js 14](https://img.shields.io/badge/Next.js-14.2.15-black.svg)](https://nextjs.org/)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Redis 7](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)

ReLoop is an enterprise-grade circular commerce platform built for authenticated serialized electronics, 50-point technical grading certification, anti-hoarding checkout leases, and double-entry financial escrow accounting.

---

## 🏛️ Architecture Highlights

- **Modular Monolith Architecture:** Built with **Java 17**, **Spring Boot 3.3.4**, and **Spring Modulith** with automated architectural boundary verification (`ModulithArchitectureTest`).
- **Double-Entry Financial Ledger:** Every monetary movement (Escrow Hold, Platform Commission, Seller Payout, Partial Dispute Refund) is recorded in balanced Debit/Credit (`DR`/`CR`) journal lines with zero financial discrepancies ($\sum \text{Debits} = \sum \text{Credits}$).
- **Anti-Hoarding Checkout Lease:** Two-stage reservation system where adding to cart does not block inventory, but initiating checkout acquires a pessimistic row lock (`SELECT ... FOR UPDATE`) with a 15-minute lease guarded by a PostgreSQL **Partial Unique Index**.
- **Algorithmic Math Engines:**
  - *Multiplicative Trade-In Valuation:* Dimensionless condition, battery, and accessory factors multiplied against depreciated MSRP before repair cost deduction.
  - *Bayesian Seller Reputation:* Volume-weighted confidence curve eliminating cold-start bias for new sellers.
  - *50-Point Technical Inspection:* Weighted physical (40%), hardware (40%), and software (20%) grading (A+ to D) with critical failure circuit breaker.
- **Transactional Outbox Worker:** Background scheduled event poller using `SELECT ... FOR UPDATE SKIP LOCKED` for reliable async side effects.

---

## 🛠️ Technology Stack

### Backend
- **Java 17 LTS**
- **Spring Boot 3.3.4** (Spring Security, Spring Data JPA, Spring Modulith)
- **PostgreSQL 16** (Flyway Migrations V1 to V8)
- **Redis 7** (Redisson Distributed Locks)
- **JUnit 5 & AssertJ**

### Frontend
- **Next.js 14 (App Router)** & **TypeScript**
- **Tailwind CSS** (Design-MD & Stripe/Linear Tokens)
- **TanStack Query v5**
- **Lucide React Icons**

---

## 📁 Repository Structure

```text
reloop/
├── backend/                  # Spring Boot 3.3.4 Modular Monolith
│   ├── src/main/java/com/reloop/
│   │   ├── auth/             # JWT, Refresh Token Rotation, RBAC
│   │   ├── catalog/          # Canonical ProductModels & Categories
│   │   ├── units/            # Serialized ProductUnits & Physical Custody
│   │   ├── listings/         # Verified Seller Listings & Pricing Snapshots
│   │   ├── checkout/         # 15-min Anti-Hoarding Leases & Checkout Saga
│   │   ├── orders/           # Master Orders & Sub-Fulfillment Orders
│   │   ├── ledger/           # Double-Entry Financial Journal & Accounts
│   │   ├── tradein/          # Multiplicative Valuation Calculator
│   │   ├── inspections/      # 50-Point Technical Grading Engine
│   │   ├── warranties/       # Warranty Claims & Protection Policies
│   │   ├── disputes/         # Arbitrated Dispute Resolution & Split Refunds
│   │   ├── sellers/          # Bayesian Seller Reputation Metrics
│   │   ├── outbox/           # Transactional Outbox Poller Worker
│   │   └── audit/            # Immutable Append-Only Audit Trail
│   └── src/main/resources/db/migration/ # Flyway SQL Migrations (V1 to V8)
│
├── frontend/                 # Next.js 14 + Tailwind + TanStack Query
│   ├── src/app/
│   │   ├── page.tsx          # Certified Marketplace Landing Page
│   │   ├── catalog/          # Serialized Listing Catalog & 50-Pt Report
│   │   ├── checkout/[id]/    # Anti-Hoarding 15-min Countdown Lease Timer
│   │   ├── trade-in/         # Real-time Algorithmic Valuation Calculator
│   │   ├── warranties/       # Customer Warranty & Dispute Center
│   │   ├── seller/           # Seller Dashboard & Double-Entry Ledger View
│   │   └── login/ & register/# Authentication Pages
│   └── src/lib/              # queryKeys.ts & apiClient.ts
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

### 3. Run Backend (Spring Boot 3)
```bash
cd backend
mvn spring-boot:run
```
Backend starts on `http://localhost:8080` and applies Flyway migrations `V1` to `V8` automatically.

### 4. Run Backend Test Suite
```bash
cd backend
mvn clean test
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

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register new user account |
| `POST` | `/api/v1/auth/login` | Login and acquire Access Token + Refresh Token |
| `POST` | `/api/v1/auth/refresh` | Rotate refresh token with token-family reuse protection |
| `GET` | `/api/v1/catalog/models` | Fetch canonical product models |
| `GET` | `/api/v1/listings` | Fetch active verified listings with grading snapshots |
| `POST` | `/api/v1/checkout/reserve` | Acquire 15-minute anti-hoarding lease on serialized unit |
| `POST` | `/api/v1/checkout/confirm-payment` | Confirm payment, hold escrow, and mark unit sold |
| `POST` | `/api/v1/trade-in/calculate` | Calculate instant multiplicative valuation |
| `GET` | `/api/v1/sellers/{id}/metrics` | Fetch Bayesian reputation score and seller metrics |
| `GET` | `/api/v1/warranties/my` | Get active customer warranties |
| `POST` | `/api/v1/disputes` | File a post-purchase transaction dispute |
| `POST` | `/api/v1/disputes/{id}/resolve` | Arbitrate dispute with partial split ledger settlement |
