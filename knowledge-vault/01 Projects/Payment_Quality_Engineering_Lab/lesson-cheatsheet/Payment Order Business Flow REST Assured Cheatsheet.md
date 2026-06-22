# Payment Order Business Flow REST Assured Cheatsheet

## Cel Lekcji

Ta klasa testowa pokazuje, jak testowac REST API na poziomie prawdziwego HTTP flow: role, tokeny, headers, body, status codes, response headers, tenant isolation i idempotency.

Plik kodu: `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderBusinessFlowRestAssuredTest.java`

## Mental Model

Test nie sprawdza pojedynczej metody Javy. Test sprawdza zachowanie API widziane przez klienta HTTP.

Czytaj kazdy scenariusz jako sekwencje requestow:

```text
Given: aktywny merchant + token z rola
When: HTTP request z poprawnym path, headers i body
Then: status code + headers + JSON body potwierdzaja kontrakt API
```

## Warstwy W Teście

| Element | Rola w tescie |
|---|---|
| `@SpringBootTest(RANDOM_PORT)` | Uruchamia prawdziwa aplikacje na losowym porcie |
| `@LocalServerPort int port` | Port, na ktory REST Assured wysyla requesty |
| Testcontainers PostgreSQL | Prawdziwa baza dla testu integracyjnego |
| `TestJwtSupport` | Tworzy testowe JWT z rolami i `merchant_id` |
| `MerchantApiTestSupport` | Buduje bazowy REST Assured request z tokenem |
| `PaymentApiTestSupport` | Buduje dane testowe: merchant, payment body, idempotency key |
| REST Assured | Klient HTTP w tescie |

## REST Assured: Given When Then

Typowy schemat:

```java
paymentCreatorRequest(token, idempotencyKey, correlationId)
    .body(body)
    .when()
    .post("/api/merchants/{merchantId}/payment-orders", merchantId)
    .then()
    .statusCode(201)
    .body("status", equalTo("CREATED"));
```

Znaczenie:

| Fragment | Znaczenie |
|---|---|
| `paymentCreatorRequest(...)` | Given: port, auth, content type, headers |
| `.body(body)` | Given: JSON payload |
| `.when()` | Koniec przygotowania requestu |
| `.post(...)` / `.get(...)` | Faktyczne wyslanie HTTP requestu |
| `.then()` | Start asercji odpowiedzi |
| `.statusCode(...)` | Asercja HTTP statusu |
| `.header(...)` | Asercja response headera |
| `.body(...)` | Asercja JSON body przez JSON path |
| `.extract().path(...)` | Wyciagniecie danych z odpowiedzi do kolejnego kroku |

## Najwazniejsze Endpointy

| Operacja | Metoda HTTP | Endpoint | Wymagany sens biznesowy |
|---|---|---|---|
| Create payment order | `POST` | `/api/merchants/{merchantId}/payment-orders` | Creator tworzy order dla swojego merchanta |
| Read payment order | `GET` | `/api/merchants/{merchantId}/payment-orders/{paymentOrderId}` | Reader czyta istniejacy order |
| Create merchant setup | `POST` | `/api/merchants` | Platform operator tworzy merchanta |
| Activate merchant setup | `POST` | `/api/merchants/{id}/activate` | Platform operator aktywuje merchanta |

## Tokeny I Role

| Token | Uzycie | Co reprezentuje |
|---|---|---|
| `platformOperatorToken()` | Setup merchanta | Operator platformy moze tworzyc i aktywowac merchantow |
| `merchantPaymentCreatorToken(merchantId)` | `POST /payment-orders` | Uzytkownik merchanta z prawem tworzenia payment orders |
| `merchantPaymentReaderToken(merchantId)` | `GET /payment-orders/{id}` | Uzytkownik merchanta z prawem odczytu |
| `platformPaymentReaderToken()` | Odczyt platformowy | Platforma moze czytac ordery wielu merchantow |

Kluczowa lekcja: token to nie tylko auth. Token okresla aktora, role i scope danych.

## Headers Cheatsheet

| Header | Request/Response | Po co jest |
|---|---|---|
| `Authorization: Bearer <jwt>` | Request | Uwierzytelnienie i role |
| `Content-Type: application/json` | Request | Mowi API, ze body jest JSON-em |
| `Idempotency-Key` | Request create | Chroni przed duplikatem przy retry `POST` |
| `X-Correlation-ID` | Request i response | Trace pojedynczego technicznego wywolania |
| `Location` | Response `201` | URL nowo utworzonego zasobu |
| `ETag` | Response | Wersja/reprezentacja zasobu, przydatna do cache/concurrency |

