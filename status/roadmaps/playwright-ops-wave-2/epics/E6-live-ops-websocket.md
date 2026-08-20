---
name: epic-e6-live-ops-websocket
parent: playwright-ops-wave-2
epic: E6
tasks: [PW-OPS-T10, PW-OPS-T11, PW-OPS-T22]
last_updated: 2026-08-20
---

# Epic E6 — Live Operations Feed / WebSocket (#12)

**Cel produktowy:** na Overview widać push eventów płatności (i later cases) ze statusem połączenia.  
**Cel dydaktyczny:** prawdziwy WS vs polling (`usePaymentStatusPolling` już jest); duplicate / out-of-order / malformed / disconnect **bez** `routeWebSocket`.

**Gate:** zgoda na `spring-boot-starter-websocket`. Payments już emitują lifecycle. Inject do chaosu.

Nuxt UI: `UPopover`, `UChip`, `UBadge`, `UToast`, `UTimeline` (fallback `ol`).  
POM: `OpsFeedComponent`.

Przeciwwaga: polling na Payment Detail zostaje. Dwa modele w curriculum.

---

## Story E6-S1 — Kanał WS + inject

**Task:** `PW-OPS-T10` · P0

### Jako / chcę / aby

Jako operator widzę zdarzenia w czasie rzeczywistym bez odświeżania Overview.

### Business case

`BC-OPS-12` — Ops feed. Nie Kafka. Jedna instancja `--app`, SimpleBroker.

### Use case

`UC-OPS-24` — Browser → `wss?` **same-origin** `ws://localhost:3000/api/ops/feed` (Nitro) → Spring z Bearer z sesji.

### Architecture rules

- Token **nie** w URL i nie w JS. Nitro upgrade + backendApi JWT.
- Frame JSON:

```json
{
  "eventId": "uuid",
  "occurredAt": "2026-08-20T10:42:03Z",
  "merchantId": "uuid",
  "paymentOrderId": "uuid",
  "type": "PAYMENT_CAPTURED",
  "label": "PO-1008  CAPTURED"
}
```

- `POST /api/ops/feed/inject` authority `platform:ops:inject` only. Body pozwala: duplicate same `eventId`, `occurredAt` w przeszłości (out-of-order), `raw` malformed string.
- Disconnect inject: `POST /api/ops/feed/disconnect-me` zamyka sesję WS caller (albo `setOffline` w teście — oba w AC).
- Tenant/merchant filter: manager dostaje tylko swoje; readonly dostaje feed read.

### Acceptance criteria

- [ ] `ops` module + Flyway location (V34 może iść z E7; WS może działać in-memory bez tabeli).
- [ ] 401 handshake → UI disconnected.
- [ ] REST Assured nie musi otwierać WS (opcjonalnie Spring test); kontrakt inject RA.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| RA-OPS-125 | RA | inject 201; readonly inject 403 |
| RA-OPS-126 | RA | inject malformed accepted (stored) 201 |
| RA-OPS-127 | RA | manager inject 403 |
| PW-OPS-API-020 | PW REST | inject przez BFF cookie admin |

**Zakaz:** `page.routeWebSocket`. Learning mock WS = learner copy only, nie product.

---

## Story E6-S2 — UI feed + live frame oracle

**Task:** `PW-OPS-T11`, `PW-OPS-T22` · P0

### Use case

`UC-OPS-25`

```text
Live Operations                           ● connected
10:42:01  PO-1002  AUTHORIZED
10:42:03  PO-1008  CAPTURED
```

Prawdziwy capture w drugim context / `BffClient` → `waitForFrameReceived` + wiersz UI.

### Acceptance criteria

- [ ] Chip connected/disconnected (`UChip`).
- [ ] Helper POM `waitForOpsEvent(eventId | type+orderRef)`.
- [ ] Capture z API (nie UI) wystarcza jako producer.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-120 | E2E | BffClient capture → websocket framereceived → row visible |

---

## Story E6-S3 — Duplicate, out-of-order, malformed

**Task:** `PW-OPS-T11` · P0

### Use case

`UC-OPS-26`

```text
1. inject CAPTURED eventId=X
2. inject CAPTURED eventId=X   ← duplicate
UI raz.
```

```text
inject CAPTURED occurredAt=T2
inject AUTHORIZED occurredAt=T1
UI posortowane T1 potem T2 (chronologia biznesowa, nie kolejność ramek).
```

Malformed: UI toast „Ignored invalid event”; app nie crashuje; timeline bez śmieci.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-121 | E2E | duplicate eventId → jeden wiersz |
| PW-OPS-E2E-122 | E2E | out-of-order occurredAt → sort UI |
| PW-OPS-E2E-123 | E2E | malformed → toast; feed nadal connected |

---

## Story E6-S4 — Disconnect / reconnect

**Task:** `PW-OPS-T11` · P0

### Acceptance criteria

- [ ] `browserContext.setOffline(true)` → chip disconnected (offline banner payments może też być — nie konflikt).
- [ ] `setOffline(false)` → reconnect; **nie** duplikować historii (snapshot last N z REST `GET /api/ops/feed/recent` przy handshake).
- [ ] Alternatywa: inject disconnect-me.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-124 | E2E | setOffline true/false chip |
| PW-OPS-E2E-125 | E2E | reconnect nie dubluje eventId z recent |

### Learning

Distributed-system testing na żywym kanale. TypeScript: `OpsFeedEvent` union per `type`.
