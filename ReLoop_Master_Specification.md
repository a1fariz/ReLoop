# ReLoop Circular Commerce Platform
## PRD + Security + Architecture + Data + Business Logic + Testing + Deployment Master Specification

> **Status:** Production-Hardened Engineering Master Specification (v2.0)  
> **Scope:** Full-system architectural synthesis, security hardening, and domain correction  
> **Primary Goal:** Provide a deterministic, internally consistent, auditable, and resilient circular-commerce platform.

---

# 0. Executive Summary & Root Cause Corrections

ReLoop is an enterprise circular-commerce platform managing the complete physical and economic lifecycle of serialized electronic devices:

- Canonical Product Catalog & Specification Models (`ProductModel`)
- Serialized Product Units & Physical Custody Tracking (`ProductUnit`)
- Verified Seller Listings & Pricing Snapshots (`Listing`)
- Anti-Hoarding Shopping Cart & Two-Stage Reservations (`unit_reservations`)
- Multi-Seller Sub-Order Splitting & Idempotent Checkout Saga
- Double-Entry Financial Ledger & Escrow Settlement (Full/Partial Disputes, Platform Commission, Payouts)
- Legal Ownership Provenance & Immutable Custody Chain
- Multiplicative Trade-In Valuation & Counter-Offer Workflows
- Multi-Tier Technical Inspection, Grading (A+ to D), and Template Versioning
- Refurbishment, Component Replacement Tracking, and Re-Grading
- Deterministic Warranty Activation & Claim Resolution
- Multi-Stage Returns & Evidence Preservation
- Managed Dispute Resolution with Arbitrated Ledger Adjustments
- Bayesian-Smoothed Seller Reputation & Verified Reviews
- Transactional Outbox Pattern for Guaranteed Asynchronous Side Effects
- Append-Only Audit Logging & High-Fidelity Observability

---

### Root Flaws in Prior Drafts Corrected in This Specification:

1. **Elimination of Dual-State Redundancy (`ProductUnit` vs `Inventory`):**  
   In a serialized 1-to-1 inventory model, maintaining separate `inventory.status` and `product_units.current_status` tables creates split-brain risks. Availability is consolidated directly inside `product_units` and guarded by transient `unit_reservations`.
2. **Closed-Loop State Transitions & Trade-In Rejection Path:**  
   Resolved dead-ends in `IN_INSPECTION`. Added explicit rejection/counter-offer decline paths transitioning to `RETURNED_TO_OWNER` and `IN_TRANSIT` with physical logistics tracking.
3. **Mathematical Dimensionality in Valuation:**  
   Fixed calculation bug where monetary sums were directly added to percentage rates. Battery health and accessory bonuses are strictly defined as dimensionless multiplicative coefficients prior to currency-denominated repair deductions.
4. **Anti-Hoarding Cart Architecture:**  
   Prevented Denial-of-Inventory attacks. Adding items to a shopping cart is a read-only snapshot; pessimistic database locks (`SELECT ... FOR UPDATE`) and 15-minute reservation tokens are acquired strictly at Checkout initiation (*Intent to Pay*).
5. **Double-Entry Financial Ledger for Escrow & Partial Settlements:**  
   Replaced binary escrow states with a double-entry ledger (`financial_accounts`, `financial_journal_entries`, `financial_ledger_lines`) supporting platform commission retention (*take-rate*), seller wallet balances, and split payouts during partial dispute resolutions.
6. **Bayesian Smoothing for Seller Reputation:**  
   Eliminated cold-start bias where new sellers without transaction history received inflated 70/100 scores. Reputations start at a neutral 50 with a volume-confidence damper.
7. **Lock Hierarchy & Concurrency Discipline:**  
   Established deterministic lock acquisition ordering (`ORDER BY id ASC`) and scheduled worker isolation using `SELECT ... FOR UPDATE SKIP LOCKED` to prevent deadlocks and `@Version` collisions.
