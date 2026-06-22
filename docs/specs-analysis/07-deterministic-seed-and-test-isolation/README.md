# Spec 07: Deterministic Seed and Test Isolation

**Status:** 🔲 Spec gotowa (requirements + design + tasks) — implementacja nierozpoczęta  
**Pliki źródłowe:** `.kiro/specs/deterministic-seed-and-test-isolation/`  
**Roadmap position:** SPEC #5 — **capstone** pierwszej piątki (Playwright/SDET roadmap §17)

---

## Cel

Deterministyczny dataset + feature-flagged test-only reset/seed REST affordance.
Capstone enabler dla przyszłych lekcji Playwright — przewidywalne, reset-owalne dane
across wszystkich poprzednich specs. **Backend/infra spec (nie frontend).**

---

## Kluczowe decyzje

| # | Decyzja | Uzasadnienie |
|---|---|---|
| A | Profile-gated seeder + feature-flagged reset endpoint | NIE Flyway repeatable (uruchamiałby się w prod); profil `seed` + flag `app.testing.enabled` |
| B | Predictable IDs (fixed UUID + natural keys w jednym Fixtures) | deterministyczne targety dla testów |
| C | DB seed (tenants/merchants/payment orders); realm users z #1 (alignment, nie duplikacja) | Keycloak single source of truth, brak dual-write |
| D | Reset endpoint: default OFF, prod forces OFF, 404 gdy disabled (nie 403) | non-disclosure; backdoor niemożliwy w prod |
| OQ1 | Nowy moduł `testing` (`lab.paymentquality.testing`) | single gating point, honest dependency graph |
| Module pattern | Module_Seed_Capability PUBLIC per moduł (TenantSeedCapability...) | Modulith-safe; testing woła tylko PUBLIC API |
| OQ4 | Seed tylko pod profilem `seed` (nie `test`) | zachowuje istniejący deleteAll unit pattern |

---

## Defense-in-depth (test-reset = wrażliwy backdoor)

| Gate | Mechanizm |
|---|---|
| Startup seeding | `@Profile("seed")` na SeedRunner |
| Endpoint registration | `@ConditionalOnProperty(app.testing.enabled=true)` na TestController |
| Prod fail-safe | `@Profile("!prod")` na obu |
| Prod flag override | `application-prod.yml` → `app.testing.enabled: false` |
| Disabled = route absent | bean nie zarejestrowany → 404 (nie 403-filter) |

---

## Fixtures Catalog (natural keys fixed, UUID finalizowane w design)

**Tenants:** PLATFORM_TENANT (PLATFORM), TENANT_ALPHA (STANDARD), PLACEHOLDER_TENANT_ID (STANDARD)
**Merchants:** MERCHANT_ALPHA_001/002 (TENANT_ALPHA), MERCHANT_BETA_001 (PLATFORM_TENANT)
**Payment orders:** statusy CREATED/AUTHORIZED/CAPTURED/CANCELLED/REFUNDED + blok ekspansji dla paginacji

Terminal-status seeding wstawia zamówienia bezpośrednio w docelowym statusie (świadomy bypass lifecycle; spójność gwarantowana przez curated fixtures + syntetyczny status-history entry).

---

## Endpointy (test-only, za flagą)

| Metoda | Ścieżka | Disabled |
|---|---|---|
| POST | /api/test/reset | 404 |
| POST | /api/test/seed | 404 |

X-Correlation-ID na odpowiedziach; minimalne body; brak secrets w logach.

---

## Właściwości poprawności (PBT — jqwik)

| P | Opis |
|---|---|
| P1 | Seed idempotency — N seedów = 1 seed |
| P2 | Fixed identities stabilne i dokładne (catalog UUIDs/refs) |
| P3 | Reset → Baseline_State, FK-safe, schema intact |
| P4 | Prod safety total (flag false + 404 regardless of config) |
| P5 | Disabled → 404 (nie 403) |
| P6 | Realm-alignment — każdy tenant_id/merchant_id realm usera ma seeded record |

---

## Implementacja inkrementalna (kluczowe!)

| Stage | Zależność | Zakres | Wave |
|---|---|---|---|
| 1 | **brak — buildable teraz** | testing module, SeedRunner, TestController, Fixtures, Merchant/PaymentSeedCapability, guards | 1–3 |
| 2 | po tenant-model #2 | TenantSeedCapability + przypisanie merchant→tenant | 4 |
| 3 | po iam-roles #1 + user-management #3 | realm-alignment (P6) | 5 |
| 4 | po audit #4 (warunkowo OQ2) | AuditSeedCapability | 6 |

**Stage 1 można zaimplementować już teraz** (merchants+payments istnieją w kodzie).

NON-optional: seed-profile IT, endpoint ITs (enabled/disabled→404/prod→404), TestingModuleTest.

---

## Uwagi

- Brak migracji Flyway dla seedu (profile-gated code only; ddl-auto:validate nietknięty)
- Istniejący per-class Testcontainers + deleteAll unit pattern zachowany (endpoint to E2E/manual)
- Brak plików Playwright (konceptualne usage: storage-state per role, API-driven setup/cleanup, reset between runs)
- Module_Seed_Capability = additive PUBLIC API per moduł; brak zmian istniejących kontraktów
