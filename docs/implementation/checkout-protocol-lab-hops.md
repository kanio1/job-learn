# Checkout Protocol Lab — hop map

Educational protocol (not a real PSP). Flag: `app.checkout-lab.enabled` (dev default true on this branch).

## Hops

| Hop | HTTP | Auth | Oracle |
|---|---|---|---|
| `POST /api/checkout-lab/oauth/token` | 200 form-urlencoded | client_id/secret | lab Bearer `lab.*` |
| `POST /api/checkout-lab/sessions` | **302** + `Location` + JSON | lab Bearer | RA `follow(false)` |
| `GET /api/checkout-lab/hosted/sessions/{id}` | 200 public DTO | none | no `notifyUrl`; issues `simulateToken` |
| `POST .../hosted/sessions/{id}/simulate` | 200 | `Lab-Simulate-Token` from GET | session CREATED/PENDING only |
| `POST /api/checkout-lab/notify` | 202 / 400 / 503 / 200 duplicate | HMAC `Lab-Signature` | raw body bytes |
| Worker SKIP LOCKED | n/a | n/a | fulfillment after refetch |
| `GET .../hosted/sessions/{id}/fulfillment` | 200 | none | return-page oracle |
| Cash `POST /api/checkout-lab/bookings` | 200 CONFIRMED | lab Bearer via BFF | no `checkout_session` row |

## Signature

```
Lab-Signature: t=<epoch>,v1=<hex>
signed_payload = "{t}." + raw_body
v1 = HMAC_SHA256(hmacSecret, signed_payload)
```

400 = do not retry. 503 = retry (stub: 100ms/200ms). 202 = queued, not fulfilled.

## Hosted capability token

```
GET hosted → simulateToken + simulateTokenExpiresAt
POST simulate → Lab-Simulate-Token: <hex>
signed_payload = "simulate.{sessionId}.{expEpoch}"
HMAC_SHA256(hmacSecret, signed_payload)
```

403 = missing/invalid/expired token (not Keycloak 401). Public PSP page stays cookie-free.

## Identity contrast

| World | Where | Not used for |
|---|---|---|
| Keycloak JWT | dashboard Booking / Inspector / hub | notify, hosted page |
| Lab OAuth Bearer | create session, bookings, inspector APIs (BFF) | HMAC notify |
| HMAC | notify receiver | dashboard login |
| Hosted capability | GET issues token, POST simulate consumes it | merchant create |

## Provider matrix (lab vs PayU-like)

| PayU-like | Lab |
|---|---|
| OpenPayu-Signature | Lab-Signature |
| notifyUrl IPN | POST /api/checkout-lab/notify |
| continueUrl | untrusted hint |
| OrderCreate 302 | POST /sessions 302 |
| Hosted session token | Lab-Simulate-Token |

## Non-goals

No Kafka, real PayU/Stripe, PAN/3DS, settlement, realm changes, `payment_orders` coupling.

See `status/roadmaps/checkout-protocol-lab/`.