8. **Multi-Seller Fulfillment Splitting:**  
   Decomposed master orders into per-seller `fulfillment_orders` to accommodate independent shipping timelines and isolated escrow releases.
9. **UI/UX Pro Max & Design-MD Integration:**  
   Standardized design tokens, accessible color pairings (WCAG AA 4.5:1), responsive layout breakpoints, and TanStack Query server-state invalidation contracts.

---

# 1. Constitutional Engineering Invariants

Every service, transaction, and pull request MUST adhere to these non-negotiable rules:

- **Rule 001 (PostgreSQL Authority):** PostgreSQL is the sole transactional source of truth. Redis is restricted to caching, rate limiting, and short-lived coordination.
- **Rule 002 (Single-Unit Invariant):** One physical unit (`ProductUnit`) CANNOT simultaneously have more than one active reservation, active listing, or active legal owner.
- **Rule 003 (Encapsulated State Mutation):** Direct SQL updates to entity statuses are prohibited. All transitions MUST execute through domain-specific state transition managers.
- **Rule 004 (Immutable Audit Ledger):** `audit_logs` and `lifecycle_events` are append-only. `UPDATE` and `DELETE` privileges are revoked at the database role level.
- **Rule 005 (Double-Entry Financial Integrity):** Every financial movement (escrow hold, release, fee deduction, refund, payout) MUST balance to zero across debit and credit ledger lines.
- **Rule 006 (Server-Authoritative Pricing):** Client-submitted prices, discounts, and fee calculations are completely untrusted. The backend recomputes all sums from authoritative database records.
- **Rule 007 (Idempotent Execution):** Every mutation endpoint requires a unique `Idempotency-Key`. Retrying an operation with the same payload yields the identical response without duplicate side effects.
- **Rule 008 (Outbox Pattern for Side Effects):** No external network calls (emails, push notifications, webhooks, payment gateway sync) are permitted inside database transaction boundaries. All events pass through `outbox_events`.
- **Rule 009 (Strict Decimal Arithmetic):** All monetary values MUST use `NUMERIC(15,2)` in PostgreSQL and `BigDecimal` in application code. Floating-point types (`float`, `double`) are banned.
- **Rule 010 (UTC Timestamp Standard):** All temporal fields MUST use `TIMESTAMPTZ` stored in UTC. Timezone conversions occur strictly at the frontend display layer.
- **Rule 011 (Resource-Level IDOR Protection):** Role-based checks (`@PreAuthorize`) MUST be paired with resource ownership verification (`resource.owner_id == auth.user_id`).
- **Rule 012 (Zero Unbounded Queries):** All list endpoints enforce mandatory pagination (`limit` $\le 100$) and explicit sort-column allowlists.

---

# 2. System Architecture & Modular Monolith

ReLoop is structured as a **Modular Monolith** using **Spring Modulith** and **ArchUnit** enforcement to prevent cyclic dependencies and illegal cross-domain coupling.

```
reloop/
├── auth/           # Authentication, Token Rotation, MFA, RBAC
├── users/          # User Profiles, KYC, Verification
├── sellers/        # Seller Profiles, Wallets, Reputation Metrics
├── catalog/        # Product Categories, Brands, ProductModels
├── units/          # ProductUnits, Serial Numbers, Physical Custody
├── listings/       # Seller Listings, Price Snapshots, Listing Status
├── cart/           # Shopping Cart Snapshots (No Inventory Lock)
├── checkout/       # Checkout Orchestration, Saga Compensation
├── orders/         # Master Orders & Per-Seller Fulfillment Sub-Orders
├── payments/       # Payment Attempts, Gateway Integrations, Webhooks
├── ledger/         # Double-Entry Accounts, Journal Entries, Payouts
├── escrow/         # Escrow Contract Lifecycle & Settlement Rules
├── ownership/      # Legal Ownership Provenance & Transfer Records
├── tradein/        # Trade-In Inquiries, Valuations, Counter-Offers
├── inspections/    # Technical Checklists, Grade Calculations, QC
├── refurbishment/  # Repair Tickets, Component Replacement Records
├── warranties/     # Warranty Policies, Claims, Repair Resolutions
├── returns/        # Return Authorizations, Logistics, Inspection
├── disputes/       # Multi-Party Dispute Arbitration & Split Settlement
├── reviews/        # Verified Transactional Reviews & Moderation
├── notifications/  # Notification Dispatcher (In-App, Email, Push)
├── audit/          # Immutable System Mutation Audit Logger
├── outbox/         # Transactional Outbox Poller & Dead-Letter Manager
└── common/         # Global Exceptions, Security Filters, Base DTOs
```

