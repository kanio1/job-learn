# Spec 03: Payment Operations Dashboard

**Status:** ✅ COMPLETE_AND_KIRO_MARKED — current execution status: see `status/specs/payment-operations-dashboard.md`
**Pliki źródłowe:** `.kiro/specs/payment-operations-dashboard/`  
**Zależność od:** istniejący backend + Nuxt frontend

---

## Cel

Brownfield enhancement frontendu Nuxt 4 — dashboard operacyjny dla płatności,
który jednocześnie służy jako HTTP learning surface (ETag, If-Match, Idempotency-Key,
Vary, Cache-Control, X-Correlation-ID, problem+json).

**Kluczowy gap do zamknięcia:** plain `$fetch` odrzuca response headers i status —
zastąpiony przez `useApiClient` oparty na `$fetch.raw`.

---

## Zakres

| Obszar | Opis |
|---|---|
| Header-aware API client | `useApiClient` → `$fetch.raw` → `ApiResponse<T>` z headers + status |
| Domain composables | `useMerchantsApi`, `usePaymentOrdersApi`, `usePaymentLifecycleApi` |
| Reusable components | `BusinessStatusBadge`, `HttpStatusBadge`, `HeaderKeyValuePanel`, `ProblemDetailsCard`, `RawJsonViewer`, `IdempotencyKeyInput`, `EtagDisplay`, `IfMatchInput`, `ApiDebugPanel`, state components |
| Merchant screens | List + create + detail + activate/suspend |
| Payment order screens | List + filters + pagination + create + detail (UTabs) + lifecycle + history |
| Error Lab | 9 scenariuszy: 400/401/403/404/406/409/412/415/428 |
| Dashboard Overview | Summary cards + recent orders |

**Nie ma zmian backendowych.**  
**Playwright — skipped per user decision** (Tasks 9.3, 10.5, 11.4, 12.3, 13.2 mają status `[-]`).

---

## Stan realizacji (tasks.md)

| Wave | Zadania | Status |
|---|---|---|
| W0: Foundation | audit, vitest setup, types/schemas | ✅ |
| W1: API client + nav | `useApiClient`, nav links | ✅ |
| W2: Domain composables | `useMerchantsApi`, `usePaymentOrdersApi`, `usePaymentLifecycleApi`, store delegate | ✅ |
| W3: Shared components | wszystkie shared components | ✅ |
| W4: Component tests + screens | property tests, merchant + PO screens | ✅ |
| W5: Detail/lifecycle/labs/overview | detail tabs, lifecycle, Error Lab, Overview | ✅ |
| W6: Screen tests + testid hardening | | ✅ (Playwright skipped) |
| Cleanup | | ❌ Tasks 10.3\*, 10.4\*, 11.3\*, 14.3\*, 14.4\*, 15.2\* (opcjonalne property tests) |
| Final | Task 16.1 typecheck + unit, 16.2 README, 17 checkpoint | ❌ |

---

## Właściwości poprawności (fast-check + Vitest, 16 properties)

| Property | Obszar |
|---|---|
| P1: No fabricated metric | Overview (req 1.7) |
| P2: Status badges distinguishable | BusinessStatusBadge |
| P3: Outbound request gating by form schema | CreatePaymentOrderForm |
| P4: Inbound response validation gating | useApiClient |
| P5: HTTP status category mapping | HttpStatusBadge |
| P6: Raw JSON round-trip | RawJsonViewer |
| P7: Problem details empty indicators | ProblemDetailsCard |
| P8: Header panel with empty indicator | HeaderKeyValuePanel |
| P9: Token confidentiality + masking | HeaderKeyValuePanel, ApiDebugPanel |
| P10: Displayed status = proxied status | useApiClient |
| P11: If-Match round-trip | lifecycle/ETag |
| P12: Idempotency-Key reuse | CreatePaymentOrderForm |
| P13: Filter params subset + bounded | paymentOrderListQuerySchema |
| P14: History ascending by timestamp | history timeline |
| P15: Non-display actor fields never rendered | history actors |
| P16: Test-ids unique per page | Playwright testability |

---

## Uwagi

- Spec zbudowana bez warstwy ról (przed `iam-roles`) — używa `platform-operator` storage-state
- Po implementacji `iam-roles`, role-aware rendering z `useAuthorization` zostanie nałożony na istniejący dashboard
- `MerchantTable`, `CreateMerchantForm`, `PaymentOrderLifecycleActions` będą rozszerzane w `iam-roles` o `data-testid` i role-gating

---

## Pozostałe zadania do ukończenia

```
10.3* Property 3 (request gating)
10.4* Property 12 (idempotency reuse)
11.3* P14/P15 (history ordering + actor masking)
14.3* P1 (no fabricated metric)
14.4* Overview deterministic states
15.2* P16 (test-id uniqueness)
16.1  typecheck + unit tests
16.2  README update
17    Final checkpoint
```
