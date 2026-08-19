# 06 — Equivalence partitioning i Boundary Value Analysis

Techniki ISTQB FL 4.2.1–4.2.2. Źródło walidacji: `CreateCheckoutSessionRequest` (`@Min(1) @Max(100_000_000)`, `@Size(max=120)`), domain `SUPPORTED_CURRENCIES = {PLN,EUR,USD}`, Zod booking `extOrderId.min(3)`, HMAC tolerance 300s, simulate aliases.

Każda partycja mapuje na `PW-API-*` / `PW-E2E-*`. Kolumna **Reprezentant** = konkretna wartość do TC.

---

## 6.1 `amountMinor` (long)

Valid domain: **[1, 100_000_000]**.  
BVA 3-value (ISTQB: min, min+1, max-1, max + invalid min-1, max+1) — tu klasyczny zestaw interview.

| ID | Klasa | Wartość | Oczekiwany HTTP | `error` | Mapowanie |
|---|---|---|---|---|---|
| BVA-001 | invalid below | `0` | 400 | `validation` | PW-API-032 |
| BVA-002 | min valid | `1` | 302 | — | PW-API-034 |
| BVA-003 | min+1 | `2` | 302 | — | designed (można scalić z 034) |
| BVA-004 | nominal | `1999` | 302 | — | PW-API-020 |
| BVA-005 | max-1 | `99999999` | 302 | — | designed |
| BVA-006 | max valid | `100000000` | 302 | — | PW-API-034 |
| BVA-007 | max+1 | `100000001` | 400 | `validation` | PW-API-033 |
| EP-001 | ujemna | `-1` | 400 | `validation` | designed |
| EP-002 | null / omit | omit | 400 | `validation` (`@NotNull`) | designed |
| EP-003 | nie-liczba | `"abc"` | 400 | `validation` / bind | designed |

UI Zod (booking): te same granice → PW-E2E-015.

**Booking CASH:** `amountMinor` jest `long` bez `@Min` na rekordzie — osobna partycja EP-004: CASH `0` — **zaobserwować** czy 400 domain czy 200 CONFIRMED (luka kontraktu).

---

## 6.2 `extOrderId`

Backend create: `@NotBlank @Size(max=120)`.  
Frontend booking: `.min(3)` — **rozjazd 1–2 znaki**: UI blokuje, API sessions przyjęłoby 1–2 znaki jeśli nieblank.

| ID | Klasa | Wartość | Warstwa | HTTP / UI | Mapowanie |
|---|---|---|---|---|---|
| BVA-010 | empty | `""` | API | 400 | PW-API-035 |
| BVA-011 | whitespace | `"   "` | API | 400 `@NotBlank` | designed |
| BVA-012 | 1 znak | `"A"` | API sessions | **302** (valid backend) | designed — dokumentuj rozjazd |
| BVA-013 | 2 znaki | `"AB"` | UI booking | brak POST | PW-E2E-014 |
| BVA-014 | 3 znaki | `"ABC"` | UI + API | 200/302 | designed |
| BVA-015 | 120 znaków | 120×`x` | API | 302 | PW-API-036 |
| BVA-016 | 121 znaków | 121×`x` | API | 400 details `extOrderId` | PW-API-035 |
| EP-010 | unicode | `"Zamówienie-żółć"` | API | 302 jeśli ≤120 | designed |

---

## 6.3 `currency`

Valid: dokładnie `PLN`, `EUR`, `USD` (Set, **case-sensitive**).

| ID | Klasa | Wartość | HTTP | Mapowanie |
|---|---|---|---|---|
| EP-020 | valid PLN | `PLN` | 302 | PW-API-020 |
| EP-021 | valid EUR | `EUR` | 302 | designed |
| EP-022 | valid USD | `USD` | 302 | designed |
| EP-023 | ISO inne | `GBP`, `CHF`, `CZK` | 400 `validation` | PW-API-031 |
| EP-024 | lowercase | `pln` | 400 | designed |
| EP-025 | blank | `""` | 400 `@NotBlank` | designed |
| EP-026 | null | omit | 400 | designed |

UI: select tylko trzy wartości — EP-023 w UI nieosiągalny bez API.

---

## 6.4 `validitySeconds`

`@NotNull @Min(1)` — **brak max** w bean.