### Module Boundary Enforcement:
```
[ Controller ]
      ↓
[ Application Service ]
      ↓
[ Domain Service / Model ]
      ↓
[ Repository ]
```
- Modules interact via **Public Application Service Interfaces** or **Spring ApplicationEvents** (via Outbox).
- Cross-module direct JPA entity relationships and cross-repository queries are prohibited.

---

# 3. Technology Stack & Runtime Infrastructure

### Backend:
- **Runtime:** Java 21 LTS
- **Framework:** Spring Boot 3.3+ (Spring Security, Spring Data JPA, Spring Modulith)
- **Database:** PostgreSQL 16+ (Connection Pooling via HikariCP)
- **Database Migrations:** Flyway (Strict validation, immutable versioned scripts)
- **Cache & Fast Coordination:** Redis 7+ (Redisson client for distributed locks)
- **Validation:** Hibernate Validator (Bean Validation 3.0)
- **Testing:** JUnit 5, AssertJ, Testcontainers (PostgreSQL & Redis), WireMock, ArchUnit

### Frontend:
- **Core:** React 18+ / Next.js 14+ (App Router), TypeScript 5.4+
- **Server State:** TanStack Query v5 (React Query)
- **Forms & Validation:** React Hook Form + Zod
- **Styling:** Tailwind CSS v3.4+
- **Design System:** Design-MD tokens + UI/UX Pro Max standards
- **Icons:** Lucide React (SVG-only, no raw emoji icons)

---

# 4. Data Models & Database Invariants

