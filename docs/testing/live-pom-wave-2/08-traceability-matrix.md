# 08 — Macierz śledzenia (FR → testy)

Pokrycie: `existing-pom` | `existing-ra` | `designed` | `blocked` | `docs-only`.  
UC = [07](07-istqb-decision-state-usecase.md). Spec = konkretny `test('…')` w [03](03-playwright-e2e-catalog.md).

## Sesja / gość

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| FR-W2-01 unauth merchants | E2E-001, UC-01 | existing-pom `session-guest` | P0 |
| FR-W2-01 unauth session-lab | E2E-002 | existing-pom | P0 |
| Unauth inne `/admin` | E2E-003 | existing-pom `session-guest` | P1 |
| FR-W2-02 logout aplikacji | E2E-010, UC-02 | existing-pom `session.spec` (nie OIDC) | P0 |
| FR-OIDC / FR-S04b End OIDC | E2E-013, UC-18 | existing-pom `session.spec.ts` | P0 |
| Cookie &lt; 4 KB / brak id_token | SEC-005, UC-19 | existing-pom | P0 |
| SameSite / Secure vs policy JSON | SEC-006 | existing-pom `"Lax"`; TLS E2E-056 existing | P1 |
| FR-W2-03 HttpOnly / no JWT | E2E-011, SEC-001–003 | existing-pom | P0 |
| Idle 121s Unlock → login | E2E-012, EP-013, UC-16 | existing-pom `session-lab` | P0 |
| Ponowny `/admin` po idle unlock | MRL E2E-022, EP-014 | existing-pom `session-lab.spec.ts` | P1 |
| Redirect po login | SEC-011 | existing-pom `session-guest` | P1 |
| Guest API 401 | SEC-030 | existing-pom `session-guest` | P1 |

## Merchants

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| FR-W2-04 persist GET | E2E-020/022, UC-03 | existing-pom | P0 |
| FR-W2-05 409 BFF | E2E-026, API-002 | existing-pom | P0 |
| FR-W2-05 409 w UI | E2E-025 | existing-pom `merchants.spec.ts` | P1 |
| FR-W2-06 Zod empty | E2E-023, BVA-020 | existing-pom | P0 |
| FR-W2-07 lifecycle | E2E-021, ST-01 | existing-pom | P0 |
| UI create + tenant | E2E-024 | existing-pom `merchants.spec.ts` | P1 |
| POST without tenant | API-003 | existing-pom `admin-bff` | P1 |
| GET 404 | API-004 | existing-pom `admin-bff` | P1 |

## UX / a11y

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| FR-W2-08 palette + ARIA | E2E-030, UC-13 | existing-pom + snapshot yml | P1 |
| Palette inne cele | E2E-031 | existing-pom `command-palette.spec.ts` | P2 |
| ARIA login | E2E-032 | existing-pom `session-guest.spec.ts` | P2 |

## Notes / risk / RBAC

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| FR-W2-09 notes | E2E-040, UC-04 | existing-pom 201\|403 | P0 |
| FR-W2-15 manager create order | API-010, UC-08 | existing-pom | P0 |
| Admin create order 403 | API-011 | existing-pom `admin-bff` | P1 |
| Manager notes hidden | E2E-041 | existing-pom `payments-lifecycle` | P1 |
| FR-W2-10 risk | E2E-050, UC-12 | existing-pom 200\|403 | P1 |
| Alpha vs Beta vs Users | E2E-100, UC-11 | existing-pom `auth-rbac` | P0 |

## Payments HTTP

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| Idempotency-Key UI | E2E-090, UC-08 | existing-pom | P0 |
| Replay 200 / mismatch 409 | E2E-091, DT-03 | existing-pom | P0 |
| Authorize+capture If-Match | E2E-092, UC-09 | existing-pom | P0 |
| Stale 412, stan CREATED | E2E-093 | existing-pom | P0 |
| Cancel confirm / dismiss | E2E-094/096, UC-10 | existing-pom | P0 |
| Filtry vs API | E2E-095 | existing-pom + RFC | P0 |
| Evidence + CSV no token | E2E-097, UC-14 | existing-pom | P1 |
| Tenant If-Match | E2E-112, UC-15 | existing-pom | P1 |
| Users / audit | E2E-110/111 | existing-pom | P1 |
| Dual-control refund | UC-W2-22, [09 BC-OP-07](09-core-domain-flows.md) | existing-ra + `payments-refund-dual-control.spec.ts` | P0 |
| Tenant admin vs Beta 404 | UC-W2-20 | existing-ra + existing-pom `tenant-scope.spec.ts` | P0 |
| Manager vs ALPHA_002 create | UC-W2-21 | existing-ra + existing-pom SCN-ISO-10 | P0 |
| Caddy / TLS / Location względny | UC-W2-23, UC-W3-09 | existing-ra + setup oracle | P0 |
| Mix `--app` token + `api.` HTTPS | UC-W2-23 | **401** iss; EG-W2-13 | P0 |

