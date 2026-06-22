# PaymentOrderRestAssuredTest — Cheatsheet

Plik: `PaymentOrderRestAssuredTest.java` — 10 testów kontraktowych API.
Cel: udowodnić, że klient HTTP dostaje poprawny kontrakt dla każdego scenariusza create/read.

## Typy testów

### 1. Kontrakt sukcesu — Create 201

| Aspekt | Co sprawdza |
|---|---|
| **HTTP** | `POST` → `201 Created` |
| **REST** | `Location` wskazuje nowy zasób |
| **Headers** | `ETag` (wersja), `X-Correlation-ID` (echo) |
| **RA** | `.statusCode(201)`, `.header()`, `.body()`, Hamcrest `notNullValue`, `equalTo`, `startsWith`, `containsString` |
| **Cel** | Dowód: szczęśliwa ścieżka create zwraca kompletny kontrakt |

### 2. Idempotentność — Replay 200

| Aspekt | Co sprawdza |
|---|---|
| **HTTP** | Drugi `POST` z tym samym `Idempotency-Key` + tym samym body → `200 OK` |
| **RA** | `.extract().path("paymentOrderId")` → `.body("paymentOrderId", equalTo(firstId))` |
| **Cel** | Dowód: retry nie tworzy duplikatu — ten sam ID wraca |

### 3. Idempotentność — Conflict 409

| Aspekt | Co sprawdza |
|---|---|
| **HTTP** | Ten sam key + **inne** body → `409 Conflict` |
| **RA** | `.body("error", equalTo("idempotency_conflict"))` |
| **Cel** | Dowód: nie można podmienić intencji biznesowej pod tym samym key |

### 4. Walidacja — 400 (invalid amount)

| Aspekt | Co sprawdza |
|---|---|
| **HTTP** | Amount=0 → `400 validation` |
| **RA** | `.body("error", equalTo("validation"))` |
| **Cel** | Dowód: reguły domenowe są egzekwowane na poziomie HTTP |

### 5. Walidacja — 400 (brak headera)

| Aspekt | Co sprawdza |
|---|---|
| **HTTP** | Brak `Idempotency-Key` → `400` |
| **RA** | `.statusCode(400)` |
| **Cel** | Dowód: wymagany header jest egzekwowany |

### 6. Biznesowa — 409 (merchant nieaktywny)

| Aspekt | Co sprawdza |
|---|---|
| **HTTP** | Nieaktywny merchant → `409 merchant_not_payment_eligible` |
| **RA** | `.body("error", equalTo("merchant_not_payment_eligible"))` |
| **Cel** | Dowód: reguła biznesowa blokuje create dla DRAFT/SUSPENDED |

### 7. Odczyt — 200 + ETag

| Aspekt | Co sprawdza |
|---|---|
| **HTTP** | `GET` → `200 OK` |
| **REST** | Resource access przez reader token (nie creator) |
| **Headers** | `ETag` zwracany na read |
| **RA** | `.header("ETag", startsWith(...))`, flow create → `.extract()` → read → `.body()` |
| **Cel** | Dowód: odczyt działa, role oddzielone (creator ≠ reader) |

### 8. Tenant isolation — Masked 404

| Aspekt | Co sprawdza |
|---|---|
| **HTTP** | Reader merchanta B czyta order A → `404 not_found` |
| **REST** | Resource existence masked przed innym tenantem |
| **RA** | `.body("error", equalTo("not_found"))` |
| **Cel** | Dowód: BOLA/IDOR prevented — merchant B nie wie, że order A istnieje |

### 9. Platform read — Cross-merchant 200

| Aspekt | Co sprawdza |
|---|---|
| **HTTP** | `platform:payments:read` → `200 OK` dla dowolnego merchanta |
| **RA** | `platformPaymentReaderToken()` → `.statusCode(200)` |
| **Cel** | Dowód: platform reader ma cross-merchant dostęp — support scenario |

### 10. Edge case — Platform reader + zły merchant path → 404

| Aspekt | Co sprawdza |
|---|---|
| **HTTP** | Platform reader podaje `merchantId` merchanta B, ale order należy do A → `404` |
| **RA** | `.statusCode(404)`, `.body("error", equalTo("not_found"))` |
| **Cel** | Dowód: nawet platform reader nie widzi ordera przez ścieżkę innego merchanta |

## REST Assured — użyte metody (jednym rzutem)

```
.contentType(JSON)  .header("Idempotency-Key")  .header("X-Correlation-ID")
.body(Map)          .post(path, pathParams)      .get(path, pathParams)
.statusCode(int)    .header("Location")          .header("ETag")
.header("X-Correl...")  .body("jsonpath", matcher)
.extract().path("field")
```

## Hamcrest Matchers — użyte

| Matcher | Przykład |
|---|---|
| `equalTo(value)` | `.body("status", equalTo("CREATED"))` |
| `notNullValue()` | `.body("paymentOrderId", notNullValue())` |
| `startsWith(prefix)` | `.header("ETag", startsWith("\"po-"))` |
| `containsString(sub)` | `.header("Location", containsString("/payment-orders/"))` |

## Status codes — szybka mapa

| Status | Testy | Znaczenie |
|---|---|---|
| `201` | #1, #2, #3, #6, #7, #8, #9, #10 | Zasób utworzony |
| `200` | #2, #7, #9 | OK — replay lub read |
| `400` | #4, #5 | Walidacja odrzucona |
| `404` | #8, #10 | Nie znaleziono / masked |
| `409` | #3, #6 | Konflikt idempotency / merchant eligibility |