```sql
-- 1. Serialized Product Unit (Consolidated Lifecycle & Physical Custody)
CREATE TYPE product_unit_status AS ENUM (
    'DRAFT',
    'AVAILABLE',
    'RESERVED',
    'SOLD',
    'IN_TRANSIT',
    'DELIVERED',
    'OWNED',
    'TRADE_IN_PENDING',
    'IN_INSPECTION',
    'IN_REPAIR',
    'REFURBISHED',
    'READY_FOR_LISTING',
    'LISTED',
    'RETURN_INSPECTION',
    'RETURNED_TO_OWNER',
    'DAMAGED',
    'DISPOSED'
);

CREATE TYPE physical_custody_type AS ENUM (
    'OWNER',
    'PLATFORM_HUB',
    'SELLER',
    'LOGISTICS_3PL',
    'BUYER'
);

CREATE TABLE product_units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_model_id UUID NOT NULL REFERENCES product_models(id),
    serial_number VARCHAR(100) NOT NULL,
    current_owner_id BIGINT NOT NULL REFERENCES users(id),
    current_custody physical_custody_type NOT NULL DEFAULT 'OWNER',
    status product_unit_status NOT NULL DEFAULT 'DRAFT',
    grade VARCHAR(5), -- 'A+', 'A', 'B+', 'B', 'C', 'D'
    cost_price NUMERIC(15,2),
    selling_price NUMERIC(15,2),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_model_serial UNIQUE (product_model_id, serial_number)
);

-- 2. Anti-Hoarding Transient Unit Reservations
CREATE TABLE unit_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES product_units(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    listing_id UUID NOT NULL REFERENCES listings(id),
    token UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'CONVERTED', 'EXPIRED', 'CANCELLED'
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reservation_ttl CHECK (expires_at > created_at)
);

-- Partial Unique Index: Guarantees exactly 1 active reservation per unit
CREATE UNIQUE INDEX uq_single_active_unit_reservation 
ON unit_reservations (unit_id) 
WHERE status = 'ACTIVE';

-- 3. Verified Seller Listings
CREATE TABLE listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES product_units(id),
    seller_id BIGINT NOT NULL REFERENCES sellers(id),
    title VARCHAR(255) NOT NULL,
    asking_price NUMERIC(15,2) NOT NULL CHECK (asking_price > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'PENDING_REVIEW', 'ACTIVE', 'PAUSED', 'SOLD', 'REMOVED'
    grade_snapshot VARCHAR(5) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_single_active_listing_per_unit 
ON listings (unit_id) 
WHERE status = 'ACTIVE';

-- 4. Master Order & Multi-Seller Sub-Orders
CREATE TABLE master_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(64) NOT NULL UNIQUE,
    buyer_id BIGINT NOT NULL REFERENCES users(id),
    total_amount NUMERIC(15,2) NOT NULL CHECK (total_amount >= 0),
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE fulfillment_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    master_order_id UUID NOT NULL REFERENCES master_orders(id),
    seller_id BIGINT NOT NULL REFERENCES sellers(id),
    unit_id UUID NOT NULL REFERENCES product_units(id),
    subtotal_amount NUMERIC(15,2) NOT NULL CHECK (subtotal_amount > 0),
    platform_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    seller_net_amount NUMERIC(15,2) NOT NULL CHECK (seller_net_amount >= 0),
    fulfillment_status VARCHAR(30) NOT NULL DEFAULT 'PROCESSING',
    escrow_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    tracking_number VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

# 5. Double-Entry Financial Ledger & Escrow Settlement

All financial transactions follow standard accounting ledger rules. Balance columns are calculated views; movements are immutable ledger lines.

```sql
CREATE TABLE financial_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE, -- 'ESCROW_HELD', 'PLATFORM_REVENUE', 'SELLER_PAYABLE:101', 'GATEWAY_CLEARING'
    account_type VARCHAR(30) NOT NULL, -- 'ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'
    currency VARCHAR(3) NOT NULL DEFAULT 'IDR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE financial_journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_type VARCHAR(50) NOT NULL, -- 'ORDER_PAYMENT', 'ESCROW_SETTLEMENT', 'PARTIAL_REFUND', 'PAYOUT'
    reference_id VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE financial_ledger_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_entry_id UUID NOT NULL REFERENCES financial_journal_entries(id),
    account_id UUID NOT NULL REFERENCES financial_accounts(id),
    entry_type VARCHAR(2) NOT NULL, -- 'DR' (Debit) or 'CR' (Credit)
    amount NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### Escrow State Machine:
```
PENDING
  ↓
HELD (Payment Confirmed)
  ├── Full Delivery → SETTLED (Payout Split: Seller Net to SELLER_PAYABLE, Platform Fee to PLATFORM_REVENUE)
  ├── Customer Cancellation (Pre-shipment) → FULLY_REFUNDED
  └── Dispute Opened → DISPUTED
         ├── Dispute Rejected → SETTLED
         ├── Full Buyer Fault/Return → FULLY_REFUNDED
         └── Arbitrated Partial Agreement → PARTIALLY_SETTLED (Split DR/CR Lines)
```

---

# 6. Complete Domain State Machines

### 6.1 ProductUnit State Machine
```
[DRAFT] → [IN_INSPECTION] → [READY_FOR_LISTING] → [LISTED] ⇄ [RESERVED]
              │                                      │
              ├─ (Repair needed) → [IN_REPAIR] → [REFURBISHED] ┘
              ├─ (Unrepairable) → [DAMAGED] → [DISPOSED]
              └─ (Trade-in rejected) → [RETURNED_TO_OWNER]

[RESERVED] → [SOLD] → [IN_TRANSIT] → [DELIVERED] → [OWNED] (Warranty Active)
                                         │
                                         └─ (Return requested) → [RETURN_INSPECTION]
                                                                     ├── Accepted → [IN_REPAIR] / [READY_FOR_LISTING]
                                                                     └── Rejected → [RETURNED_TO_OWNER]
```

