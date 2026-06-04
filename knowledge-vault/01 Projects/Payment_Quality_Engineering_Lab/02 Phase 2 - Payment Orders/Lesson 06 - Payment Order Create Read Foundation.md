 
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

| Actor                       | Create Own | Create Other | Read Own |                      Read Other | Platform Read |
| --------------------------- | ---------: | -----------: | -------: | ------------------------------: | ------------: |
| Unauthenticated             |      `401` |        `401` |    `401` |                           `401` |         `401` |
| Denied identity             |      `403` |        `403` |    `403` |                           `403` |         `403` |
| `merchant:payments:create`  |  `201/200` |        `403` |    `403` |                       `403/404` |         `403` |
| `merchant:payments:read`    |      `403` |        `403` |    `200` |                           `404` |         `403` |
| `merchant:payments:operate` |      `403` |        `403` |    `403` |                           `403` |         `403` |
| `platform:payments:read`    |      `403` |        `403` |    `200` | `200` in correct nested context |         `200` |

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

### 9.1. Jak czytać tę tabelę od początku

Ta tabela nie odpowiada tylko na pytanie, czy user istnieje. Ona pokazuje pełną decyzję bezpieczeństwa dla API:

- Czy request ma poprawny token? To jest authentication.
- Czy token ma właściwą rolę do typu operacji? To jest authorization by role.
- Czy user działa na swoim merchantcie/tenancie? To jest tenant isolation albo ownership.

Najprostsza analogia: system jest jak budynek z wieloma firmami.

- Keycloak jest recepcją budynku.
- Realm jest osobnym światem logowania, czyli np. całym budynkiem `payment-lab`.
- User jest osobą, która przychodzi do budynku.
- Token JWT jest badge'em wydanym przez recepcję.
- Roles są napisami na badge'u, np. `merchant:payments:create` albo `merchant:payments:read`.
- `merchant_id` claim mówi, dla której firmy/merchanta ta osoba pracuje.
- Spring Security jest ochroną przy wejściu do konkretnych drzwi API.
- Tenant isolation jest zasadą, że pracownik firmy A nie może zobaczyć dokumentów firm
y B.

Keycloak wystawia badge. Spring Security sprawdza badge. Backend sprawdza, czy osoba wchodzi do właściwego pokoju i czy dokument należy do jej firmy.

### 9.2. Authentication vs Authorization

Authentication odpowiada na pytanie: kim jesteś i czy masz ważny token?

Przykłady problemów authentication:

- Request nie ma tokena.
- Token jest uszkodzony.
- Token wygasł.
- Token nie pochodzi z oczekiwanego Keycloak realm.

Wtedy API zwraca `401 Unauthorized`.

Proste zdanie: nie wiem, kim jesteś, więc nie wpuszczam cię.

Authorization odpowiada na pytanie: wiem, kim jesteś, ale czy wolno ci wykonać tę operację?

Przykłady problemów authorization:

- User ma poprawny token, ale nie ma roli `merchant:payments:create`.
- User ma rolę read, ale próbuje wykonać create.
- User ma rolę merchantową, ale próbuje wykonać operację platformową.

Wtedy API zwraca `403 Forbidden`.

Proste zdanie: znam cię, ale nie masz prawa zrobić tej rzeczy.

### 9.3. Realm, roles, claims i Spring Security

Realm w Keycloak to osobna przestrzeń logowania. Można o nim myśleć jak o osobnym świecie z własnymi userami, rolami, konfiguracją tokenów i regułami logowania.

W tej lekcji realm dodaje role paymentowe:

- `merchant:payments:create`
- `merchant:payments:read`
- `merchant:payments:operate`
- `platform:payments:read`

Keycloak nie wykonuje logiki paymentów. Keycloak nie decyduje, czy konkretny `paymentOrderId` należy do danego merchanta. Keycloak głównie mówi backendowi: ten user jest zalogowany, ma takie role i ma taki `merchant_id`.

Spring Security działa po stronie backendu. Sprawdza, czy request ma ważny token, czy token pochodzi z zaufanego realm i jakie role ma user.

Sama rola często nie wystarcza. Aplikacja musi jeszcze sprawdzić ownership, czyli czy `merchant_id` z tokena pasuje do merchanta w ścieżce API albo do właściciela `paymentOrder`.

Rola odpowiada na pytanie: co możesz robić?

Ownership odpowiada na pytanie: czy możesz to zrobić na tym konkretnym zasobie?

### 9.4. Znaczenie kolumn w tabeli

