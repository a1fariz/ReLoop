# ReLoop Backend — Improvement Suggestions

Companion document to the Quarkus migration (`backend-quarkus/`). Findings below
were identified while porting the Spring Boot backend; items marked
**[Implemented in backend-quarkus]** are live in the new codebase, the rest are
recommended follow-ups.

---

## P1 — High impact

### 1. Transactional Outbox was a no-op dispatch [Implemented in backend-quarkus]
**Finding:** `OutboxPollerWorker` (legacy) only logged a "simulated async side
effect dispatch" and marked events PROCESSED. Nothing consumed `outbox_events`,
and no code anywhere even *created* outbox events.
**Why it matters:** The outbox table is the audit/consistency backbone for
async side effects; simulating dispatch means lost integrations silently.
**Fix:** The Quarkus worker now dispatches for real: `reloop.outbox.dispatch=KAFKA`
publishes a JSON envelope per event to topic `reloop-domain-events` via SmallRye
Reactive Messaging (`docker-compose.kafka.yml`); `EMAIL` aggregate events are
delivered through `quarkus-mailer` (Mailpit in dev). Failures fall into the
existing exponential-backoff retry path and eventually DEAD_LETTER.
Producers now exist: `ORDER_PAID`, `EMAIL_ORDER_CONFIRMATION`, `LEASE_EXPIRED`,
`DISPUTE_RESOLVED`, and `FULFILLMENT_SHIPPED/DELIVERED/COMPLETED`. Buyer email
travels from the JWT `email` claim via `CurrentUser.email()`.

### 2. Redis provisioned but never used (dead weight Redisson) [Implemented in backend-quarkus]
**Finding:** `redisson-spring-boot-starter` was on the classpath and Redis ran in
compose, but zero Java code used Redis or Redisson.
**Fix:** backend-quarkus uses Redis for:
- **Catalog/listing hot-read cache** — `@CacheResult` with the Redis cache backend
  (`quarkus-redis-cache`, TTL 300s, configurable).
- **Login rate limiting** — fixed-window counter per normalized email
  (default 10 attempts / 60 s, returns `429 RATE_LIMIT_EXCEEDED`). Degrades
  open if Redis is down so a cache outage cannot cause a login outage.

### 3. No integration tests at all [Implemented in backend-quarkus]
**Finding:** All 10 legacy test classes were Mockito unit tests; Testcontainers
was a declared dependency but unused. Nothing verified SQL, migrations, locks,
or HTTP contracts end-to-end.
**Fix:** `AuthAndCheckoutFlowIT` (`@QuarkusTest` + Testcontainers PostgreSQL)
covers register → login → auth-guard → correlation-id round-trip → 15-minute
lease → confirm-payment with the 85/15 escrow split → idempotent replay.
Runs with `mvn verify` when Docker is available; skips otherwise.

### 4. Spring Modulith boundary verification had no equivalent in Quarkus [Implemented in backend-quarkus]
**Fix:** `ModuleBoundaryArchitectureTest` (ArchUnit) re-encodes the exact
`allowedDependencies` graph as rules (leaf modules, checkout's orchestration
boundary, only-common-depends-on-auth) plus per-module forbid rules. Runs in
every `mvn test`.

### 5. Backend was not containerized [Implemented in backend-quarkus]
**Fix:** `backend-quarkus/Dockerfile` (multi-stage Maven build → quarkus-app
runtime image) and a `reloop-backend` service in `docker-compose.yml` wired to
postgres/redis/mailpit. The gateway's `backend_service` upstream can now
finally resolve to a real container once you point nginx at `reloop-backend`.

---

## P2 — Medium impact (recommended next)

### 6. Produce outbox events from real state transitions — *partially done*
`DisputeService` now writes a `DISPUTE_RESOLVED` event (see
`BACKEND_CODE_REVIEW.md` S2). Still missing: `ORDER_PAID` from
`CheckoutSagaService` and `LEASE_EXPIRED` from the reaper.