### 6.2 Listing State Machine
```
DRAFT → PENDING_REVIEW → ACTIVE ⇄ PAUSED
                            │
                            ├─ (Unit Reserved/Sold) → SOLD
                            ├─ (Seller Deleted) → REMOVED
                            └─ (Listing Inactive TTL) → EXPIRED
```

### 6.3 Order Fulfillment State Machine
```
PENDING_PAYMENT → PAID → PROCESSING → SHIPPED → DELIVERED → COMPLETED
       │                   │                                    │
       └─ (Timeout)        └─ (Pre-ship Cancel)                 ├─ (Dispute) → DISPUTED
          CANCELLED           CANCELLED                         └─ (Return)  → RETURNED
```

---

# 7. Mathematical Formulas & Valuation Models

### 7.1 Multiplicative Trade-In Valuation Formula
All condition, functional, and component adjustments are dimensionless multipliers against the depreciated base value:

$$\text{AgeInYears} = \frac{\text{CurrentDate} - \text{ReleaseDate}}{365.25}$$

$$\text{BaseValue} = \text{MSRP} \times (1 - \text{AnnualDepreciationRate})^{\text{AgeInYears}}$$

$$\text{ConditionFactor} = \begin{cases} 
0.95 & \text{EXCELLENT} \\ 
0.85 & \text{GOOD} \\ 
0.70 & \text{FAIR} \\ 
0.50 & \text{POOR} \\ 
0.30 & \text{DAMAGED} 
\end{cases}, \quad 
\text{FunctionalFactor} = \begin{cases} 
1.00 & \text{FULLY\_FUNCTIONAL} \\ 
0.80 & \text{MINOR\_ISSUES} \\ 
0.50 & \text{MAJOR\_ISSUES} \\ 
0.20 & \text{NOT\_WORKING} 
\end{cases}$$

$$\text{BatteryMultiplier} = \begin{cases}
1.00 & \text{Health } \ge 90\% \\
0.98 & 80\% \le \text{Health} \le 89\% \\
0.95 & 70\% \le \text{Health} \le 79\% \\
0.90 & 60\% \le \text{Health} \le 69\% \\
0.85 & \text{Health } < 60\%
\end{cases}, \quad
\text{AccessoriesMultiplier} = \begin{cases}
1.03 & \text{Original Box + Accessories} \\
1.00 & \text{Device Only}
\end{cases}$$

$$\text{AdjustedValue} = \text{BaseValue} \times \text{ConditionFactor} \times \text{FunctionalFactor} \times \text{BatteryMultiplier} \times \text{AccessoriesMultiplier}$$

$$\text{NetValue} = \max(0, \text{AdjustedValue} - \text{RepairEstimate})$$

$$\text{FinalTradeInOffer} = \text{NetValue} \times (1 - \text{PlatformMarginRate})$$

---

### 7.2 Bayesian-Smoothed Seller Reputation Formula
To avoid cold-start distortion for new sellers, reputation is calculated using a volume-weighted confidence curve anchored at a neutral baseline of 50.0:

$$\text{VolumeWeight} = \min\left(\frac{\text{CompletedOrders}}{50.0}, 1.0\right)$$

$$\text{RatingScore} = \left(\frac{\text{AverageRating}}{5.0}\right) \times 40.0 \quad (\text{Max 40 pts})$$
$$\text{QualityScore} = (1.0 - \text{ReturnRate}) \times 25.0 \quad (\text{Max 25 pts})$$
$$\text{TrustScore} = (1.0 - \text{DisputeRate}) \times 20.0 \quad (\text{Max 20 pts})$$
$$\text{ResponseScore} = \text{ResponseRate} \times 15.0 \quad (\text{Max 15 pts})$$

