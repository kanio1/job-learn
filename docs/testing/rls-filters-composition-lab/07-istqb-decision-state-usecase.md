# 07 — Decision tables, state, use case, pairwise, error guessing

## DT-RFC-01 — RLS list vs compare

| JWT `PLATFORM_TENANT`? | `platform:payments:read` | Sesja DB | Endpoint | Wynik |
|---|---|---|---|---|
| nie | nie | SET LOCAL tenant | GET items | 1 wiersz Alpha |
| nie | nie | SET LOCAL tenant | GET items/a2 | 404 `not_found` |
| nie | nie | — | GET compare | 403 Spring (RA-014) |
| nie | tak | — | GET compare | 403 `rls_forbidden` (RA-018) |
| tak | tak | BYPASSRLS role | GET compare | `restrictedWithoutTenantGuc=0`, `bypassRoleCount=2`, `unprotected=2` (RA-019) |
| — | — | brak GUC (`rls_lab_app`) | SELECT rls_lab_item | 0 |
| — | — | fake `app.rls_bypass` | SELECT rls_lab_item | 0 |
| — | — | `rls_lab_bypass` / owner | SELECT rls_lab_item | 2 |

Pokrycie: existing-ra `RlsLabRestAssuredTest` + existing-pom E2E-002–005.

## ST-RFC-042 — If-Match (istniejący)

CREATED --authorize(fresh ETag `"v{n}"`)→ AUTHORIZED; CREATED --authorize(`"v99"`)→ 412, stan CREATED. Malformed ETag → 400.  
Pokrycie: existing-pom `payments-lifecycle.spec.ts`; HTTPS: E2E-052 `tls-lab.spec.ts`.

## UC-RFC-040 — Lie return ≠ fulfillment

Hosted „success” copy vs `fulfillment-status` nie CONFIRMED.  
Pokrycie: existing-pom `checkout-lab.spec.ts`.

## UC-RFC-041 — Notify ack vs fulfillment

CPL notify 202/200 vs wiersz fulfillment. Katalog CPL 05; nie dublować RA.

## UC-RFC-032 — Modal dismiss

Operator otwiera Cancel → drawer Submit → modal „Confirm Cancel” → **dismiss** (`confirm-action-dismiss`, copy „Go back”).  
Oracle: brak POST `/cancel`; GET `CREATED`; heading Confirm count 0.  
Nie klikać przycisku o nazwie „Cancel” (to akcja w drawerze).  
Pokrycie: existing-pom `payments-hard-controls.spec.ts` (E2E-032).

## UC-RFC-056 — Platforma widzi lekcję wycieku, merchant nie

Merchant: panel compare ukryty; API 403 bez `rls_forbidden`.  
Platform admin: `restrictedWithoutTenantGuc=0`, unprotected > 0.  
JWT z `platform:payments:read` i tenantem Alpha: 403 `rls_forbidden` (nie wyciek liczb).  
Pokrycie: existing-pom E2E-004/005 + existing-ra RA-014/018/019. Wave B (HTTPS, origin): [09](09-wave-b-stack-tls-catalog.md).

## PWISE-RFC-01 — Filtry

Zredukowane pary: (status=CREATED, date=UTC today, reference=unique); (status=CREATED, currency=PLN vs EUR); (minAmount, maxAmount); (page stale, Apply).  
Pełny kartezjan status×currency×date×amount nie jest w kontrakcie (status API tylko CREATED).

## EG-RFC-043 — QUERY vs GET-with-body

Spring nie ma QUERY. GET+body → 403 `get_with_body` (CPL existing-ra). Wave A nie implementuje QUERY.

## EG-RFC-044 — Superuser jako fałszywy oracle RLS

Jeśli test JDBC użyje `postgres.getUsername()`, FORCE RLS nie zadziała (BYPASSRLS). Dlatego rola `rls_lab_app`.

## EG-RFC-052 / 053 (Wave A powierzchnia UI)

Boolean prop `canRunLifecycle` ukrywa Cancel — capability tylko z `useAuthorization`. Overlay vue-tsc nie jest dismissowany z POM.  
Pełny zapis: [09](09-wave-b-stack-tls-catalog.md) `EG-RFC-052`, `EG-RFC-053`.

QUERY / TLS: [09](09-wave-b-stack-tls-catalog.md) + [wave-3](../wave-3-compose-tls-pom/). `EG-RFC-054` = QUERY/versioning/retry docs-only. E2E-061 (FE off + Spring on) zostaje designed P2.
