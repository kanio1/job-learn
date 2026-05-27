---
type: session-summary
status: ready
area: REST API From Zero
project: Payment Quality Engineering Lab
date: 2026-05-24
tags:
  - session-summary
  - rest-api
  - rest-assured
  - java
  - merchant-registry
  - backend-testing
  - sdet
---

# REST, REST Assured, Java - Session Summary - Merchant API Tests

## Cel notatki

Ta notatka podsumowuje, czego nauczylismy sie podczas pracy z `MerchantRestAssuredTest` i kopia cwiczeniowa `MyMerchantRestAssuredTest`.

Zakres:

- REST API i kontrakt HTTP.
- REST Assured jako klient testowy.
- Java uzywana w testach API.
- Helpery testowe i dane testowe.
- Praktyczne znaczenie testu listy merchantow.

Powiazane pliki:

- `apps/backend/src/test/java/lab/paymentquality/rest/MerchantRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/MyMerchantRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/MerchantApiTestSupport.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/TestJwtSupport.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PostgresContainerSupport.java`

## 1. REST API - co utrwalilismy

REST test w tym module nie testuje UI. Test wysyla prawdziwy HTTP request do backendu i sprawdza odpowiedz.

Najwazniejszy mental model:

```text
REST Assured -> HTTP request -> Spring Security -> Controller -> Service -> Repository -> PostgreSQL -> HTTP response -> assertions
```

W Merchant API cwiczymy:

- `POST /api/merchants` - utworzenie merchanta.
- `GET /api/merchants/{id}` - odczyt merchanta po ID.
- `GET /api/merchants` - lista merchantow.
- `POST /api/merchants/{id}/activate` - aktywacja merchanta.
- `POST /api/merchants/{id}/suspend` - zawieszenie merchanta.

Status codes, ktore pojawily sie w rozmowie:

- `201 Created` - merchant zostal utworzony.
- `200 OK` - odczyt, lista albo zmiana statusu zakonczyla sie powodzeniem.
- `400 Bad Request` - niepoprawny request, np. malformed UUID albo validation error.
- `404 Not Found` - merchant o danym ID nie istnieje.
- `409 Conflict` - konflikt biznesowy, np. duplicate reference albo invalid transition.

Wazna zasada:

Test API powinien sprawdzac nie tylko status HTTP, ale tez sens odpowiedzi, np. `merchantReference`, `displayName`, `status`, `error`.

## 2. REST Assured - co utrwalilismy

REST Assured jest programowalnym klientem HTTP dla testow Java/JUnit. Nie jest czescia aplikacji produkcyjnej.

Podstawowy uklad:

```java
operatorRequest(port)
.when()
    .get("/api/merchants/{id}", id)
.then()
    .statusCode(200)
    .body("merchantId", equalTo(id));
```

Znaczenie krokow:

- `operatorRequest(port)` przygotowuje request z portem i tokenem operatora.
- `.when()` oznacza moment wykonania akcji HTTP.
- `.get(...)` albo `.post(...)` wysyla request.
- `.then()` przechodzi do asercji odpowiedzi.
- `.statusCode(...)` sprawdza HTTP status.
- `.body(...)` sprawdza pole w JSON response.
- `.extract().path(...)` wyciaga wartosc z JSON do zmiennej Java.

Przyklad create + extract:

```java
String id = createMerchant(reference, "Flow Merchant")
        .then()
        .statusCode(201)
        .body("merchantReference", equalTo(reference))
        .body("displayName", equalTo("Flow Merchant"))
        .body("status", equalTo("DRAFT"))
        .extract().path("merchantId");
```

Sens:

```text
create merchant -> sprawdz kontrakt response -> wyciagnij merchantId -> uzyj ID w kolejnych requestach
```

Wazna obserwacja:

- Literowka w JSON path, np. `displayNAme` zamiast `displayName`, powinna zepsuc test.
- To jest dobre, bo test chroni dokladny kontrakt response.

## 3. Auth w REST Assured

Metoda `.auth()` rozpoczyna konfiguracje uwierzytelnienia requestu.

W naszym projekcie najwazniejszy wariant to:

```java
publicRequest(port).auth().oauth2(token)
```

To dodaje naglowek:

```http
Authorization: Bearer <token>
```

W `MerchantApiTestSupport` mamy trzy poziomy requestow:

```java
publicRequest(port)       // bez tokena
operatorRequest(port)     // token platform operatora
requestWithToken(port, token) // dowolny token
```

Znaczenie testowe:

- bez tokena albo z invalid tokenem oczekujemy `401`,
- z tokenem bez wymaganej roli oczekujemy `403`,
- z poprawnym tokenem i rola request przechodzi do controllera.

## 4. `MerchantApiTestSupport` - po co istnieje

`MerchantApiTestSupport` to utility class dla testow. Ma usuwac powtarzalny techniczny szum, ale nie ukrywac sensu testu.

