---
type: lesson
status: planned
area: REST API From Zero
lesson: 13
module: HTTP Caching Deep Dive, CORS Configuration, and API Versioning Strategies
date: 2026-05-31
tags:
  - rest-api
  - http-caching
  - cors
  - api-versioning
  - lesson-13
  - senior-sdet
---

# Lesson 13 — HTTP Caching Deep Dive, CORS Configuration, and API Versioning Strategies

> **Evidence link:** `SecurityConfig.java`, `PaymentOrderController.java`
>
> **Navigation:** [[REST API From Zero MOC]] | [[Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Zrozumieć zaawansowane HTTP/REST concepts (implementation level):
- **HTTP caching deep dive** — Cache-Control, ETag, Vary, conditional requests
- **CORS configuration** — kiedy i jak skonfigurować CORS w Spring Boot
- **API versioning strategies** — URL path, header, query param, content negotiation

## 2. Prerequisites

- HTTP methods, status codes, headers (Lesson 01-04).
- ETag basics (Lesson 06, 10).
- CORS awareness (Lesson 11).
- API versioning awareness (Lesson 11).

## 3. Code Reading Map

| Plik | Co czytać |
|---|---|
| `SecurityConfig.java` | Obecny brak CORS configuration |
| `PaymentOrderController.java` | Obecny brak Cache-Control headers |
| `CorrelationIdFilter.java` | Custom filter (może być rozszerzony dla caching) |

## 4. Kluczowe Pojęcia

### 4.1 HTTP Caching Deep Dive

```http
Response z Cache-Control:
HTTP/1.1 200 OK
Cache-Control: no-cache, no-store, must-revalidate
ETag: "po-123-v1"
Vary: Authorization

Conditional request:
GET /api/merchants/123/payment-orders/456
If-None-Match: "po-123-v1"

Response (jeśli niezmieniony):
HTTP/1.1 304 Not Modified
ETag: "po-123-v1"

Response (jeśli zmieniony):
HTTP/1.1 200 OK
ETag: "po-123-v2"
{ ... }
```

**Cache-Control directives:**

| Directive | Znaczenie | Kiedy używać |
|---|---|---|
| `no-cache` | Browser musi revalidować z serverem (użyj If-None-Match) | Dynamic data (payment orders) |
| `no-store` | Browser nie może cache'ować w ogóle | Sensitive data (auth tokens, personal data) |
| `must-revalidate` | Stale cache nie może być użyty bez revalidacji | Critical data (payment status) |
| `max-age=3600` | Cache ważny przez 3600 sekund (1 godzina) | Static data (merchant info, currency list) |
| `private` | Cache tylko w browser (nie w CDN/proxy) | User-specific data (payment orders) |
| `public` | Cache w browser i CDN/proxy | Public data (API documentation) |

**ETag + If-None-Match workflow:**
1. Server zwraca response z `ETag: "po-123-v1"`
2. Browser cache'uje response + ETag
3. Następny request: browser wysyła `If-None-Match: "po-123-v1"`
4. Server sprawdza czy resource się zmienił (porównuje ETag)
5. Jeśli niezmieniony → `304 Not Modified` (brak body, oszczędność bandwidth)
6. Jeśli zmieniony → `200 OK` z nowym ETag i body

**Vary header:**
- `Vary: Authorization` — cache key zależy od Authorization header
- Dlaczego? Response dla merchant A ≠ response dla merchant B
- Bez `Vary` → cache mógłby zwrócić wrong data dla wrong user

**Implementacja w Spring Boot:**
```java
@GetMapping("/{paymentOrderId}")
public ResponseEntity<PaymentOrderResponse> getPaymentOrder(@PathVariable UUID paymentOrderId) {
    PaymentOrder order = paymentOrderService.getOrder(paymentOrderId);
    PaymentOrderResponse response = PaymentOrderMapper.toResponse(order);
    
    String etag = "\"" + order.getVersion() + "\"";
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
        .header(HttpHeaders.ETAG, etag)
        .header(HttpHeaders.VARY, "Authorization")
        .body(response);
}
```

### 4.2 CORS Configuration

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")  // Frontend origin
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);  // Preflight cache: 1 hour
    }
}
```

**CORS headers:**

| Header | Znaczenie |
|---|---|
| `Access-Control-Allow-Origin` | Dozwolone origins (np. `http://localhost:3000`) |
| `Access-Control-Allow-Methods` | Dozwolone methods (np. `GET, POST, PUT, DELETE`) |
| `Access-Control-Allow-Headers` | Dozwolone headers (np. `Authorization, Content-Type`) |
| `Access-Control-Allow-Credentials` | Czy browser może wysyłać credentials (cookies, auth) |
| `Access-Control-Max-Age` | Jak długo browser może cache'ować preflight response |