## Body Cheatsheet

Create payment order body:

```json
{
  "amountMinor": 5000,
  "currency": "EUR",
  "clientOrderReference": "PAY-RETRY-AB12CD34"
}
```

| Pole | Znaczenie testowe |
|---|---|
| `amountMinor` | Kwota w minor units, np. grosze/centy |
| `currency` | Waluta, np. `PLN`, `EUR`, `USD` |
| `clientOrderReference` | Referencja klienta, unikalna dla czytelnosci i izolacji danych |

Dobre dane testowe sa celowe:

| Dane | Dlaczego sa generowane unikalnie |
|---|---|
| `merchantReference` | Zeby nie wpasc w duplicate merchant reference |
| `clientOrderReference` | Zeby scenariusze nie mieszaly sie w bazie/logach |
| `idempotencyKey` | Zeby kazdy test mial wlasna intencje biznesowa |
| `correlationId` | Zeby odroznic konkretne techniczne requesty |

## Idempotency: Najwazniejsza Idea

`Idempotency-Key` odpowiada na pytanie:

```text
Czy ten POST jest nowa intencja biznesowa, czy retry poprzedniej intencji?
```

Decyzja systemu:

| Sytuacja | Oczekiwany wynik |
|---|---|
| Nowy key + poprawne body | `201 Created`, powstaje nowy payment order |
| Ten sam key + to samo body | `200 OK`, wraca oryginalny payment order |
| Ten sam key + inne body | `409 Conflict`, `idempotency_conflict` |

Wazne rozroznienie:

| Mechanizm | Co identyfikuje |
|---|---|
| `Idempotency-Key` | Intencje biznesowa |
| `X-Correlation-ID` | Pojedyncze wywolanie techniczne HTTP |

Dlatego retry ma ten sam `Idempotency-Key`, ale inny `X-Correlation-ID`.

## Scenariusz 1: Creator Tworzy, Reader Czyta

Cel: potwierdzic pelny happy path create + read.

Flow:

```text
1. Utworz aktywnego merchanta
2. Wygeneruj creator token dla tego merchanta
3. Wygeneruj reader token dla tego merchanta
4. Creator wysyla POST z body i Idempotency-Key
5. API zwraca 201 Created + Location + ETag + JSON
6. Test wyciaga paymentOrderId
7. Reader wysyla GET po ten paymentOrderId
8. API zwraca 200 OK i te sama reprezentacje biznesowa
```

Dlaczego sa dwa tokeny?

```text
creatorToken -> sprawdza uprawnienie do POST
readerToken  -> sprawdza uprawnienie do GET
```

Ten test uczy separacji rol: tworzenie i odczyt to rozne capability.

## Scenariusz 2: Retry Z Tym Samym Idempotency-Key

Cel: potwierdzic, ze retry `POST` nie tworzy duplikatu.

Flow:

```text
1. Utworz aktywnego merchanta
2. Wygeneruj creator token
3. Przygotuj jeden Idempotency-Key
4. Przygotuj jedno body i zapisz do zmiennej
5. Pierwszy POST -> 201 Created
6. Zapisz paymentOrderId
7. Drugi POST z tym samym key i tym samym body -> 200 OK
8. Sprawdz, ze paymentOrderId jest taki sam
```

Dlaczego `body` jest w zmiennej?

```java
Map<String, Object> body = PaymentApiTestSupport.createPaymentOrderBody(5_000, "EUR", clientReference);
```

Bo oba requesty musza miec dokladnie te sama intencje biznesowa. To nie jest podobne body. To ma byc ten sam payload logiczny.

Najwazniejsza asercja:

```java
.body("paymentOrderId", equalTo(paymentOrderId))
```

To dowodzi, ze drugi POST zwrocil oryginalny order, a nie utworzyl nowego.

## Scenariusz 3: Ten Sam Key, Inne Body = Conflict

Cel: potwierdzic, ze API nie pozwala uzyc jednego key do innej intencji biznesowej.

Flow:

```text
1. Pierwszy POST: key K + body A -> 201 Created
2. Drugi POST: key K + body B -> 409 Conflict
```

To chroni przed bledem klienta:

```text
Nie wolno pod tym samym Idempotency-Key ukryc innego zamowienia.
```

Oczekiwany blad:

```json
{
  "error": "idempotency_conflict",
  "message": "...",
  "correlationId": "corr-business-conflict-second"
}
```

## Scenariusz 4: Tenant Isolation I Masked 404

Cel: potwierdzic, ze merchant B nie moze odczytac ordera merchanta A.

Flow:

```text
1. Utworz merchant A i merchant B
2. Creator A tworzy payment order dla A
3. Reader B probuje czytac order A
4. API zwraca 404 Not Found
```

Dlaczego `404`, a nie `403`?

```text
404 maskuje istnienie cudzego zasobu.
```

To jest ochrona przed BOLA/IDOR: klient nie powinien dowiedziec sie, ze cudzy `paymentOrderId` istnieje.

## Scenariusz 5: Platform Reader Czyta, Ale Nie Tworzy

Cel: rozdzielic role platformowego odczytu od tworzenia.

Flow:

```text
1. Merchant creator tworzy payment order
2. Platform reader czyta payment order -> 200 OK
3. Platform reader probuje tworzyc payment order -> 403 Forbidden
```

Lekcja:

```text
Read permission != create permission
Platform visibility != platform write access
```

## Status Codes: Szybka Mapa

| Status | Znaczenie w tych testach |
|---|---|
| `201 Created` | Nowy payment order zostal utworzony |
| `200 OK` | Odczyt OK albo idempotent replay istniejacego ordera |
| `403 Forbidden` | Token jest poprawny, ale rola nie pozwala wykonac operacji |
| `404 Not Found` | Zasob nie istnieje albo jest celowo zamaskowany przed innym tenantem |
| `409 Conflict` | Ten sam `Idempotency-Key` zostal uzyty z inna intencja |

## Dobra Kombinacja Danych W Testach REST

Minimalny zestaw dla `POST /payment-orders`:

| Element | Skad brac | Dlaczego |
|---|---|---|
| `merchantId` | `activeMerchant()` | Endpoint wymaga istniejacego aktywnego merchanta |
| `creatorToken` | `merchantPaymentCreatorToken(merchantId)` | Token musi pasowac do merchanta i operacji |
| `Idempotency-Key` | `uniqueIdempotencyKey(label)` | Kazda intencja create musi byc rozpoznawalna |
| `X-Correlation-ID` | jawny string w tescie | Asercja traceability |
| body | `createPaymentOrderBody(...)` | Dane biznesowe payment ordera |

Minimalny zestaw dla `GET /payment-orders/{id}`:

| Element | Skad brac | Dlaczego |
|---|---|---|
| `merchantId` | z setupu | Scope endpointu |
| `paymentOrderId` | z odpowiedzi create | Odczytujemy realnie utworzony zasob |
| reader token | merchant albo platform reader | Sprawdza model dostepu |
| `X-Correlation-ID` | jawny string | Potwierdza propagacje naglowka |

## Najczestsze Pulapki

| Pulapka | Objaw |
|---|---|
| Uzycie `readerToken` do create | `403 Forbidden` |
| Literowka w headerze `ETag` / `X-Correlation-ID` | Asercja headera pada |
| Literowka w JSON path, np. `clintOrderReference` | Asercja body pada |
| Porownanie liczby jako string, np. `"12_500"` | Asercja typu pada |
| Nowe body przy tym samym idempotency key | `409 Conflict` zamiast replay |
| Inny merchant w tokenie niz w path | `403` albo masked `404`, zalezne od endpointu |
| Brak aktywnego merchanta | create payment order nie powinien przejsc |

## Jak Czytac Taka Klase Testowa

Czytaj od danych do kontraktu:

```text
1. Jaki aktor? token/role/merchant_id
2. Jaki zasob? path params: merchantId, paymentOrderId
3. Jaka intencja? metoda HTTP + endpoint
4. Jakie headers? auth, content type, idempotency, correlation
5. Jakie body? amount, currency, client reference
6. Jaki oczekiwany status?
7. Jakie response headers potwierdzaja kontrakt?
8. Jakie pola JSON potwierdzaja zachowanie biznesowe?
```

## Esencja

Ta klasa nie jest tylko testem REST Assured. To lekcja projektowania kontraktu API.

Pokazuje, ze dobry test REST powinien sprawdzac:

| Obszar | Co sprawdzamy |
|---|---|
| HTTP | metoda, status, headers, body |
| REST | zasoby, Location, reprezentacja JSON |
| Security | role, scope merchanta, forbidden vs masked not found |
| Idempotency | retry bez duplikatu i conflict przy innej intencji |
| Observability | correlation id wraca w odpowiedzi |
| Data isolation | unikalne dane i tenant boundaries |
| Test design | jeden scenariusz = jedna konkretna regula biznesowo-techniczna |

Najkrotsza regula do zapamietania:

```text
REST Assured test ma udowodnic, ze klient HTTP dostaje poprawny kontrakt API dla konkretnego aktora, danych, headers i scenariusza ryzyka.
```