### 7. Listing write-path — ✅ [DONE]
`POST /api/v1/listings`, `POST /{id}/pause`, `POST /{id}/resume` with unit
ownership checks, unit state transitions and the partial-unique-index conflict
mapped to 409. Write endpoints are method-scoped authenticated even though the
GET surface is public.

### 8. Trade-in, inspections, and warranties — ✅ [MOSTLY DONE]
Trade-in intake now exists: `POST /api/v1/trade-in/requests` recomputes the
offer server-side and emits `TRADEIN_SUBMITTED`; `GET /requests/my` lists a
user's requests. Pickup scheduling / counter-offer workflow remains future work.
- Seller metrics aggregate real fulfillment/dispute data (rating & returns
  remain placeholders until those modules exist).
- `POST /api/v1/inspections` records 50-point inspections; the grade flows onto
  the serialized unit; `GET /api/v1/inspections/unit/{unitId}` exposes the latest.
- Warranties are issued automatically when a fulfillment is completed.
- Trade-in remains calculate-only (no request intake workflow).

### 9. Pagination — ✅ [PARTIALLY DONE]
`/api/v1/orders/my` and `/api/v1/orders/seller/my` are also paged now.
`GET /api/v1/listings/search` adds page/size (cap 100), price-range and grade
filters, price/newest sorting via a generic `Page<T>` envelope. The legacy
`GET /listings` contract is unchanged for frontend compatibility. Remaining:
paginate `/disputes/my` and seller/order lookups the same way.

### 10. Idempotency + correlation coverage
The `Idempotency-Key` machinery only protects `/checkout/confirm-payment`. Any
POST with financial side effects (dispute resolution, future payment flows)
should reuse `CheckoutSagaService`'s idempotency block (extract it to a shared
interceptor/filter).

---

## P3 — Hardening (before real users)

### 11. JWT secret & config defaults
Legacy required `JWT_SECRET`/`DB_PASSWORD` env vars with no defaults — keep that
discipline in production. backend-quarkus ships dev defaults so `mvn quarkus:dev`
works out of the box; the compose service still demands `JWT_SECRET`. For
production, rotate to RS256/ES256 with a JWKS so services can verify without
sharing the signing key.

### 12. Refresh-token rotation edge case
`revokeFamily` on reuse detection is good; consider also sending a security
event through the outbox when a family is revoked (reuse = likely token theft).

### 13. Observability — ✅ [DONE]
Business counters live at `/q/metrics`: `reloop.checkout.saga.completed`,
`reloop.checkout.lease.expired`, `reloop.fulfillment.completed`,
`reloop.fulfillment.payout`, `reloop.tradein.submitted`. Redis/Postgres
readiness health checks are automatic.

### 14. Native image (optional)
Quarkus makes GraalVM native builds cheap (`mvn package -Dnative`). Sub-100ms
cold start and ~50MB RSS for the API layer — useful if you downscale the
deployment. Hibernate + Postgres enums are native-compatible; test the
`FOR UPDATE SKIP LOCKED` native queries under native profile.

### 15. AuditLog is dead schema
`audit_logs` table + entity exist, nothing writes them. Either wire an entity
listener / CDI event interceptor to populate it, or drop the module.

---

## Migration notes (behavior preserved)

- Same REST contract: paths, `ApiResponse`/`ApiErrorResponse` envelopes, error codes.
- Same JWT format (HS256, sub=user id, `role` claim) — old tokens stay valid.
- Same BCrypt(12) hashes; Flyway migrations V1–V8 copied verbatim.
- Pessimistic locks and `FOR UPDATE SKIP LOCKED` queries preserved exactly.
- One deliberate deviation: `JwtAuthenticationFilter` (Spring Security filter
  chain) became a custom Quarkus `HttpAuthenticationMechanism` + path policies;
  `@RolesAllowed("ADMIN")` now enforces the dispute-resolve rule natively.

---

## Round 2 (Batches F–I, frontend + security hardening)