$$\text{CalculatedPerformance} = \text{RatingScore} + \text{QualityScore} + \text{TrustScore} + \text{ResponseScore}$$

$$\text{ReputationScore} = \left(50.0 \times (1.0 - \text{VolumeWeight})\right) + \left(\text{CalculatedPerformance} \times \text{VolumeWeight}\right)$$

$$\text{FinalScore} = \text{clamp}\left(\text{ReputationScore} - (\text{ActiveDisputes} \times 5.0), 0.0, 100.0\right)$$

---

# 8. Concurrency, Locking & Anti-Hoarding Checkout

### 8.1 Shopping Cart vs Checkout Locking
```
[User Action]                [System Behavior]
Add to Cart            ───>  Snapshot verification only (NO database lock, NO timer).
Proceed to Checkout    ───>  Acquire 15-minute lease:
                             1. SELECT * FROM product_units WHERE id = :id FOR UPDATE
                             2. Assert status == 'AVAILABLE'
                             3. INSERT INTO unit_reservations (status = 'ACTIVE', expires_at = NOW() + 15m)
                             4. UPDATE product_units SET status = 'RESERVED'
```

### 8.2 Scheduled Reservation Reaping
Worker instances sweep expired leases without lock contention:
```sql
SELECT * FROM unit_reservations 
WHERE status = 'ACTIVE' AND expires_at < NOW()
ORDER BY id ASC
FOR UPDATE SKIP LOCKED
LIMIT 100;
```

---

# 9. UI/UX Design System & Frontend Architecture

### 9.1 Design System Tokens & Brand Guide
Adhering to **Design-MD (Stripe/Linear)** and **UI/UX Pro Max Marketplace Specifications**:

```css
:root {
  /* Brand Color Tokens */
  --color-primary: #2563EB;          /* Deep Tech Blue */
  --color-on-primary: #FFFFFF;
  --color-secondary: #3B82F6;        /* Modern Slate Blue */
  --color-accent: #16A34A;           /* Trust / Certified Green */
  --color-background: #F8FAFC;       /* Crisp Surface Neutral */
  --color-foreground: #0F172A;       /* High-contrast Slate */
  --color-card: #FFFFFF;
  --color-card-foreground: #0F172A;
  --color-muted: #F1F5F9;
  --color-muted-foreground: #64748B;
  --color-border: #E2E8F0;
  --color-destructive: #DC2626;      /* Error Red */
  --color-ring: #2563EB;

  /* Typography Scale */
  --font-sans: 'Atkinson Hyperlegible', 'Inter', -apple-system, sans-serif;
  --font-mono: 'JetBrains Mono', monospace;

  /* Spacing Scale (8px Grid) */
  --space-1: 0.25rem;  /* 4px */
  --space-2: 0.5rem;   /* 8px */
  --space-3: 0.75rem;  /* 12px */
  --space-4: 1.0rem;   /* 16px */
  --space-6: 1.5rem;   /* 24px */
  --space-8: 2.0rem;   /* 32px */
}
```

### 9.2 Grading Badges & Visual Accessibility
- **Grade A+ / A (Pristine):** Badge `#DCFCE7` text `#15803D` (4.8:1 contrast).
- **Grade B+ / B (Good / Minor Wear):** Badge `#DBEAFE` text `#1E40AF` (5.2:1 contrast).
- **Grade C / D (Fair / Heavily Discounted):** Badge `#FEF3C7` text `#92400E` (4.6:1 contrast).
- **Iconography:** Strictly Lucide React SVG icons. Raw text emojis as UI icons are banned.