**Preflight request (OPTIONS):**
```http
OPTIONS /api/merchants/123/payment-orders
Origin: http://localhost:3000
Access-Control-Request-Method: POST
Access-Control-Request-Headers: Authorization, Content-Type

Response:
HTTP/1.1 204 No Content
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

**Kiedy konfigurować CORS?**
- Gdy frontend bezpośrednio woła backend (bez proxy)
- Gdy mobile app woła backend
- Gdy third-party API consumer woła backend

**Dlaczego nasz system NIE potrzebuje CORS?**
- Frontend używa **Nuxt server proxy** (`server/api/merchants/...`)
- Proxy działa server-side (Node.js), nie browser-side
- Browser widzi request do `localhost:3000/api/merchants/...` (same origin)
- Proxy forwarduje request do `localhost:8080/api/merchants/...` (server-to-server, nie cross-origin)

### 4.3 API Versioning Strategies

**URL path versioning:**
```http
GET /api/v1/merchants/123/payment-orders
GET /api/v2/merchants/123/payment-orders
```

**Header versioning:**
```http
GET /api/merchants/123/payment-orders
Accept: application/vnd.myapi.v1+json
```

**Query param versioning:**
```http
GET /api/merchants/123/payment-orders?version=1
GET /api/merchants/123/payment-orders?version=2
```

**Content negotiation versioning:**
```http
GET /api/merchants/123/payment-orders
Accept: application/json; version=1
```

**Trade-offs:**

| Strategia | Zalety | Wady |
|---|---|---|
| URL path | Explicit, easy to understand, cacheable | Breaking change = nowy URL, URL pollution |
| Header | Clean URL, flexible | Less visible, harder to test (curl, browser) |
| Query param | Simple, easy to test | Pollutes query string, cache key issues |
| Content negotiation | Standard HTTP, clean URL | Complex implementation, less visible |

**Kiedy versionować?**
- Gdy zmieniasz response shape (breaking change)
- Gdy usuwasz endpoint (deprecation)
- Gdy zmieniasz semantics (np. `status` field zmienia meaning)
- Gdy masz multiple consumers z different requirements

**Dlaczego nasz system NIE versionuje?**
- Internal API (nie public)
- Jeden consumer (frontend)
- Frontend i backend deployowane razem (no version skew)
- Lesson 09: frontend używa Zod schemas do walidacji response shape

**Implementacja URL path versioning w Spring Boot:**
```java
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/payment-orders")
public class PaymentOrderControllerV1 {
    // V1 implementation
}

@RestController
@RequestMapping("/api/v2/merchants/{merchantId}/payment-orders")
public class PaymentOrderControllerV2 {
    // V2 implementation (breaking changes)
}
```

## 5. Walkthrough — Od Brak Caching Do Full Caching

```
PRZED (brak caching):
1. Browser wysyła GET request
2. Server zwraca 200 OK z body
3. Browser cache'uje response (default behavior)
4. Następny request: browser wysyła GET request
5. Server zwraca 200 OK z body (nawet jeśli niezmieniony)
6. Wasted bandwidth (body wysyłane za każdym razem)