### F — Frontend wiring + bilingual UI [DONE]
- `/trade-in`: server-side valuation via `POST /trade-in/calculate`, official
  request intake `POST /trade-in/requests`, `GET /requests/my` table. The old
  client-side duplicate of the valuation model is deleted.
- `/warranties`: real `GET /warranties/my` (paged), `GET /disputes/my` (paged),
  buyer dispute filing `POST /disputes`.
- Home page: showcase now queries live listings (`/listings/search?sort=newest`);
  fake serial-lookup verifier removed (it matched only hardcoded serials).
- `/register`: role dropdown removed — backend never read it (all registrations
  are CUSTOMER by default; exposing TECHNICIAN in the UI was a lie).
- Bilingual (id/en) via `lib/i18n.ts` zustand store persisted as `reloop-lang`;
  language toggle in the Navbar (desktop pill + mobile menu). All 14 pages
  translated.
- Dead deps removed: `next-themes`, `@tanstack/react-query-devtools`,
  `three`, `@types/three`.

### G — Security/correctness backend fixes [DONE]
- **IDOR fix (Rule 011 / spec P0-4):** `DisputeService.createDispute` now loads
  the master order behind the fulfillment and rejects non-buyers with 403
  `DISPUTE_FORBIDDEN`.
- **One open dispute per fulfillment:** 409 `DISPUTE_ALREADY_OPEN` on a second
  OPEN dispute for the same fulfillment.
- **Escrow DISPUTED linkage (spec §5/§6.3):** opening a dispute freezes a HELD
  escrow to `DISPUTED` (new enum value; column is VARCHAR so no migration
  needed). REPAIR/REPLACEMENT resolutions return it to HELD. Full/partial
  refunds and releases settle as before.
- **BigDecimal depreciation (Rule 009 / spec P0-2):** trade-in base value now
  computes integer-year `BigDecimal.pow` with linear interpolation for the
  fractional year — no `double` in the money path. Pinned by an exact-value
  unit test.
- **Ownership/custody transfer (spec P0-1, partial):** `complete()` now sets
  `ProductUnit.currentOwnerId = buyer`, custody `BUYER`, status `OWNED`.
  ArchUnit `ordersDependencies` widened to allow `..units..` (documented
  orchestration, same precedent as disputes → orders/ledger).

### H — Reviews [DONE]
- `/orders`: review modal (rating stars + comment) → `POST /reviews`; backend
  already enforces buyer-ownership + COMPLETED + one-review-per-fulfillment.
- `/seller`: Seller Reviews panel via `GET /reviews/seller/{id}`.
- Frontend API/types/queryKeys for reviews, trade-in, warranties, catalog models.

### I — Hardening [DONE]
- `app/loading.tsx` + `app/error.tsx` (route-level error boundary with retry).
- CORS: gateway origin `http://localhost:8000` added to the allowlist.
- Pagination: `GET /disputes/my` and `GET /warranties/my` now return
  `Page<T>` (clamped 1–100), matching orders/listings/admin feeds.
- Backend suite: 82 tests green (unit + ArchUnit), frontend typecheck + build
  green.

### Remaining (next round candidates)
- Idempotency-Key on remaining financial mutations (ship/resolve/payout).
- `EXPIRED` listing state + TTL reaper; `PENDING_REVIEW` enforcement.
- Returns module, refurbishment tickets, trade-in pickup scheduling (spec §0).
- Seller reputation from real reviews (currently placeholder multipliers).
- JWT RS256/JWKS migration, `iss`/`aud` enforcement (Sec6 backlog).
- In-app notifications module (spec §2).

---

## Round 3 — Live smoke test against running infra (Docker + quarkus:dev)

Full end-to-end verification through the Nginx gateway (:8000). The smoke run
uncovered and fixed five runtime bugs that unit tests (all mocked) could never
catch, plus several environment issues:

