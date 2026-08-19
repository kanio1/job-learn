# 05 — Hard controls, Keycloak, BFF composition

## Dual storageState

Live POM `chromium-rbac` ładuje `rls-lab.spec.ts`. Dwa konteksty: `merchant-manager.json` i `platform-admin.json` (hasła tylko z env).

| Case | Rola | Oczekiwanie |
|---|---|---|
| Lista RLS | merchant.manager | 1 wiersz Alpha |
| Compare UI | merchant.manager | panel ukryty (`canReadPlatformPayments` false) |
| Compare API | merchant.manager | 403 Spring (brak `platform:payments:read`, nie `rls_forbidden`) |
| Compare UI | platform.admin | `rls-lab-compare-restricted-no-tenant` = 0; unprotected ≥ 2 |
| Compare API | platform.admin | 200 `restrictedWithoutTenantGuc=0`, `bypassRoleCount` / `unprotected` ≥ 2 |
| Compare API | support.agent (RA) | 200 — ma `platform:payments:read` + `PLATFORM_TENANT` |

Uczy: Keycloak role ≠ Postgres role. JWT `tenant_id` zasila `SET LOCAL app.tenant_id`; realm role zasila to, czy UI w ogóle pokazuje leak demo. Platforma nie ustawia GUC `app.rls_bypass`.

## USelect / badge / modal

Nie używamy `page.on('dialog')`. ConfirmModal to `UModal` z `data-testid="confirm-action-modal"`. Dismiss: `confirm-action-dismiss`; confirm: `confirm-action-confirm`. POM nie klika przycisku o nazwie „Cancel” (drawer lifecycle).

Capability Cancel na detail: wyłącznie `useAuthorization().can.canRunLifecycle` — **bez** Boolean prop (Vue Boolean trap, `EG-RFC-052`).

Users/audit mają to samo `UPagination` + reset `page: 0` — analog edukacyjny, **bez** nowych speców w Wave A.

## BFF composition

Przeglądarka: `GET http://localhost:3000/api/merchants/{id}/payment-orders`.  
Nitro: `backendApi` dokłada Bearer z sealed session, woła `:8080`.

POM asertuje `new URL(request.url()).port === '3000'`. To jest lekcja API composition bez gateway produktu.

## Timeout / ack / delivery (istniejący stos)

| Temat | Gdzie | Oracle |
|---|---|---|
| Ambiguous timeout | Network Lab abort (`existing-pom`) | request aborted ≠ success |
| Ack vs fulfillment | Checkout lie return (`existing-pom`) | `fulfillment-status` nie CONFIRMED |
| Check-then-act | If-Match 412 (`existing-pom`) + modal dismiss `confirm-action-dismiss` (brak POST cancel) | GET status CREATED |
| Idempotency | payments-create replay (`existing-pom`) | ten sam paymentOrderId |

Nie dodajemy Kafki. Delivery semantics = fulfillment/DB, nie JSON `success`.