PO (full caching):
1. Browser wysyła GET request
2. Server zwraca 200 OK z body + ETag + Cache-Control
3. Browser cache'uje response + ETag
4. Następny request: browser wysyła GET z If-None-Match: "etag-value"
5. Server sprawdza ETag (resource niezmieniony)
6. Server zwraca 304 Not Modified (brak body)
7. Saved bandwidth (brak body w response)
```

## 6. Learning Delta — Co Nowe vs Lessons 06-12

| Temat | Lesson 06-12 | Lesson 13 |
|---|---|---|
| HTTP caching | ETag basics (Lesson 06, 10) | Full caching workflow (Cache-Control, Vary, conditional requests) |
| CORS | Awareness (Lesson 11) | Implementation (Spring Boot CorsConfig) |
| API versioning | Awareness (Lesson 11) | Implementation (URL path, header, query param) |

## 7. Typowe Błędy

1. **Cache-Control: max-age dla sensitive data.** Payment orders to sensitive data. `max-age=3600` = cache przez 1 godzinę = risk. Lepiej `no-store`.
2. **ETag bez Vary.** Jeśli response zależy od user (Authorization), cache key musi zawierać Authorization. Bez `Vary: Authorization` → cache zwraca wrong data.
3. **CORS "just in case".** Jeśli frontend używa proxy, CORS nie jest potrzebny. Dodanie CORS bez powodu = security risk (allows cross-origin requests).
4. **API versioning "just in case".** Versioning dodaje complexity. Jeśli nie masz multiple consumers, YAGNI.
5. **URL path versioning bez deprecation strategy.** Jeśli zmieniasz z v1 do v2, musisz supportować v1 przez jakiś czas (deprecation period).
6. **Zapominanie o preflight requests.** Browser wysyła OPTIONS request przed POST/PUT/DELETE. Server musi odpowiadać na OPTIONS z CORS headers.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Narysuj workflow: ETag + If-None-Match + 304 Not Modified | 20 min |
| 2 | Wyjaśnij dlaczego `Vary: Authorization` jest ważne dla multi-tenant API | 15 min |
| 3 | Napisz test REST Assured dla `Cache-Control: no-store` | 20 min |
| 4 | Napisz test REST Assured dla `ETag` + `If-None-Match` → 304 | 30 min |
| 5 | Skonfiguruj CORS w Spring Boot dla frontend origin | 25 min |
| 6 | Napisz test REST Assured dla CORS preflight request (OPTIONS) | 30 min |
| 7 | Zaimplementuj URL path versioning (v1, v2) dla PaymentOrderController | 40 min |
| 8 | Porównaj 4 API versioning strategies (trade-offs) | 25 min |

## 9. Pytania

1. Jaka jest różnica między `Cache-Control: no-cache` a `no-store`?
2. Dlaczego `Vary: Authorization` jest ważne dla multi-tenant API?
3. Kiedy API powinno zwracać `304 Not Modified`?
4. Jak browser wie kiedy wysłać `If-None-Match`?
5. Kiedy konfigurować CORS w Spring Boot?
6. Co to jest preflight request (OPTIONS)?
7. Dlaczego URL path versioning jest najpopularniejsze?
8. Kiedy API versioning jest konieczne?
9. Jak zaimplementować deprecation strategy dla v1 → v2?
10. Jak testować CORS z REST Assured?

## 10. Testy

| Test | Co sprawdza |
|---|---|
| `responseContainsCacheControlNoStore` | Cache-Control header obecny |
| `responseContainsVaryAuthorization` | Vary header obecny |
| `ifNoneMatchReturns304WhenUnchanged` | Conditional request works |
| `corsPreflightReturnsAllowedOrigins` | CORS preflight works |
| `corsRequestFromAllowedOriginSucceeds` | CORS request works |
| `v1EndpointReturnsV1Response` | URL path versioning works |
| `v2EndpointReturnsV2Response` | URL path versioning works |

## 11. Powiązane Notatki

- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson 10 - HTTP Semantics, Content Negotiation, and Error Contract Hardening]]
- [[Lesson 11 - CORS, Caching Headers, and API Versioning Awareness]]
- [[Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability]]
- [[Senior SDET Competency Coverage Matrix]]