Najwazniejsze metody:

```java
public static RequestSpecification publicRequest(int port)
public static RequestSpecification operatorRequest(int port)
public static RequestSpecification requestWithToken(int port, String token)
public static Map<String, Object> createMerchantBody(String reference, String displayName)
public static String uniqueMerchantReference(String label)
```

Wazne dobre praktyki:

- helper moze ukryc port, token i standardowe body,
- helper nie powinien ukrywac endpointu, oczekiwanego statusu i kluczowych asercji,
- test nadal powinien mowic, jaki kontrakt API chroni.

## 5. `createMerchantBody` - Map i JSON body

Metoda:

```java
public static Map<String, Object> createMerchantBody(String reference, String displayName) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("merchantReference", reference);
    body.put("displayName", displayName);
    return Map.copyOf(body);
}
```

Tworzy dane requestu, ktore REST Assured serializuje do JSON:

```json
{
  "merchantReference": "MERCH-FLOW-12345678",
  "displayName": "Flow Merchant"
}
```

`Map<String, Object>` oznacza:

- klucz jest `String`, np. `merchantReference`,
- wartosc moze byc dowolnym obiektem, np. `String`, `Integer`, `Boolean`, nested `Map`.

W tym konkretnym body wystarczyloby `Map<String, String>`, ale `Map<String, Object>` jest bardziej elastyczne dla przyszlych JSON payloadow.

`LinkedHashMap` zachowuje kolejnosc dodawania pol, co pomaga w czytelnosci.

`Map.copyOf(body)` zwraca niemodyfikowalna kopie mapy. Po zbudowaniu body test nie powinien go przypadkowo zmieniac.

## 6. `uniqueMerchantReference` - unikalne dane testowe

Metoda:

```java
public static String uniqueMerchantReference(String label) {
    return "MERCH-" + label + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
}
```

Tworzy unikalny reference, np.:

```text
MERCH-FLOW-A1B2C3D4
```

Po co:

- testy nie koliduja ze soba,
- unikamy przypadkowego `409 duplicate_merchant_reference`,
- testy sa bardziej odporne na ponowne i rownolegle uruchomienia.

Rozmawialismy tez o `StringBuilder` i `StringBuffer`.

Wniosek:

- `StringBuilder` technicznie by zadzialal, ale nie jest tu potrzebny.
- `StringBuffer` tez by zadzialal, ale jest synchronizowany i jeszcze mniej potrzebny.
- Zwykla konkatenacja stringow jest tu najlepsza, bo kod jest krotki i czytelny.

Czytelniejsza wersja edukacyjna moglaby wygladac tak:

```java
public static String uniqueMerchantReference(String label) {
    String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    return "MERCH-" + label + "-" + randomSuffix;
}
```

## 7. Java - elementy, ktore cwiczylismy

### `final class` i prywatny konstruktor

`MerchantApiTestSupport` jest utility class:

```java
public final class MerchantApiTestSupport {
    private MerchantApiTestSupport() {
    }
}
```

Znaczenie:

- `final` blokuje dziedziczenie,
- prywatny konstruktor blokuje `new MerchantApiTestSupport()`,
- metody sa `static`, bo to pomocnicze funkcje testowe.

### `.class` w adnotacji

```java
@Import(TestJwtConfiguration.class)
```

`.class` przekazuje Springowi obiekt typu `Class<TestJwtConfiguration>`, czyli informacje o klasie. Spring sam tworzy i rejestruje konfiguracje w testowym kontekscie.

### `extends`

```java
class MerchantRestAssuredTest extends PostgresContainerSupport
```

Test dziedziczy wspolne metody konfiguracji PostgreSQL Testcontainers, np. `newPostgresContainer(...)` i `registerPostgresProperties(...)`.

### `private` helper method

```java
private Response createMerchant(String reference, String displayName)
```

Metoda jest `private`, bo jest lokalnym helperem tylko dla tej klasy testowej. To dobry kompromis, jesli helper usuwa powtarzalny request setup, ale nie ukrywa celu testu.

## 8. Adnotacje testowe, ktore omowilismy

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

Uruchamia pelny Spring Boot context i backend HTTP na losowym porcie.

```java
@ActiveProfiles("test")
```

Wlacza profil testowy.

```java
@Import(TestJwtConfiguration.class)
```

Dodaje testowa konfiguracje JWT, zeby nie trzeba bylo uruchamiac prawdziwego Keycloaka.

```java
@Testcontainers
```

Informuje JUnit/Testcontainers, ze test uzywa kontenerow.

```java
@Container
```

Oznacza konkretny kontener PostgreSQL zarzadzany przez Testcontainers.

```java
@DynamicPropertySource
```

Przekazuje Springowi dynamiczne properties do polaczenia z kontenerem PostgreSQL.

```java
@LocalServerPort
```

Wstrzykuje losowy port uruchomionej aplikacji testowej.