`Create Own` oznacza, że user tworzy payment order dla swojego merchanta.

Przykład: user z `merchant_id = merchant-A` tworzy order dla `merchant-A`.

`Create Other` oznacza, że user próbuje tworzyć payment order dla innego merchanta.

Przykład: user z `merchant_id = merchant-A` próbuje tworzyć order dla `merchant-B`.

`Read Own` oznacza, że user czyta payment order swojego merchanta.

Przykład: user z `merchant_id = merchant-A` czyta order należący do `merchant-A`.

`Read Other` oznacza, że user próbuje czytać payment order innego merchanta.

Przykład: user z `merchant_id = merchant-A` próbuje czytać order należący do `merchant-B`.

`Platform Read` oznacza odczyt z perspektywy platformy, np. supportu, audytu albo compliance.

Przykład: platform user z rolą `platform:payments:read` czyta payment order w poprawnym kontekście platformowym.

### 9.5. Use cases dla wierszy tabeli

`Unauthenticated` oznacza request bez poprawnego tokena.

Use case: ktoś wysyła request bez `Authorization` albo z wygasłym tokenem. Backend nie wie, kim jest caller. Wszystkie operacje dają `401`.

Proste zdanie: najpierw pokaż ważny badge.

`Denied identity` oznacza zalogowanego usera, który nie ma wymaganych payment permissions.

Use case: user istnieje w Keycloak, ale nie ma żadnej roli potrzebnej do payment order API. Authentication jest OK, ale authorization nie przechodzi. Wszystkie operacje dają `403`.

Proste zdanie: znam cię, ale nie wolno ci korzystać z tej części systemu.

`merchant:payments:create` pozwala tworzyć payment orders dla własnego merchanta.

Use case: merchant employee z `merchant_id = merchant-A` tworzy payment order dla `merchant-A`. Backend może zwrócić `201 Created` przy pierwszym utworzeniu albo `200 OK` przy idempotentnym replayu tej samej operacji.

Ta sama rola nie pozwala czytać orders, jeśli user nie ma roli read. Dlatego `Read Own` daje `403`.

Create dla innego merchanta daje `403`, bo user jest authenticated i ma rolę create, ale nie ma prawa działać w cudzym tenant scope.

`merchant:payments:read` pozwala czytać payment orders własnego merchanta.

Use case: merchant employee z `merchant_id = merchant-A` czyta order należący do `merchant-A`. Backend zwraca `200 OK`.

Ta rola nie pozwala tworzyć nowych payment orders, więc create daje `403`.

Jeśli merchant reader próbuje czytać order innego merchanta, backend zwraca `404`, żeby nie ujawnić, czy cudzy `paymentOrderId` istnieje.

`merchant:payments:operate` wygląda jak przyszła rola operacyjna, np. do capture, cancel, refund albo retry. W Lesson 6 takich operacji jeszcze nie ma, więc ta rola nie daje prawa do create/read i wszystkie pokazane operacje dają `403`.

Proste zdanie: rola operate może być ważna później, ale w tej lekcji nie oznacza dostępu do wszystkiego.

`platform:payments:read` pozwala czytać payment orders z perspektywy platformy.

Use case: support platformy sprawdza payment order dla merchanta po zgłoszeniu problemu. User ma szerszy odczyt niż merchant user, ale nadal nie może tworzyć payment orders. Dlatego create daje `403`, a platform read daje `200`.

Platform read nie oznacza platform write. Odczyt platformowy powinien działać tylko w poprawnym kontekście API, a nie jako przypadkowe omijanie tenant isolation.

### 9.6. Dlaczego cross-tenant read daje `404`

Cross-tenant read to próba odczytu danych innego merchanta.

Przykład: user z `merchant_id = merchant-A` próbuje czytać order z `merchant-B`.

Backend mógłby zwrócić `403`, ale wtedy zdradzałby, że taki order istnieje. `404` jest bezpieczniejsze, bo mówi: dla ciebie ten zasób nie istnieje.

To chroni przed enumeracją cudzych `paymentOrderId` i przed wyciekiem informacji między tenantami.

### 9.7. Dlaczego create scope mismatch daje `403`

Create scope mismatch oznacza, że user próbuje tworzyć zasób dla innego merchanta niż ten z tokena.

Przykład: user z `merchant_id = merchant-A` próbuje utworzyć payment order dla `merchant-B`.

Tutaj backend wie, że caller jest authenticated i próbuje wykonać zabronioną akcję w cudzym scope. Dlatego `403` jest właściwe.