| ID | Klasa | Wartość | HTTP | Mapowanie |
|---|---|---|---|---|
| BVA-020 | 0 | `0` | 400 | PW-API-038 |
| BVA-021 | 1 | `1` | 302; link ~1s | designed + clock |
| BVA-022 | omit | — | 400 | PW-API-037 |
| EP-030 | 900 default booking | omit na **booking** | 200, validity ~15 min | PW-API-302 |
| EP-031 | bardzo duży | `31536000` (1y) | 302 (brak max) | designed — ryzyko lab clock |

---

## 6.5 `continueUrl` / `notifyUrl`

`@NotBlank` na create session. Booking ONLINE: opcjonalne z defaultami.

| ID | Klasa | HTTP | Mapowanie |
|---|---|---|---|
| EP-040 | oba obecne | 302 | PW-API-020 |
| EP-041 | omit continueUrl na sessions | 400 | PW-API-037 |
| EP-042 | omit na booking ONLINE | 200 + default return URL | PW-API-302 |
| EP-043 | nie-URL string `"foo"` | 302 (brak @URL) | designed — słaba walidacja, zanotować |

---

## 6.6 Booking `mode`

| ID | Klasa | Wartość | Wynik | Mapowanie |
|---|---|---|---|---|
| EP-050 | CASH | `CASH` | 200 CONFIRMED, no session | PW-API-300 |
| EP-051 | cash | `cash` | jak CASH | PW-API-301 |
| EP-052 | ONLINE | `ONLINE` | 200 AWAITING + session | PW-API-302 |
| EP-053 | online | `online` | **ONLINE path** (nie equalsIgnoreCase CASH → createOnline) | designed |
| EP-054 | blank | `""` | 400 `@NotBlank` | PW-API-307 |
| EP-055 | inne | `CARD`, `WIRE` | ONLINE path (nie-CASH) | designed — pułapka |
| EP-056 | omit | — | 400 | designed |

---

## 6.7 Simulate `outcome`

| ID | Klasa | Wartość | Session status | Notify? | Mapowanie |
|---|---|---|---|---|---|
| EP-060 | completed | `COMPLETED`, `APPROVED`, `completed` | COMPLETED | tak (happy) | PW-API-113/114/121 |
| EP-061 | canceled | `CANCELED`, `CANCELLED`, `DECLINED` | CANCELED | tak (canceled event) | PW-API-115 |
| EP-062 | pending | `PENDING` | PENDING | nie | PW-API-116 |
| EP-063 | unknown | `CAPTURED`, `FOO` | — | — | PW-API-117 **400** |
| EP-064 | blank | `""` | — | — | PW-API-118 **400** |

---

## 6.8 OAuth form

| ID | Klasa | Wejście | HTTP | Mapowanie |
|---|---|---|---|---|
| EP-070 | valid form | form-urlencoded + matching secret | 200 | PW-API-010 |
| EP-071 | wrong secret | — | 401 empty | PW-API-011 |
| EP-072 | JSON CT | application/json | 401 nie 415 | PW-API-012 |
| EP-073 | wrong grant | `password` / `authorization_code` | 401 | PW-API-013 |
| EP-074 | missing fields | no client_id/secret/grant | 401 | PW-API-014 |

---

## 6.9 HMAC timestamp (BVA ±300 s)

Niech `T` = `clock.now()` w sekundach. Signature payload `"{t}."+raw`.

| ID | `t` | Klasa | HTTP | `error` | Mapowanie |
|---|---|---|---|---|---|
| BVA-030 | T | nominal | 202 | — | PW-API-200 |
| BVA-031 | T-300 | min in | 202 jeśli `<=` | — | PW-API-206 |
| BVA-032 | T+300 | max in | 202 jeśli `<=` | — | PW-API-206 |
| BVA-033 | T-301 | min-1 out | 400 | `invalid_signature` | PW-API-205 |
| BVA-034 | T+301 | max+1 out | 400 | `invalid_signature` | PW-API-205 |
| EP-080 | missing header | invalid | 400 | `invalid_signature` | PW-API-203 |
| EP-081 | tampered v1 | invalid | 400 | `invalid_signature` | PW-API-204 |
| EP-082 | malformed | invalid | 400 | `invalid_signature` | PW-API-207 |

---

## 6.10 Simulate token