```java
@Disabled
```

W `MyMerchantRestAssuredTest` oznacza, ze klasa jest sandboxem do nauki i nie uruchamia sie automatycznie.

## 9. Endpointy jako stringi - dobra praktyka na teraz

Rozmawialismy o tym, czy endpointy powinny byc stringami, stalymi albo enumami.

Wniosek na teraz:

- inline stringi sa OK na poczatku, bo pokazuja kontrakt HTTP,
- dla powtarzanych endpointow warto uzyc `private static final String`,
- enum ma sens dopiero pozniej, np. przy security authorization matrix.

Praktyczny kompromis:

```java
private static final String MERCHANTS = "/api/merchants";
private static final String MERCHANT_BY_ID = "/api/merchants/{id}";
private static final String ACTIVATE_MERCHANT = "/api/merchants/{id}/activate";
private static final String SUSPEND_MERCHANT = "/api/merchants/{id}/suspend";
```

Effective Java lens:

- unikaj stringow tam, gdzie lepszy jest typ,
- ale nie tworz przedwczesnej abstrakcji,
- zachowaj minimalna widocznosc (`private static final`),
- test powinien nadal byc czytelny jako kontrakt HTTP.

## 10. Test `listReturnsSeededMerchantsNewestFirst`

Idea testu:

```text
Create A, B, C -> GET list -> expect C, B, A
```

Test sprawdza, czy `GET /api/merchants` zwraca merchantow od najnowszego do najstarszego.

Kluczowy fragment:

```java
List<Map<String, Object>> merchants = operatorRequest(port)
        .when().get("/api/merchants")
        .then().statusCode(200)
        .extract().path("merchants");

List<String> orderedReferences = merchants.stream()
        .map(row -> (String) row.get("merchantReference"))
        .filter(reference -> reference.startsWith(prefix))
        .toList();
```

Znaczenie:

- `extract().path("merchants")` wyciaga liste merchantow z JSON response.
- `List<Map<String, Object>>` oznacza liste obiektow JSON, gdzie kazdy merchant jest mapa pol.
- `stream()` zaczyna przetwarzanie listy.
- `map(...)` zamienia kazdy wiersz mapy na samo `merchantReference`.
- `filter(...)` zostawia tylko dane utworzone przez ten test.
- `toList()` zbiera wynik do listy stringow.

Ten stream:

```java
merchants.stream()
        .map(row -> (String) row.get("merchantReference"))
        .filter(reference -> reference.startsWith(prefix))
        .toList();
```

jest odpowiednikiem petli:

```java
List<String> orderedReferences = new ArrayList<>();

for (Map<String, Object> row : merchants) {
    String reference = (String) row.get("merchantReference");

    if (reference.startsWith(prefix)) {
        orderedReferences.add(reference);
    }
}
```

Na koncu:

```java
assertThat(orderedReferences).containsExactly(third, second, first);
```

sprawdza dokladna kolejnosc.

## 11. Co bylo najwazniejsze w tej sesji

- REST Assured testuje backend API bez UI.
- Test API powinien pokazywac method, endpoint, status code, body assertions i dane testowe.
- `createMerchant(...)` jest lokalnym helperem, ktory upraszcza powtarzalny `POST /api/merchants`.
- `operatorRequest(port)` przygotowuje request z tokenem operatora.
- `createMerchantBody(...)` buduje JSON body jako `Map<String, Object>`.
- `uniqueMerchantReference(...)` chroni testy przed kolizjami danych.
- `extract().path(...)` pozwala uzyc response z jednego requestu w kolejnym requestcie.
- Stream `map/filter/toList` pozwala przeksztalcic liste JSON rows na liste konkretnych wartosci.
- Na tym etapie proste stale endpointow sa lepsze niz enum framework.

## 12. Nastepne cwiczenia

1. W `MyMerchantRestAssuredTest` przepisz recznie test `createReadListActivateAndSuspendMerchant` bez patrzenia na oryginal przez pierwsze 5 minut.
2. Popraw albo celowo wprowadz literowke w JSON path, np. `displayNAme`, i przewidz jaki bedzie blad.
3. Przepisz `listReturnsSeededMerchantsNewestFirst` najpierw z petla `for`, potem ze streamem.
4. Dodaj lokalne stale endpointow jako `private static final String` i zastap nimi stringi w `MyMerchantRestAssuredTest`.
5. Dopiero po zrozumieniu flow usun `@Disabled` lokalnie i uruchom klase testowa.

## 13. Interview answer

**Question:** How do you structure REST Assured tests for backend APIs?

**Answer:** I keep the HTTP contract visible: method, endpoint, status code, response body and important headers. I use small helpers for repeated technical setup like port, auth token and request body, but I avoid hiding the scenario itself. I create unique test data, extract IDs from responses for multi-step flows and verify behavior at the API boundary while leaving domain and database-specific rules to lower-level tests when appropriate.