Proste zdanie: możesz mieć rolę create, ale tylko dla swojego merchanta.

### 9.8. Dlaczego UI hiding nie wystarcza

Frontend może ukryć przycisk create albo read. To jest dobre dla wygody użytkownika, ale to nie jest prawdziwe security.

Ktoś może wysłać request poza UI:

- przez REST Assured,
- przez Postman,
- przez curl,
- przez własny skrypt,
- przez zmodyfikowany frontend.

Dlatego backend musi egzekwować reguły authentication, authorization, roles i tenant isolation.

Proste zdanie: frontend może ukrywać akcje, ale backend musi blokować akcje.

### 9.9. Najważniejsze do zapamiętania

`401` znaczy: nie jesteś poprawnie zalogowany.

`403` znaczy: jesteś znany, ale nie masz prawa do tej operacji.

`404` przy cross-tenant read znaczy: ten zasób nie istnieje dla ciebie.

Rola znaczy: co możesz robić?

`merchant_id` claim znaczy: dla którego merchanta możesz to robić?

Tenant isolation znaczy: nie możesz zobaczyć ani użyć danych innego merchanta.

Keycloak mówi backendowi, kim jesteś i jakie masz role.

Spring Security sprawdza token i role przy wejściu do API.

Backendowa logika ownership sprawdza, czy konkretny `paymentOrder` należy do twojego merchanta.

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

|   # | Test                                      | Cel             | Ryzyko                       | Assertions                                                 | Expected |
| --: | ----------------------------------------- | --------------- | ---------------------------- | ---------------------------------------------------------- | -------- |
|   1 | `createPaymentOrderReturns201WithHeaders` | pierwszy create | brak contract headers        | `201`, `Location`, `ETag`, `X-Correlation-ID`, body fields | `201`    |
|   2 | `idempotentReplayReturns200WithSameId`    | replay          | duplicate order on retry     | first `201`, second `200`, same ID                         | `200`    |
|   3 | `idempotencyConflictReturns409`           | conflict        | same key different intent    | `409`, `error=idempotency_conflict`                        | `409`    |
|   4 | `crossTenantReadReturns404`               | tenant masking  | BOLA/data leak               | `404`, `error=not_found`                                   | `404`    |
|   5 | `platformReaderCanReadCrossMerchant`      | platform read   | role matrix drift            | `200`, same payment ID                                     | `200`    |
|   6 | missing `Idempotency-Key`                 | required header | unsafe create                | `400`                                                      | `400`    |
|   7 | invalid currency                          | validation      | invalid money data           | `400`, `error=validation`                                  | `400`    |
|   8 | inactive merchant                         | eligibility     | orders for inactive merchant | `409`, `merchant_not_payment_eligible`                     | `409`    |
|   9 | merchant scope mismatch create            | ownership       | cross-merchant create        | `403`, `forbidden`                                         | `403`    |
|  10 | platform read wrong nested merchant path  | route semantics | path/body contradiction      | `404`, `not_found`                                         | `404`    |

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

   PL: `201 Created` oznacza, że powstał nowy payment order. Replay z tym samym `Idempotency-Key` zwraca `200 OK`, bo zasób już istnieje i system tylko odtwarza wcześniejszy wynik.

   EN: `201 Created` means a new payment order was created. A replay with the same `Idempotency-Key` returns `200 OK` because the resource already exists and the system returns the previous result.

2. Co oznacza `Location` header?

   PL: `Location` wskazuje URL nowo utworzonego zasobu. Klient może użyć go do późniejszego odczytu payment order.

   EN: `Location` points to the URL of the newly created resource. The client can use it later to read the payment order.

3. Dlaczego `ETag` jest zwracany, mimo że `If-Match` jest deferred?

   PL: `ETag` już teraz buduje kontrakt pod przyszłe optimistic locking/cache validation. `If-Match` jest odłożone, ale klient może już widzieć wersję zasobu.

   EN: `ETag` prepares the contract for future optimistic locking or cache validation. `If-Match` is deferred, but the client can already see the resource version.

4. Kiedy `403` jest lepsze niż `404`?

   PL: `403` jest lepsze, gdy użytkownik jest uwierzytelniony, ale nie ma uprawnień do operacji. To jasny sygnał problemu z autoryzacją, nie z brakiem zasobu.

   EN: `403` is better when the user is authenticated but not allowed to perform the operation. It clearly signals an authorization problem, not a missing resource.