### Runtime bugs found & fixed [DONE]
1. **Hibernate camelCase vs Flyway snake_case schema** — the whole ORM was
   mapping `batteryHealthPercentage` -> `batteryhealthpercentage` (single-word
   columns worked, multi-word columns 500'd on `/catalog/models`). Fixed with
   `quarkus.hibernate-orm.physical-naming-strategy=CamelCaseToUnderscoresNamingStrategy`.
2. **jsonb writes as varchar** — six entities (`OutboxEvent`, `AuditLog`,
   `Listing`, `MasterOrder`, `IdempotencyKeyRecord`, `TechnicalInspection`,
   `ProductUnit`-adjacent) bound Java `String` to jsonb columns without
   `@JdbcTypeCode(SqlTypes.JSON)`; every outbox INSERT aborted its transaction
   (register 500'd because the welcome email event rolled the user INSERT back).
3. **AuditLog jsonb states** — `from_state`/`to_state` are jsonb but received
   bare enum strings (`PROCESSING`); ship/deliver/complete all 500'd at the
   audit write. `AuditService` now wraps raw strings as JSON scalars.
4. **Panache `find(field, value, sort)` argument order** — resolves to
   `find(query, Object...)` varargs instead of `find(query, sort, params)`;
   "No parameter labelled '?2'" on every paged seller/buyer/warranty/dispute
   feed. Reordered to `find("field = ?1", Sort, value)` across
   OrderFulfillmentService / DisputeService / WarrantyService.
5. **`/admin/users/{id}/unlock` not transactional** — 500 "Transaction is not
   active"; added `@Transactional`.
6. **POST-without-body 415** — class-level `@Consumes(JSON)` on OrderController
   made body-less `deliver/complete/payout` reject requests without a JSON
   Content-Type (broke both curl-style clients and the axios frontend path).
   Moved `@Consumes` to the ship method only.
7. **Seller profile vs user id identity mismatch** — `fulfillment_orders.seller_id`
   references `sellers.id` (store profile) while RBAC uses user ids, so sellers
   could never ship their own orders (403) and `/orders/seller/my` returned
   nothing. Added native-sql translators in FulfillmentOrderRepository
   (`resolveSellerUserId` / `resolveSellerProfileId`) and SellerService;
   module boundaries kept intact (native SQL, no Java dependency).
8. **Stale serial sequences** — V8 seeded users/sellers with explicit ids
   without advancing `sellers_id_seq`/`users_id_seq`; first runtime seller
   INSERT collided with `sellers_pkey`. V11 re-syncs both sequences.

### Environment/config fixes [DONE]
- Kafka DevServices no longer auto-starts (it crashed `quarkus:dev` when the
  JVM can't see Docker): `quarkus.kafka.devservices.enabled=false`, and the
  outbox channel's health checks are disabled so a broker-less LOG mode stays
  green (`mp.messaging.outgoing.reloop-events.health-enabled=false`).
- CORS allowlist now includes the gateway origin `http://localhost:8000`.
- `/api/v1/reviews/*` reads are public; review POSTs require auth (path policy).
- Seed login for `admin@reloop.com` fails (seeded bcrypt hash does not match
  the documented `SecurePass123!`) — documented, untested path; smoke tests
  register + promote their own admin/technician/seller accounts instead.

### Verified end-to-end (gateway :8000, 18/19 checks green)
register/login/refresh-rotation, catalog models, listings search + detail,
trade-in calculate (BigDecimal) + request intake + my list, seller metrics,
reviews submit/list, checkout reserve -> confirm-payment (Idempotency-Key
replay dedup), seller ship, admin deliver/complete (escrow SETTLED, unit
ownership + custody BUYER/OWNED transferred, warranty issued to buyer),
admin payout, dispute IDOR 403 for non-buyer, one-open-dispute 409, buyer
dispute -> admin FULL_REFUND resolve -> escrow FULLY_REFUNDED, technician
inspection + unit grade write, admin unlock. Backend suite 82/82 green
(including ArchUnit), frontend typecheck + production build green.
