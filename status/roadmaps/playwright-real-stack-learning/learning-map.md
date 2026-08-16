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
| 40–46 `APIRequestContext` | BFF cookie REST | `tests/live/http/bff-rest-contract.spec.ts` |
| 41 `request.head()` | Nitro `*.head.ts` + Spring HEAD | `conditional-get-and-head.spec.ts`, bff-rest-contract |
| 41 / 78 multipart | POST evidence `multipart` | bff-rest-contract; RA evidence |
| 43 Zod `safeParse` | runtime contract w teście | bff-rest-contract `paymentOrderSchema` / `problemSchema` |
| 9 / 72 `getByAltText` | thumbnail evidence PNG/JPEG | EvidenceUpload + GET bytes |
| 10 / 19 radio | kategoria evidence | `tests/e2e/evidence-upload.spec.ts` |
| 78 `dragAndDrop` / dropzone | `evidence-dropzone` | evidence-upload e2e |
| 73 clipboard | Copy reference | `payment-order-read.spec.ts` |
| 82 `context.setOffline` | payments offline banner | `payments-offline.spec.ts` |
| 81 clock vs DB | expiration sweep POST | RA `expirationSweepRequiresPlatformLifecycle`; UI button |
| 59 RP logout (`end_session`, nie RFC 7009 revoke) | Session lab `session-lab-end-oidc` + POST `/api/session-lab/end-session` | Produkt: UI + BFF. Spec: **designed** `session-lab.spec.ts` (E2E-013 / MRL E2E-026). Menu Sign out = inny UC (`session.spec.ts` E2E-010) |
| 9 `getByTitle` | badge `title=STATUS` | payment-order-read |
| 16 / 5 202+Location | export-jobs | RA PlaywrightLearningStack; live bff-rest-contract; `payments-async-export.spec.ts` |
| 56 / 63 two-context | refund dual-control | `payments-refund-dual-control.spec.ts`; RA maker 409 |
| 74 axe | `@axe-core/playwright` | `a11y-axe.spec.ts` |
| 63 ConfirmActionModal | mirror bank approve | `mirror-lab-rbac.spec.ts` (confirm click) |

Świadomie poza mapą: native `page.on('dialog')`, WebAuthn, WS, GraphQL, i18n, mobile, Kafka, PSP.
