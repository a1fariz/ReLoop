# ReLoop Circular Commerce Platform ♻️

Enterprise-grade circular commerce platform for authenticated serialized electronics, technical grading certification, anti-hoarding checkout leases, and double-entry financial escrow accounting.

---

## 🏛️ Architecture Highlights

- **Modular Monolith:** Built with **Java 17**, **Spring Boot 3.3.4**, and **Spring Modulith** with automated architectural boundary verification.
- **Double-Entry Financial Ledger:** Every monetary movement (Escrow Hold, Platform Commission, Seller Payout, Partial Dispute Refund) is recorded in balanced Debit/Credit (`DR`/`CR`) journal lines with zero financial discrepancies ($\sum \text{Debits} = \sum \text{Credits}$).
- **Anti-Hoarding Checkout Lease:** Two-stage reservation system where adding to cart does not block inventory, but initiating checkout acquires a pessimistic row lock (`SELECT ... FOR UPDATE`) with a 15-minute lease guarded by a PostgreSQL **Partial Unique Index**.
- **Algorithmic Math Engines:**
  - *Multiplicative Trade-In Valuation:* Dimensionless condition, battery, and accessory factors multiplied against depreciated MSRP before repair cost deduction.
  - *Bayesian Seller Reputation:* Volume-weighted confidence curve eliminating cold-start bias for new sellers.
  - *50-Point Technical Inspection:* Weighted physical (40%), hardware (40%), and software (20%) grading (A+ to D) with critical failure circuit breaker.
- **Transactional Outbox Worker:** Background scheduled event poller using `SELECT ... FOR UPDATE SKIP LOCKED` for reliable async side effects.

---

## 🛠️ Tech Stack

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

## 🚀 Quick Start Guide

### 1. Start Infrastructure via Docker Compose
```bash
docker compose up -d
```
Services started:
- PostgreSQL on port `5433` (DB: `reloop_db`, User: `reloop_app`, Password: `reloop_secret_password`)
- Redis on port `6379`
- Mailpit Web UI on port `8025` (SMTP port `1025`)

### 2. Run Backend
```bash
cd backend
mvn spring-boot:run
```
Backend will start on `http://localhost:8080` and execute Flyway migrations V1 to V8 automatically.

### 3. Run Backend Test Suite
```bash
cd backend
mvn clean test
```

### 4. Run Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend will be available at `http://localhost:3000`.

---

## 📚 API Endpoints Summary

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register new user (Customer, Seller, Technician) |
| `POST` | `/api/v1/auth/login` | Authenticate and obtain JWT + Refresh Token |
| `POST` | `/api/v1/auth/refresh` | Rotate refresh token with token-family reuse detection |
| `GET` | `/api/v1/catalog/models` | List all canonical product models |
| `GET` | `/api/v1/listings` | List active verified serialized listings |
| `POST` | `/api/v1/checkout/reserve` | Acquire 15-minute anti-hoarding checkout lease |
| `POST` | `/api/v1/trade-in/calculate` | Calculate real-time algorithmic valuation |
| `GET` | `/api/v1/sellers/{id}/metrics` | Fetch Bayesian seller reputation & metrics |
| `GET` | `/api/v1/warranties/my` | List authenticated user active warranties |
| `POST` | `/api/v1/disputes` | Register a transaction dispute |
| `POST` | `/api/v1/disputes/{id}/resolve` | Arbitrate dispute with partial split settlement |
