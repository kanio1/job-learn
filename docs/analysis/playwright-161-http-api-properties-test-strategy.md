# Playwright 1.61 — HTTP/API Headers, Response Properties & Problem Details: Strategia testowania

> **Wersja analizy:** 1.0  
> **Data:** 2026-06-28  
> **Repo branch:** `001-project-foundation`  
> **Playwright w repo:** `1.60.0` (NIE 1.61 — wymagany upgrade)  
> **Język:** Polski  
> **Metodologia:** discovery-first, 13 agentów-specjalistów, kategoryzacja MVP/Phase-2/Phase-3/Reject

---

## Spis treści

1. [Cel dokumentu](#1-cel-dokumentu)
2. [Stan aktualny — discovery](#2-stan-aktualny--discovery)
3. [Playwright 1.60 → 1.61 — ocena upgrade'u](#3-playwright-160--161--ocena-upgradeu)
4. [Istniejące headery — co już działa](#4-istniejące-headery--co-już-działa)
5. [Cele testowania HTTP/API w Playwright](#5-cele-testowania-httpapi-w-playwright)
6. [Kategoryzacja: MVP / Phase 2 / Phase 3 / Reject](#6-kategoryzacja-mvp--phase-2--phase-3--reject)
7. [Kategoria A — Observability: X-Correlation-ID, traceparent](#7-kategoria-a--observability-x-correlation-id-traceparent)
8. [Kategoria B — Idempotency: Idempotency-Key, Idempotency-Replayed](#8-kategoria-b--idempotency-idempotency-key-idempotency-replayed)
9. [Kategoria C — Cache & Conditional: ETag, If-Match, If-None-Match, 304](#9-kategoria-c--cache--conditional-etag-if-match-if-none-match-304)
10. [Kategoria D — Rate Limiting: Retry-After, RateLimit-*](#10-kategoria-d--rate-limiting-retry-after-ratelimit-)
11. [Kategoria E — Content Negotiation: Accept, Content-Type, Accept-Patch](#11-kategoria-e--content-negotiation-accept-content-type-accept-patch)
12. [Kategoria F — Pagination](#12-kategoria-f--pagination)
13. [Kategoria G — Upload/Download: Content-Disposition, Content-Length, Last-Modified](#13-kategoria-g--uploaddownload-content-disposition-content-length-last-modified)
14. [Kategoria H — Security headers: WWW-Authenticate, Access-Control-Expose-Headers, CORS](#14-kategoria-h--security-headers-www-authenticate-access-control-expose-headers-cors)
15. [Kategoria I — CORS preflight](#15-kategoria-i--cors-preflight)
16. [Kategoria J — Problem Details RFC 9457 extensions](#16-kategoria-j--problem-details-rfc-9457-extensions)
17. [Kategoria K — Tenant context headers](#17-kategoria-k--tenant-context-headers)
18. [Kategoria L — API versioning & lifecycle headers](#18-kategoria-l--api-versioning--lifecycle-headers)
19. [Kategoria M — Async operations: Location, 202](#19-kategoria-m--async-operations-location-202)
20. [Error Lab — zmiany i nowe triggery](#20-error-lab--zmiany-i-nowe-triggery)
21. [Frontend Components — wpływ na istniejące komponenty](#21-frontend-components--wpływ-na-istniejące-komponenty)
22. [Nuxt Server Proxy — uzupełnienie forwardBackendHeaders](#22-nuxt-server-proxy--uzupełnienie-forwardbackendheaders)
23. [Zod Schema — uzupełnienie schematów](#23-zod-schema--uzupełnienie-schematów)
24. [APIRequestContext Helpers — projekt API](#24-apirequestcontext-helpers--projekt-api)
25. [UI/Network Assertion Helpers — Page Object Models](#25-uinetwork-assertion-helpers--page-object-models)
26. [20 obowiązkowych scenariuszy Playwright](#26-20-obowiązkowych-scenariuszy-playwright)
27. [Wpływ na multi-role setup](#27-wpływ-na-multi-role-setup)
28. [Kompatybilność z REST Assured](#28-kompatybilność-z-rest-assured)
29. [Kompatybilność z OpenAPI / springdoc](#29-kompatybilność-z-openapi--springdoc)
30. [Kolejność implementacji](#30-kolejność-implementacji)
31. [Czego NIE dodawać — lista odrzuconych](#31-czego-nie-dodawać--lista-odrzuconych)
32. [Najważniejsze ryzyko security](#32-najważniejsze-ryzyko-security)
33. [Najważniejsze ryzyko overengineeringu](#33-najważniejsze-ryzyko-overengineeringu)
34. [Rekomendacja końcowa](#34-rekomendacja-końcowa)

---

## 1. Cel dokumentu

Dokument opisuje, które HTTP/API headers, response properties, metadata fields i Problem Details extensions warto **dodać albo ustandaryzować** w systemie Payment Quality Engineering Lab, aby maksymalizować wartość edukacyjną dla SDET/Senior QA Automation pracującego z Playwright 1.61 i REST Assured 6.

Analiza obejmuje trzy warstwy:

| Warstwa | Zakres |
|---------|--------|
| Backend (Java/Spring) | Co backend produkuje lub musi zacząć produkować |
| BFF Proxy (Nuxt server) | Co `backendApi.ts` propaguje przez `/server/api/**` |
| Frontend (Nuxt/Vue) | Co `useApiClient`, `ApiHeaders` i komponenty UI konsumują i wyświetlają |

**Zasada:** Każdy header/pole musi przynosić wartość edukacyjną i mieć co najmniej jeden testowalny aspekt w Playwright. Headery istniejące tylko dla kompletności specyfikacji HTTP są odrzucane.

---

## 2. Stan aktualny — discovery

### 2.1 Playwright

```
Plik: apps/frontend/package.json
"@playwright/test": "1.60.0"   ← NIE 1.61, upgrade wymagany
```

```
Plik: apps/frontend/playwright.config.ts
- fullyParallel: false           ← musi zostać (brak data isolation)
- retries: CI ? 1 : 0
- workers: CI ? 2 : undefined
- trace: 'on-first-retry'        ← brak screenshot, video
- Projekty: 2 (auth-setup + chromium)
- Jedyna rola: platform-operator (storageState)
- Brak multi-role setup
```

**Anty-wzorzec w kodzie:**
```
apps/frontend/tests/e2e/payment-order-create.spec.ts:16
await page.waitForTimeout(500)  ← wymiana na waitForResponse/expect.poll
```

**Brak:**
- `APIRequestContext` (nie jest używany w żadnym teście)
- `page.waitForResponse()` do asercji network
- Scoping na sieć (brak `expect(response).toMatchObject(...)`)
- Multi-role projects
- Worker-scoped fixtures dla auth
- Screenshot/video w konfiguracji CI

### 2.2 Headery produkowane przez backend

| Header | Backend | `backendApi.ts` forward | `useApiClient` capture | `ApiHeaders` type |
|--------|---------|------------------------|----------------------|------------------|
| `X-Correlation-ID` | ✅ `CorrelationIdFilter` @Order(1) | ✅ | ✅ `correlationId` | ✅ |
| `ETag` | ✅ `PaymentEtag` | ✅ | ✅ `etag` | ✅ |
| `Location` | ✅ 201 Created | ✅ | ✅ `location` | ✅ |
| `Cache-Control: no-store` | ✅ wszystkie payment | ✅ | ✅ `cacheControl` | ✅ |
| `Vary: Authorization` | ✅ + warianty | ✅ | ✅ `vary` | ✅ |
| `Accept-Patch` | ✅ 405, 415 | ✅ | ✅ `acceptPatch` | ✅ |
| `Allow` | ✅ 405 | ✅ | ✅ `allow` | ✅ |
| `WWW-Authenticate` | ✅ Spring Security 401 | ❌ | ❌ | ❌ |
| `Retry-After` | ❌ brak impl. | ❌ | ❌ | ❌ |
| `If-None-Match` (request) | ❌ nie obsługiwany | n/d | n/d | n/d |
| `Content-Disposition` | ❌ | ❌ | ❌ | ❌ |
| `Content-Length` | ✅ Spring auto | ❌ | ❌ | ❌ |
| `Last-Modified` | ❌ | ❌ | ❌ | ❌ |
| `RateLimit-Limit` | ❌ | ❌ | ❌ | ❌ |

### 2.3 Problem Details — stan

`GlobalExceptionHandler` produkuje:
```json
{
  "type": "https://api.payment-quality.local/problems/method-not-allowed",
  "title": "Method Not Allowed",
  "status": 405,
  "detail": "...",
  "correlationId": "uuid",
  "error": "method_not_allowed"
}
```

`PaymentErrorResponse` produkuje dodatkowo: `code`, `message`, `details` (fieldErrors).

`ProblemDetailsCard.vue` wyświetla **tylko** standard RFC 7807: `type, title, status, detail, instance`.
Pola `correlationId`, `fieldErrors`, `error/code` są widoczne tylko w `RawJsonViewer`.

### 2.4 CORS

`SecurityConfig` ma CORS bean — niedostępny dla Playwright bez inspekcji runtime. Brak `Access-Control-Expose-Headers` dla custom headers (`ETag`, `X-Correlation-ID`, `Retry-After`).

### 2.5 Istniejące Error Lab triggery

```
server/api/error-lab/trigger-400.post.ts   ✅
server/api/error-lab/trigger-401.get.ts    ✅
server/api/error-lab/trigger-403.get.ts    ✅
server/api/error-lab/trigger-404.get.ts    ✅
server/api/error-lab/trigger-406.get.ts    ✅
server/api/error-lab/trigger-409.post.ts   ✅
server/api/error-lab/trigger-412.post.ts   ✅
server/api/error-lab/trigger-415.post.ts   ✅
server/api/error-lab/trigger-428.post.ts   ✅

Brak: 429, 304, idempotency-replay, cross-tenant (403-tenant-boundary)
```

### 2.6 `Idempotency-Replayed` — kluczowa luka

Backend zwraca kod 200 przy replay (nie 201), ale nie wysyła żadnego headeru wskazującego, że odpowiedź jest replayed. Pole `Idempotency-Replayed: true` pozwoliłoby Playwrightowi w sieci odróżnić nowe żądanie od replay bez parsowania treści.

### 2.7 `PaymentErrorResponse` — ważna korekta vs. roadmap

```java
// PaymentExceptionHandler.java
// IdempotencyCreateInProgressException → HttpStatus.CONFLICT (409), NIE 425 Too Early
```

Roadmap zakładał 425, backend implementuje 409. Wszystkie scenariusze test muszą oczekiwać **409**, nie 425.

---

## 3. Playwright 1.60 → 1.61 — ocena upgrade'u

### 3.1 Co wnosi 1.61 (istotne dla tego projektu)

| API | Status w 1.60 | Status w 1.61 | Wartość edukacyjna |
|-----|--------------|--------------|-------------------|
| `page.localStorage` | ❌ brak | ✅ nowe API | Asercja braku tokenów w localStorage |
| `page.sessionStorage` | ❌ brak | ✅ nowe API | Weryfikacja że session jest server-side |
| `expect(response).toMatchObject()` | ❌ brak | ✅ nowe API | Network assertion bez custom helpers |
| Video mode `retain-on-failure` | ✅ istnieje | ✅ poprawione | Lepsza diagnostyka |
| `expect.soft()` | ✅ istnieje | ✅ stabilne | Bez zmian |
| `expect.poll()` | ✅ istnieje | ✅ stabilne | Zamiana `waitForTimeout` |
| `APIRequestContext` | ✅ istnieje | ✅ bez zmian | Kluczowe dla tego projektu |

**KOREKTA z Addendum roadmapu:** `expect.soft.poll()` NIE istnieje ani w 1.60, ani w 1.61. Używaj `expect.soft()` i `expect.poll()` osobno.

### 3.2 Minimalna ścieżka upgrade

```bash
# 1. Zmień wersję w package.json
"@playwright/test": "1.61.0"

# 2. Zainstaluj
corepack pnpm install

# 3. Zaktualizuj przeglądarki
corepack pnpm exec playwright install chromium

# 4. Sprawdź typy
corepack pnpm typecheck

# 5. Uruchom smoke suite
corepack pnpm exec playwright test --project=chromium
```

**Rekomendacja:** Upgrade natychmiast jako niezależny PR, ZANIM zaczną się prace nad multi-role setup i APIRequestContext helpers. `page.localStorage` / `page.sessionStorage` są potrzebne do asercji bezpieczeństwa tokenów.

### 3.3 Breaking changes w 1.61

Brak breaking changes istotnych dla tego projektu. Wszystkie istniejące testy powinny przejść bez modyfikacji.

---

## 4. Istniejące headery — co już działa

### 4.1 X-Correlation-ID — pełny przepływ

```
[CorrelationIdFilter @Order(1)]
  → generuje UUID jeśli brak w żądaniu
  → ustawia w MDC("correlationId")
  → ustawia X-Correlation-ID na response
  
[GlobalExceptionHandler / PaymentExceptionHandler]
  → czyta MDC, wstawia w Problem Details body

[backendApi.ts]
  → forwarduje 'X-Correlation-ID' do Nuxt response

[useApiClient extractHeaders()]
  → headers.get('x-correlation-id') → ApiHeaders.correlationId

[ApiDebugPanel / HeaderKeyValuePanel]
  → wyświetla w panelu response headers
```

**Wniosek:** `X-Correlation-ID` ma kompletny przepływ end-to-end. Nadaje się do testowania korelacji żądań w Playwright (ten sam correlationId w UI i w sieci).

### 4.2 ETag — kompletny ale niekompletny

ETag jest generowany (`"v1"`, `"v2"`) i wyświetlany w `EtagDisplay.vue`. Backend **nie obsługuje** `If-None-Match` w GET — nigdy nie zwraca 304. Edukacyjny potencjał jest niepełny bez tego.

### 4.3 Vary — prawidłowy wzorzec

```
GET  /payment-orders/{id}  → Vary: Authorization
POST /payment-orders       → Vary: Authorization, Idempotency-Key  
PATCH /payment-orders/{id} → Vary: Authorization, If-Match
```

To jest wzorzec wart testowania — różne kombinacje Vary w zależności od metody HTTP.

---

## 5. Cele testowania HTTP/API w Playwright

Priorytetowe cele edukacyjne (kolejność ważności):

1. **Network-level assertions** — weryfikacja headerów HTTP bezpośrednio przez `page.waitForResponse()` i `APIRequestContext`, nie tylko przez UI
2. **Problem Details RFC 9457** — asercja pól body, kodów błędów, korelacji z headerami
3. **Conditional requests** — ETag → If-Match → 412; ETag → If-None-Match → 304
4. **Idempotency** — rozróżnienie 201 (nowe) vs 200 (replay) vs 409 (conflict)
5. **Security token hygiene** — brak tokenu w DOM, localStorage, sessionStorage
6. **Rate limiting** — Retry-After, 429 cycle, backoff pattern
7. **CORS** — expose custom headers, allow Idempotency-Key/If-Match
8. **Content negotiation** — 406, 415, Accept-Patch header
9. **Multi-role isolation** — ten sam zasób, różne uprawnienia, różne kody

---

## 6. Kategoryzacja: MVP / Phase 2 / Phase 3 / Reject

### MVP (Phase 1) — dodaj teraz, wysoka wartość, niski koszt

| # | Header / Pole | Gdzie zmiana | Uzasadnienie |
|---|--------------|--------------|-------------|
| M1 | `Retry-After` (przy 429) | backend + proxy + `ApiHeaders` | `ApiHeaders.RETRY_AFTER` już istnieje w Javie; `assertRetryAfterIsValid()` gotowa; Error Lab trigger brakuje |
| M2 | `Idempotency-Replayed: true/false` | backend response header | Edukacyjnie krytyczne — jedyna sieciowa sygnalizacja replay bez parsowania body |
| M3 | `If-None-Match` → 304 Not Modified | backend GET handler | ETag istnieje od dawna, brakuje tylko sprawdzenia warunkowego |
| M4 | `fieldErrors` w ProblemDetailsCard | Vue component | Już przychodzą z backendu, tylko nie wyświetlane w karcie |
| M5 | `correlationId` w ProblemDetailsCard | Vue component | Kluczowe dla debugowania; wartość jest w body |
| M6 | `WWW-Authenticate` forward | `backendApi.ts` | Spring Security wysyła, proxy nie propaguje; blokuje test 401 flow |
| M7 | `retryAfter` w `ApiHeaders` TypeScript | `api.ts` + `useApiClient` | Potrzebne do wyświetlenia Retry-After w UI i do asercji network |
| M8 | Error Lab trigger `429` | `server/api/error-lab/` | Kompletuje Error Lab; powiązane z M1 |
| M9 | `Access-Control-Expose-Headers` | `SecurityConfig` | `ETag`, `X-Correlation-ID`, `Retry-After` muszą być expose'owane żeby Playwright mógł je odczytać przez sieć |
| M10 | `requiredHeader` w Problem Details body (428) | `PaymentExceptionHandler` | Mówi klientowi konkretnie, jakiego headeru brakuje |

### Phase 2 — wysoka wartość, wyższy koszt implementacji

| # | Header / Pole | Gdzie zmiana | Uzasadnienie |
|---|--------------|--------------|-------------|
| P2-1 | `Content-Disposition` | backend CSV export + proxy | Wymaga implementacji CSV export; edukacyjne download flow |
| P2-2 | `Last-Modified` + `If-Modified-Since` | backend payment order | Uzupełnienie cache story; wymaga persystowania updatedAt do nagłówka |
| P2-3 | `RateLimit-Limit`, `RateLimit-Remaining`, `RateLimit-Reset` | backend + proxy + `ApiHeaders` | Draft IETF standard; wymaga wdrożenia rate limiter |
| P2-4 | `traceparent` (W3C Trace Context) | backend (Otel/Micrometer) | Zastępuje X-B3 sprawdzaniem standardu; wymaga Otel setup |
| P2-5 | `retryable: boolean` w Problem Details | GlobalExceptionHandler | Dla 429, 503 — upraszcza logikę retry w kliencie |
| P2-6 | `retryAfterSeconds: integer` w Problem Details | GlobalExceptionHandler | Redundantne z Retry-After, ale UI-friendly; spójne z `retryAfter` w ApiHeaders |
| P2-7 | `Server-Timing` (tylko dev profile) | backend filter | `db;dur=12ms, auth;dur=3ms`; wizualizacja w ApiDebugPanel |
| P2-8 | Multi-role Playwright projects (5 ról) | `playwright.config.ts` | Merchant-Admin, Support, Risk-Reviewer itp.; blokuje testy tenant isolation |

### Phase 3 — niska pilność, wysokie wymagania wstępne

| # | Header / Pole | Uzasadnienie |
|---|--------------|-------------|
| P3-1 | `Link` header (paginacja) | Wymaga wdrożenia cursor pagination |
| P3-2 | `Deprecation` + `Sunset` headers | Wymaga wersjonowania API |
| P3-3 | `tracestate` / `baggage` | Wymaga pełnego Otel distributed tracing |
| P3-4 | `Content-Security-Policy` | Wymaga frontend security audit |
| P3-5 | `Idempotency-Replay-Count` | Rozszerzenie idempotency dla zaawansowanych scenariuszy |

### Reject — nie dodawaj

Patrz sekcja 31 — pełna lista z uzasadnieniem.

---

## 7. Kategoria A — Observability: X-Correlation-ID, traceparent

### Stan aktualny

`X-Correlation-ID` jest kompletnie zaimplementowany (patrz sekcja 4.1). Brak `traceparent`.

### Co dodać (MVP)

**Brak akcji dla X-Correlation-ID** — jest kompletny. Akcja dotyczy tylko **Playwright helpers** do asercji.

**`traceparent` (Phase 2)** — wymaga Micrometer Tracing + OtelTracer. Nie blokuj Phase 1 na to.

### Wartość testowa w Playwright

```typescript
// Asercja: X-Correlation-ID jest UUID i pojawia się w obu miejscach — w sieci i w UI
const [response] = await Promise.all([
  page.waitForResponse(r => r.url().includes('/api/merchants') && r.status() === 200),
  page.getByRole('button', { name: 'List Merchants' }).click(),
])
const networkCorrelationId = response.headers()['x-correlation-id']
expect(networkCorrelationId).toMatch(/^[0-9a-f-]{36}$/)
await expect(page.getByTestId('http-headers-panel')).toContainText(networkCorrelationId)
```

### Reguła

`X-Correlation-ID` musi być widoczny w panelu response headers ZAWSZE, nawet przy błędach.

---

## 8. Kategoria B — Idempotency: Idempotency-Key, Idempotency-Replayed

### Stan aktualny

`Idempotency-Key` jako request header: zaimplementowany. `IdempotencyKeyInput.vue` istnieje.

Backend przy replay: zwraca 200 zamiast 201. Brak headeru `Idempotency-Replayed`.

**Ważna korekta:** `IdempotencyCreateInProgressException` → **409 CONFLICT** (nie 425 Too Early).

### Co dodać (MVP — M2)

```java
// PaymentOrderController.java — metoda createPaymentOrder
// Przy wykryciu replay (isReplayed = result.isReplayed()):
return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.ok(), VARY_AUTHORIZATION_IDEMPOTENCY_KEY)
    .header("ETag", etag)
    .header("Idempotency-Replayed", "true")   // ← nowy header
    .body(response);

// Przy nowym żądaniu (nie replay):
return PaymentHttpHeaders.sensitivePaymentResponse(ResponseEntity.created(location), ...)
    .header("Idempotency-Replayed", "false")   // ← opcjonalnie
    .body(response);
```

```typescript
// backendApi.ts — dodaj do forwardBackendHeaders
'Idempotency-Replayed'
```

```typescript
// api.ts — ApiHeaders
idempotencyReplayed?: string

// useApiClient.ts — extractHeaders
idempotencyReplayed: headers.get('idempotency-replayed') ?? undefined,
```

### Wartość testowa w Playwright

```typescript
// test: CREATE → CREATE z tym samym kluczem → sprawdź replay header
// Pierwsze żądanie: status 201, Idempotency-Replayed: false
// Drugie żądanie: status 200, Idempotency-Replayed: true
```

### Scenariusze Error Lab

Error Lab trigger `trigger-409.post.ts` już istnieje dla idempotency conflict (409 przy create_in_progress).
Dodać trigger `trigger-idempotency-replay.post.ts` demonstrujący replay (200 z `Idempotency-Replayed: true`).

---

## 9. Kategoria C — Cache & Conditional: ETag, If-Match, If-None-Match, 304

### Stan aktualny

- `ETag` produkowany: ✅ (`"v1"`, `"v2"` itd.)
- `If-Match` obsługiwany: ✅ `PaymentVersionPrecondition`
- `If-None-Match` obsługiwany: ❌ — backend ignoruje ten header
- 304 Not Modified: ❌ — nigdy nie zwracany
- `EtagDisplay.vue`: ✅ wyświetla ETag

### Co dodać (MVP — M3)

```java
// PaymentOrderController.java — metoda getPaymentOrder
@GetMapping(value = "/{paymentOrderId}", produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<PaymentOrderResponse> getPaymentOrder(
        @PathVariable UUID merchantId,
        @PathVariable UUID paymentOrderId,
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
        // ...
) {
    PaymentOrder order = paymentOrderService.findForMerchant(merchantId, paymentOrderId);
    String etag = PaymentEtag.from(order);
    
    if (etag.equals(ifNoneMatch)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .header("ETag", etag)
                .header(X_CORRELATION_ID, PaymentHttpHeaders.correlationId())
                .build();
    }
    // ... reszta normalnie
}
```

Analogicznie w `headPaymentOrder`.

### Wartość testowa w Playwright

```typescript
// Scenariusz: GET → zapamiętaj ETag → GET If-None-Match: etag → asercja 304
const api = await request.newContext({ baseURL: 'http://localhost:3000' })
const firstResp = await api.get(`/server/api/merchants/${merchantId}/payment-orders/${id}`)
const etag = firstResp.headers()['etag']

const secondResp = await api.get(
  `/server/api/merchants/${merchantId}/payment-orders/${id}`,
  { headers: { 'If-None-Match': etag } }
)
expect(secondResp.status()).toBe(304)
expect(secondResp.headers()['etag']).toBe(etag)
```

### Error Lab trigger

Dodać `trigger-304.get.ts` — GET z `If-None-Match` pasującym ETag → demonstruje 304.

---

## 10. Kategoria D — Rate Limiting: Retry-After, RateLimit-*

### Stan aktualny

- `ApiHeaders.RETRY_AFTER` stała w Javie: ✅
- `assertRetryAfterIsValid()` w `HeaderAssertions.java`: ✅
- `PaymentRateLimitContractRestKitTest` w restkit suite: ✅ (contract test)
- Backend implementation 429: ❌ — brak
- `backendApi.ts` forward Retry-After: ❌
- `ApiHeaders.retryAfter` TypeScript: ❌
- Error Lab trigger 429: ❌

### Co dodać

**MVP — M1, M7, M8:**

```typescript
// apps/frontend/app/types/api.ts
export interface ApiHeaders {
  // ... istniejące ...
  retryAfter?: string          // ← nowe M7
  wwwAuthenticate?: string     // ← nowe M6
  idempotencyReplayed?: string // ← nowe M2
}
```

```typescript
// apps/frontend/app/composables/useApiClient.ts — extractHeaders
retryAfter: headers.get('retry-after') ?? undefined,
wwwAuthenticate: headers.get('www-authenticate') ?? undefined,
idempotencyReplayed: headers.get('idempotency-replayed') ?? undefined,
```

```typescript
// apps/frontend/server/utils/backendApi.ts — forwardBackendHeaders
for (const name of [
  'ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID',
  'Location', 'Accept-Patch', 'Allow',
  'Retry-After',           // ← M1
  'WWW-Authenticate',      // ← M6
  'Idempotency-Replayed',  // ← M2
]) { ... }
```

```typescript
// apps/frontend/server/api/error-lab/trigger-429.post.ts (nowy plik — M8)
export default defineEventHandler(() => {
  setResponseStatus(event, 429)
  setResponseHeader(event, 'Retry-After', '30')
  setResponseHeader(event, 'Content-Type', 'application/problem+json')
  return {
    type: 'https://api.payment-quality.local/problems/rate-limit-exceeded',
    title: 'Too Many Requests',
    status: 429,
    detail: 'Rate limit exceeded. Please wait before retrying.',
    correlationId: crypto.randomUUID(),
    error: 'rate_limit_exceeded',
    retryAfterSeconds: 30,
    retryable: true,
  }
})
```

**Phase 2 — RateLimit-* headers (draft IETF standard):**

```
RateLimit-Limit: 100
RateLimit-Remaining: 47
RateLimit-Reset: 1735689600
```

Wymagają wdrożenia Bucket4j lub Spring Rate Limiter. Nie blokuj Phase 1.

### Wartość testowa

```typescript
// Scenariusz: 429 → asercja Retry-After w sieci i w UI
const response = await api.post('/server/api/error-lab/trigger-429')
expect(response.status()).toBe(429)
expect(parseInt(response.headers()['retry-after'])).toBeGreaterThan(0)
// UI: ProblemDetailsCard shows retryAfterSeconds, retryable badge
```

---

## 11. Kategoria E — Content Negotiation: Accept, Content-Type, Accept-Patch

### Stan aktualny

- 406 Not Acceptable: ✅ backend + Error Lab trigger
- 415 Unsupported Media Type: ✅ backend + Error Lab trigger
- `Accept-Patch: application/merge-patch+json`: ✅ forward + capture

### Problem — `Accept-Patch` przy 405

`GlobalExceptionHandler.handleHttpMediaTypeNotSupported()` dodaje `Accept-Patch` header. Ale czy pojawia się też przy zwykłym PATCH bez `Content-Type`? Warto sprawdzić.

### Brak (nie MVP)

Nagłówek `Accept` jako request header jest testowany przez istniejące Error Lab 406.
Brak potrzeby dodawania nowych headerów w tej kategorii.

### Wartość testowa

```typescript
// Scenariusz: PATCH bez Content-Type → 415 → asercja Accept-Patch header
const response = await api.patch(url, { 
  data: '{}',
  headers: { 'Content-Type': 'text/plain' }
})
expect(response.status()).toBe(415)
expect(response.headers()['accept-patch']).toBe('application/merge-patch+json')
```

---

## 12. Kategoria F — Pagination

### Stan aktualny

`GET /api/merchants/{merchantId}/payment-orders` zwraca `PaymentOrderListResponse` — lista bez paginacji headerów (brak `Link`, `X-Total-Count`, cursor).

### Decyzja

**Nie dodawaj headerów paginacji w Phase 1.** Brak paginacji backendowej. `Link` header (RFC 8288) wymaga implementacji cursor/offset paginacji, co jest poza scope'm.

**Phase 3** — jeśli paginacja zostanie zaimplementowana, dodaj `Link` header z relacjami `next`/`prev`.

---

## 13. Kategoria G — Upload/Download: Content-Disposition, Content-Length, Last-Modified

### Stan aktualny

Brak endpointów download/export CSV.

### Decyzja

**Phase 2** — po implementacji eksportu CSV/XLSX payment orders.

Wzorzec:
```
Content-Disposition: attachment; filename="payment-orders-2026-06-28.csv"
Content-Length: 45231
Content-Type: text/csv; charset=utf-8
Last-Modified: Thu, 28 Jun 2026 10:30:00 GMT
```

### `Last-Modified` dla payment orders (Phase 2)

Payment order ma `updatedAt` — warto dodać `Last-Modified` header do GET i HEAD responses:

```java
DateTimeFormatter.RFC_1123_DATE_TIME.format(order.getUpdatedAt().atOffset(ZoneOffset.UTC))
```

Umożliwia testowanie `If-Modified-Since` i pokazuje różnicę od `ETag`/`If-None-Match`.

---

## 14. Kategoria H — Security headers: WWW-Authenticate, Access-Control-Expose-Headers, CORS

### 14.1 WWW-Authenticate (MVP — M6)

Spring Security wysyła `WWW-Authenticate: Bearer realm="payment-api", error="..."` przy 401.
**Problem:** `backendApi.ts` nie forwarduje tego headeru do Nuxt response.

**Wartość testowa:** Asercja, że 401 response ma `WWW-Authenticate` z `Bearer` — standard dla API.

### 14.2 Access-Control-Expose-Headers (MVP — M9)

```java
// SecurityConfig.java — konfiguracja CORS
CorsConfiguration config = new CorsConfiguration();
// ... istniejące ...
config.setExposedHeaders(List.of(
    "ETag",
    "X-Correlation-ID",
    "Location",
    "Accept-Patch",
    "Allow",
    "Retry-After",
    "WWW-Authenticate",
    "Idempotency-Replayed",
    "Cache-Control",
    "Vary"
));
```

**Bez tego** Playwright odczytujący headery przez `page.waitForResponse().headers()` może nie widzieć custom headerów przy cross-origin requests.

### 14.3 Access-Control-Allow-Headers (MVP)

```java
config.setAllowedHeaders(List.of(
    "Authorization",
    "Content-Type",
    "Idempotency-Key",   // ← musi być explicit
    "If-Match",          // ← musi być explicit
    "If-None-Match",     // ← nowe M3
    "X-Correlation-ID",  // ← opcjonalnie
    "Prefer"             // ← jeśli planujesz Prefer header
));
```

### 14.4 Authorization token — security invariant

`HeaderKeyValuePanel.vue` maskuje `Authorization` jako `Bearer ••••••••`. Musi być testowane:

```typescript
// Asercja security: Authorization header nigdy nie pokazuje rzeczywistego tokenu
await expect(page.getByTestId('http-headers-panel')).not.toContainText('Bearer eyJ')
await expect(page.getByTestId('http-headers-panel')).toContainText('Bearer ••••••••')
```

---

## 15. Kategoria I — CORS preflight

### Wartość testowa

```typescript
// CORS preflight OPTIONS — sprawdź expose list
const response = await api.fetch(`/api/merchants/${merchantId}/payment-orders`, {
  method: 'OPTIONS',
  headers: {
    'Origin': 'http://localhost:3000',
    'Access-Control-Request-Method': 'POST',
    'Access-Control-Request-Headers': 'Idempotency-Key, Content-Type',
  }
})
expect(response.status()).toBe(200)
const expose = response.headers()['access-control-expose-headers']
expect(expose).toContain('ETag')
expect(expose).toContain('X-Correlation-ID')
```

**Uwaga:** W Nuxt BFF architekturze (Playwright testuje `/server/api/**`), CORS preflight jest zarządzany przez Nuxt, nie Spring. CORS backend jest widoczny tylko w testach REST Assured bezpośrednio.

---

## 16. Kategoria J — Problem Details RFC 9457 extensions

### Stan aktualny

Istniejące pola w body (poza RFC 7807):
- `correlationId` — UUID korelacji ✅
- `error` — snake_case error code ✅
- `code` — UPPER_SNAKE_CASE (redundantne z `error`) ⚠️
- `message` — redundantne z `detail` ⚠️
- `details` — lista fieldErrors ✅

### Co dodać (MVP — M4, M5, M10)

**M10 — `requiredHeader` w Problem Details 428:**
```java
// PaymentExceptionHandler — obsługa MissingRequestHeaderException
problemBody.put("requiredHeader", ex.getHeaderName());  // "If-Match", "Idempotency-Key"
```

**M4/M5 — rozszerzenie ProblemDetailsCard.vue:**
```vue
<!-- ProblemDetailsCard.vue — dodaj sekcję extensions -->
<div v-if="problem.correlationId" class="flex gap-2">
  <dt>Correlation ID</dt>
  <dd class="font-mono">{{ problem.correlationId }}</dd>
</div>
<div v-if="problem.requiredHeader" class="flex gap-2">
  <dt>Required Header</dt>
  <dd class="font-mono">{{ problem.requiredHeader }}</dd>
</div>
<ul v-if="problem.details?.length" data-testid="field-errors-list">
  <li v-for="err in (problem.details as FieldError[])" :key="err.field">
    <span class="font-mono">{{ err.field }}</span>: {{ err.message }}
  </li>
</ul>
```

**Phase 2 — `retryable` i `retryAfterSeconds`:**
```java
// GlobalExceptionHandler — przy 429
problemBody.put("retryable", true);
problemBody.put("retryAfterSeconds", 30);
```

### Czego NIE dodawać w Problem Details

- Stack trace (security risk)
- Szczegóły infrastruktury (np. hostname, DB connection string)
- Internal user ID lub rola
- Pełne dane wejściowe requestu
- Wersje bibliotek

---

## 17. Kategoria K — Tenant context headers

### Analiza

Headery tenant context w response (np. `X-Tenant-ID: TENANT_ALPHA`) to **security risk**:
- Ujawniają internal tenant mapping
- Naruszają multi-tenancy isolation
- Nie są potrzebne klientowi, który już zna swojego tenanta

**Decyzja: REJECT wszystkie tenant context response headers.**

### Co jest dozwolone

`Vary: Authorization` jako pośredni sygnał, że response zależy od identity tenant/merchant — wystarczające i bezpieczne.

---

## 18. Kategoria L — API versioning & lifecycle headers

### Analiza

- `Deprecation: true` i `Sunset: 2027-12-31` headers (RFC 8594) — wymagają wersjonowania API
- API nie ma wersjonowania (`/api/v1/...`) i nie jest planowane
- `API-Version` vendor header — nie ma standardowego odpowiednika który by wystarczał, ale projekt nie planuje wersjonowania

**Decyzja: REJECT w całości dla Phase 1 i 2. Phase 3 jeśli wersjonowanie zostanie dodane.**

---

## 19. Kategoria M — Async operations: Location, 202

### Stan aktualny

`Location` header jest wysyłany przy 201 Created — wskazuje na nowo utworzony payment order.

`assertLocationPointsToOperation()` w `HeaderAssertions.java` wskazuje, że planowane są async operations (`/operations/` URL pattern).

### Stan

**Phase 3** — brak endpointów 202 Accepted w obecnym systemie. Lifecycle payment (authorize, capture, cancel, refund) jest synchroniczny i zwraca 200.

### Wartość testowa dla istniejącego Location

```typescript
// Location przy 201 Created — asercja formatu URL
const response = await api.post(`/server/api/merchants/${merchantId}/payment-orders`, { data: body })
expect(response.status()).toBe(201)
const location = response.headers()['location']
expect(location).toMatch(/\/api\/merchants\/[0-9a-f-]+\/payment-orders\/[0-9a-f-]+/)
```

---

## 20. Error Lab — zmiany i nowe triggery

### Istniejące triggery (11 plików)

| Trigger | Status | Uwagi |
|---------|--------|-------|
| `trigger-400.post.ts` | ✅ | Problem Details z `details`/fieldErrors |
| `trigger-401.get.ts` | ✅ | Sprawdź czy forward `WWW-Authenticate` (M6) |
| `trigger-403.get.ts` | ✅ | |
| `trigger-404.get.ts` | ✅ | |
| `trigger-406.get.ts` | ✅ | |
| `trigger-409.post.ts` | ✅ | Idempotency conflict (409) |
| `trigger-412.post.ts` | ✅ | Precondition Failed |
| `trigger-415.post.ts` | ✅ | Sprawdź czy zawiera `Accept-Patch` |
| `trigger-428.post.ts` | ✅ | Dodaj `requiredHeader` w body (M10) |
| `trigger.post.ts` | ✅ | Generic |

### Nowe triggery do dodania

| Trigger | Priorytet | Sekcja |
|---------|-----------|--------|
| `trigger-429.post.ts` | **MVP** | §10, M8 |
| `trigger-304.get.ts` | **MVP** | §9, M3 |
| `trigger-idempotency-replay.post.ts` | Phase 2 | §8 |

### Zmiany w istniejących triggerach (MVP)

**`trigger-428.post.ts`** — dodaj pole `requiredHeader` w body:
```typescript
// Dodaj do response body:
requiredHeader: 'If-Match',
```

**`trigger-401.get.ts`** — sprawdź, czy zwraca `WWW-Authenticate`. Jeśli nie, dodaj:
```typescript
setResponseHeader(event, 'WWW-Authenticate', 'Bearer realm="payment-api", error="invalid_token"')
```

---

## 21. Frontend Components — wpływ na istniejące komponenty

### 21.1 ProblemDetailsCard.vue — wymagane rozszerzenie (MVP)

**Obecny stan:** Wyświetla tylko `type, title, status, detail, instance` (RFC 7807 standard).

**Brakuje:** `correlationId`, `fieldErrors` (`details`), `requiredHeader`, `error/code`.

**Zmiany (MVP — M4, M5):**
- Dodaj sekcję "Extension Fields" dla `correlationId`
- Dodaj sekcję "Field Errors" dla `details` array
- Dodaj `data-testid="correlation-id-value"` do elementu z correlationId
- Dodaj `data-testid="field-errors-list"` do listy fieldErrors
- Dodaj `data-testid="required-header-value"` dla 428

**Zachowaj:** `passthrough()` w Zod schema (już jest) zapewnia że rozszerzenia nie są gubione.

### 21.2 EtagDisplay.vue — bez zmian

Ma `data-testid="etag-display"`. Wystarczający dla MVP.

### 21.3 HeaderKeyValuePanel.vue — bez zmian strukturalnych

Wyświetla wszystkie headery jakie otrzyma. Gdy `backendApi.ts` zacznie forwardować `Retry-After` i `WWW-Authenticate`, automatycznie będą widoczne.

### 21.4 ApiDebugPanel.vue — bez zmian

Dziedziczy `HeaderKeyValuePanel` — automatycznie skorzysta z nowych headerów.

### 21.5 IdempotencyKeyInput.vue — bez zmian

Wystarczające dla MVP.

### 21.6 Nowe komponenty (Phase 2)

- `RetryAfterBadge.vue` — wyświetla Retry-After w sekundach z progress barem countdown
- `RateLimitIndicator.vue` — wyświetla `RateLimit-Remaining/Limit` jako pasek
- `ServerTimingPanel.vue` — tabela z `Server-Timing` breakdown (tylko dev)
- `FieldErrorsList.vue` — ekstrakcja z ProblemDetailsCard do osobnego komponentu

---

## 22. Nuxt Server Proxy — uzupełnienie forwardBackendHeaders

### Minimalna zmiana MVP

```typescript
// apps/frontend/server/utils/backendApi.ts
// Zmień listę headerów w forwardBackendHeaders:

const FORWARDED_RESPONSE_HEADERS = [
  'ETag',
  'Cache-Control',
  'Vary',
  'X-Correlation-ID',
  'Location',
  'Accept-Patch',
  'Allow',
  // MVP additions:
  'Retry-After',
  'WWW-Authenticate',
  'Idempotency-Replayed',
]
```

### Phase 2 additions

```typescript
const FORWARDED_RESPONSE_HEADERS_PHASE2 = [
  ...FORWARDED_RESPONSE_HEADERS,
  'Last-Modified',
  'Content-Disposition',
  'Content-Length',
  'RateLimit-Limit',
  'RateLimit-Remaining',
  'RateLimit-Reset',
  'Server-Timing',
]
```

### Request headers forwarding (bez zmian)

`If-Match` i `Idempotency-Key` są już forwardowane. Dodać `If-None-Match` do forwardowania requestu:

```typescript
// W backendApi.ts — forwarding request headers:
if (event.headers.get('If-None-Match')) {
  backendHeaders['If-None-Match'] = event.headers.get('If-None-Match')!
}
```

---

## 23. Zod Schema — uzupełnienie schematów

### 23.1 problem-details.schema.ts — minimalne zmiany

Schema już ma `.passthrough()` — rozszerzenia nie są gubione. Dodaj type hints dla znanych extensions:

```typescript
// apps/frontend/app/schemas/problem-details.schema.ts
export const problemDetailsSchema = z.object({
  type: z.string().optional(),
  title: z.string().optional(),
  status: z.number().int().optional(),
  detail: z.string().optional(),
  instance: z.string().optional(),
  // Known extensions (backend-specific):
  correlationId: z.string().uuid().optional(),
  error: z.string().optional(),
  requiredHeader: z.string().optional(),   // ← M10
  retryable: z.boolean().optional(),        // ← Phase 2
  retryAfterSeconds: z.number().int().optional(), // ← Phase 2
  details: z.array(z.object({              // ← fieldErrors
    field: z.string(),
    message: z.string(),
  })).optional(),
}).passthrough()
```

### 23.2 Brak potrzeby nowych schematów w MVP

Schemat `payment-order.schema.ts`, `merchant.schema.ts` są wystarczające dla MVP. Nowe schematy (`rate-limit.schema.ts`, `server-timing.schema.ts`) — Phase 2.

---

## 24. APIRequestContext Helpers — projekt API

### Lokalizacja

```
apps/frontend/tests/helpers/
├── api-assertions.ts       ← helper funkcje
├── api-fixtures.ts         ← fixture setup
└── page-objects/
    ├── HeaderPanel.ts
    ├── ProblemDetailsCard.ts
    └── NetworkAssertions.ts
```

### 24.1 api-assertions.ts

```typescript
import type { APIResponse } from '@playwright/test'
import { expect } from '@playwright/test'

/**
 * Sprawdza X-Correlation-ID: obecny, format UUID.
 * Zwraca wartość do dalszego użytku.
 */
export function expectCorrelationId(response: APIResponse): string {
  const id = response.headers()['x-correlation-id']
  expect(id, 'X-Correlation-ID musi być obecny').toBeTruthy()
  expect(id, 'X-Correlation-ID musi być UUID v4').toMatch(
    /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
  )
  return id
}

/**
 * Sprawdza Cache-Control: no-store na wrażliwych odpowiedziach.
 */
export function expectNoStore(response: APIResponse): void {
  expect(
    response.headers()['cache-control'],
    'Cache-Control musi zawierać no-store dla danych płatności'
  ).toContain('no-store')
}

/**
 * Sprawdza ETag: obecny i w formacie "v<n>".
 * Zwraca wartość do If-None-Match / If-Match.
 */
export function expectVersionEtag(response: APIResponse): string {
  const etag = response.headers()['etag']
  expect(etag, 'ETag musi być obecny').toBeTruthy()
  expect(etag, 'ETag musi zaczynać się od "v').toMatch(/^"v\d+/)
  return etag
}

/**
 * Sprawdza kompletny zestaw headerów dla payment response (GET/POST).
 */
export function expectSensitivePaymentHeaders(response: APIResponse): void {
  expectNoStore(response)
  expectVersionEtag(response)
  const vary = response.headers()['vary'] ?? ''
  expect(vary, 'Vary musi zawierać Authorization').toContain('Authorization')
  expectCorrelationId(response)
}

/**
 * Sprawdza Problem Details RFC 9457 body.
 * Waliduje status, typ i opcjonalnie error code.
 */
export async function expectProblemDetails(
  response: APIResponse,
  expectedStatus: number,
  opts?: { errorCode?: string; requiredHeader?: string }
): Promise<Record<string, unknown>> {
  expect(response.status()).toBe(expectedStatus)
  expect(
    response.headers()['content-type'],
    'Problem response musi mieć content-type application/problem+json'
  ).toContain('application/problem+json')
  
  const body = await response.json() as Record<string, unknown>
  expect(body['status']).toBe(expectedStatus)
  expect(body['type']).toMatch(/^https:\/\/api\.payment-quality\.local\/problems\//)
  expect(body['correlationId']).toMatch(/^[0-9a-f-]{36}$/)
  
  if (opts?.errorCode) {
    expect(String(body['error'])).toBe(opts.errorCode)
  }
  if (opts?.requiredHeader) {
    expect(body['requiredHeader']).toBe(opts.requiredHeader)
  }
  return body
}

/**
 * Sprawdza 304 Not Modified: status, ETag, brak body.
 */
export async function expectNotModified(
  response: APIResponse,
  expectedEtag: string
): Promise<void> {
  expect(response.status()).toBe(304)
  expect(response.headers()['etag']).toBe(expectedEtag)
  const text = await response.text()
  expect(text).toBe('')
}

/**
 * Sprawdza 429 z Retry-After.
 * Zwraca sekundy retry-after.
 */
export async function expectRateLimited(response: APIResponse): Promise<number> {
  expect(response.status()).toBe(429)
  const retryAfter = response.headers()['retry-after']
  expect(retryAfter, 'Retry-After musi być obecny przy 429').toBeTruthy()
  const seconds = parseInt(retryAfter, 10)
  expect(seconds, 'Retry-After musi być dodatnią liczbą całkowitą').toBeGreaterThan(0)
  return seconds
}

/**
 * Sprawdza Idempotency-Replayed header.
 */
export function expectIdempotencyReplayed(response: APIResponse, expected: boolean): void {
  const replayed = response.headers()['idempotency-replayed']
  expect(replayed, 'Idempotency-Replayed header musi być obecny').toBeTruthy()
  expect(replayed).toBe(expected ? 'true' : 'false')
}

/**
 * Sprawdza WWW-Authenticate przy 401.
 */
export function expectWwwAuthenticate(response: APIResponse): void {
  const www = response.headers()['www-authenticate']
  expect(www, 'WWW-Authenticate musi być obecny przy 401').toBeTruthy()
  expect(www).toMatch(/^Bearer/)
}

/**
 * Sprawdza że Authorization token nie wyciekł do response body lub headerów.
 */
export async function expectNoTokenLeak(response: APIResponse): Promise<void> {
  const responseHeaders = response.headers()
  expect(
    responseHeaders['authorization'],
    'Authorization nie może być zwracany jako response header'
  ).toBeUndefined()
  
  const body = await response.text()
  expect(body, 'Treść response nie może zawierać "Bearer "').not.toContain('Bearer ')
}
```

### 24.2 api-fixtures.ts

```typescript
import { test as base, request } from '@playwright/test'

type ApiFixtures = {
  platformApi: import('@playwright/test').APIRequestContext
}

/**
 * Fixture dla APIRequestContext z autoryzacją platform-operator.
 * Używa mocked session (domyślnie) lub prawdziwego Keycloak.
 */
export const test = base.extend<ApiFixtures>({
  platformApi: async ({ playwright }, use) => {
    const context = await playwright.request.newContext({
      baseURL: 'http://localhost:3000',
      extraHTTPHeaders: {
        'X-Correlation-ID': `test-${crypto.randomUUID()}`,
      },
    })
    await use(context)
    await context.dispose()
  },
})
```

---

## 25. UI/Network Assertion Helpers — Page Object Models

### 25.1 HeaderPanelObject

```typescript
// apps/frontend/tests/helpers/page-objects/HeaderPanel.ts
import { type Locator, type Page, expect } from '@playwright/test'

export class HeaderPanelObject {
  readonly panel: Locator

  constructor(page: Page) {
    this.panel = page.getByTestId('http-headers-panel')
  }

  async expectVisible() {
    await expect(this.panel).toBeVisible()
  }

  async expectHeader(name: string) {
    await expect(this.panel).toContainText(name)
  }

  async expectHeaderValue(name: string, value: string) {
    const row = this.panel.locator(`[data-header-name="${name.toLowerCase()}"]`)
    await expect(row).toContainText(value)
  }

  /** Kluczowy invariant bezpieczeństwa: Authorization zawsze zamaskowane */
  async expectAuthorizationMasked() {
    await expect(this.panel).toContainText('Bearer ••••••••')
    await expect(this.panel).not.toContainText('Bearer eyJ')
  }

  async expectCorrelationId() {
    const text = await this.panel.innerText()
    const match = text.match(/x-correlation-id[:\s]+([0-9a-f-]{36})/i)
    expect(match, 'X-Correlation-ID nie znaleziony w panelu headerów').not.toBeNull()
    return match![1]
  }

  async expectRetryAfter(): Promise<number> {
    const text = await this.panel.innerText()
    const match = text.match(/retry-after[:\s]+(\d+)/i)
    expect(match, 'Retry-After nie znaleziony w panelu headerów').not.toBeNull()
    return parseInt(match![1], 10)
  }
}
```

### 25.2 ProblemDetailsCardObject

```typescript
// apps/frontend/tests/helpers/page-objects/ProblemDetailsCard.ts
import { type Locator, type Page, expect } from '@playwright/test'

export class ProblemDetailsCardObject {
  readonly card: Locator

  constructor(page: Page) {
    this.card = page.getByTestId('problem-details-card')
  }

  async expectVisible() {
    await expect(this.card).toBeVisible()
  }

  async expectStatus(status: number) {
    await expect(this.card).toContainText(status.toString())
  }

  async expectDetail(text: string | RegExp) {
    const detail = this.card.locator('[data-field="detail"]')
    await expect(detail).toContainText(text)
  }

  async expectCorrelationId() {
    const el = this.card.getByTestId('correlation-id-value')
    await expect(el).toBeVisible()
    const text = await el.innerText()
    expect(text).toMatch(/^[0-9a-f-]{36}$/)
    return text
  }

  async expectFieldError(field: string, messagePart?: string) {
    const list = this.card.getByTestId('field-errors-list')
    await expect(list).toBeVisible()
    const item = list.locator(`[data-field="${field}"]`)
    await expect(item).toBeVisible()
    if (messagePart) {
      await expect(item).toContainText(messagePart)
    }
  }

  async expectRequiredHeader(headerName: string) {
    const el = this.card.getByTestId('required-header-value')
    await expect(el).toContainText(headerName)
  }

  async expectRetryable(retryAfterSeconds?: number) {
    const el = this.card.getByTestId('retryable-badge')
    await expect(el).toBeVisible()
    if (retryAfterSeconds !== undefined) {
      const retryEl = this.card.getByTestId('retry-after-seconds')
      await expect(retryEl).toContainText(retryAfterSeconds.toString())
    }
  }
}
```

### 25.3 NetworkAssertions

```typescript
// apps/frontend/tests/helpers/NetworkAssertions.ts
import { type Page, expect } from '@playwright/test'

export class NetworkAssertions {
  constructor(private page: Page) {}

  /**
   * Przechwytuje response i zwraca ETag.
   * Używaj z Promise.all aby uniknąć race condition.
   */
  async captureEtag(urlPattern: string | RegExp): Promise<string> {
    const response = await this.page.waitForResponse(
      r => (typeof urlPattern === 'string' ? r.url().includes(urlPattern) : urlPattern.test(r.url()))
        && r.status() < 400
    )
    const etag = response.headers()['etag']
    expect(etag, `ETag nie znaleziony dla ${urlPattern}`).toBeTruthy()
    return etag
  }

  /**
   * Przechwytuje response i zwraca X-Correlation-ID.
   */
  async captureCorrelationId(urlPattern: string | RegExp): Promise<string> {
    const response = await this.page.waitForResponse(
      r => (typeof urlPattern === 'string' ? r.url().includes(urlPattern) : urlPattern.test(r.url()))
    )
    const id = response.headers()['x-correlation-id']
    expect(id, 'X-Correlation-ID nie znaleziony').toBeTruthy()
    return id
  }

  /**
   * Zamiana waitForTimeout(n) — czeka na response zamiast na czas.
   */
  async waitForApiResponse(
    urlPattern: string | RegExp,
    trigger: () => Promise<void>
  ): Promise<import('@playwright/test').Response> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r =>
        typeof urlPattern === 'string' ? r.url().includes(urlPattern) : urlPattern.test(r.url())
      ),
      trigger(),
    ])
    return response
  }

  /**
   * Asercja brak tokenu w DOM (Playwright 1.61+: page.localStorage).
   */
  async expectNoTokenInStorage() {
    // localStorage — po upgrade do 1.61
    const localStorageEntries = await this.page.evaluate(() =>
      Object.entries(localStorage).map(([k, v]) => ({ k, v }))
    )
    for (const { k, v } of localStorageEntries) {
      expect(v, `localStorage["${k}"] nie powinien zawierać tokenu JWT`).not.toMatch(/^eyJ/)
    }

    const sessionStorageEntries = await this.page.evaluate(() =>
      Object.entries(sessionStorage).map(([k, v]) => ({ k, v }))
    )
    for (const { k, v } of sessionStorageEntries) {
      expect(v, `sessionStorage["${k}"] nie powinien zawierać tokenu JWT`).not.toMatch(/^eyJ/)
    }
  }
}
```

---

## 26. 20 obowiązkowych scenariuszy Playwright

### Scenariusz 1 — X-Correlation-ID: spójność UI ↔ sieć

```
Given: strona z listą merchant orders
When: kliknięcie "Refresh" / akcja wywołująca GET
Then: X-Correlation-ID widoczny w HeaderKeyValuePanel
And: ten sam UUID co w network response (waitForResponse)
```

### Scenariusz 2 — ETag: wyświetlanie wersji w EtagDisplay

```
Given: szczegóły payment order
When: strona załadowana (GET 200)
Then: EtagDisplay shows "v1" (lub wyższy)
And: network response ETag header = "v1"
```

### Scenariusz 3 — If-Match + 412 Precondition Failed (UI flow)

```
Given: payment order w stanie CREATED (ETag "v1")
When: PATCH/lifecycle z If-Match: "v0" (stary ETag)
Then: UI pokazuje ProblemDetailsCard
And: status = 412
And: correlationId visible in ProblemDetailsCard
```

### Scenariusz 4 — If-None-Match → 304 Not Modified (APIRequestContext)

```
Given: GET /payment-orders/{id} → ETag: "v1"
When: GET /payment-orders/{id} If-None-Match: "v1"
Then: status 304
And: body empty
And: ETag header present = "v1"
```

### Scenariusz 5 — Idempotency replay: 201 → 200 + Idempotency-Replayed

```
Given: POST create payment order z Idempotency-Key: uuid-A
When: POST drugi raz z tym samym Idempotency-Key: uuid-A
Then: pierwsza odpowiedź: 201, Idempotency-Replayed: false
And: druga odpowiedź: 200, Idempotency-Replayed: true
And: body obydwu odpowiedzi identyczny
```

### Scenariusz 6 — Idempotency conflict: 409 (nie 425)

```
Given: POST create payment order (in-flight via mock)
When: drugi POST z tym samym kluczem podczas pierwszego
Then: status 409 (CONFLICT, nie 425)
And: error = "create_in_progress"
And: correlationId in Problem Details body
```

### Scenariusz 7 — 429 Retry-After w UI i w sieci

```
Given: Error Lab strona
When: kliknięcie "Trigger 429"
Then: network response: status 429
And: Retry-After header > 0
And: ProblemDetailsCard shows status 429
And: Retry-After widoczny w HeaderKeyValuePanel
```

### Scenariusz 8 — 428 Precondition Required z requiredHeader

```
Given: Error Lab strona
When: kliknięcie "Trigger 428"
Then: status 428
And: Problem Details body.requiredHeader = "If-Match"
And: ProblemDetailsCard shows requiredHeader = "If-Match"
```

### Scenariusz 9 — 401 + WWW-Authenticate header

```
Given: Error Lab strona
When: kliknięcie "Trigger 401"
Then: status 401
And: WWW-Authenticate header: starts with "Bearer"
And: header widoczny w HeaderKeyValuePanel
```

### Scenariusz 10 — 400 z fieldErrors w ProblemDetailsCard

```
Given: formularz create payment order
When: submit z pustym polem amountMinor
Then: status 400
And: ProblemDetailsCard.field-errors-list visible
And: fieldError for "amountMinor" visible
```

### Scenariusz 11 — Authorization header nigdy nie wycieka do DOM

```
Given: zalogowany użytkownik
When: dowolna strona z HeaderKeyValuePanel
Then: panel zawiera "Bearer ••••••••"
And: panel NIE zawiera "Bearer eyJ"
And: localStorage NIE zawiera JWT (eyJ...)
And: sessionStorage NIE zawiera JWT
```

### Scenariusz 12 — Cache-Control: no-store na payment GET

```
Given: APIRequestContext z auth
When: GET /server/api/merchants/{id}/payment-orders/{orderId}
Then: status 200
And: Cache-Control header contains "no-store"
And: ETag header present
And: X-Correlation-ID header present
```

### Scenariusz 13 — Vary: Authorization + Idempotency-Key (POST create)

```
Given: APIRequestContext
When: POST create payment order
Then: status 201
And: Vary header contains "Authorization"
And: Vary header contains "Idempotency-Key"
And: Location header matches /\/api\/merchants\/.*\/payment-orders\/.*/
```

### Scenariusz 14 — 406 Not Acceptable: problem+json zawsze zwracany

```
Given: Error Lab
When: Trigger 406
Then: status 406
And: Content-Type = application/problem+json
And: body is valid Problem Details
And: error = "not_acceptable"
```

### Scenariusz 15 — 415 Unsupported Media Type z Accept-Patch

```
Given: Error Lab
When: Trigger 415
Then: status 415
And: Accept-Patch header = "application/merge-patch+json"
And: ProblemDetailsCard shows status 415
```

### Scenariusz 16 — 405 Method Not Allowed z Allow header

```
Given: APIRequestContext
When: DELETE /server/api/merchants/{id}  (metoda niedozwolona)
Then: status 405
And: Allow header present (np. "GET, POST")
And: problem body error = "method_not_allowed"
```

### Scenariusz 17 — HEAD Payment Order: ETag bez body

```
Given: APIRequestContext
When: HEAD /api/merchants/{id}/payment-orders/{orderId}
Then: status 200
And: ETag header present
And: Cache-Control: no-store
And: body empty (Content-Length może być 0 lub absent)
```

### Scenariusz 18 — Correlation-ID: przepływ błędu end-to-end

```
Given: Error Lab
When: dowolny trigger błędu (np. 400)
Then: sieć: X-Correlation-ID header w response
And: body.correlationId === header X-Correlation-ID
And: ProblemDetailsCard shows ten sam correlationId
(Wszystkie trzy źródła muszą być identyczne)
```

### Scenariusz 19 — Multi-merchant isolation (API level)

```
Given: merchant-A auth token, payment order należący do merchant-B
When: GET /api/merchants/{merchant-B-id}/payment-orders/{orderId}
Then: status 404 (nie 403 — ukrywamy istnienie zasobu)
And: problem body error = odpowiedni kod 404
```

### Scenariusz 20 — Content negotiation: full cycle Accept header

```
Given: APIRequestContext
When: GET /api/merchants/{id} z Accept: text/html
Then: status 406
And: Content-Type = application/problem+json
And: body.title = "Not Acceptable"
And: X-Correlation-ID present
```

---

## 27. Wpływ na multi-role setup

### Stan aktualny

Jeden projekt Playwright: `chromium` z `storageState: platform-operator.json`.

### Wymagane projekty dla pełnych testów (Phase 2)

```typescript
// playwright.config.ts — docelowe projekty
projects: [
  { name: 'auth-setup-platform-operator', testMatch: /auth\.setup\.ts/ },
  { name: 'auth-setup-merchant-admin', testMatch: /merchant-auth\.setup\.ts/ },
  {
    name: 'platform-operator',
    dependencies: ['auth-setup-platform-operator'],
    use: { storageState: 'tests/.auth/platform-operator.json' },
  },
  {
    name: 'merchant-admin',
    dependencies: ['auth-setup-merchant-admin'],
    use: { storageState: 'tests/.auth/merchant-admin.json' },
  },
]
```

**Wpływ na headery:** Test Scenariusza 19 wymaga min. 2 ról (platform-operator i merchant-admin) z różnymi `merchant_id` claims w JWT. Brak multi-role = nie da się w pełni przetestować cross-tenant isolation w Playwright (tylko via REST Assured bezpośrednio).

**Nie rób tego w MVP:** `fullyParallel: false` musi pozostać do czasu wdrożenia data isolation (niezależne dane per test).

---

## 28. Kompatybilność z REST Assured

### Istniejące `HeaderAssertions` — kompletne dla MVP

```java
assertCorrelationId()           ✅
assertVersionEtag()             ✅
assertNoStore()                 ✅
assertVaryContainsAuthorization() ✅
assertVaryContainsIfMatch()     ✅
assertVaryContainsIdempotencyKey() ✅
assertAcceptPatchMergePatchJson() ✅
assertWwwAuthenticatePresent()  ✅
assertRetryAfterIsValid()       ✅ (gotowe, brak backend impl)
assertAuthorizationTokenIsNotLeaked() ✅
assertLocationPointsToPaymentOrder() ✅
```

### Do dodania w REST Assured (Phase 2)

```java
assertIdempotencyReplayed(Response response, boolean expected)
assertNotModified(Response response, String expectedEtag)
assertRateLimitHeaders(Response response)   // RateLimit-Limit/Remaining/Reset
assertLastModifiedPresent(Response response)
```

### Strategia spójności Playwright ↔ REST Assured

| Asercja | REST Assured | Playwright |
|---------|-------------|-----------|
| Correlation ID UUID format | `assertCorrelationId()` | `expectCorrelationId()` |
| Cache-Control: no-store | `assertNoStore()` | `expectNoStore()` |
| ETag format "v\<n\>" | `assertVersionEtag()` | `expectVersionEtag()` |
| 304 Not Modified | nowa metoda | Scenariusz 4 |
| Problem Details structure | brak explicit | `expectProblemDetails()` |

---

## 29. Kompatybilność z OpenAPI / springdoc

### Stan aktualny

Jeśli springdoc jest skonfigurowany (niezweryfikowane), headery muszą być udokumentowane. Rekomendacja:

```java
// Dla każdego endpointu payment — dodaj @Header do @ApiResponse
@ApiResponse(responseCode = "200", headers = {
    @Header(name = "ETag", description = "Wersja zasobu"),
    @Header(name = "X-Correlation-ID", description = "UUID korelacji"),
    @Header(name = "Cache-Control", description = "Dyrektywa no-store"),
    @Header(name = "Vary", description = "Authorization, opcjonalnie If-Match lub Idempotency-Key"),
})
```

Dokumentowanie headerów w OpenAPI jest prerequisitem dla automatycznej walidacji kontraktu (contract testing Phase 3).

---

## 30. Kolejność implementacji

### Sprint 1 (MVP) — priorytety według zależności

```
1. [M6] backendApi.ts: dodaj WWW-Authenticate do forwardBackendHeaders
   ↓
2. [M7] api.ts + useApiClient: dodaj retryAfter, wwwAuthenticate, idempotencyReplayed
   ↓
3. [M8] Error Lab: trigger-429.post.ts
   ↓
4. [M1] Backend: implementacja rate limiter + Retry-After header w 429
      (lub uproszczenie: hardcoded 429 endpoint w backendzie dla lab)
   ↓
5. [M9] SecurityConfig: Access-Control-Expose-Headers
   ↓
6. [M3] Backend: If-None-Match → 304 w PaymentOrderController
   ↓
7. [M2] Backend: Idempotency-Replayed response header
   ↓
8. [M10] PaymentExceptionHandler: requiredHeader w 428 body
   ↓
9. [M4+M5] ProblemDetailsCard.vue: correlationId + fieldErrors + requiredHeader
   ↓
10. Playwright upgrade: 1.60.0 → 1.61.0
    ↓
11. Playwright helpers: api-assertions.ts, page-objects/
    ↓
12. Scenariusze 1-10 (podstawowe)
```

### Sprint 2 (Phase 2) — po MVP

```
1. Multi-role Playwright projects (2 role: platform-op + merchant-admin)
2. Scenariusze 11-20
3. Last-Modified + If-Modified-Since w backend
4. ProblemDetailsCard: retryable + retryAfterSeconds
5. RateLimit-* headers (wymaga rate limiter)
```

### Sprint 3 (Phase 3)

```
1. traceparent (W3C Trace Context via Otel)
2. Content-Disposition (po implementacji CSV export)
3. Server-Timing (dev profile only)
4. Multi-role: 5 projektów Playwright
5. Playwright: fullyParallel: true (po data isolation)
```

---

## 31. Czego NIE dodawać — lista odrzuconych

### Reject: headery ujawniające internals

| Header | Powód |
|--------|-------|
| `X-Tenant-ID` w response | Security risk: ujawnia wewnętrzne mapowanie tenant |
| `X-Merchant-ID` w response | Security risk: ujawnia merchant ID (jest w URL) |
| `X-User-ID` / `X-Actor-ID` | Security risk: ujawnia tożsamość użytkownika |
| `X-Role` / `X-Permissions` | Security risk: ujawnia uprawnienia aktora |
| Stack trace w Problem Details | Security risk: ujawnia wewnętrzną strukturę kodu |
| `X-Internal-*` headers | Wystawianie szczegółów infrastruktury |

### Reject: zduplikowane lub przestarzałe

| Header | Powód |
|--------|-------|
| `X-Request-ID` | Duplikat `X-Correlation-ID` (już istnieje) |
| `X-Response-Time` | Zastąpione przez standardowy `Server-Timing` |
| `X-B3-TraceId` / `X-B3-SpanId` | Zastąpione przez standardowy `traceparent` (W3C) |
| `Pragma: no-cache` | HTTP/1.0 legacy, `Cache-Control` wystarczy |
| `Content-MD5` | Deprecated RFC 7231 §3.3 |
| `Age` | Brak CDN layer, cache nigdy nie jest "aged" |
| `Surrogate-Control` | Brak CDN layer |

### Reject: nadmierna inżynieria

| Header / Pole | Powód |
|--------------|-------|
| `RateLimit-*` w Phase 1 | Wymaga rate limiter; brak wartości bez implementacji |
| Cursor pagination `Link` | Brak paginacji backendowej |
| `Deprecation` + `Sunset` | Brak wersjonowania API |
| `tracestate` + `baggage` | Wymaga pełnego distributed tracing |
| `Digest` (body checksum) | Overengineering, brak potrzeby weryfikacji integrity |
| `Range` / `Content-Range` | Brak large file downloads |
| `Preference-Applied` | Brak `Prefer` header w obecnym API |
| `X-Workflow-ID` | Brak engine workflow |
| `X-Retry-Count` | Wartość edukacyjna minimalna bez retry middleware |
| `X-Retryable` jako header | `retryable: boolean` w Problem Details body wystarczy |

### Reject: testy anty-wzorce

| Wzorzec | Powód |
|---------|-------|
| `waitForTimeout(n)` | Flaky, brak gwarancji; zamień na `waitForResponse` lub `expect.poll` |
| Testy oparte na kolorze CSS | Kruche, zależne od theme |
| `expect.soft.poll()` | NIE ISTNIEJE w Playwright (ani 1.60, ani 1.61) |
| `page.on('dialog')` dla `ConfirmActionModal` | ConfirmActionModal to DOM modal (UModal), nie native dialog |
| Over-mocking gdzie mock nic realnie nie testuje | Traci wartość integracyjną |
| Hardcoded UUIDs w testach (merchantId, orderId) | Zależy od kolejności seed data; użyj dynamic allocation |

---

## 32. Najważniejsze ryzyko security

### Risk #1 — Authorization token w response headers lub DOM

**Opis:** Jeśli ktokolwiek doda `Authorization` do listy `setExposedHeaders()` lub `forwardBackendHeaders()`, token JWT wycieknie do przeglądarki przez network response, a następnie potencjalnie do localStore, logów konsoli lub DOM.

**Istniejące zabezpieczenie:** `HeaderKeyValuePanel.vue` maskuje `Authorization` jako `Bearer ••••••••`. Ale to ochrona wyświetlania, nie ochrona przed wyciekiem w sieci.

**Rekomendacja:**
1. Playwright test (Scenariusz 11) musi asertować brak `eyJ` w localStorage i sessionStorage
2. `assertAuthorizationTokenIsNotLeaked()` w REST Assured musi być uruchamiana przy każdym nowym endpoincie
3. Eksplicitnie NIGDY nie dodawaj `Authorization` do `Access-Control-Expose-Headers`

### Risk #2 — X-Tenant-ID / X-Merchant-ID jako response headers

**Opis:** Naturalna pokusa dodania tenant context headers "dla wygody debugowania" może ujawnić informacje o separacji tenantów. Atakujący może skanować headery żeby odkryć tenant mapping.

**Rekomendacja:** REJECT wszystkich response headers ujawniających tenant/merchant identity (sekcja 31).

### Risk #3 — Problem Details stack trace

**Opis:** W trybie development Spring Boot może dołączyć stack trace do error response. Jeśli trafi do frontendu i będzie wyświetlony w `RawJsonViewer`, ujawnia wewnętrzną strukturę kodu.

**Rekomendacja:** `GlobalExceptionHandler` nie powinien nigdy przekazywać `exception` ani `trace` do body. Zweryfikować profile `test`/`dev`/`prod`.

---

## 33. Najważniejsze ryzyko overengineeringu

### Risk #1 — RateLimit-* headers bez rate limitera

**Opis:** Dodanie `RateLimit-Limit`, `RateLimit-Remaining`, `RateLimit-Reset` do `ApiHeaders` TypeScript, `backendApi.ts` i `ProblemDetailsCard` bez wcześniejszej implementacji rate limitera w backendzie tworzy martwy kod. Testy dla tych headerów zawsze będą zielone (header absent = asercja `undefined`), co daje fałszywe poczucie bezpieczeństwa.

**Rekomendacja:** Phase 2 wyłącznie, po Bucket4j lub Spring Rate Limiter.

### Risk #2 — 5 ról Playwright przed data isolation

**Opis:** Uruchomienie 5 projektów Playwright (5 ról) przy `fullyParallel: false` i wspólnych danych seed sprawi, że testy będą kolidować. Merchant-admin z TENANT_ALPHA będzie widział payment orders TENANT_BETA jeśli dane seed nie są izolowane per-test.

**Rekomendacja:** Data isolation strategy (unique merchant per test lub testcontainers z reset) musi poprzedzać multi-role setup.

### Risk #3 — `traceparent` bez Otel

**Opis:** Dodanie `traceparent` header do `ApiHeaders` i testów Playwright bez wdrożenia Micrometer Tracing / OpenTelemetry w backendzie oznacza, że header nigdy nie pojawi się w response.

**Rekomendacja:** `traceparent` — Phase 2 lub 3, po Otel setup w backendzie.

### Risk #4 — Server-Timing w produkcji

**Opis:** `Server-Timing: db;dur=12ms, auth;dur=3ms` ujawnia informacje o wewnętrznym podziale czasu — potencjalnie pomocne przy timing attacks. Bezpieczny tylko w dev/lab profile.

**Rekomendacja:** `Server-Timing` wyłącznie za flagą `@ConditionalOnProperty` lub `@Profile("dev")`. Nigdy w `prod` profile.

---

## 34. Rekomendacja końcowa

### Top 10 headers/properties do dodania

Priorytetyzowane według value/cost:

| # | Header / Pole | Priorytet | Koszt | Wartość edukacyjna |
|---|--------------|-----------|-------|-------------------|
| 1 | `Idempotency-Replayed` response header | **MVP** | Niski | Unikalna edukacja replay vs new |
| 2 | `WWW-Authenticate` w proxy forward | **MVP** | Minimalny | Kompletuje 401 flow |
| 3 | `If-None-Match` → 304 Not Modified | **MVP** | Niski | Kluczowy pattern conditional GET |
| 4 | `Retry-After` (header + proxy + ApiHeaders) | **MVP** | Średni | Wymagany dla 429 Error Lab |
| 5 | `correlationId` w ProblemDetailsCard | **MVP** | Minimalny | Widoczność korelacji błędów |
| 6 | `fieldErrors` w ProblemDetailsCard | **MVP** | Minimalny | Widoczność walidacji |
| 7 | `requiredHeader` w 428 body | **MVP** | Minimalny | Samodokumentujące błędy |
| 8 | `Access-Control-Expose-Headers` | **MVP** | Niski | Umożliwia Playwright network assertions |
| 9 | `Last-Modified` w GET payment order | Phase 2 | Niski | Uzupełnia cache story |
| 10 | `RateLimit-*` headers | Phase 2 | Wysoki | Wymaga rate limiter |

### Top 5 Playwright scenariuszy (najwyższy priorytet)

1. **Scenariusz 11** — Authorization token nigdy nie wycieka (security invariant)
2. **Scenariusz 18** — Correlation-ID spójny: sieć == body == UI (end-to-end observable)
3. **Scenariusz 4** — 304 Not Modified via APIRequestContext (conditional GET)
4. **Scenariusz 5** — Idempotency replay: 201 → 200 + Idempotency-Replayed header
5. **Scenariusz 7** — 429 + Retry-After: sieć + UI ProblemDetailsCard

### Top 5 rzeczy, których NIE dodawać

1. `X-Tenant-ID` / `X-Merchant-ID` w response — security risk
2. Stack trace w Problem Details — security risk
3. `RateLimit-*` headers bez rate limitera — martwy kod
4. Multi-role Playwright (5 projektów) bez data isolation — kolidujące testy
5. `traceparent` bez Otel infrastructure — header nigdy nie pojawi się w response

### Najważniejsze ryzyko security

**Authorization token leak** — jedyna luka, która ma natychmiastowe konsekwencje. Playwright Scenariusz 11 musi być uruchamiany w każdym CI pipeline jako gate.

### Najważniejsze ryzyko overengineeringu

**`RateLimit-*` headers bez implementacji rate limitera** — tworzy martwy kod po obu stronach stosu (backend, proxy, TypeScript types, Vue components) i daje fałszywe testy zielone.

---

*Dokument wygenerowany na podstawie analizy kodu źródłowego repozytorium i specyfikacji Playwright 1.61. Wszystkie decyzje oparte na weryfikacji aktualnego kodu — nie na założeniach z roadmapu.*