5. Dlaczego cross-tenant read maskujemy przez `404`?

   PL: `404` nie zdradza, czy zasób istnieje w innym tenantcie. To ogranicza ryzyko BOLA i wycieku informacji.

   EN: `404` does not reveal whether the resource exists in another tenant. This reduces BOLA and information leakage risk.

REST Assured:

6. Dlaczego samo `.statusCode(201)` jest słabym oracle?

   PL: Sam status nie potwierdza poprawności body, headers ani danych biznesowych. Test może przejść mimo błędnego kontraktu odpowiedzi.

   EN: Status alone does not verify the body, headers, or business data. The test may pass even when the response contract is wrong.

7. Kiedy użyć `.extract().path(...)`?

   PL: Użyj tego, gdy wartość z odpowiedzi jest potrzebna w kolejnych asercjach lub requestach. Przykład: wyciągnięcie `paymentOrderId` po create.

   EN: Use it when a response value is needed in later assertions or requests. Example: extracting `paymentOrderId` after create.

8. Dlaczego idempotency replay test musi użyć tego samego key?

   PL: Idempotency działa tylko dla tego samego `Idempotency-Key`. Nowy key oznacza dla backendu nową operację.

   EN: Idempotency works only with the same `Idempotency-Key`. A new key means a new operation from the backend perspective.

9. Jak testujesz headers w REST Assured?

   PL: Używam `.header(...)` albo `.headers(...)` w sekcji `then()`. Sprawdzam np. `Location`, `ETag` i wymagane nagłówki błędów.

   EN: I use `.header(...)` or `.headers(...)` in the `then()` section. I verify headers such as `Location`, `ETag`, and required error headers.

10. Kiedy test REST Assured powinien tworzyć dane przez API zamiast SQL?

    PL: Gdy test sprawdza zachowanie systemu z perspektywy klienta API. SQL warto używać tylko do setupu technicznego lub weryfikacji DB constraints.

    EN: When the test verifies system behavior from the API client perspective. SQL should be used mainly for technical setup or DB constraint verification.

Java/JDK 25:

11. Po co są value objects zamiast `long amountMinor` wszędzie?

    PL: Value object ukrywa reguły walidacji i znaczenie danych w jednym miejscu. Dzięki temu kod jest czytelniejszy i trudniej pomylić wartości.

    EN: A value object keeps validation rules and meaning in one place. This makes the code clearer and reduces accidental misuse.

12. Dlaczego amount jest minor units, a nie `double`?

    PL: Minor units unikają błędów zaokrągleń typowych dla `double`. Pieniądze powinny być reprezentowane deterministycznie.

    EN: Minor units avoid rounding errors common with `double`. Money should be represented deterministically.

13. Jak `record` pomaga w DTO/result models?

    PL: `record` daje niemutowalny, krótki model danych z automatycznym konstruktorem i accessorami. Jest dobry dla DTO bez logiki biznesowej.

    EN: A `record` gives an immutable, concise data model with an automatic constructor and accessors. It is useful for DTOs without business logic.

14. Co oznacza `@Transactional` w service layer?

    PL: Oznacza, że operacja service wykonuje się w jednej transakcji DB. Jeśli wystąpi błąd, zmiany mogą zostać wycofane.

    EN: It means the service operation runs within one database transaction. If an error occurs, changes can be rolled back.

15. Dlaczego exceptions są mapowane w `PaymentExceptionHandler`?

    PL: Handler zamienia wyjątki domenowe/aplikacyjne na spójne odpowiedzi HTTP. Dzięki temu kontrolery są prostsze, a API ma przewidywalny error contract.

    EN: The handler maps domain/application exceptions to consistent HTTP responses. This keeps controllers simpler and gives the API a predictable error contract.

SQL/Flyway:

16. Które reguły waliduje DB?

    PL: DB waliduje reguły integralności, np. `NOT NULL`, FK, unique constraints i check constraints. Chroni dane nawet wtedy, gdy aplikacja ma błąd.

    EN: The DB validates integrity rules such as `NOT NULL`, foreign keys, unique constraints, and check constraints. It protects data even if the application has a bug.

17. Dlaczego unique constraint jest częścią idempotency?

    PL: Unique constraint blokuje zapis dwóch rekordów dla tego samego merchant i idempotency key. To zabezpiecza także przed race condition.

    EN: A unique constraint prevents two records for the same merchant and idempotency key. It also protects against race conditions.

18. Po co `payment_order_status_history`?

    PL: Historia statusów daje audytowalność zmian payment order. Pomaga też testować i diagnozować przejścia stanu.

    EN: Status history gives auditability of payment order changes. It also helps test and diagnose state transitions.

