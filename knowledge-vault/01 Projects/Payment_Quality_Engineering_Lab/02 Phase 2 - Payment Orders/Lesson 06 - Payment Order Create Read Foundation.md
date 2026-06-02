 
# Lesson 06 - Payment Order Create Read Foundation

> **Status:** READY — code implemented, tests passing, lesson note complete
>
> **Navigation:** [[START HERE - Learning Dashboard]] | [[Current Lesson]] | [[Current Sprint]] | [[Curriculum Backbone]]
>
> **What to do NOW:** Study §2-§6 first, then practice REST Assured exercises in §11, answer questions in §13.
>
> **What NOT to touch yet:** Authorize/capture/cancel lifecycle, Kafka, GraphQL, gRPC, PSP integration. See [[Current Lesson#DEFERRED]] for full list.

## 1. Cel Lekcji

Lekcja 6 przechodzi z nauki syntaktycznej REST Assured do nauki przez realny vertical slice: `Payment Order Create/Read`.

Celem jest zrozumienie nowej funkcjonalności od kodu produkcyjnego, przez SQL/Flyway i security, aż po testowanie REST API w REST Assured.

Ta lekcja jest kontynuacją Learning OS, nie osobnym dokumentem. Nie powtarza szeroko Lessons 1-5. Zakłada, że znasz już podstawy `given()`, `when()`, `then()`, path params, headers, JSON body i podstawowe response assertions.

## 2. Co Zbudowaliśmy

Aktorzy:

- `merchant:payments:create` - tworzy payment order tylko dla swojego `merchant_id`.
- `merchant:payments:read` - czyta payment order tylko w swoim merchant scope.
- `merchant:payments:operate` - istnieje jako rola przyszłościowa, ale nie daje create/read w tym slice.
- `platform:payments:read` - czyta payment orders w endpointach merchant-scoped, ale nadal w kontekście path `merchantId`.

Główne flow:

- Merchant user tworzy payment order przez `POST /api/merchants/{merchantId}/payment-orders`.
- Backend waliduje role, `merchant_id`, merchant eligibility, body i `Idempotency-Key`.
- System zapisuje `payment_orders`, `idempotency_records`, `payment_order_status_history`.
- Retry z tym samym `Idempotency-Key` i tym samym fingerprint zwraca `200 OK` z tym samym `paymentOrderId`.
- Retry z tym samym key, ale innym body zwraca `409 idempotency_conflict`.
- Merchant reader czyta order przez `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`.
- Cross-tenant merchant read zwraca masked `404 not_found`.

Backend module:

- `apps/backend/src/main/java/lab/paymentquality/payment/`
- Spring Modulith module boundary dla payment.
- Payment używa publicznego merchant API: `lab.paymentquality.merchant.MerchantPaymentEligibilityService`.

Frontend flow:

- `CreatePaymentOrderForm.vue` wysyła payment order przez Nuxt server proxy.
- Proxy przekazuje `Authorization` i `Idempotency-Key` do backendu.
- Detail page pokazuje read model payment order.

Baza danych:

- `payment_orders`
- `idempotency_records`
- `payment_order_status_history`

Testy:

- REST contract: `PaymentOrderRestAssuredTest`
- Security matrix: `PaymentOrderSecurityTest`
- Service/idempotency: `PaymentOrderServiceTest`, `PaymentOrderIdempotencyConcurrencyTest`
- Repository/Flyway/constraints: `JpaPaymentOrderRepositoryTest`
- Modulith boundary: `PaymentModuleTest`

## 3. Learning Delta Względem Lessons 1-5

Nie powtarzamy szeroko:

- czym jest REST Assured,
- podstawowego `given()`, `when()`, `then()`,
- podstaw path params,
- podstaw headers,
- podstaw request body i JSON,
- prostych response assertions z merchant flow.

Nowe centrum Lekcji 6:

- idempotent create,
- `Idempotency-Key`,
- request fingerprint,
- tenant isolation,
- `merchant_id` claim,
- role matrix dla payment,
- `Location`, `ETag`, `X-Correlation-ID`,
- DB constraints jako część kontraktu jakości,
- Flyway migration dla payment tables,
- frontend jako realny API consumer,
- REST Assured jako narzędzie do ochrony contractu biznesowego.

## 4. Mapa Kodu

### Backend Domain

Pliki:

- `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentAmount.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/CurrencyCode.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/ClientOrderReference.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/IdempotencyKey.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/RequestFingerprint.java`

Po co istnieje:

- Zamienia primitive values na reguły domenowe.
- Uczy, że walidacja nie powinna być rozproszona po controllerze, serwisie i testach.

Czytaj najpierw:

1. `PaymentAmount`
2. `CurrencyCode`
3. `IdempotencyKey`
4. `RequestFingerprint`

Pytanie QA:

- Czy walidacja domenowa pokrywa te same reguły, które widzi klient API?

### Backend Entities

Pliki:

- `PaymentOrder.java`
- `IdempotencyRecord.java`
- `PaymentOrderStatusHistory.java`

Po co istnieje:

- Mapują zachowanie create/read/idempotency/audit na PostgreSQL.

Czytaj najpierw:

1. `PaymentOrder.create(...)`
2. `IdempotencyRecord.reserve(...)`
3. `PaymentOrderStatusHistory.creationEntry(...)`

Pytanie QA:

- Które reguły są wymuszone w Java, a które w DB?

### Application Service

Plik:

- `PaymentOrderService.java`

Po co istnieje:

- Zawiera transaction boundary i business flow create/read.

Czytaj najpierw:

1. `create(...)`
2. `resolveExistingIdempotencyRecord(...)`
3. `findForMerchant(...)`
4. `findForPlatform(...)`

Pytanie QA:

- Czy retry i race condition kończą się deterministycznym wynikiem?

### Web/API

Pliki:

- `PaymentOrderController.java`
- `CreatePaymentOrderRequest.java`
- `PaymentOrderResponse.java`
- `PaymentExceptionHandler.java`
- `PaymentErrorResponse.java`

Po co istnieje:

- Mapują HTTP na domenę i domenę na HTTP.

Czytaj najpierw:

1. `createPaymentOrder(...)`
2. `getPaymentOrder(...)`
3. exception handlers dla `validation`, `forbidden`, `not_found`, `idempotency_conflict`, `merchant_not_payment_eligible`.

Pytanie QA:

- Czy status codes, headers i error codes są zgodne z contractem?

### Infrastructure

Pliki:

- `JpaPaymentOrderRepository.java`
- `JpaIdempotencyRecordRepository.java`
- `JpaPaymentOrderStatusHistoryRepository.java`

Po co istnieje:

- Obsługują persistence i specjalne SQL dla idempotency reservation.

Czytaj najpierw:

- `JpaIdempotencyRecordRepository.reserveIfAbsent(...)`

Pytanie QA:

- Czy repozytorium korzysta z DB constraint jako mechanizmu jakości, a nie tylko jako awaryjnego błędu?

### Public Merchant Boundary

Pliki:

- `apps/backend/src/main/java/lab/paymentquality/merchant/MerchantPaymentEligibility.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/MerchantPaymentEligibilityService.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantPaymentEligibilityAdapter.java`

Po co istnieje:

- Payment może pytać merchant module o eligibility bez importowania `merchant.internal`.

Pytanie QA:

- Czy Spring Modulith boundary chroni architekturę przed skrótem przez internal package?

### Security

Pliki:

- `SecurityConfig.java`
- `KeycloakRealmRoleConverter.java`
- `TestJwtSupport.java`
- `infra/keycloak/realms/payment-quality-realm.json`

Po co istnieje:

- Role i JWT claims są mapowane na Spring authorities.

Pytanie QA:

- Czy role authorization i tenant ownership są testowane osobno?

### Flyway

Plik:

- `apps/backend/src/main/resources/db/migration/payment/V2__create_payment_orders.sql`

Po co istnieje:

- Utrwala DB contract.

Pytanie QA:

- Czy constraints są testowalne i pokrywają realne ryzyka?

### REST Assured

Pliki:

- `PaymentOrderRestAssuredTest.java`
- `PaymentOrderSecurityTest.java`
- `PaymentApiTestSupport.java`

Po co istnieje:

- Sprawdzają API przez prawdziwy HTTP.

Pytanie QA:

- Czy test chroni kontrakt API, czy tylko status code?

### Frontend

Pliki:

- `CreatePaymentOrderForm.vue`
- `PaymentOrderDetail.vue`
- `server/api/merchants/[merchantId]/payment-orders/index.post.ts`
- `server/api/merchants/[merchantId]/payment-orders/[paymentOrderId].get.ts`
- `payment-order.schema.ts`
- `payment-orders.ts`

Po co istnieje:

- UI jest klientem API i pokazuje, jakie założenia kontraktu musi spełnić realny consumer.

Pytanie QA:

- Czy frontend zachowuje stabilny `Idempotency-Key` dla retry?

## 5. Architecture Walkthrough

Payment module używa publicznego merchant API, bo merchant eligibility jest wiedzą merchant module, ale payment nie powinien znać wewnętrznych encji merchant. To jest podstawowa zasada modular monolith: zależność przez publiczny boundary, nie przez `merchant.internal`.

Payment nie importuje `merchant.internal`, bo wtedy złamałby Spring Modulith boundary. Test architektury ma wykrywać takie skróty zanim staną się długiem architektonicznym.

Granica modułu:

- Publiczne API merchant: `lab.paymentquality.merchant.*`
- Internal payment: `lab.paymentquality.payment.internal.*`
- Payment web API: `/api/merchants/{merchantId}/payment-orders`

Transaction boundary:

- `PaymentOrderService` jest oznaczony `@Transactional`.
- Create flow waliduje merchant eligibility, fingerprint, idempotency, zapis order, zapis history, complete idempotency record.
- Read flow jest `@Transactional(readOnly = true)`.

Idempotency wpływa na architekturę, bo `POST` przestaje być zwykłym create. Musi stać się retry-safe operation, gdzie DB unique constraint na `(merchant_id, idempotency_key_hash)` jest częścią modelu biznesowego.

Tenant isolation:

- Create wymaga role `merchant:payments:create` oraz claim `merchant_id == path merchantId`.
- Merchant read wymaga role `merchant:payments:read` oraz matching `merchant_id`.
- Cross-tenant read maskuje zasób przez `404`.
- Platform read używa `platform:payments:read`, ale endpoint nadal jest merchant-nested.

Ryzyka:

- Idempotency race condition.
- Różnica między `403` i masked `404`.
- Drift między Spring Security authority mapping a ręcznym parsowaniem JWT.
- Frontend retry z nowym idempotency key.
- DB constraints niepokryte testami.
- Nieświadome rozszerzenie scope w lifecycle actions.

## 6. HTTP I REST API

### Create Payment Order

Endpoint:

```http
POST /api/merchants/{merchantId}/payment-orders
Authorization: Bearer <token>
Content-Type: application/json
Idempotency-Key: idem-lesson6-001
X-Correlation-ID: lesson6-corr-001
```

Request:

```json
{
  "amountMinor": 12500,
  "currency": "PLN",
  "clientOrderReference": "PAY-LESSON6-001"
}
```

Success first create:

```http
HTTP/1.1 201 Created
Location: /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
ETag: "po-{paymentOrderId}-v0"
X-Correlation-ID: lesson6-corr-001
```

Replay:

```http
HTTP/1.1 200 OK
ETag: "po-{paymentOrderId}-v0"
X-Correlation-ID: lesson6-corr-001
```

### Read Payment Order

Endpoint:

```http
GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
Authorization: Bearer <token>
```

Success read:

```http
HTTP/1.1 200 OK
ETag: "po-{paymentOrderId}-v0"
X-Correlation-ID: <correlation-id>
```

Status codes:

| Status | Meaning |
|---:|---|
| `201 Created` | pierwszy successful create |
| `200 OK` | idempotent replay albo read |
| `400 validation` | invalid body, invalid UUID, invalid/missing idempotency key |
| `401` | missing/invalid/expired token |
| `403 forbidden` | authenticated, ale brak roli albo create scope mismatch |
| `404 not_found` | payment order nie istnieje albo merchant cross-tenant read |
| `409 idempotency_conflict` | ten sam key, inny fingerprint |
| `409 merchant_not_payment_eligible` | merchant nie istnieje albo nie jest aktywny dla payments |

Curl create:

```bash
curl -i -X POST "http://localhost:8080/api/merchants/$MERCHANT_ID/payment-orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: idem-lesson6-001" \
  -H "X-Correlation-ID: lesson6-corr-001" \
  -d '{"amountMinor":12500,"currency":"PLN","clientOrderReference":"PAY-LESSON6-001"}'
```

Curl read:

```bash
curl -i "http://localhost:8080/api/merchants/$MERCHANT_ID/payment-orders/$PAYMENT_ORDER_ID" \
  -H "Authorization: Bearer $TOKEN"
```

## 7. JDK 25 I Java Code Reading

| Element | Co testować | Jakie ryzyko redukuje | Jak czytać jako QA Automation Engineer |
|---|---|---|---|
| `PaymentAmount` | `1`, `100000000`, `0`, negative, over max | amount poza zakresem | value object pokazuje legalny zakres inputu |
| `CurrencyCode` | `PLN`, `EUR`, `USD`, lowercase, `GBP` | niespójna waluta | walidacja string API value |
| `ClientOrderReference` | blank, length, valid ref | nieczytelne referencje klienta | oddziel syntaktykę od uniqueness |
| `IdempotencyKey` | blank, oversized, non-printable, valid | retry safety zależy od key | raw key nie jest trzymany, backend używa hash |
| `RequestFingerprint` | same body same hash, changed fields different hash | replay innej operacji | fingerprint jest oracle dla idempotency |
| `PaymentOrder` | initial status `CREATED`, timestamps, amount, currency | zły stan startowy | status lifecycle jest minimalny |
| `IdempotencyRecord` | reserve, complete, uniqueness | duplicate orders przy retry/race | DB constraint i service logic współpracują |
| `PaymentOrderStatusHistory` | `from_status = null`, `to_status = CREATED` | brak audit trail | historia jest dowodem create |
| `PaymentOrderService` | active/inactive merchant, replay, conflict, read | business logic drift | najważniejszy plik flow |
| `PaymentOrderController` | status codes, headers, tenant ownership | role bez ownership | granica HTTP/security |
| `PaymentExceptionHandler` | stable error codes | klient nie rozpoznaje błędu | error contract jest częścią API |

## 8. SQL, PostgreSQL I Flyway

`payment_orders`:

- `payment_order_id UUID PRIMARY KEY`
- `merchant_id UUID NOT NULL`
- FK do `merchants`
- `amount_minor BIGINT`
- `currency VARCHAR(3)`
- `status VARCHAR(20)`
- `version BIGINT`

DB rules:

- `amount_minor BETWEEN 1 AND 100000000`
- `currency IN ('PLN', 'EUR', 'USD')`
- `status IN ('CREATED')`

`idempotency_records`:

- Unique `(merchant_id, idempotency_key_hash)`
- Unique `payment_order_id`
- Hash format check: 64 hex chars
- FK do merchant i payment order

`payment_order_status_history`:

- FK do payment order
- `from_status` nullable for creation
- `to_status IN ('CREATED')`
- actor subject i correlation id jako audit context

Reguły według warstwy:

- Java domain: szybka walidacja wejścia i czytelne exceptions.
- REST/security: role, tenant scope, HTTP status code.
- DB constraints: ostatnia linia obrony dla integralności danych.
- Tests: dokumentują, która warstwa odpowiada za które ryzyko.

Ćwiczenia SQL:

> Guided beginner lesson: [[Lesson 06D - SQL and Flyway Constraints for Payment Orders]] in `PostgreSQL and SQL From Zero`.

```sql
INSERT INTO payment_orders (
  payment_order_id, merchant_id, client_order_reference, amount_minor, currency, status
) VALUES (
  gen_random_uuid(), '<merchant-uuid>', 'PAY-SQL-001', 12500, 'PLN', 'CREATED'
);
```

```sql
INSERT INTO payment_orders (
  payment_order_id, merchant_id, client_order_reference, amount_minor, currency, status
) VALUES (
  gen_random_uuid(), '<merchant-uuid>', 'PAY-SQL-002', 12500, 'GBP', 'CREATED'
);
```

```sql
INSERT INTO payment_orders (
  payment_order_id, merchant_id, client_order_reference, amount_minor, currency, status
) VALUES (
  gen_random_uuid(), '<merchant-uuid>', 'PAY-SQL-003', 0, 'PLN', 'CREATED'
);
```

```sql
SELECT payment_order_id, amount_minor, currency, status, created_at
FROM payment_orders
WHERE merchant_id = '<merchant-uuid>'
ORDER BY created_at DESC;
```

```sql
SELECT payment_order_id, from_status, to_status, actor_subject, correlation_id, created_at
FROM payment_order_status_history
WHERE payment_order_id = '<payment-order-uuid>'
ORDER BY created_at ASC;
```

## 9. Security I Tenant Isolation

Security matrix:

| Actor | Create Own | Create Other | Read Own | Read Other | Platform Read |
|---|---:|---:|---:|---:|---:|
| Unauthenticated | `401` | `401` | `401` | `401` | `401` |
| Denied identity | `403` | `403` | `403` | `403` | `403` |
| `merchant:payments:create` | `201/200` | `403` | `403` | `403/404` | `403` |
| `merchant:payments:read` | `403` | `403` | `200` | `404` | `403` |
| `merchant:payments:operate` | `403` | `403` | `403` | `403` | `403` |
| `platform:payments:read` | `403` | `403` | `200` | `200` in correct nested context | `200` |

Różnica:

- Role authorization odpowiada na pytanie: czy user ma prawo do typu operacji?
- Tenant ownership odpowiada na pytanie: czy user ma prawo do tego konkretnego merchant resource?

Dlaczego cross-tenant read daje `404`:

- Żeby nie ujawniać, czy `paymentOrderId` istnieje u innego merchanta.

Dlaczego create scope mismatch daje `403`:

- User jest authenticated i ma potencjalnie rolę create, ale nie ma prawa tworzyć dla tego path merchant.

Keycloak:

- Realm dodaje payment roles.
- `merchant_id` mapper wystawia claim.
- Testy używają `TestJwtSupport`, żeby deterministycznie tworzyć tokeny z rolami i merchant scope.

Pytania kontrolne:

- Czy samo `merchant:payments:create` wystarczy do create?
- Czy `merchant_id` claim jest wymagany dla merchant create/read?
- Czy platform reader powinien tworzyć payment orders?
- Czy merchant reader może odczytać order innego merchanta?
- Czy UI hiding akcji wystarcza jako security?

## 10. Idempotency Deep Dive

`Idempotency-Key` chroni klienta przed skutkiem retry. Jeśli klient wysłał request, ale zgubił response, może powtórzyć request z tym samym key.

Request fingerprint:

- `merchantId`
- `amountMinor`
- `currency`
- `clientOrderReference`
- bez `Authorization`
- bez `X-Correlation-ID`
- bez timestampów
- bez volatile headers

Replay:

- Same merchant, same key, same fingerprint.
- Backend zwraca `200 OK`.
- Body zawiera ten sam `paymentOrderId`.

Conflict:

- Same merchant, same key, different fingerprint.
- Backend zwraca `409 idempotency_conflict`.

Retry po timeout jest trudny, bo klient nie wie, czy backend zdążył zapisać order. Dlatego frontend nie może generować nowego key przy każdym retry. Musi utrzymać stabilny key dla jednej próby biznesowej.

Decision table:

| Scenario | Expected |
|---|---|
| first request | `201 Created`, new order |
| same key + same body | `200 OK`, same order |
| same key + different body | `409 idempotency_conflict` |
| different key + same body | new operation, possible new order |
| same key + different merchant | separate idempotency scope |

Race conditions:

- Two requests same key at the same time.
- One should win reservation.
- Loser should resolve existing record or fail deterministically.
- DB unique constraint is part of the design, not just an accident.

## 11. REST Assured Learning Path

`PaymentOrderRestAssuredTest` jest głównym materiałem Lekcji 6.

Co ćwiczymy:

- Arrange: create active merchant, create token, build request body, generate idempotency key.
- Act: `post(...)` albo `get(...)`.
- Assert: status, headers, body, error code.
- Extraction: `extract().path("paymentOrderId")`.
- Reuse: same idempotency key in replay test.
- Security: separate `PaymentOrderSecurityTest`.
- Parallel-safe data: unique merchant references and unique idempotency keys.

Ćwiczenia:

| # | Test | Cel | Ryzyko | Assertions | Expected |
|---:|---|---|---|---|---|
| 1 | `createPaymentOrderReturns201WithHeaders` | pierwszy create | brak contract headers | `201`, `Location`, `ETag`, `X-Correlation-ID`, body fields | `201` |
| 2 | `idempotentReplayReturns200WithSameId` | replay | duplicate order on retry | first `201`, second `200`, same ID | `200` |
| 3 | `idempotencyConflictReturns409` | conflict | same key different intent | `409`, `error=idempotency_conflict` | `409` |
| 4 | `crossTenantReadReturns404` | tenant masking | BOLA/data leak | `404`, `error=not_found` | `404` |
| 5 | `platformReaderCanReadCrossMerchant` | platform read | role matrix drift | `200`, same payment ID | `200` |
| 6 | missing `Idempotency-Key` | required header | unsafe create | `400` | `400` |
| 7 | invalid currency | validation | invalid money data | `400`, `error=validation` | `400` |
| 8 | inactive merchant | eligibility | orders for inactive merchant | `409`, `merchant_not_payment_eligible` | `409` |
| 9 | merchant scope mismatch create | ownership | cross-merchant create | `403`, `forbidden` | `403` |
| 10 | platform read wrong nested merchant path | route semantics | path/body contradiction | `404`, `not_found` | `404` |

Hint:

- Nie zaczynaj od helperów.
- Najpierw napisz test jawnie.
- Potem dopiero wyciągaj wspólne operacje do `PaymentApiTestSupport`.

## 12. Frontend Jako API Consumer

Nuxt server proxy:

- `server/api/merchants/[merchantId]/payment-orders/index.post.ts`
- Forwarduje request do backendu.
- Dodaje `Authorization`.
- Forwarduje `Idempotency-Key`.

Payment form:

- `CreatePaymentOrderForm.vue`
- Zbiera `amountMinor`, `currency`, `clientOrderReference`.
- Tworzy stable `idempotencyKey`.
- Resetuje key po sukcesie albo zmianie danych.

Payment detail:

- `PaymentOrderDetail.vue`
- Pokazuje odczytany order.
- Jest konsumentem read contractu.

Pinia store:

- `payment-orders.ts`
- Trzyma loading/error/current/lastCreated state.

Zod schema:

- `payment-order.schema.ts`
- Waliduje input po stronie frontend, ale nie zastępuje backend validation.

Co frontend powinien przekazywać:

- Stable `Idempotency-Key`.
- Token przez server session.
- Body zgodne z backend DTO.
- Optional `X-Correlation-ID`, jeśli istnieje.

Co frontend powinien pokazać:

- Validation errors.
- Forbidden/not found.
- Conflict idempotency.
- Backend unavailable.

## 12a. Assertion Strategy (REST Assured vs AssertJ vs DB)

Kiedy użyć którego narzędzia do asercji:

| Kontekst | Narzędzie | Przykład |
|---|---|---|
| HTTP contract (status, nagłówki, proste body) | REST Assured `.then().statusCode().body()` | `.statusCode(201).body("paymentOrderId", notNullValue())` |
| Złożone porównania pól, listy, tuple | AssertJ `assertThat().extracting()` | `assertThat(merchants).extracting("reference").containsExactly("A", "B")` |
| Sprawdzenie stanu DB po operacji | Direct DB query + AssertJ | `assertThat(db.findMerchantById(id)).get().extracting(Merchant::status).isEqualTo("ACTIVE")` |
| Wielopolowe sprawdzenie obiektu | AssertJ `usingRecursiveComparison()` | `assertThat(orderResponse).usingRecursiveComparison().isEqualTo(expected)` |
| Nietrywialne kolekcje z filtrami | AssertJ `filteredOn`, `extracting` | `assertThat(orders).filteredOn("status", "CREATED").hasSize(1)` |

Zasada seniorska: **Assert what proves the behavior, not everything everywhere.**

- REST Assured body assertions chronią kontrakt HTTP.
- AssertJ extraction służy do diagnozy złożonych struktur.
- DB assertions służą tam, gdzie stan DB jest częścią ryzyka (constraint, transakcja, audit).
- Nie zaglądaj do DB w każdym teście — użyj API GET do weryfikacji, jeśli testujesz flow biznesowy.

## 12b. Database Verification as Test Layer

Decision table: kiedy weryfikować przez API, a kiedy przez DB:

| Test | Weryfikuj przez |
|---|---|
| zwykły contract test API (status, body, headers) | API GET lub response POST |
| test walidacji DTO | API (response body) |
| test flow create → get | API GET |
| test constraint enforcement (unique, FK, check) | DB query |
| test audit/status history | DB query (`payment_order_status_history`) |
| test migracji (czysta baza, poprzednia wersja) | DB schema validation |
| test transakcji/rollback | DB state + API status |
| test idempotency / race condition | DB constraints + API replay |
| test RLS / multi-tenant data isolation | DB query z różnymi tenant contexts |

Zasada seniorska:

> DB probe jest narzędziem diagnostycznym, nie skrótem.
> Nie omijaj API, żeby sprawdzić stan — użyj API GET.
> Użyj DB query, gdy API nie może dać ci odpowiedzi (constraint, audit, transakcja).

ćwiczenie: Dla każdego testu w `PaymentOrderRestAssuredTest` odpowiedz: czy ten test potrzebuje weryfikacji DB, czy wystarczy API?

## 12c. Test Data Ownership

Obecna strategia test data w Payment Order:

| Aspekt | Strategia |
|---|---|
| Merchant creation | Per-test: każdy test tworzy własnego merchanta |
| Namespacing referencji | Unikalne `MERCH-{uuid}`, `PAY-{uuid}` |
| Idempotency keys | `PaymentApiTestSupport.uniqueIdempotencyKey(suffix)` — unikalne per test |
| Cleanup | Immutable records (create-only) — brak potrzeby cleanup |
| Parallel safety | Każdy Testcontainer ma własną bazę, bez współdzielonych danych |
| Debuggability | Po teście dane zostają — można ręcznie sprawdzić stan |

Co NIE jest jeszcze zrobione (deferred do Lesson 8b):
- Strategia cleanup dla mutable data (np. lifecycle transitions).
- Worker-namespaced data dla równoległego wykonania w CI.
- Seed/reference data strategy.
- Fixtures dla trudnych do osiągnięcia stanów.

Pytanie seniorskie: **Jeśli test pada, czy dane zostają w bazie do debugowania, czy są automatycznie czyszczone?** Obecna strategia: zostają (lepsze do nauki i debugowania).

## 13. Pytania Do Samodzielnej Odpowiedzi

HTTP/REST:

1. Dlaczego first create zwraca `201`, a replay `200`?
2. Co oznacza `Location` header?
3. Dlaczego `ETag` jest zwracany, mimo że `If-Match` jest deferred?
4. Kiedy `403` jest lepsze niż `404`?
5. Dlaczego cross-tenant read maskujemy przez `404`?

REST Assured:

6. Dlaczego samo `.statusCode(201)` jest słabym oracle?
7. Kiedy użyć `.extract().path(...)`?
8. Dlaczego idempotency replay test musi użyć tego samego key?
9. Jak testujesz headers w REST Assured?
10. Kiedy test REST Assured powinien tworzyć dane przez API zamiast SQL?

Java/JDK 25:

11. Po co są value objects zamiast `long amountMinor` wszędzie?
12. Dlaczego amount jest minor units, a nie `double`?
13. Jak `record` pomaga w DTO/result models?
14. Co oznacza `@Transactional` w service layer?
15. Dlaczego exceptions są mapowane w `PaymentExceptionHandler`?

SQL/Flyway:

16. Które reguły waliduje DB?
17. Dlaczego unique constraint jest częścią idempotency?
18. Po co `payment_order_status_history`?
19. Jak Flyway pomaga w testach?
20. Dlaczego check constraints nie zastępują testów?

Security:

21. Jaka jest różnica między role i ownership?
22. Co robi `merchant_id` claim?
23. Dlaczego `merchant:payments:operate` nie daje read?
24. Co testuje `PaymentOrderSecurityTest`, czego nie testuje `PaymentOrderRestAssuredTest`?
25. Jak rozpoznać BOLA risk w tym API?

## 14. Zadania Praktyczne

| Zadanie | Files to inspect | Command | Expected outcome | Acceptance criteria |
|---|---|---|---|---|
| Code reading | `PaymentOrderController`, `PaymentOrderService`, `V2__create_payment_orders.sql` | `./mvnw -Dtest=PaymentOrderServiceTest test` | umiesz narysować create flow | wskazujesz, gdzie powstaje `201`, `200 replay`, `409` |
| API manual testing | `contracts/payment-order-api.md` | curl create/read | ręcznie odtwarzasz happy path i replay | zapisujesz `paymentOrderId` i powtarzasz request z tym samym key |
| SQL inspection | `V2__create_payment_orders.sql` | query local/test DB | rozumiesz constraints | wyjaśniasz FK, unique, check |
| REST Assured implementation | `PaymentOrderRestAssuredTest` | `./mvnw -Dtest=PaymentOrderRestAssuredTest test` | umiesz dodać contract test | test sprawdza status, header i body |
| Negative tests | `PaymentOrderRestAssuredTest` | `./mvnw -Dtest=PaymentOrderRestAssuredTest#idempotencyConflictReturns409 test` | rozumiesz `409` | test odróżnia conflict od validation |
| Security tests | `PaymentOrderSecurityTest` | `./mvnw -Dtest=PaymentOrderSecurityTest test` | rozumiesz role matrix | potrafisz dodać denied identity case |
| Exploratory charter | REST + security tests | manual/API | sprawdzasz tenant isolation | create mismatch `403`, merchant cross-read `404`, platform correct path `200`, platform wrong path `404` |
| Business-readable naming | `PaymentOrderRestAssuredTest` | IDE | dodajesz `@DisplayName` do 3 testów | nazwy testów mówią co system gwarantuje, nie jak to robi |
| Negative-path first | `PaymentOrderRestAssuredTest` | `./mvnw -Dtest=PaymentOrderRestAssuredTest test` | piszesz test `409` PRZED testem `201` | rozumiesz, że odrzucenie nieprawidłowego requestu jest równie ważne jak sukces |

## 15. Mini Interview Prep

Jak działa idempotent create?

> Idempotent create uses a client-provided `Idempotency-Key` scoped by merchant. The backend stores a request fingerprint and returns the same payment order for repeated requests with the same key and same fingerprint, while returning `409 Conflict` if the same key is reused for a different request.

Dlaczego `Idempotency-Key` nie może być generowany od nowa przy każdym retry?

> A new key means a new operation from the backend perspective. If the first response was lost but the order was created, retrying with a new key can create a duplicate payment order.

Jak testujesz tenant isolation?

> I create two merchants and scoped tokens, then verify that a user from merchant B cannot read or create resources for merchant A. For reads, I expect masked `404`; for create scope mismatch, I expect `403`.

Co testujesz w REST Assured, a co w domain/repository tests?

> REST Assured verifies the HTTP contract: status, headers, response body, auth behavior. Domain tests verify value object rules and service decisions. Repository tests verify database constraints and persistence mappings.

Jak Flyway pomaga w jakości?

> Flyway makes database schema changes versioned, repeatable and testable. Integration tests run against the same migrations, so schema drift is detected early.

Po co DB constraints, skoro mamy walidację w Java?

> Java validation improves error handling and developer readability, but DB constraints protect data integrity even if another path, bug or future process bypasses application validation.

Jak rozdzielasz 401, 403, 404 i 409?

> `401` means unauthenticated or invalid token. `403` means authenticated but not allowed. `404` can mean missing resource or masked cross-tenant read. `409` means a valid request conflicts with current business state or idempotency record.

Jak zaprojektować testy równoległe dla payment order create?

> Use unique merchant references, unique client order references and controlled idempotency keys. For replay tests, reuse the same key intentionally. Avoid shared mutable fixtures and isolate data per test.

## 16. Verification Commands

Backend full suite:

```bash
./mvnw test
```

Selected REST Assured:

```bash
./mvnw -Dtest=PaymentOrderRestAssuredTest test
```

Selected security:

```bash
./mvnw -Dtest=PaymentOrderSecurityTest test
```

Selected service:

```bash
./mvnw -Dtest=PaymentOrderServiceTest test
```

Selected repository:

```bash
./mvnw -Dtest=JpaPaymentOrderRepositoryTest test
```

Frontend typecheck:

```bash
corepack pnpm typecheck
```

Optional frontend E2E:

```bash
corepack pnpm test:e2e
```

## 17. Learning Outcome Checklist

Po Lekcji 6 learner powinien umieć wyjaśnić:

- [ ] Jak działa create/read payment order.
- [ ] Jak działa `Idempotency-Key`.
- [ ] Dlaczego replay zwraca `200`, a first create `201`.
- [ ] Jak odróżnić `401`, `403`, `404`, `409`.
- [ ] Jak działa merchant-scoped access.
- [ ] Jak DB constraints wspierają jakość.
- [ ] Jak REST Assured chroni API contract.
- [ ] Kiedy użyć REST Assured body assertions vs AssertJ vs DB query.
- [ ] Jak zdecydować, czy test wymaga weryfikacji przez DB.
- [ ] Jak działa strategia test data ownership.

Powinien umieć zaimplementować:

- [ ] REST Assured test z body, headers i extraction.
- [ ] Negative test dla validation/conflict.
- [ ] Security test dla role/ownership.
- [ ] Repository test dla DB constraint.
- [ ] Prosty SQL inspection query.
- [ ] Business-readable test name z `@DisplayName`.
- [ ] Negative-path test napisany przed happy-path testem.

Powinien umieć przetestować:

- [ ] Happy path create.
- [ ] Idempotent replay.
- [ ] Idempotency conflict.
- [ ] Missing header.
- [ ] Invalid amount/currency.
- [ ] Inactive merchant.
- [ ] Cross-tenant read.
- [ ] Platform read.

Powinien umieć nazwać ryzyka:

- [ ] Duplicate payment order on retry.
- [ ] BOLA/cross-tenant leak.
- [ ] Role/claim drift.
- [ ] Frontend retry z nowym key.
- [ ] Merchant eligibility niezależna od scope.
- [ ] DB schema drift.
- [ ] Weak REST Assured assertions.
- [ ] Race condition in idempotent create.

## 18. Powiązane Notatki W Vault

Backlinks:

- `[[Payment Gateway SDET Learning Plan]]`
- `[[Lesson Evidence Tracker]]`
- `[[Senior SDET Competency Coverage Matrix]]`
- `[[Phase 1 - Merchant Registry and Activation]]`
- `[[Phase 2 - Payment Orders]]`
- `[[REST REST Assured Java - Session Summary - Merchant API Tests]]`
- `[[Architecture - Modular Monolith with Spring Modulith]]`
- `[[Infrastructure - Local PostgreSQL 18 and Keycloak 26.6.1]]`
- `[[Testing - Parallel Readiness Principles]]`

Suggested updates rather than duplicate notes:

- REST Assured details can extend `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/01-12 REST Assured Foundations.md`.
- SQL/Flyway practice can extend `knowledge-vault/02 Areas/Technical Learning/PostgreSQL and SQL From Zero/README.md` and `knowledge-vault/02 Areas/Technical Learning/Spring Data JPA and Flyway/README.md`.
- Security/ownership learning can extend `knowledge-vault/02 Areas/Technical Learning/Security and Authorization Testing/README.md`.
- Modulith boundary learning can extend or create a payment-specific note under `knowledge-vault/02 Areas/Technical Learning/Spring Modulith/`.
