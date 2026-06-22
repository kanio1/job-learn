---
type: lesson
status: planned
area: REST API From Zero
lesson: 11
module: CORS, Caching Headers, and API Versioning Awareness
date: 2026-05-31
tags:
  - rest-api
  - cors
  - caching
  - api-versioning
  - lesson-11
  - senior-sdet
---

# Lesson 11 — CORS, Caching Headers, and API Versioning Awareness

> **Evidence link:** `SecurityConfig.java`, `PaymentOrderController.java`
>
> **Navigation:** [[REST API From Zero MOC]] | [[Lesson 11 - REST Assured Framework Architecture and Test Organization]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Zrozumieć zaawansowane HTTP/REST concepts, które senior SDET musi znać (nawet jeśli nie są jeszcze zaimplementowane w systemie):
- **CORS** (Cross-Origin Resource Sharing) — dlaczego frontend potrzebuje CORS, jak testować
- **Caching headers** (`Cache-Control`, `ETag`, `Vary`) — kiedy API powinno cache'ować responses
- **API versioning** — strategie versionowania API (URL path, header, query param)
- **HATEOAS / HAL** — hypermedia links w responses (awareness)

## 2. Prerequisites

- HTTP methods, status codes, headers (Lesson 01-04).
- `ETag` i `If-None-Match` (Lesson 06, 10).
- `X-Correlation-ID` (Lesson 06).
- Frontend jako API consumer (Lesson 09).

## 3. Code Reading Map

| Plik | Co czytać |
|---|---|
| `SecurityConfig.java` | Obecny brak CORS configuration (dlaczego?) |
| `PaymentOrderController.java` | `ETag` w create/read responses, brak `Cache-Control` |
| `apps/frontend/server/utils/backendApi.ts` | Frontend proxy (dlaczego nie potrzebuje CORS?) |

## 4. Kluczowe Pojęcia

### 4.1 CORS (Cross-Origin Resource Sharing)

```java
// Spring Boot CORS configuration:
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")  // Frontend origin
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

**Dlaczego CORS?**
- Browser blokuje cross-origin requests (frontend `localhost:3000` → backend `localhost:8080`)
- CORS headers (`Access-Control-Allow-Origin`) mówią browserowi "zezwalam na ten origin"
- Bez CORS → browser blokuje request → frontend nie działa

**Dlaczego nasz system NIE potrzebuje CORS?**
- Frontend używa **Nuxt server proxy** (`server/api/merchants/...`)
- Proxy działa server-side (Node.js), nie browser-side
- Browser widzi request do `localhost:3000/api/merchants/...` (same origin)
- Proxy forwarduje request do `localhost:8080/api/merchants/...` (server-to-server, nie cross-origin)

**Kiedy testować CORS?**
- Gdy frontend bezpośrednio woła backend (bez proxy)
- Gdy mobile app woła backend
- Gdy third-party API consumer woła backend

### 4.2 Caching Headers

```http
Response:
HTTP/1.1 200 OK
Cache-Control: no-cache, no-store, must-revalidate
ETag: "po-123-v1"
Vary: Authorization
```

**Cache-Control directives:**
- `no-cache` — browser musi revalidować z serverem (użyj `If-None-Match`)
- `no-store` — browser nie może cache'ować w ogóle (sensitive data)
- `must-revalidate` — stale cache nie może być użyty bez revalidacji
- `max-age=3600` — cache ważny przez 3600 sekund (1 godzina)

**ETag + If-None-Match:**
```http
Request:
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

**Vary header:**
- `Vary: Authorization` — cache key zależy od Authorization header
- Dlaczego? Response dla merchant A ≠ response dla merchant B
- Bez `Vary` → cache mógłby zwrócić wrong data dla wrong user

**Dlaczego summary NIE ma ETag?**
- Summary to agregacja (total orders, total amount)
- Agregacja zmienia się przy każdym create/delete
- ETag wymagałby versioning agregacji (trudne)
- Lepiej `Cache-Control: no-cache` (revaliduj za każdym razem)

### 4.3 API Versioning Strategies

| Strategia | Przykład | Trade-offs |
|---|---|---|
| URL path | `/api/v1/merchants` | Explicit, ale breaking change = nowy URL |
| Header | `Accept: application/vnd.myapi.v1+json` | Clean URL, ale mniej visible |
| Query param | `/api/merchants?version=1` | Simple, ale pollutes query string |
| Content negotiation | `Accept: application/json; version=1` | Standard HTTP, ale złożone |

**Kiedy versionować?**
- Gdy zmieniasz response shape (breaking change)
- Gdy usuwasz endpoint (deprecation)
- Gdy zmieniasz semantics (np. `status` field zmienia meaning)

**Dlaczego nasz system NIE versionuje?**
- Internal API (nie public)
- Jeden consumer (frontend)
- Frontend i backend deployowane razem (no version skew)
- Lesson 09: frontend używa Zod schemas do walidacji response shape

### 4.4 HATEOAS / HAL (Hypermedia)

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
    "cancel": { "href": "/api/merchants/456/payment-orders/123/cancel" }
  }
}
```

**Dlaczego HATEOAS?**
- Client odkrywa dostępne akcje dynamicznie (z `_links`)
- Nie hardcoded URLs w client code
- Self-documenting API

**Dlaczego nasz system NIE używa HATEOAS?**
- Prosty API (create, read, list, summary)
- Frontend zna wszystkie endpoints (Zod schemas)
- HATEOAS dodaje complexity (link generation, conditional links)
- YAGNI (You Aren't Gonna Need It)

## 5. Walkthrough — Od CORS Do Caching

```
1. Frontend (browser) wysyła request do backend (cross-origin)
2. Browser sprawdza CORS headers (Access-Control-Allow-Origin)
3. Jeśli CORS dozwolony → browser wysyła request
4. Backend przetwarza request i zwraca response
5. Response zawiera Cache-Control (np. no-cache)
6. Response zawiera ETag (np. "po-123-v1")
7. Browser cache'uje response (jeśli Cache-Control pozwala)
8. Następny request: browser wysyła If-None-Match: "po-123-v1"
9. Backend sprawdza czy resource się zmienił
10. Jeśli niezmieniony → 304 Not Modified (oszczędność bandwidth)
11. Jeśli zmieniony → 200 OK z nowym ETag
```

## 6. Learning Delta — Co Nowe vs Lessons 06-10

| Temat | Lesson 06-10 | Lesson 11 |
|---|---|---|
| CORS | Nie wspomniano | Full explanation (dlaczego nie potrzebujemy) |
| Cache-Control | Nie wspomniano | `no-cache`, `no-store`, `must-revalidate` |
| ETag + If-None-Match | Lesson 06 (create/read), Lesson 10 (summary brak) | Full caching workflow |
| Vary header | Nie wspomniano | Cache key zależy od Authorization |
| API versioning | Nie wspomniano | 4 strategie + trade-offs |
| HATEOAS / HAL | Nie wspomniano | Awareness (dlaczego nie używamy) |

## 7. Typowe Błędy

1. **Dodawanie CORS "just in case".** Jeśli frontend używa proxy, CORS nie jest potrzebny. Dodanie CORS bez powodu = security risk.
2. **Cache-Control: max-age dla sensitive data.** Payment orders to sensitive data. `max-age=3600` = cache przez 1 godzinę = risk. Lepiej `no-store`.
3. **ETag bez Vary.** Jeśli response zależy od user (Authorization), cache key musi zawierać Authorization. Bez `Vary: Authorization` → cache zwraca wrong data.
4. **API versioning "just in case".** Versioning dodaje complexity. Jeśli nie masz multiple consumers, YAGNI.
5. **HATEOAS dla prostego API.** HATEOAS jest dobre dla complex APIs z wieloma akcjami. Dla prostego CRUD = over-engineering.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Wyjaśnij dlaczego nasz system NIE potrzebuje CORS | 15 min |
| 2 | Narysuj workflow: ETag + If-None-Match + 304 Not Modified | 20 min |
| 3 | Wyjaśnij dlaczego `Vary: Authorization` jest ważne dla caching | 15 min |
| 4 | Porównaj 4 API versioning strategies (trade-offs) | 20 min |
| 5 | Wyjaśnij dlaczego HATEOAS jest over-engineering dla naszego API | 15 min |
| 6 | Napisz test REST Assured dla `Cache-Control: no-store` | 20 min |
| 7 | Napisz test REST Assured dla `ETag` + `If-None-Match` → 304 | 30 min |

## 9. Pytania

1. Dlaczego browser blokuje cross-origin requests?
2. Jak Nuxt server proxy eliminuje potrzebę CORS?
3. Jaka jest różnica między `Cache-Control: no-cache` a `no-store`?
4. Dlaczego `Vary: Authorization` jest ważne dla multi-tenant API?
5. Kiedy API powinno zwracać `304 Not Modified`?
6. Dlaczego URL path versioning (`/api/v1/...`) jest najpopularniejsze?
7. Kiedy HATEOAS jest wartościowe?
8. Dlaczego nasz system NIE używa HATEOAS?
9. Jak testować CORS z REST Assured?
10. Jak testować caching z REST Assured?

## 10. Testy (Awareness)

| Test | Co sprawdza |
|---|---|
| `responseContainsCacheControlNoStore` | Sensitive data nie cache'owane |
| `responseContainsVaryAuthorization` | Cache key zależy od user |
| `ifNoneMatchReturns304WhenUnchanged` | ETag + If-None-Match workflow |
| `corsPreflightReturnsAllowedOrigins` | CORS configuration (jeśli dodane) |

## 11. Powiązane Notatki

- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson 08 - Summary Endpoint Contract, Status Codes, and Error Taxonomy]]
- [[Lesson 10 - HTTP Semantics, Content Negotiation, and Error Contract Hardening]]
- [[Lesson 11 - REST Assured Framework Architecture and Test Organization]]
- [[Senior SDET Competency Coverage Matrix]]