| ID | Klasa | HTTP | `error` | Mapowanie |
|---|---|---|---|---|
| EP-090 | missing | 403 | `missing_simulate_token` | PW-API-110 |
| EP-091 | garbage / tamper | 403 | `invalid_simulate_token` | PW-API-111 |
| EP-092 | other session | 403 | `invalid_simulate_token` | PW-API-112 |
| EP-093 | valid z GET | 200 | — | PW-API-113 |
| EP-094 | GET po terminal | token null — POST niemożliwy nowym tokenem | — | PW-API-103 |
| BVA-040 | TTL = min(now+300s, validityUntil) | token działa do exp | — | unit + designed clock |

---

## 6.11 `Lab-Force-Scenario`

| ID | Klasa | HTTP create | Mapowanie |
|---|---|---|---|
| EP-100 | blank / omit | 302, HAPPY | PW-API-070 |
| EP-101 | każdy enum UPPER | 302 | 070–075 |
| EP-102 | lowercase enum | 302 (`toUpperCase`) | PW-API-077 |
| EP-103 | unknown | 400 `validation` | PW-API-029 |
| EP-104 | `OOO_EVENTS` | 302 ale **brak** reorder | blocked GAP-01 |

---

## 6.12 Idempotency-Key

Fingerprint = `extOrderId|amountMinor|currency|continueUrl|notifyUrl` (**bez** validitySeconds). Hash = SHA-256(**tylko** key).

| ID | Klasa | HTTP | Headers out | Mapowanie |
|---|---|---|---|---|
| EP-110 | brak key | 302 unique ids | — | PW-API-027 |
| EP-111 | same key + same fp | 302 | `Idempotency-Replayed: true` | PW-API-024 |
| EP-112 | same key + other fp | 409 | — | PW-API-025 `idempotency_conflict` |
| EP-113 | same key + other validitySeconds | 302 replay | Replayed | PW-API-026 **existing-ra** (GAP-07) |
| EP-114 | inny key + ten sam body | 302 dwa sessionId | — | designed |

---

## 6.13 Session GET path `sessionId`

| ID | Klasa | HTTP | `error` | Mapowanie |
|---|---|---|---|---|
| EP-120 | existing UUID | 200 | — | PW-API-050 |
| EP-121 | random UUID | 404 | `not_found` | PW-API-051 |
| EP-122 | not a UUID | 400 Spring bind (nie CPL handler) | — | designed — zanotować typ |
| EP-123 | hosted vs Bearer DTO | hosted bez notifyUrl | — | PW-API-100 vs 050 |

---

## 6.14 Event `processStatus` / `ackStatus` (output partitions)

Valid enum: `RECEIVED | PROCESSING | DONE | FAILED | DUPLICATE`.  
`ackStatus` CHECK: NULL lub `{200,202,400,503}`.

| ID | Klasa | Kiedy | Mapowanie |
|---|---|---|---|
| EP-130 | RECEIVED | tuż po 202 przed workerem (race) | designed + await |
| EP-131 | DONE | po workerze happy | PW-API-055 |
| EP-132 | DUPLICATE | drugi notify 200 — wiersz nie nowy; status oryginału DONE | PW-API-208 |
| EP-133 | ack 202 | ingest | PW-API-055 |
| EP-134 | delivery 503 then 202 | 5xx scenario | PW-API-057 |

FAILED — jeśli worker rzuci; zaprojektować gdy da się wymusić (P2).

---

## 6.15 Fulfillment status (output)

| ID | Klasa | Trigger | Mapowanie |
|---|---|---|---|
| EP-140 | AWAITING_PAYMENT | create / lie return | PW-API-059, PW-E2E-040 |
| EP-141 | CONFIRMED | completed event + worker / CASH | PW-API-060, 300 |
| EP-142 | CANCELLED | canceled event + worker | PW-API-115 + worker |
| EP-143 | EXPIRED | expired simulate / clock | PW-API-130 |

---

## 6.16 Coverage EP/BVA vs existing RA

RA już: nominal amount, invalid currency, idempotency replay+conflict, missing simulate token, HMAC bad outbound, 5xx, clock expiry, cash, duplicate event.  
**Nie** RA: amount 0/1/max, extOrderId 120/121, HMAC ±300s, outcome aliases, mode lowercase, fingerprint validitySeconds, unknown scenario, UUID bind 400.