## Checkout (delta)

Hosted hops, HMAC, `Idempotency-Key` na **sesji** CPL, PAY_NO_RETURN: [CPL 08](../checkout-protocol-lab/08-traceability-matrix.md) i [CPL README](../checkout-protocol-lab/README.md). Tu tylko delta dashboardu.

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| ONLINE confirm | E2E-060, UC-05 | existing-pom | P0 |
| Lie return | E2E-061 | existing-pom | P0 |
| FR-W2-11 CASH | E2E-062 | existing-pom | P0 |
| FR-W2-12 Decline CANCELLED | E2E-063 | existing-pom | P0 |
| EXPIRED_LINK hosted | E2E-065 | existing-pom `mirror-lab` | P1 |
| Lab flag off | E2E-064 | existing-pom skip | P1 |
| HMAC/notify | — | CPL, nie tu | — |
| PAY_NO_RETURN close-tab | — | CPL PW-E2E-043 existing-pom | — |
| `RETURN_LIE_SUCCESS` przez header API | — | CPL PW-API-071 existing-ra | — |

## Support / Error Lab / laby

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| FR-W2-13 IDOR | E2E-070, UC-06 | existing-pom | P0 |
| Admin Support Beta | E2E-071 | existing-pom `support-admin` | P1 |
| FR-W2-14 400/401/412 | E2E-080…082, UC-07 | existing-pom | P0 |
| 403/404/406/409/415/428/304 | E2E-083 | existing-pom `error-lab` | P1 |
| 429 | EP-043 | docs-only mock | — |
| CSRF fail | E2E-121, UC-16 | existing-pom | P0 |
| CSRF happy | session-lab csrf-ok | existing-pom | P1 |
| 503 retry live | E2E-123 | existing-pom | P0 |
| RLS / TLS | E2E-125/126 | existing-pom → RFC/09 | P0 |

## Infra / anti-cases

| Wymaganie | ID | Pokrycie |
|---|---|---|
| Overlay click | EG-01 | fixtures |
| IPv4 BFF vs OIDC | EG-02 | `BffClient` |
| Cookie blob `eyJ` | EG-03 | E2E-011 |
| Nazwa spec notes | EG-04 | `internal-notes.spec.ts` |
| Double status query | EG-05 | E2E-063 |
| USelect / Confirm testid | EG-06/07 | E2E-096 |
| CSRF happy vs merchant POST | SEC-031 | existing-pom lab csrf-ok; merchant POST bez CSRF 201 `admin-bff.spec.ts` |
| Learner | README | `tests-pom-learner` |

## Heatmapa

| Obszar | existing-pom | Zostaje designed |
|---|---|---|
| Guest / logout / cookie / idle unlock + re-goto | tak | — |
| Merchant persist / 409 API / Zod / ST / UI+tenant / UI 409 | tak | — |
| Palette ARIA Error Lab + destynacje + login snapshot + `/forbidden` | tak | — |
| Notes / risk | 201\|403 | realm roles (E2E-041 existing) |
| Payments idempotency / ETag / cancel | tak | — |
| Dual-control refund | RA + `payments-refund-dual-control.spec.ts` | — |
| Tenant.admin / ALPHA_002 BOLA | RA + `tenant-scope.spec.ts` | — |
| merchant.denied GET merchants 403 + UI deny | `denied-rbac.spec.ts` | — |
| CASH / decline / lie / expired hosted / PAY_NO_RETURN | tak | — |
| Support IDOR | tak | — |
| Error Lab 400/401/412 + remaining BFF | tak | 429 mock |
| CSRF fail / CSRF happy / 503 / RLS / TLS | tak | — |
| Caddy Location / CORS / Secure cookie | RA + TLS POM | CPL hosted pełny na `--full` |
