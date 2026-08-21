# Tenant payment policy (E10 / PW-OPS-T16)

Policy is stored on `tenants.payment_policy` (JSONB, Flyway `V36__tenant_payment_policy.sql`) and exposed on the existing GET/PATCH `/api/tenants/current/settings` resource. ETag remains `"v{settings_version}"`. There is no second CRUD and no auto-capture worker/scheduler — capture stays manual.

## Domain

| Field | Rule |
|---|---|
| `autoCapture` | boolean |
| `maxAutoCaptureMinor` | integer ≥ 0; **required and min 1 when `autoCapture` is true**; ignored / stored as `0` when false |
| `riskThreshold` | 0..100 inclusive |
| `refundPolicy` | `MANUAL` \| `AUTOMATIC` |

Authority: existing `platform:tenant:settings:read` / `platform:tenant:settings:update`. No new Keycloak composites.

## AUTOMATIC refund vs E5 dual-control PIN

PIN threshold (E5, already in code): `amountMinor > 100000` requires a verified PIN challenge. AUTOMATIC **does not** disable that.

| refundPolicy | amountMinor | Dual-control approval | PIN challenge |
|---|---|---|---|
| MANUAL | any | required (existing E5) | required when `amountMinor > 100000` |
| AUTOMATIC | ≤ 100000 | lab: may skip dual-control | not required (at/under PIN threshold) |
| AUTOMATIC | > 100000 | still required | **still required** (high-value PIN stays) |

Execution of AUTOMATIC skip-at-or-under-threshold is **not** wired in T16. Policy is stored only. Auto-capture worker is P2 / out of scope.

## Isolation

`/api/tenants/current/settings` is JWT-current-tenant. Another tenant's token cannot read or mutate this tenant's `payment_policy`. Unresolvable `tenant_id` is 403 (`tenant_access_denied`), matching the existing tenant-settings mask (payments mask foreign ids as 404).
