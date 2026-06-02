---
type: lesson
status: planned
area: REST API From Zero
lesson: 12
module: HATEOAS, Content Negotiation Deep Dive, and Rate Limiting
date: 2026-05-31
tags:
  - rest-api
  - hateoas
  - content-negotiation
  - rate-limiting
  - lesson-12
  - senior-sdet
---

# Lesson 12 — HATEOAS, Content Negotiation Deep Dive, and Rate Limiting

> **Evidence link:** `PaymentOrderController.java`, `SecurityConfig.java`
>
> **Navigation:** [[REST API From Zero MOC]] | [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Zrozumieć zaawansowane REST API concepts (awareness level — nie implementujemy):
- **HATEOAS / HAL** — hypermedia links w responses
- **Content negotiation deep dive** — `Accept`, `Content-Type`, `415`, `406`
- **Rate limiting** — `429 Too Many Requests`, `Retry-After`
- **API pagination strategies** — offset vs cursor

## 2. Prerequisites

- HTTP methods, status codes, headers (Lesson 01-04).
- `Accept` i `Content-Type` (Lesson 10, 11).
- Pagination (Lesson 07 — offset-based).

## 3. Code Reading Map

| Plik | Co czytać |
|---|---|
| `PaymentOrderController.java` | Obecny brak HATEOAS links |
| `PaymentOrderListResponse.java` | Offset-based pagination (`page`, `size`, `totalElements`) |
| `PaymentExceptionHandler.java` | Brak `429 Too Many Requests` handler |

## 4. Kluczowe Pojęcia

### 4.1 HATEOAS / HAL (Hypermedia as the Engine of Application State)

```json
{
  "paymentOrderId": "123",
  "merchantId": "456",
  "amountMinor": 5000,
  "currency": "PLN",
  "status": "CREATED",
  "_links": {
    "self": { "href": "/api/merchants/456/payment-orders/123" },
    "merchant": { "href": "/api/merchants/456" },
    "cancel": { "href": "/api/merchants/456/payment-orders/123/cancel" },
    "capture": { "href": "/api/merchants/456/payment-orders/123/capture" }
  }
}
```

**Dlaczego HATEOAS?**
- Client odkrywa dostępne akcje dynamicznie (z `_links`)
- Nie hardcoded URLs w client code
- Self-documenting API (links pokazują co można zrobić z resource)
- Conditional actions (np. `cancel` link obecny tylko gdy `status == "CREATED"`)

**HAL (Hypertext Application Language):**
- Standard dla HATEOAS (RFC draft)
- `_links` object z named links
- `_embedded` object dla nested resources
- Spring HATEOAS library wspiera HAL

**Dlaczego nasz system NIE używa HATEOAS?**
- Prosty API (create, read, list, summary)
- Frontend zna wszystkie endpoints (Zod schemas)
- HATEOAS dodaje complexity (link generation, conditional links)
- YAGNI (You Aren't Gonna Need It)

**Kiedy HATEOAS jest wartościowe?**
- Complex APIs z wieloma akcjami (np. payment lifecycle: authorize, capture, cancel, refund)
- Public APIs (third-party consumers nie znają wszystkich endpoints)
- Dynamic workflows (akcje zależą od state)

### 4.2 Content Negotiation Deep Dive

```http
Request:
GET /api/merchants/123/payment-orders/456
Accept: application/json, application/xml;q=0.9

Response (JSON preferred):
HTTP/1.1 200 OK
Content-Type: application/json
Vary: Accept

{ ... }

Response (jeśli server nie wspiera XML):
HTTP/1.1 406 Not Acceptable
Content-Type: application/json

{
  "error": "not_acceptable",
  "message": "Supported formats: application/json"
}
```

**Content negotiation workflow:**
1. Client wysyła `Accept` header z preferred formats (i optional quality values `q=0.9`)
2. Server sprawdza czy może wyprodukować requested format
3. Jeśli tak → zwraca response z `Content-Type` matching best match
4. Jeśli nie → zwraca `406 Not Acceptable`

**Quality values (`q`):**
- `Accept: application/json, application/xml;q=0.9` — JSON preferred (q=1.0 implicit), XML acceptable (q=0.9)
- Server wybiera format z highest quality value
- Jeśli wszystkie formats mają q=0 → `406 Not Acceptable`

**`415 Unsupported Media Type` vs `406 Not Acceptable`:**
- `415` — request body ma unsupported format (np. client wysyła XML, server akceptuje tylko JSON)
- `406` — server nie może wyprodukować requested response format (np. client żąda XML, server zwraca tylko JSON)

**Dlaczego nasz system NIE implementuje content negotiation?**
- Tylko JSON (proste, consistent)
- Frontend żąda JSON (Zod schemas)
- Brak use case dla XML, CSV, etc.
- YAGNI

### 4.3 Rate Limiting

```http
Request:
POST /api/merchants/123/payment-orders
Authorization: Bearer <token>

Response (jeśli rate limit exceeded):
HTTP/1.1 429 Too Many Requests
Retry-After: 60
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1234567890

{
  "error": "rate_limit_exceeded",
  "message": "Rate limit exceeded. Retry after 60 seconds."
}
```

**Rate limiting headers:**
- `X-RateLimit-Limit` — maximum requests per time window (np. 100 requests per minute)
- `X-RateLimit-Remaining` — remaining requests w current window
- `X-RateLimit-Reset` — timestamp gdy window się zresetuje
- `Retry-After` — seconds to wait before retrying (dla `429` i `503`)

**Rate limiting strategies:**
- **Fixed window:** 100 requests per minute (reset co minute)
- **Sliding window:** 100 requests w last 60 seconds (rolling)
- **Token bucket:** Burst allowance (np. 20 requests immediately, then 1 per second)
- **Leaky bucket:** Smooth out bursts (np. max 10 requests per second)

**Dlaczego nasz system NIE implementuje rate limiting?**
- Internal API (nie public)
- Jeden consumer (frontend)
- Brak abuse risk (authenticated, authorized)
- YAGNI

**Kiedy rate limiting jest wartościowe?**
- Public APIs (third-party consumers)
- APIs z expensive operations (np. payment processing)
- APIs z abuse risk (np. login, registration)
- Multi-tenant systems (fair usage per tenant)

### 4.4 API Pagination Strategies

**Offset-based (obecny system):**
```http
GET /api/merchants/123/payment-orders?page=2&size=20

Response:
{
  "content": [ ... ],
  "page": 2,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

**Cursor-based (alternative):**
```http
GET /api/merchants/123/payment-orders?cursor=eyJpZCI6MTIzfQ&limit=20

Response:
{
  "content": [ ... ],
  "nextCursor": "eyJpZCI6NDU2fQ",
  "hasMore": true
}
```

**Trade-offs:**

| Strategia | Zalety | Wady |
|---|---|---|
| Offset-based | Proste, client może jump to any page | Wolne dla large offsets (skip 10000 rows), inconsistent jeśli data zmienia się między pages |
| Cursor-based | Szybkie (index seek), consistent (no duplicates/skips) | Client nie może jump to arbitrary page, cursor jest opaque |

**Kiedy używać cursor-based?**
- Large datasets (> 100k records)
- Real-time data (data zmienia się często)
- Infinite scroll UI (nie traditional pagination)
- APIs z expensive queries (cursor = last seen ID, nie offset)

**Dlaczego nasz system używa offset-based?**
- Małe datasets (< 10k records per merchant)
- Frontend używa traditional pagination (page numbers)
- Proste do implementacji (Spring Data JPA `Pageable`)
- YAGNI (cursor-based nie jest potrzebne)

## 5. Walkthrough — Od Offset Do Cursor Pagination

```
Offset-based (obecny):
1. Client żąda page=2, size=20
2. Server oblicza offset = page * size = 40
3. Server wykonuje SQL: SELECT * FROM payment_orders LIMIT 20 OFFSET 40
4. Server zwraca content + totalElements + totalPages
5. Client wyświetla page 2 z 8 total pages

Cursor-based (alternative):
1. Client żąda cursor=<base64-encoded-id>, limit=20
2. Server dekoduje cursor aby dostać last seen ID (np. 123)
3. Server wykonuje SQL: SELECT * FROM payment_orders WHERE id > 123 ORDER BY id LIMIT 20
4. Server zwraca content + nextCursor (base64-encoded last ID)
5. Client używa nextCursor aby żądać next page
```

## 6. Learning Delta — Co Nowe vs Lessons 06-11

| Temat | Lesson 06-11 | Lesson 12 |
|---|---|---|
| HATEOAS | Nie wspomniano | Full explanation (dlaczego nie używamy) |
| Content negotiation | `Accept`, `406` (Lesson 10, 11) | Deep dive (quality values, `415` vs `406`) |
| Rate limiting | Nie wspomniano | `429`, `Retry-After`, strategies |
| Pagination | Offset-based (Lesson 07) | Cursor-based alternative |

## 7. Typowe Błędy

1. **Dodawanie HATEOAS "just in case".** HATEOAS dodaje complexity. Jeśli nie masz multiple consumers lub dynamic workflows, YAGNI.
2. **Implementacja content negotiation dla jednego formatu.** Jeśli API zwraca tylko JSON, nie dodawaj XML support "for future". YAGNI.
3. **Rate limiting bez monitoring.** Rate limiting powinno być monitored (ile requests blocked, które tenants). Bez monitoring = blind.
4. **Cursor-based pagination dla small datasets.** Cursor-based jest complex. Jeśli masz < 100k records, offset-based jest prostsze.
5. **Zapominanie o `Vary: Accept`.** Jeśli response zależy od `Accept` header, cache key musi zawierać Accept. Bez `Vary: Accept` → cache zwraca wrong format.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Wyjaśnij dlaczego HATEOAS jest over-engineering dla naszego API | 15 min |
| 2 | Narysuj workflow: content negotiation z quality values | 20 min |
| 3 | Wyjaśnij różnicę między `415` a `406` | 15 min |
| 4 | Porównaj rate limiting strategies (fixed window, sliding window, token bucket) | 25 min |
| 5 | Wyjaśnij kiedy cursor-based pagination jest lepsze niż offset-based | 20 min |
| 6 | Napisz test REST Assured dla `429 Too Many Requests` (jeśli rate limiting dodane) | 30 min |
| 7 | Napisz test REST Assured dla `406 Not Acceptable` (jeśli content negotiation dodane) | 20 min |

## 9. Pytania

1. Dlaczego HATEOAS jest wartościowe dla public APIs?
2. Jak quality values (`q=0.9`) wpływają na content negotiation?
3. Jaka jest różnica między `415` a `406`?
4. Dlaczego `Vary: Accept` jest ważne dla caching?
5. Kiedy rate limiting jest konieczne?
6. Jak `Retry-After` header różni się od `X-RateLimit-Reset`?
7. Dlaczego cursor-based pagination jest szybsze dla large datasets?
8. Kiedy offset-based pagination jest wystarczające?
9. Jak testować rate limiting z REST Assured?
10. Jak testować content negotiation z REST Assured?

## 10. Testy (Awareness)

| Test | Co sprawdza |
|---|---|
| `responseContainsHateoasLinks` | HATEOAS links obecne (jeśli dodane) |
| `contentNegotiationReturnsJsonForAcceptJson` | JSON returned for `Accept: application/json` |
| `contentNegotiationReturns406ForAcceptXml` | 406 returned for `Accept: application/xml` |
| `rateLimitingReturns429WhenExceeded` | 429 returned when rate limit exceeded |
| `rateLimitingReturnsRetryAfterHeader` | `Retry-After` header obecny w 429 response |

## 11. Powiązane Notatki

- [[Lesson 07 - Payment Order List Filter Search]]
- [[Lesson 10 - HTTP Semantics, Content Negotiation, and Error Contract Hardening]]
- [[Lesson 11 - CORS, Caching Headers, and API Versioning Awareness]]
- [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]]
- [[Senior SDET Competency Coverage Matrix]]
