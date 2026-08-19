---
name: playwright-real-stack-learning-map
origin: POST_KIRO_WORK
status: IMPLEMENTED
last_updated: 2026-08-15
---

# Mapa lekcja → feature → spec

Curriculum: 90 lekcji Playwright 1.61. Poniżej tylko luki, które wymagały produktu. Reszta (1–15 locatory, 20–21 RBAC, 47–58 network/session lab, 72–77 visual) już miała powierzchnię.

| Lekcje / kontrolka | Feature | Spec |
|---|---|---|
| 40–46 `APIRequestContext` | BFF cookie REST | `tests-pom/specs/admin-bff.spec.ts`, `payments-conditional.spec.ts`, `payments-create.spec.ts` |
| 41 `request.head()` | Nitro `*.head.ts` + Spring HEAD | `tests-pom/specs/payments-conditional.spec.ts` |
| 41 / 78 multipart | POST evidence `multipart` | `payments-evidence-export.spec.ts`; RA evidence |
| 43 Zod `safeParse` | runtime contract w teście | `tests-pom/utils/problem.ts` + BFF response validation |
| 9 / 72 `getByAltText` | thumbnail evidence PNG/JPEG | EvidenceUpload + GET bytes |
| 10 / 19 radio | kategoria evidence | `payments-evidence-export.spec.ts` |
| 78 `dragAndDrop` / dropzone | `evidence-dropzone` | `payments-evidence-export.spec.ts` |
| 73 clipboard | Copy reference | payment detail (live POM) |
| 82 `context.setOffline` | payments offline banner | `tests-pom/specs/payments-offline.spec.ts` |
| 81 clock vs DB | expiration sweep POST | RA `expirationSweepRequiresPlatformLifecycle`; `payments-expiration.spec.ts` |
| 59 RP logout (`end_session`, nie RFC 7009 revoke) | Session lab `session-lab-end-oidc` + POST `/api/session-lab/end-session` | Produkt: UI + BFF. Spec hopu **designed** (E2E-013). Menu Sign out = `session.spec.ts` E2E-010. Revoke lab device click = `session.spec.ts` E2E-122 |
| 9 `getByTitle` | badge `title=STATUS` | payment list badge (`payment-status-badge`) |
| 16 / 5 202+Location | export-jobs | RA PlaywrightLearningStack; `payments-async-export.spec.ts` |
| 56 / 63 two-context | refund dual-control | `payments-refund-dual-control.spec.ts`; RA maker 409 |
| 74 axe | `@axe-core/playwright` | `tests-pom/specs/a11y-axe.spec.ts` |
| 63 ConfirmActionModal | mirror bank approve | `mirror-lab-rbac.spec.ts` (confirm click) |

Świadomie poza mapą: native `page.on('dialog')`, WebAuthn, WS, GraphQL, i18n, mobile, Kafka, PSP.