### 9.3 TanStack Query Key Contract
```ts
export const queryKeys = {
  auth: { me: ['auth', 'me'] },
  products: {
    list: (filters: Record<string, unknown>) => ['products', 'list', filters],
    detail: (slug: string) => ['products', 'detail', slug],
  },
  units: {
    detail: (unitId: string) => ['units', 'detail', unitId],
    provenance: (unitId: string) => ['units', 'provenance', unitId],
  },
  listings: {
    search: (params: Record<string, unknown>) => ['listings', 'search', params],
    detail: (listingId: string) => ['listings', 'detail', listingId],
  },
  cart: { current: ['cart', 'current'] },
  checkout: { session: (sessionId: string) => ['checkout', 'session', sessionId] },
  orders: {
    list: (filters: Record<string, unknown>) => ['orders', 'list', filters],
    detail: (orderId: string) => ['orders', 'detail', orderId],
  },
  tradeIn: {
    valuation: (specId: string) => ['tradein', 'valuation', specId],
    requests: ['tradein', 'requests'],
  },
  seller: {
    metrics: ['seller', 'metrics'],
    wallet: ['seller', 'wallet'],
  }
};
```

---

# 10. Transactional Outbox Pattern & Reliability

```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL, -- 'ORDER', 'PRODUCT_UNIT', 'ESCROW', 'TRADE_IN'
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,   -- 'ORDER_PAID', 'UNIT_STATUS_CHANGED', 'ESCROW_SETTLED'
    payload JSONB NOT NULL,
    correlation_id UUID NOT NULL,
    idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'PROCESSED', 'FAILED', 'DEAD_LETTER'
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_unprocessed 
ON outbox_events (status, next_retry_at) 
WHERE status IN ('PENDING', 'FAILED');
```

---

# 11. Security, Authorization & IDOR Defenses

```
┌────────────────────────────────────────────────────────┐
│               Security Enforcement Pipeline            │
│                                                        │
│  1. JWT Authentication (15m access token, hashed RT)   │
│                          ↓                             │
│  2. Global Rate Limiter (Redis token bucket per IP/ID) │
│                          ↓                             │
│  3. RBAC Policy Check (@PreAuthorize("hasRole(...)"))  │
│                          ↓                             │
│  4. ABAC / IDOR Resource Ownership Verification       │
│     (Assert entity.owner_id == currentUser.id)         │
│                          ↓                             │
│  5. Execution with Append-Only Audit Logging           │
└────────────────────────────────────────────────────────┘
```

- **IDOR Protection Rule:** Every service method mutating or querying private resources (Orders, Listings, Addresses, Claims) MUST evaluate owner equality in the database predicate (`WHERE id = :id AND owner_id = :currentUserId`).
- **File Upload Security:** Magic-byte header verification, image transcoding via libvips/ImageIO to strip EXIF data, random UUID filenames, and direct private S3 presigned upload URLs.

---

# 12. Verification, Testing Matrix & Release Gates

```text
[Unit Tests]       ──>  Pure math, state transitions, valuation models, Bayesian scores (100% Branch Coverage)
[Integration]      ──>  Testcontainers (PostgreSQL 16 + Redis 7), Flyway migrations, outbox polling
[Concurrency]      ──>  100 simultaneous threads attempting single-unit reservation (Assert 1 success, 99 409-conflicts)
[Idempotency]      ──>  20 duplicate checkout requests with identical Idempotency-Key (Assert 1 debit, 1 order)
[Failure Injection]──>  Payment Gateway timeout simulation (Assert Saga compensation & lease release)
```

### P0 Blocking Criteria for Deployment:
1. Any untested state transition or unhandled dead-end in `ProductUnit`.
2. Any monetary calculation performed without `BigDecimal` or failing two-decimal bank rounding.
3. Any financial journal entry where $\sum(\text{Debits}) \neq \sum(\text{Credits})$.
4. Any endpoint permitting access to another user's order or claim via ID tampering (IDOR).
5. Any direct mutation of `ProductUnit.status` bypassing the lifecycle transition manager.

---

This master specification represents the complete, hardened, and definitive architectural contract for the ReLoop platform. All backend and frontend code must strictly conform to these models, formulas, and invariants.