19. Jak Flyway pomaga w testach?

    PL: Flyway uruchamia te same migracje w testach i środowiskach aplikacji. Dzięki temu testy szybciej wykrywają schema drift.

    EN: Flyway runs the same migrations in tests and application environments. This helps tests detect schema drift early.

20. Dlaczego check constraints nie zastępują testów?

    PL: Check constraints chronią DB, ale nie sprawdzają kontraktu HTTP, komunikatów błędów ani ścieżek biznesowych. Testy pokazują zachowanie systemu z perspektywy użytkownika.

    EN: Check constraints protect the DB, but they do not verify HTTP contracts, error messages, or business paths. Tests show system behavior from the user perspective.

Security:

21. Jaka jest różnica między role i ownership?

    PL: Role mówi, co użytkownik może robić. Ownership mówi, do których danych użytkownik ma prawo dostępu.

    EN: Role defines what the user is allowed to do. Ownership defines which data the user is allowed to access.

22. Co robi `merchant_id` claim?

    PL: `merchant_id` wiąże token z konkretnym merchantem. Backend używa go do sprawdzania tenant ownership.

    EN: The `merchant_id` claim binds the token to a specific merchant. The backend uses it to verify tenant ownership.

23. Dlaczego `merchant:payments:operate` nie daje read?

    PL: Operate i read to osobne uprawnienia, żeby nie rozszerzać dostępu przypadkiem. Użytkownik może mieć prawo tworzenia/operowania bez prawa odczytu.

    EN: Operate and read are separate permissions to avoid accidental access expansion. A user may be allowed to operate/create without being allowed to read.

24. Co testuje `PaymentOrderSecurityTest`, czego nie testuje `PaymentOrderRestAssuredTest`?

    PL: `PaymentOrderSecurityTest` skupia się na rolach, claimach, ownership i odmowie dostępu. `PaymentOrderRestAssuredTest` głównie sprawdza kontrakt REST i flow API.

    EN: `PaymentOrderSecurityTest` focuses on roles, claims, ownership, and access denial. `PaymentOrderRestAssuredTest` mainly verifies the REST contract and API flow.

25. Jak rozpoznać BOLA risk w tym API?

    PL: BOLA risk pojawia się, gdy użytkownik może odczytać lub zmienić zasób innego merchanta przez manipulację ID/path parametrem. Testuj cross-tenant access i oczekuj odmowy lub maskowanego `404`.

    EN: BOLA risk appears when a user can read or modify another merchant's resource by manipulating an ID or path parameter. Test cross-tenant access and expect denial or masked `404`.

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

### Rozwiązania / wskazówki

1. Code reading: create flow zaczyna się w controllerze, przechodzi przez service i kończy zapisem w DB. `201` powstaje przy pierwszym zapisie, `200` przy idempotent replay, a `409` przy tym samym key i innym fingerprint.
2. API manual testing: utwórz order z jednym `Idempotency-Key`, zapisz `paymentOrderId`, a potem powtórz identyczny request. Oczekuj `201` za pierwszym razem i `200` przy replay.
3. SQL inspection: wskaż FK do merchanta, unique constraint dla idempotency i check constraints dla danych typu amount/currency/status. DB constraints są ostatnią linią obrony integralności.
4. REST Assured implementation: test powinien sprawdzać status, `Location`, `ETag` i body z business fields. Sam `statusCode(201)` nie wystarcza.

```java
then()
    .statusCode(201)
    .header("Location", containsString("/payment-orders/"))
    .header("ETag", not(blankOrNullString()))
    .body("currency", equalTo("PLN"));
```

5. Negative tests: reuse tego samego `Idempotency-Key` z innym body powinien dać `409`, nie validation error. To sprawdza conflict semantyki idempotency, a nie poprawność pól.
6. Security tests: dodaj denied identity albo token z niepasującym `merchant_id`. Oczekuj `403` dla create mismatch i masked `404` dla cross-tenant read.
7. Exploratory charter: sprawdź minimum cztery ścieżki: own merchant success, create mismatch, cross-tenant read i platform read. Notuj status, body i czy API ujawnia obcy zasób.
8. Business-readable naming: `@DisplayName` powinien opisywać gwarancję systemu, np. "replay returns the original payment order". Unikaj nazw typu "testCreate1".
9. Negative-path first: najpierw napisz test odrzucenia konfliktu lub invalid request, potem happy path. To pomaga pilnować, że API jest bezpieczne także poza sukcesem.

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
