# api-tests — framework testów black-box API (specyfikacja dla Claude Code CLI)

> Dokument implementacyjny dla agenta kodującego. Opisuje **co** zbudować, **jak dokładnie**
> i **dlaczego tak** — z twardymi inwariantami, pełną powierzchnią Rest Assured i AssertJ,
> kontraktem API do pokrycia oraz kolejnością prac.
> Szkielet (sygnatury + `TODO`) już istnieje — zadaniem jest implementacja, nie przeprojektowanie.
> Poniższe sekcje są **wiążące**: każda decyzja projektowa ma uzasadnienie i musi zostać zachowana.

---

## 1. Cel i zakres

Framework testuje backend `payment-quality-lab` jako **czarną skrzynkę**: zna go wyłącznie
przez HTTP. Stack (PostgreSQL 18 + Keycloak 26 + backend) wstaje przez **Testcontainers**;
backend trafia jako **gotowy obraz Dockera**, nie jest budowany ani linkowany przez ten moduł.

**TO JEST:** osobny moduł Mavena (`apps/api-tests/`), zależny tylko od Rest Assured /
Testcontainers / JUnit 5 / AssertJ / Awaitility.

**TO NIE JEST:** `@SpringBootTest`, MockMvc, ani test z backendem na classpathie.
Brak importów `lab.paymentquality.*` z `apps/backend`. Brak `restkit/`, `testsupport/`.

---

## 2. Stack i wersje

| Element | Wersja | Uwaga |
|---|---|---|
| JDK | 25.0.3 (LTS) | `maven.compiler.release=25`; `ScopedValue`/virtual threads finalne |
| Rest Assured | 6.0.0 | domyślny Jackson 2 po stronie testu (§11) |
| `json-schema-validator` | 6.0.0 | testy kontraktowe — musi współgrać z RA BOM |
| JUnit | 5.11.x (Jupiter) | failsafe, konwencja `*Spec` |
| Testcontainers BOM | 1.21.x | `testcontainers`, `postgresql`, `junit-jupiter` |
| Keycloak TC | `com.github.dasniko:testcontainers-keycloak:3.7.x` | Keycloak 26 |
| AssertJ | 3.27.x | asercje biznesowe; `AbstractAssert` dla `ProblemAssert` |
| Awaitility | 4.2.x | polling asynchronicznego audytu |

`pom.xml` musi importować `junit-bom` i `testcontainers-bom` przez `dependencyManagement`.
`maven-failsafe-plugin` uruchamia `*Spec` w fazie `integration-test/verify`.

---

## 3. Model uruchomienia

```
            ┌─────────────────── docker/podman network ──────────────────────┐
  test JVM  │  ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐    │
  (host)  ──┼─▶│   backend    │──▶│ postgres:18  │   │  keycloak:26     │    │
  RA + TC   │  │ (BACKEND_    │   └──────────────┘   │  (realm import)  │    │
            │  │   IMAGE)     │─────────────────────▶│  alias=keycloak  │    │
            │  └──────────────┘  issuer/jwks po aliasie └──────────────────┘  │
            └────────────────────────────────────────────────────────────────┘
  mint: test ─▶ keycloak (mappedPort z hosta) ─▶ JWT (sub, tenant_id,
                merchant_id, azp, realm_access.roles) ─▶ Authorization: Bearer …
```

Backend konfigurowany **wyłącznie przez env** (12-factor):

```
DB_URL          = jdbc:postgresql://postgres:5432/payment_quality_lab
DB_USER         = payment_quality
DB_PASSWORD     = payment_quality_dev
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI = http://keycloak:8080/realms/payment-quality
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI = http://keycloak:8080/realms/payment-quality/protocol/openid-connect/certs
EXPECTED_AZP    = payment-quality-dashboard
SPRING_PROFILES_ACTIVE = seed
```

`SPRING_PROFILES_ACTIVE=seed` aktywuje: `SeedRunner` (seed przy starcie), `TestController`
(`/api/test/seed|reset`, `permitAll`), `app.testing.enabled=true`.

Backend gotowy po `GET /api/status` == 200 (`Wait.forHttp("/api/status").forStatusCode(200)`).

Build obrazu (raz / w CI), test konsumuje tag przez env:

```bash
(cd ../backend && ./mvnw spring-boot:build-image \
  -Dspring-boot.build-image.imageName=payment-quality/backend:local)
export BACKEND_IMAGE=payment-quality/backend:local
./mvnw verify
```

Podman (Fedora 44, rootless):

```bash
systemctl --user enable --now podman.socket
export DOCKER_HOST="unix://$XDG_RUNTIME_DIR/podman/podman.sock"
export TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true
```

---

## 4. Architektura — 4 cienkie warstwy

```
WARSTWA          ODPOWIEDZIALNOŚĆ                     ZERO W SCENARIUSZU
─────────────────────────────────────────────────────────────────────────
core/stack       cykl życia kontenerów                given(), pathParam()
core/http        specyfikacje żądań i odpowiedzi       new RequestSpecBuilder()
api/*            DSL domenowy per zasób                contentType(), header()
scenarios/*      co testujemy (asercje end-to-end)     wszystko z powyższych
```

Wspólny klej: `Ctx` (`ScopedValue<TestContext>`) niesie tożsamość i `correlationId`.
Filtry HTTP czytają z `Ctx` i same wstrzykują `Authorization` oraz `X-Correlation-ID`.
Scenariusz nigdy nie dotyka nagłówków uwierzytelnienia ani korelacji ręcznie.

---

## 5. Drzewo katalogów

```
apps/api-tests/
├── pom.xml
├── Makefile
├── README.md
├── FRAMEWORK.md          ← TEN PLIK
└── src/test/
    ├── java/lab/paymentquality/apitest/
    │   ├── core/
    │   │   ├── stack/
    │   │   │   ├── BackendImage.java
    │   │   │   ├── PostgresSupport.java
    │   │   │   ├── KeycloakSupport.java
    │   │   │   ├── BackendSupport.java
    │   │   │   └── ApiStack.java
    │   │   ├── http/
    │   │   │   ├── ApiConfig.java
    │   │   │   ├── ContentTypes.java
    │   │   │   ├── Headers.java
    │   │   │   ├── RequestSpecs.java        ← NOWA NAZWA (było Specs.java)
    │   │   │   ├── ResponseSpecs.java       ← NOWY PLIK (był BRAK)
    │   │   │   ├── RestAssuredSetup.java
    │   │   │   ├── AuthFilter.java
    │   │   │   └── CorrelationFilter.java
    │   │   ├── auth/
    │   │   │   ├── Identity.java
    │   │   │   ├── Identities.java
    │   │   │   ├── TokenFactory.java
    │   │   │   └── KeycloakTokenFactory.java
    │   │   ├── context/
    │   │   │   ├── TestContext.java
    │   │   │   └── Ctx.java
    │   │   ├── problem/
    │   │   │   ├── ProblemDetail.java
    │   │   │   ├── ProblemCodes.java
    │   │   │   └── ProblemAssert.java      ← extends AbstractAssert (§9)
    │   │   ├── concurrency/
    │   │   │   ├── ETag.java
    │   │   │   ├── Versioned.java
    │   │   │   └── ConcurrencyHarness.java
    │   │   └── idempotency/
    │   │       └── IdempotencyKeys.java
    │   ├── api/
    │   │   ├── ApiClient.java
    │   │   ├── payment/
    │   │   │   ├── PaymentOrdersApi.java
    │   │   │   └── dto/
    │   │   │       ├── CreatePaymentOrder.java
    │   │   │       ├── PaymentOrderResponse.java
    │   │   │       └── PaymentSummaryQuery.java
    │   │   ├── merchant/
    │   │   │   ├── MerchantsApi.java
    │   │   │   └── dto/
    │   │   │       ├── CreateMerchant.java
    │   │   │       └── MerchantResponse.java
    │   │   ├── audit/
    │   │   │   └── AuditApi.java
    │   │   └── seed/
    │   │       └── SeedApi.java
    │   ├── support/
    │   │   ├── ApiTest.java
    │   │   ├── ApiStackExtension.java
    │   │   ├── SeedLifecycleExtension.java
    │   │   ├── ScopedContextExtension.java
    │   │   └── Eventually.java
    │   └── scenarios/
    │       ├── payment/
    │       │   ├── PaymentLifecycleSpec.java
    │       │   ├── IdempotencyReplaySpec.java
    │       │   └── OptimisticLockingSpec.java
    │       ├── tenant/
    │       │   └── TenantIsolationSpec.java
    │       └── audit/
    │           └── AuditTrailSpec.java
    └── resources/
        ├── junit-platform.properties
        ├── keycloak/
        │   └── payment-quality-realm.json
        └── schema/
            ├── payment-order.json           ← JSON Schema do walidacji kontraktu
            ├── payment-list.json
            ├── merchant.json
            └── problem.json
```

---

## 6. Inwarianty (MUST / MUST NOT)

Każde złamanie jest błędem implementacji, nie decyzją projektową do podjęcia.

- **MUST NOT** importować żadnej klasy z `apps/backend`. DTO odtworzone lokalnie w `api/*/dto`.
- **MUST NOT** używać `RestAssured.given()` w `scenarios/*` ani `api/*`. Wyłącznie przez `RequestSpecs`.
- **MUST NOT** ustawiać `Authorization` ani `X-Correlation-ID` ręcznie w scenariuszu.
- **MUST NOT** używać `ObjectMapperType.JACKSON_3` ani `jackson3ObjectMapperFactory`. Patrz §11.
- **MUST NOT** asercjonować audytu bezpośrednio po akcji (zawsze `Eventually`). Patrz §12.
- **MUST NOT** używać `RestAssured.requestSpecification`/`baseURI` jako pól statycznych globalnych
  — to współdzielony mutowalny stan, niebezpieczny przy równoległości. Wyłącznie `.spec(spec)`.
- **MUST** startować `ApiStack` dokładnie raz na sesję (`BeforeAllCallback` + root store).
- **MUST** trzymać bazowy `RequestSpecification` w `RequestSpecs` jako niemutowalny obiekt (zbudowany
  przez `RequestSpecBuilder.build()`, przypisany raz) i reużywać przez `given().spec(BASE)`.
- **MUST** każda asercja na odpowiedzi błędu przechodzić przez `ResponseSpecs.problemJson()`.
- **MUST** dla naruszenia granicy tenanta na **odczycie** oczekiwać **404**, nie 403
  (backend `enforceReadBoundary` rzuca `UserNotFoundException`/`PaymentOrderNotFoundException`).
- **MUST** dla naruszenia granicy tenanta na **zapisie** oczekiwać **403**
  (backend `enforceWriteBoundary` rzuca `TenantBoundaryViolationException`).
- **MUST** każda `Identity` mieć odpowiednika-usera w `payment-quality-realm.json`.
- **MUST** kolekcje deserializować przez `new TypeRef<List<T>>() {}`, nie `List.class`.
- **IGNORE** stare `restkit/`, `testsupport/` — nie czytać, nie migrować, nie wzorować się.

---

## 7. RequestSpecs — jeden niemutowalny szablon, kopie per żądanie

### Dlaczego `RequestSpecBuilder`, nie `given()` bezpośrednio

`RequestSpecification` z `given()` jest **mutowalny i stanowy**: doklejanie nagłówków mutuje obiekt.
Jeśli współdzielisz go między testami, headers wyciekają.
`RequestSpecBuilder.build()` produkuje niemutowalny szablon.
`given().spec(BASE)` tworzy **izolowaną kopię** szablonu — baza zostaje czysta.
To jest dokładna różnica, którą pyta rekruter: *builder = niemutowalny szablon, `given()` = mutowalna instancja*.

### Implementacja `RequestSpecs.java`

Stała `BASE` budowana raz w `RestAssuredSetup.install()` i przechowywana w `RequestSpecs`.
Wszystkie metody fabryczne zaczynają od `given().spec(BASE)`.

```java
public final class RequestSpecs {

    // niemutowalny szablon — przypisany raz przez RestAssuredSetup.install()
    static RequestSpecification BASE;

    /**
     * Baza: baseUri + accept/content JSON + AuthFilter + CorrelationFilter
     * + log-if-validation-fails (nie zaśmieca normalnych przebiegów).
     */
    public static RequestSpecification base() {
        return given().spec(BASE);
    }

    /**
     * Operacje tworzące: dorzuca Idempotency-Key.
     * Wymaga: POST /api/merchants/{id}/payment-orders i lifecycle actions.
     */
    public static RequestSpecification idempotent(String key) {
        return base().header(Headers.IDEMPOTENCY_KEY, key);
    }

    /**
     * Operacje warunkowe: dorzuca If-Match.
     * Wymaga: authorize / capture / cancel / refund / PATCH metadata.
     */
    public static RequestSpecification conditional(String ifMatch) {
        return base().header(Headers.IF_MATCH, ifMatch);
    }

    /**
     * Lifecycle: wymaga obu nagłówków jednocześnie.
     * Wywołanie: RequestSpecs.lifecycle(from.etag(), freshKey)
     */
    public static RequestSpecification lifecycle(String ifMatch, String idempotencyKey) {
        return base()
                .header(Headers.IF_MATCH, ifMatch)
                .header(Headers.IDEMPOTENCY_KEY, idempotencyKey);
    }

    /**
     * PATCH merge-patch: content-type musi być application/merge-patch+json,
     * bez doczepianego charsetu (EncoderConfig) — patrz §10.
     */
    public static RequestSpecification mergePatch(String ifMatch) {
        return base()
                .contentType(ContentTypes.MERGE_PATCH_JSON)
                .header(Headers.IF_MATCH, ifMatch);
    }

    /**
     * Bez tokenu — SeedApi, /api/status.
     * Pomija AuthFilter (nadpisując nagłówek lub tworząc spec bez filtra).
     */
    public static RequestSpecification anonymous() {
        // TODO: zbuduj przez oddzielny RequestSpecBuilder bez AuthFilter
        throw new UnsupportedOperationException("skeleton");
    }

    private RequestSpecs() {}
}
```

### Budowanie BASE w `RestAssuredSetup.install()`

```java
BASE = new RequestSpecBuilder()
        .setBaseUri(ApiConfig.fromStack().baseUri())
        .setContentType(ContentType.JSON)
        .setAccept(ContentType.JSON)
        .addFilter(new AuthFilter())
        .addFilter(new CorrelationFilter())
        .addFilter(new ErrorLoggingFilter())                    // loguje tylko błędy
        .setConfig(RestAssuredConfig.config()
            .encoderConfig(EncoderConfig.encoderConfig()
                // KRYTYCZNE: bez tego RA dokleji charset do merge-patch+json
                // "application/merge-patch+json; charset=UTF-8" != "application/merge-patch+json"
                .appendDefaultContentCharsetToContentTypeIfUndefined(false))
            .logConfig(LogConfig.logConfig()
                .enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL)
                .enablePrettyPrinting(true))
            .jsonConfig(JsonConfig.jsonConfig()
                // BigDecimal zamiast float — precyzja kwot finansowych
                .numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL)))
        .build();
```

---

## 8. ResponseSpecs — brakujący bliźniak (NOWY PLIK)

Powtarzanie asercji na `Vary`, `Cache-Control`, `X-Correlation-ID` w każdym teście to antywzorzec.
`ResponseSpecBuilder` buduje wielokrotnie używalny kontrakt odpowiedzi — to jest odpowiedź
seniora na pytanie „jak nie duplikujesz asercji nagłówków w 40 testach?".

### Implementacja `ResponseSpecs.java`

```java
public final class ResponseSpecs {

    /**
     * Baseline dla każdej odpowiedzi na zasobie wrażliwym (płatności, audyt, użytkownicy).
     * Asercjonuje: X-Correlation-ID obecny, Cache-Control: no-store, Vary zawiera Authorization.
     */
    public static ResponseSpecification sensitive() {
        return new ResponseSpecBuilder()
                .expectHeader(Headers.CORRELATION_ID, notNullValue())
                .expectHeader(Headers.CACHE_CONTROL, containsString("no-store"))
                .expectHeader(Headers.VARY, containsStringIgnoringCase("Authorization"))
                .build();
    }

    /**
     * Odpowiedzi błędów: content-type application/problem+json + sensitive().
     * Użycie: resp.then().spec(ResponseSpecs.problemJson()).statusCode(4xx)
     */
    public static ResponseSpecification problemJson() {
        return new ResponseSpecBuilder()
                .addResponseSpecification(sensitive())
                .expectContentType(ContentTypes.PROBLEM_JSON)
                .expectBody("correlationId", notNullValue())
                .expectBody("error", notNullValue())
                .expectBody("status", notNullValue())
                .build();
    }

    /**
     * Odpowiedzi akcji z If-Match: Vary musi zawierać If-Match ORAZ Authorization.
     */
    public static ResponseSpecification conditional() {
        return new ResponseSpecBuilder()
                .addResponseSpecification(sensitive())
                .expectHeader(Headers.VARY, containsStringIgnoringCase("If-Match"))
                .build();
    }

    /**
     * Odpowiedź create (POST z Idempotency-Key):
     * Vary musi zawierać Idempotency-Key + ETag obecny.
     */
    public static ResponseSpecification created() {
        return new ResponseSpecBuilder()
                .addResponseSpecification(sensitive())
                .expectHeader(Headers.VARY, containsStringIgnoringCase("Idempotency-Key"))
                .expectHeader(Headers.ETAG, matchesPattern("\"v\\d+\""))
                .build();
    }

    private ResponseSpecs() {}
}
```

### Wzorzec użycia w DSL (PaymentOrdersApi)

```java
// create: waliduje kontrakt odpowiedzi PRZED ekstrakcją body+ETagu
ExtractableResponse<Response> ex = given().spec(RequestSpecs.idempotent(key))
        .pathParam("merchantId", merchantId)
        .body(body)
        .post("/api/merchants/{merchantId}/payment-orders")
        .then()
        .spec(ResponseSpecs.created())    // kontrakt (nagłówki, content-type)
        .statusCode(anyOf(is(201), is(200)))
        .extract();

PaymentOrderResponse dto = ex.as(PaymentOrderResponse.class);
String etag = ex.header(Headers.ETAG);
return new Versioned<>(dto, etag);
```

---

## 9. Headers — pełna powierzchnia (request i response)

### Request side

```java
// pojedynczy nagłówek
given().spec(BASE).header("X-Custom", "value")

// wiele nagłówków z mapy
given().spec(BASE).headers(Map.of("A", "1", "B", "2"))

// multi-value (ten sam nagłówek wielokrotnie — rzadkie, ale istnieje)
given().spec(BASE).header(new Header("Accept", "application/json"))
                  .header(new Header("Accept", "application/problem+json"))

// auth — alternatywa dla filtra (nie stosuj, masz AuthFilter)
given().spec(BASE).auth().oauth2(token)
```

### Response side

```java
// pojedynczy
String etag = response.getHeader("ETag");

// kolekcja obiektów Headers (metadane)
Headers all = response.getHeaders();
boolean hasCorr = all.hasHeaderWithName("X-Correlation-ID");

// multi-value (Vary może mieć wiele wartości)
List<String> varies = response.getHeaders().getValues("Vary");

// asercja inline — Hamcrest matcher na wartości nagłówka
response.then()
        .header("ETag", matchesPattern("\"v\\d+\""))
        .header("Content-Type", containsString("application/json"))
        .header("Cache-Control", equalTo("no-store"));
```

### `Headers.java` — stałe (wszystkie używane w backendzie)

```java
public final class Headers {
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String IF_MATCH        = "If-Match";
    public static final String ETAG            = "ETag";
    public static final String CORRELATION_ID  = "X-Correlation-ID";
    public static final String VARY            = "Vary";
    public static final String CACHE_CONTROL   = "Cache-Control";
    public static final String ACCEPT_PATCH    = "Accept-Patch";
    public static final String CONTENT_TYPE    = "Content-Type";
    private Headers() {}
}
```

---

## 10. Body, metadata i ekstrakcja — pełna powierzchnia

### `.as(Class)` vs `.extract()` — kluczowa różnica

`.as(Class)` traci nagłówki. `.extract()` zachowuje wszystko. Zasada:
- **akcje zwracające `Versioned`** → zawsze `extract()` (potrzebujesz i DTO, i ETagu)
- **read-only, bez ETagu** → `.as(Class)` jest OK

```java
// Versioned (create, lifecycle):
ExtractableResponse<Response> ex = given().spec(RequestSpecs.lifecycle(from.etag(), key))
        .pathParam("merchantId", m).pathParam("paymentOrderId", p)
        .post("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize")
        .then().spec(ResponseSpecs.conditional()).statusCode(200).extract();
return new Versioned<>(ex.as(PaymentOrderResponse.class), ex.header(Headers.ETAG));

// proste GET (brak ETagu w asercji):
PaymentOrderResponse dto = given().spec(RequestSpecs.base())...get(...).then().statusCode(200)
                                  .extract().as(PaymentOrderResponse.class);
```

### Kolekcje — `TypeRef` (MUST)

`List.class` traci parametr generyczny. **Zawsze `TypeRef`:**

```java
// lista zamówień
List<PaymentOrderResponse> items = response.as(new TypeRef<List<PaymentOrderResponse>>() {});

// lista wpisów historii
List<StatusHistoryEntry> entries = response.jsonPath()
        .getList("content", StatusHistoryEntry.class);
```

### GPath / `jsonPath()` — ekstrakcja punktowa bez DTO

GPath to uproszczony XPath dla JSON. Przydatny gdy potrzebujesz jednego pola bez deserializacji.

```java
// skalary
long total     = response.jsonPath().getLong("totalElements");
String status  = response.jsonPath().getString("status");
UUID orderId   = UUID.fromString(response.jsonPath().getString("paymentOrderId"));

// zagnieżdżona lista (podsumowanie walutowe)
List<String> currencies = response.jsonPath().getList("byCurrency.currency");
long capturedTotal = response.jsonPath().getLong("byCurrency[0].totalAmountMinor");

// asercja inline przez Hamcrest (KONTRAKT — kształt danych):
response.then()
        .body("content.size()", greaterThan(0))
        .body("content.status", everyItem(equalTo("CREATED")))
        .body("totalElements", greaterThanOrEqualTo(1L))
        .body("content.paymentOrderId", everyItem(notNullValue()));
```

### Metadata — `@JsonRawValue` i `application/merge-patch+json`

Pole `metadata` na `PaymentOrder` jest w backendzie `@JsonRawValue` — osadzony obiekt JSON
(nie string). PATCH idzie jako `application/merge-patch+json`, kontrakt wymaga odrzucenia
nieznanych pól najwyższego poziomu (`unknown_top_level_field`).

```java
// PATCH metadata (RequestSpecs.mergePatch zapewnia prawidłowy content-type bez charsetu)
given().spec(RequestSpecs.mergePatch(from.etag()))
        .pathParam("merchantId", m).pathParam("paymentOrderId", p)
        .body(Map.of("metadata", Map.of("note", "vip", "channel", "web")))
        .patch("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}")
        .then().spec(ResponseSpecs.conditional()).statusCode(200)
        .body("metadata.note",    equalTo("vip"))     // GPath wchodzi w osadzony obiekt
        .body("metadata.channel", equalTo("web"));

// KONTRAKT: nieznane pole najwyższego poziomu odrzucone
given().spec(RequestSpecs.mergePatch(from.etag()))...
        .body(Map.of("metadata", Map.of(), "unknownField", "x"))
        .patch(...)
        .then().spec(ResponseSpecs.problemJson()).statusCode(400)
        .body("error", equalTo(ProblemCodes.UNKNOWN_TOP_LEVEL_FIELD));
```

### Logowanie warunkowe

```java
// NIE: .log().all() — zaśmieca logi przy wszystkich testach
// TAK: log tylko przy failu (skonfigurowane w RestAssuredSetup przez LogConfig)
//      + ErrorLoggingFilter dla błędów sieciowych
// Ręcznie dla debugowania (tymczasowo, nie na stałe):
given().spec(BASE).log().ifValidationFails(LogDetail.ALL)
```

### Response time (wiedzieć na interview, nie stosować jako główne asercje)

```java
long ms = response.time();                            // milisekundy
long ns = response.timeIn(TimeUnit.NANOSECONDS);
// UWAGA: wymaga rozgrzanego JVM; RA explicite dokumentuje, że nie jest
// miarą czasu przetwarzania serwera (obejmuje round-trip + narzut RA)
```

---

## 11. JSON Schema validation — testy kontraktowe (BRAKUJĄCY ELEMENT)

**JSON Schema validation to filar testów kontraktowych w Rest Assured.** Jednym matcherem
waliduje całą strukturę odpowiedzi: typy pól, wymagalność, enum-y, formaty (uuid, date-time).
Odpowiedź seniora na pytanie „jak testujesz kontrakt API?" musi zawierać JSON Schema.

Zależność (`json-schema-validator` musi współgrać z RA 6.0.0):

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>json-schema-validator</artifactId>
    <version>${restassured.version}</version>
    <scope>test</scope>
</dependency>
```

Schematy w `src/test/resources/schema/*.json`.

### `payment-order.json` (przykład; uzupełnij wg realnych pól DTO)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "PaymentOrder",
  "type": "object",
  "required": ["paymentOrderId","merchantId","status","amountMinor","currency","createdAt","updatedAt"],
  "additionalProperties": true,
  "properties": {
    "paymentOrderId":        { "type": "string", "format": "uuid" },
    "merchantId":            { "type": "string", "format": "uuid" },
    "status":                { "type": "string", "enum": ["CREATED","AUTHORIZED","CAPTURED","CANCELLED","EXPIRED","REFUNDED"] },
    "amountMinor":           { "type": "integer", "minimum": 1, "maximum": 100000000 },
    "currency":              { "type": "string", "enum": ["PLN","EUR","USD"] },
    "capturedAmountMinor":   { "type": ["integer","null"] },
    "refundedAmountMinor":   { "type": ["integer","null"] },
    "createdAt":             { "type": "string", "format": "date-time" },
    "updatedAt":             { "type": "string", "format": "date-time" }
  }
}
```

### `problem.json`

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ProblemDetail",
  "type": "object",
  "required": ["type","title","status","error","correlationId"],
  "properties": {
    "type":          { "type": "string", "format": "uri" },
    "title":         { "type": "string" },
    "status":        { "type": "integer" },
    "detail":        { "type": "string" },
    "error":         { "type": "string" },
    "correlationId": { "type": "string" },
    "details": {
      "type": ["array","null"],
      "items": {
        "type": "object",
        "required": ["field","message"],
        "properties": {
          "field":   { "type": "string" },
          "message": { "type": "string" }
        }
      }
    }
  }
}
```

### Użycie w scenariuszach

```java
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

// KONTRAKT: kształt odpowiedzi (bez semantyki, tylko struktura)
response.then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schema/payment-order.json"));

// Połączenie kontraktu i biznesu w jednym then():
response.then()
        .statusCode(201)
        .spec(ResponseSpecs.created())
        .body(matchesJsonSchemaInClasspath("schema/payment-order.json"))  // KONTRAKT
        .body("status", equalTo("CREATED"))                               // BIZNES (inline)
        .body("currency", equalTo("PLN"));                                // BIZNES (inline)
```

---

## 12. `ProblemAssert` — `extends AbstractAssert` (KONIECZNA ZMIANA)

Obecny `ProblemAssert` z własnymi metodami fluent jest poprawny, ale nie integruje się
z ekosystemem AssertJ: brak `as()`, `describedAs()`, `SoftAssertions`, czytelnych komunikatów.
**`AbstractAssert` daje to za darmo.** To klasyczne pytanie interviewowe: „czy umiesz pisać
custom assertions, a nie tylko ich używać?"

### Implementacja

```java
public final class ProblemAssert extends AbstractAssert<ProblemAssert, Response> {

    private ProblemAssert(Response actual) {
        super(actual, ProblemAssert.class);
    }

    public static ProblemAssert assertThat(Response response) {
        return new ProblemAssert(response);
    }

    public ProblemAssert hasStatus(int expected) {
        isNotNull();
        int actual = this.actual.statusCode();
        if (actual != expected)
            failWithMessage("Oczekiwano statusu <%d>, było <%d>. Body: %s",
                            expected, actual, this.actual.body().asString());
        return this;
    }

    public ProblemAssert hasContentTypeProblemJson() {
        isNotNull();
        String ct = this.actual.contentType();
        if (!ct.contains(ContentTypes.PROBLEM_JSON))
            failWithMessage("Oczekiwano Content-Type <%s>, było <%s>",
                            ContentTypes.PROBLEM_JSON, ct);
        return this;
    }

    public ProblemAssert hasError(String expectedCode) {
        isNotNull();
        String actual = this.actual.jsonPath().getString("error");
        if (!expectedCode.equals(actual))
            failWithMessage("Oczekiwano error <%s>, było <%s>. Body: %s",
                            expectedCode, actual, this.actual.body().asString());
        return this;
    }

    /** Sprawdza, że correlationId w body == correlationId w nagłówku X-Correlation-ID. */
    public ProblemAssert hasCorrelationIdConsistent() {
        isNotNull();
        String inBody   = this.actual.jsonPath().getString("correlationId");
        String inHeader = this.actual.header(Headers.CORRELATION_ID);
        if (inBody == null || !inBody.equals(inHeader))
            failWithMessage("correlationId niespójny: body=<%s>, nagłówek=<%s>",
                            inBody, inHeader);
        return this;
    }

    public ProblemAssert hasNoStore() {
        isNotNull();
        String cc = this.actual.header(Headers.CACHE_CONTROL);
        if (cc == null || !cc.contains("no-store"))
            failWithMessage("Oczekiwano Cache-Control zawierającego 'no-store', było <%s>", cc);
        return this;
    }

    public ProblemAssert varyContains(String headerName) {
        isNotNull();
        String vary = this.actual.header(Headers.VARY);
        if (vary == null || !vary.toLowerCase().contains(headerName.toLowerCase()))
            failWithMessage("Oczekiwano Vary zawierającego <%s>, było <%s>", headerName, vary);
        return this;
    }

    public ProblemAssert hasFieldError(String field) {
        isNotNull();
        List<String> fields = this.actual.jsonPath().getList("details.field");
        if (fields == null || !fields.contains(field))
            failWithMessage("Oczekiwano błędu pola <%s> w details, było <%s>", field, fields);
        return this;
    }

    public ProblemAssert matchesProblemSchema() {
        isNotNull();
        this.actual.then().body(matchesJsonSchemaInClasspath("schema/problem.json"));
        return this;
    }
}
```

### Wzorzec użycia w scenariuszu

```java
// wszystko w jednym łańcuchu, zrozumiałe komunikaty błędów przy failu:
ProblemAssert.assertThat(response)
        .hasStatus(409)
        .hasContentTypeProblemJson()
        .hasError(ProblemCodes.IDEMPOTENCY_CONFLICT)
        .hasCorrelationIdConsistent()
        .hasNoStore()
        .varyContains("Idempotency-Key")
        .matchesProblemSchema();
```

---

## 13. AssertJ — głębsze cięcia (senior/SDET)

### `usingRecursiveComparison()` — idempotency replay

Replay musi zwrócić **identyczne** body co oryginał. Pola czasu-serwera mogą się różnić.

```java
Versioned<PaymentOrderResponse> original = api.payments().create(mid, body, key1);
Versioned<PaymentOrderResponse> replay   = api.payments().create(mid, body, key1); // ten sam klucz

assertThat(replay.body())
        .usingRecursiveComparison()
        .ignoringFields("updatedAt")   // serwer może zmienić updatedAt przy replay
        .isEqualTo(original.body());
```

### `SoftAssertions` / `assertSoftly` — zbieraj wszystkie błędy

Przy walidacji wielu pól odpowiedzi (np. cały summary report):

```java
assertSoftly(soft -> {
    soft.assertThat(summary.totalOrders()).isEqualTo(6L);
    soft.assertThat(summary.byCurrency()).hasSize(3);
    soft.assertThat(summary.byStatus()).extracting("status")
        .containsExactlyInAnyOrder("CREATED","AUTHORIZED","CAPTURED","CANCELLED","REFUNDED");
});
```

### `extracting` + `filteredOn` — kolekcje (historia, audyt, lista)

```java
List<StatusHistoryEntry> history = ...;

// historia musi zawierać przejście CREATED->AUTHORIZED
assertThat(history)
        .filteredOn(e -> "AUTHORIZE".equals(e.action()))
        .hasSize(1)
        .extracting("fromStatus", "toStatus")
        .containsExactly(tuple("CREATED", "AUTHORIZED"));

// wpisy audytu:
assertThat(auditEntries)
        .extracting("action")
        .containsExactlyInAnyOrder("MERCHANT_CREATED", "PAYMENT_AUTHORIZED", "PAYMENT_CAPTURED");
```

### `as("opis")` — czytelne komunikaty przy failu

```java
assertThat(captured.status())
        .as("status po capture musi być CAPTURED")
        .isEqualTo("CAPTURED");

assertThat(captured.capturedAmountMinor())
        .as("capturedAmountMinor <= amountMinor")
        .isLessThanOrEqualTo(captured.amountMinor());
```

### `InstanceOfAssertFactories` — nawigacja po typach zagnieżdżonych

```java
assertThat(response)
        .extracting(PaymentOrderResponse::capturedAmountMinor,
                    InstanceOfAssertFactories.LONG)
        .isBetween(1L, original.amountMinor());
```

### `satisfies` — złożone warunki na jednym obiekcie

```java
assertThat(order).satisfies(o -> {
    assertThat(o.status()).isEqualTo("CAPTURED");
    assertThat(o.capturedAmountMinor()).isNotNull().isPositive();
    assertThat(o.capturedAt()).isAfter(o.authorizedAt());
});
```

---

## 14. Dwa style asercji — kontrakt vs biznes (podział obowiązków)

Rozdzielaj mentalnie i technicznie. Oba są potrzebne, każdy ma inne narzędzie.

```
ASERCJA KONTRAKTOWA                     ASERCJA BIZNESOWA
────────────────────────────────────────────────────────────
kształt: typ, pole, enum, format        znaczenie: czy wynik jest poprawny
niezależna od logiki dziedzinowej       zależna od reguł biznesowych
narzędzie: Hamcrest inline / Schema     narzędzie: AssertJ na DTO
co testuje: "czy API mówi po protokole" co testuje: "czy workflow działa"
────────────────────────────────────────────────────────────
.body("status", anyOf(is("CREATED"),…)) assertThat(dto.status()).isEqualTo("CAPTURED")
matchesJsonSchemaInClasspath(…)         assertThat(dto.capturedAmountMinor()).isEqualTo(3300L)
.header("ETag", matchesPattern(…))      assertThat(ETag.version(etag2) > ETag.version(etag1))
ProblemAssert.hasError(CODE)            assertThat(replay).usingRecursiveComparison().isEqualTo(orig)
```

### Wzorcowy scenariusz (obydwa style obok siebie)

```java
@Test
void authorize_afterCreate_statusBecomesAuthorized_andAuditEventuallyAppears() {
    // ARRANGE — tożsamość z Ctx (filtr wstrzyknie token)
    Versioned<PaymentOrderResponse> created = api.payments()
            .create(MERCHANT_ALPHA_001_ID, new CreatePaymentOrder(2200L,"EUR","ORD-001"),
                    IdempotencyKeys.fresh());

    // ACT
    Versioned<PaymentOrderResponse> authorized = api.payments()
            .authorize(MERCHANT_ALPHA_001_ID, created.body().paymentOrderId(),
                       created, IdempotencyKeys.fresh());

    // ASSERT — kontrakt (inline Hamcrest na Response; schema)
    // (realizowane wewnątrz PaymentOrdersApi.authorize przez ResponseSpecs.conditional())

    // ASSERT — biznes (AssertJ na DTO)
    assertThat(authorized.body().status())
            .as("status po authorize").isEqualTo("AUTHORIZED");
    assertThat(ETag.version(authorized.etag()))
            .as("ETag musi wzrosnąć po mutacji")
            .isGreaterThan(ETag.version(created.etag()));

    // ASSERT — audyt async (Eventually + AssertJ)
    Eventually.await(Duration.ofSeconds(10), () ->
        api.audit().listByCorrelation(Ctx.current().correlationId())
    ).satisfies(auditResp -> {
        List<String> actions = auditResp.jsonPath().getList("content.action");
        assertThat(actions).contains("PAYMENT_AUTHORIZED");
    });
}
```

---

## 15. Kontrakt błędu — pełna specyfikacja

Każda odpowiedź błędna z backendu (wszystkie `@RestControllerAdvice`) ma ten kształt:

```json
{
  "type":          "https://api.payment-quality.local/problems/{error-kebab}",
  "title":         "Bad Request",
  "status":        400,
  "detail":        "Czytelny opis",
  "code":          "VALIDATION",
  "correlationId": "uuid",
  "error":         "validation",
  "message":       "Czytelny opis",
  "details": [{ "field": "amountMinor", "message": "must be at least 1" }]
}
```

Tabela kodów błędów (pełna, wyczerpująca — bez literałów w scenariuszach):

| `ProblemCodes.STAŁA` | wartość | HTTP | kiedy |
|---|---|---|---|
| `VALIDATION` | `validation` | 400 | walidacja bean/request |
| `MALFORMED_JSON` | `malformed_json` | 400 | zły JSON w body |
| `UNKNOWN_TOP_LEVEL_FIELD` | `unknown_top_level_field` | 400 | nieznane pole (create payment, PATCH metadata) |
| `MALFORMED_IF_MATCH` | `malformed_if_match` | 400 | If-Match ≠ `"v{n}"` |
| `MISSING_REQUIRED_HEADER` | `missing_required_header` | 400 | brak wymaganego nagłówka |
| `FORBIDDEN` | `forbidden` | 403 | brak uprawnień lub naruszenie granicy tenanta (zapis) |
| `NOT_FOUND` | `not_found` | 404 | zasób nie istnieje lub odczyt cudzego tenanta |
| `IDEMPOTENCY_CONFLICT` | `idempotency_conflict` | 409 | ten sam klucz, inny fingerprint |
| `MERCHANT_NOT_ELIGIBLE` | `merchant_not_payment_eligible` | 409 | merchant nie ACTIVE |
| `DUPLICATE_MERCHANT` | `duplicate_merchant_reference` | 409 | duplikat referencji |
| `PRECONDITION_REQUIRED` | `precondition_required` | 428 | brak If-Match |
| `CONCURRENCY_CONFLICT` | `concurrency_conflict` | 412 | optimistic lock JPA |
| `VERSION_MISMATCH` | `payment_order_version_mismatch` | 412 | nieaktualna wersja ETag |
| `INVALID_TRANSITION` | `invalid_transition` | 422 | złe przejście statusu |
| `AUTHORIZATION_EXPIRED` | `authorization_expired` | 422 | auth wygasła |
| `CAPTURE_AMOUNT_EXCEEDS` | `capture_amount_exceeds_authorized` | 422 | kwota capture > authorized |
| `REFUND_AMOUNT_EXCEEDS` | `refund_amount_exceeds_captured` | 422 | kwota refund > captured |
| `UNSUPPORTED_MEDIA_TYPE` | `unsupported_media_type` | 415 | zły Content-Type |
| `NOT_ACCEPTABLE` | `not_acceptable` | 406 | Accept nie obejmuje JSON |
| `METHOD_NOT_ALLOWED` | `method_not_allowed` | 405 | niedozwolona metoda HTTP |

---

## 16. Implementacja plik po pliku

### `core/stack`

**BackendImage.java**
- `resolve()` → `DockerImageName.parse(env.getOrDefault("BACKEND_IMAGE","payment-quality/backend:local"))`
- Jeśli obrazu brak w daemonie, rzuć `IllegalStateException` z instrukcją `make backend-image`.

**PostgresSupport.java**
```java
new PostgreSQLContainer<>("postgres:18")
        .withNetwork(network)
        .withNetworkAliases("postgres")
        .withDatabaseName("payment_quality_lab")
        .withUsername("payment_quality")
        .withPassword("payment_quality_dev");
```

**KeycloakSupport.java** ← NAJTRUDNIEJSZY; patrz §17 (issuer mismatch)
```java
new KeycloakContainer("quay.io/keycloak/keycloak:26.x.x")
        .withNetwork(network)
        .withNetworkAliases("keycloak")
        .withRealmImportFile("/keycloak/payment-quality-realm.json");
// + konfiguracja hostname frontendu (§17)
```
Metody: `internalIssuerUri()` (dla backendu, po aliasie), `hostTokenEndpoint(kc)` (dla testu, po porcie).

**BackendSupport.java**
```java
new GenericContainer<>(BackendImage.resolve())
        .withNetwork(network)
        .withExposedPorts(8080)
        .withEnv("DB_URL", "jdbc:postgresql://postgres:5432/payment_quality_lab")
        .withEnv("DB_USER", "payment_quality")
        .withEnv("DB_PASSWORD", "payment_quality_dev")
        .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI",
                  KeycloakSupport.internalIssuerUri())
        .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI",
                  KeycloakSupport.internalIssuerUri() + "/protocol/openid-connect/certs")
        .withEnv("EXPECTED_AZP", "payment-quality-dashboard")
        .withEnv("SPRING_PROFILES_ACTIVE", "seed")
        .waitingFor(Wait.forHttp("/api/status").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(90)));
```

**ApiStack.java** — singleton, start raz
```java
private static volatile ApiStack INSTANCE;

public static ApiStack shared() {
    if (INSTANCE == null) synchronized (ApiStack.class) {
        if (INSTANCE == null) { INSTANCE = new ApiStack(); INSTANCE.start(); }
    }
    return INSTANCE;
}

private void start() {
    network   = Network.newNetwork();
    postgres  = PostgresSupport.create(network);  postgres.start();
    keycloak  = KeycloakSupport.create(network);  keycloak.start();
    backend   = BackendSupport.create(network);   backend.start();
}

public String backendBaseUri()  { return "http://localhost:" + backend.getMappedPort(8080); }
public String tokenEndpoint()   { return KeycloakSupport.hostTokenEndpoint(keycloak); }
```

### `core/http`

**RestAssuredSetup.java** — `install()` wywołane raz przez `ApiStackExtension`
- Zbuduj `BASE` przez `RequestSpecBuilder` (§7).
- `RestAssured.registerParser(ContentTypes.PROBLEM_JSON, Parser.JSON)` — KRYTYCZNE: bez tego
  `response.then().body(...)` na błędzie rzuca `IllegalArgumentException: Unable to parse`.

**AuthFilter.java**
```java
Identity id = Ctx.current().identity();
if (id != null && id != Identity.ANONYMOUS) {
    String token = TokenFactory.instance().mint(id);
    req.removeHeader("Authorization");
    req.addHeader("Authorization", "Bearer " + token);
}
return ctx.next(req, res);
```

**CorrelationFilter.java**
```java
req.removeHeader(Headers.CORRELATION_ID);
req.addHeader(Headers.CORRELATION_ID, Ctx.current().correlationId());
return ctx.next(req, res);
```

### `core/auth`

**Identity.java** — zamień `ANONYMOUS = null` na:
```java
public static final Identity ANONYMOUS = new Identity("anonymous","anonymous",null,null,Set.of());
```

**Identities.java** — wypełnij spójnie z `payment-quality-realm.json`:
```java
public static final Identity PLATFORM_ADMIN = new Identity(
    "platform-admin", "platform-admin", "PLATFORM_TENANT", null,
    Set.of("platform:merchants:create","platform:merchants:read","platform:merchants:update-status",
           "platform:payments:read","platform:payments:lifecycle","platform:audit:read",
           "platform:users:read","platform:users:create","platform:users:update","platform:users:assign-roles"));

public static final Identity MERCHANT_ALPHA_001_OPERATOR = new Identity(
    "merchant-alpha-001-op", "merchant-alpha-001-op", "TENANT_ALPHA",
    "00000000-0000-0000-0000-0000000000b1",  // MERCHANT_ALPHA_001_ID z Fixtures
    Set.of("merchant:payments:create","merchant:payments:read","merchant:payments:lifecycle"));

public static final Identity MERCHANT_ALPHA_001_READER = new Identity(
    "merchant-alpha-001-ro", "merchant-alpha-001-ro", "TENANT_ALPHA",
    "00000000-0000-0000-0000-0000000000b1",
    Set.of("merchant:payments:read"));

public static final Identity PLATFORM_PAYMENTS_READER = new Identity(
    "platform-reader", "platform-reader", "PLATFORM_TENANT", null,
    Set.of("platform:payments:read","platform:payments:audit"));

public static final Identity TENANT_ALPHA_AUDIT_READER = new Identity(
    "alpha-audit", "alpha-audit", "TENANT_ALPHA", null,
    Set.of("tenant:audit:read"));

public static final Identity TENANT_ALPHA_USERS_ADMIN = new Identity(
    "alpha-users-admin", "alpha-users-admin", "TENANT_ALPHA", null,
    Set.of("tenant:users:read","tenant:users:create","tenant:users:update","tenant:users:assign-roles"));
```

**KeycloakTokenFactory.java**
```java
// grant_type=password, user z realm-importu odpowiadający Identity.preferredUsername
// cache: Map<Identity, (token, expiresAt)>; odśwież 30s przed wygaśnięciem
// token endpoint: ApiStack.shared().tokenEndpoint()
private String fetchToken(Identity identity) {
    return given().contentType("application/x-www-form-urlencoded")
            .formParam("grant_type",    "password")
            .formParam("client_id",     "payment-quality-dashboard")
            .formParam("client_secret", "<secret z realm-importu>")
            .formParam("username",      identity.preferredUsername())
            .formParam("password",      "<hasło z realm-importu>")
            .formParam("scope",         "openid")
            .post(ApiStack.shared().tokenEndpoint())
            .then().statusCode(200)
            .extract().jsonPath().getString("access_token");
}
```

### `core/problem` — gotowe po implementacji §12

### `core/concurrency`

**ETag.java**
```java
public static long version(String etag) {
    // etag ma format "v123" (z cudzysłowami)
    Matcher m = Pattern.compile("^\"v(\\d+)\"$").matcher(etag.trim());
    if (!m.matches()) throw new IllegalArgumentException("Nieprawidłowy ETag: " + etag);
    return Long.parseLong(m.group(1));
}
```

**ConcurrencyHarness.java**
```java
public static <T> List<T> raceN(int n, Callable<T> action) {
    // ScopedValue dziedziczy do wątków potomnych przez where(...).call(...)
    TestContext ctx = Ctx.current();
    List<Callable<T>> tasks = Collections.nCopies(n,
            () -> ScopedValue.where(/* CURRENT */, ctx).call(action));
    try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
        return exec.invokeAll(tasks).stream()
                   .map(f -> { try { return f.get(); }
                               catch (Exception e) { throw new RuntimeException(e); }})
                   .toList();
    }
}
```

### `api/payment/PaymentOrdersApi.java`

Każda metoda DSL:
1. Buduje spec przez `RequestSpecs.*`.
2. Wykonuje żądanie (ścieżki przez `pathParam`).
3. Waliduje kontrakt odpowiedzi przez `ResponseSpecs.*` PRZED ekstrakcją.
4. Zwraca `Versioned<DTO>` (ekstrakcja przez `.extract()`) lub `Response` (`Raw`).

```java
public Versioned<PaymentOrderResponse> authorize(UUID merchantId, UUID orderId,
                                                 Versioned<?> from, String idempotencyKey) {
    ExtractableResponse<Response> ex =
            given().spec(RequestSpecs.lifecycle(from.etag(), idempotencyKey))
                   .pathParam("merchantId", merchantId)
                   .pathParam("paymentOrderId", orderId)
                   .contentType(ContentType.JSON)
                   .body("{}")  // AuthorizeRequest z opcjonalnym reason; {} = brak reason
                   .post("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize")
                   .then()
                   .spec(ResponseSpecs.conditional())  // kontrakt nagłówków
                   .statusCode(200)
                   .body(matchesJsonSchemaInClasspath("schema/payment-order.json"))  // kontrakt struktury
                   .extract();
    return new Versioned<>(ex.as(PaymentOrderResponse.class), ex.header(Headers.ETAG));
}
```

### `support`

**ScopedContextExtension.java** — dodaj `as(Identity)` helper dostępny w scenariuszach:

```java
// W ScopedContextExtension:
public static void as(Identity identity, ThrowingRunnable block) throws Throwable {
    TestContext current = Ctx.current();
    Ctx.runWith(new TestContext(identity, current.correlationId()), block::run);
}
```

Użycie w scenariuszu:
```java
ScopedContextExtension.as(Identities.PLATFORM_PAYMENTS_READER, () -> {
    Response r = api.payments().getRaw(MERCHANT_ALPHA_001_ID, orderId);
    assertThat(r.statusCode()).isEqualTo(200);
});
```

**Eventually.java**
```java
public static <T> T await(Duration timeout, Supplier<T> probe) {
    AtomicReference<T> result = new AtomicReference<>();
    Awaitility.await()
            .atMost(timeout)
            .pollInterval(Duration.ofMillis(500))
            .pollDelay(Duration.ofMillis(200))
            .until(() -> { result.set(probe.get()); return true; });
    return result.get();
}

// Wariant z warunkiem zatrzymania (audyt: czekaj aż wpis istnieje)
public static void awaitUntil(Duration timeout, Callable<Boolean> condition) {
    Awaitility.await()
            .atMost(timeout)
            .pollInterval(Duration.ofMillis(500))
            .until(condition);
}
```

---

## 17. Expert catches (NIE POMIJAJ)

### 1. Issuer mismatch — najczęstsza przyczyna 401 przy Keycloak w Testcontainers

**Problem:** token mintowany przez test niesie `iss = http://keycloak:8080/realms/payment-quality`
(hostname z wnętrza sieci). Backend próbuje zwalidować `iss` przeciwko `issuer-uri` z env.
Jeśli `issuer-uri = http://localhost:XXXXX/realms/payment-quality` (port zmapowany), walidacja
**zawsze zawiedzie** — `iss` ≠ `issuer-uri`.

**Rozwiązanie:** ustaw stały frontend URL Keycloaka na alias sieciowy, podaj go jako `issuer-uri`
backendowi, i użyj portu zmapowanego tylko do mintowania tokenu (nie zmienia `iss`).

```java
// KeycloakSupport.create():
new KeycloakContainer("quay.io/keycloak/keycloak:26.x.x")
        .withNetwork(network)
        .withNetworkAliases("keycloak")
        .withRealmImportFile(REALM_IMPORT)
        .withEnv("KC_HOSTNAME", "keycloak")         // frontend URL = alias
        .withEnv("KC_HOSTNAME_PORT", "8080")
        .withEnv("KC_HOSTNAME_STRICT", "false")     // pozwól na dostęp z hosta przez mappedPort
        .withEnv("KC_HTTP_ENABLED", "true");

// internalIssuerUri() — dla backendu (w sieci po aliasie):
static String internalIssuerUri() {
    return "http://keycloak:8080/realms/payment-quality";
}

// hostTokenEndpoint() — dla testu (z hosta przez mappedPort):
static String hostTokenEndpoint(KeycloakContainer kc) {
    return "http://localhost:" + kc.getMappedPort(8080)
           + "/realms/payment-quality/protocol/openid-connect/token";
    // token niesie iss=http://keycloak:8080/... — zgodne z issuer-uri backendu
}
```

### 2. Audyt async — zawsze Eventually

`AuditEventListener` (`@ApplicationModuleListener`) działa po commicie transakcji backendu.
Sprawdzenie audytu 1ms po wywołaniu akcji = flaky test. Zawsze:

```java
Eventually.awaitUntil(Duration.ofSeconds(10), () -> {
    Response r = api.audit().listByCorrelation(Ctx.current().correlationId());
    List<String> actions = r.jsonPath().getList("content.action");
    return actions != null && actions.contains("PAYMENT_AUTHORIZED");
});
```

### 3. Jackson — strona testu na Jackson 2 (celowo)

Backend serializuje przez Jackson 3 (`tools.jackson`). Po drucie leci zwykły tekst JSON.
Strona testu **nie używa** `ObjectMapperType.JACKSON_3` — w RA 6.0.0 `response.jsonPath()`
przy Jackson 3 cofa się do Jackson 2 (bug #1857, `jackson3ObjectMapperFactory` nie
przekazywany do `JsonPathConfig`). Zostanie naprawione w kolejnym release.
Zostań na Jackson 2 w testach — unikasz problemu i zachowujesz separację: test nie
współdzieli `ObjectMappera` z aplikacją (co mogłoby fałszować wyniki przez własną konfigurację).

### 4. EncoderConfig i merge-patch+json

Domyślnie RA dokleji charset: `application/merge-patch+json; charset=UTF-8`.
Backend może odrzucić (`415 Unsupported Media Type`) lub nie dopasować content-type.
`EncoderConfig.appendDefaultContentCharsetToContentTypeIfUndefined(false)` w `RestAssuredSetup`
rozwiązuje to globalnie. `RequestSpecs.mergePatch()` potem ustawia typ bez charsetu.

### 5. Parsowanie problem+json — krytyczne

Bez `RestAssured.registerParser("application/problem+json", Parser.JSON)` wywołanie
`response.then().body(...)` na odpowiedzi błędnej **rzuca wyjątek** zamiast failować asercję.
RA nie wie, że to JSON. Rejestracja jest obowiązkowa w `RestAssuredSetup.install()`.

### 6. Determinizm seedów — UUID są stałe

`DeterministicDataset`/`Fixtures` z backendu dają stałe UUID-y:
```
MERCHANT_ALPHA_001_ID = 00000000-0000-0000-0000-0000000000b1
MERCHANT_ALPHA_002_ID = 00000000-0000-0000-0000-0000000000b2
MERCHANT_BETA_001_ID  = 00000000-0000-0000-0000-0000000000b3
TENANT_ALPHA_ID       = 00000000-0000-0000-0000-0000000000a2
PLATFORM_TENANT_ID    = 00000000-0000-0000-0000-0000000000a1
```
Opieraj `Seeds.java` na tych wartościach. Dane tworzone ad-hoc w scenariuszu muszą być
sprzątane przez `SeedLifecycleExtension` przed następnym testem (reset → seed).

### 7. Virtual threads i ScopedValue

`ScopedValue` dziedziczy się do wątków potomnych przez `ScopedValue.where(...).run(...)`.
W `ConcurrencyHarness` opakuj każde zadanie w `ScopedValue.where(CURRENT, ctx).call(action)`
— dzięki temu filtry `AuthFilter` i `CorrelationFilter` mają dostęp do kontekstu
w każdym wątku wyścigu.

### 8. Versioned i create 201 vs replay 200

Scenariusz `IdempotencyReplaySpec` musi odróżnić pierwsze create (201) od replay (200).
`PaymentOrdersApi.createRaw()` zwraca surowy `Response` — sprawdzaj `statusCode()` przed
deserializacją. Alternatywnie `Versioned` może nieść `boolean created` (warto dodać).

---

## 18. Kontrakt API do pokrycia (pełna tabela endpointów)

| Endpoint | Metoda | Auth (authority) | Specyfika | Kody sukcesu |
|---|---|---|---|---|
| `/api/status` | GET | brak | permitAll | 200 |
| `/api/merchants` | POST | `platform:merchants:create` | – | 201 |
| `/api/merchants` | GET | `platform:merchants:read` | `?tenantId` | 200 |
| `/api/merchants/{id}` | GET | `platform:merchants:read` | – | 200 |
| `/api/merchants/{id}/activate` | POST | `platform:merchants:update-status` | – | 200 |
| `/api/merchants/{id}/suspend` | POST | `platform:merchants:update-status` | – | 200 |
| `/api/merchants/{m}/payment-orders` | POST | `merchant:payments:create` | `Idempotency-Key`; `azp`/`merchant_id` check | 201/200 |
| `/api/merchants/{m}/payment-orders` | GET | `merchant:payments:read` ∣ `platform:payments:read` | paginacja, filtry, sort | 200 |
| `/api/merchants/{m}/payment-orders/summary` | GET | jw. | `?currency,status,fromDate,toDate` | 200 |
| `/api/merchants/{m}/payment-orders/{p}` | GET | jw. | ETag w odpowiedzi | 200 |
| `/api/merchants/{m}/payment-orders/{p}` | HEAD | jw. | tylko nagłówki | 200 |
| `/api/merchants/{m}/payment-orders/{p}` | OPTIONS | brak auth | Allow + Accept-Patch | 204 |
| `/api/merchants/{m}/payment-orders/{p}/authorize` | POST | `merchant:payments:lifecycle` ∣ `platform:payments:lifecycle` | `If-Match` + `Idempotency-Key` | 200 |
| `/api/merchants/{m}/payment-orders/{p}/capture` | POST | jw. | `If-Match` + `Idempotency-Key`; opcj. `amountMinor` | 200 |
| `/api/merchants/{m}/payment-orders/{p}/cancel` | POST | jw. | `If-Match` + `Idempotency-Key`; opcj. `reason` | 200 |
| `/api/merchants/{m}/payment-orders/{p}/refund` | POST | jw. | `If-Match` + `Idempotency-Key`; opcj. `amountMinor`, `reason` | 200 |
| `/api/merchants/{m}/payment-orders/{p}` | PATCH | jw. | `If-Match`; `merge-patch+json`; tylko pole `metadata` | 200 |
| `/api/merchants/{m}/payment-orders/{p}/history` | GET | lifecycle ∣ read ∣ audit | – | 200 |
| `/api/audit` | GET | `platform:audit:read` ∣ `tenant:audit:read` | tenant-scoped dla tenant | 200 |
| `/api/audit/{id}` | GET | jw. | – | 200 |
| `/api/users` | GET | `platform:users:read` ∣ `tenant:users:read` | – | 200 |
| `/api/users` | POST | `platform:users:create` ∣ `tenant:users:create` | – | 201 |
| `/api/users/{id}` | GET | read | – | 200 |
| `/api/users/{id}` | PATCH | update | – | 200 |
| `/api/users/{id}/roles` | POST | assign-roles | – | 200 |
| `/api/test/seed` | POST | permitAll | tylko profil `seed` | 200 |
| `/api/test/reset` | POST | permitAll | tylko profil `seed` | 200 |

---

## 19. Kolejność implementacji

```
1.  core/stack           — Network + Postgres + Keycloak (z issuer fix) + backend
2.  core/http            — RestAssuredSetup (BASE + rejestracja parserów), oba filtry, RequestSpecs
3.  core/context         — Ctx (ScopedValue) z czytelnym błędem przy braku bindingu
4.  core/auth            — KeycloakTokenFactory + Identities + realm-import JSON
5.  support              — ApiStackExtension + SeedLifecycleExtension + ScopedContextExtension + ApiTest
6.  api/seed/SeedApi     — /api/test/seed|reset, bez tokenu
7.  api/payment          — PaymentOrdersApi + DTO + ResponseSpecs
8.  core/problem         — ProblemAssert extends AbstractAssert
9.  schema/              — payment-order.json + problem.json
    ↓ PIERWSZY ŻYWY TEST
10. PaymentLifecycleSpec — create→authorize→capture; weryfikacja całej hydrauliki
    ↓ RESZTA SCENARIUSZY
11. IdempotencyReplaySpec — replay + conflict + ConcurrencyHarness
12. OptimisticLockingSpec — 428/400/412
13. ResponseSpecs         — sensitive()/conditional()/created() (używane już od 7)
14. AuditApi + Eventually — AuditTrailSpec
15. MerchantsApi          — TenantIsolationSpec
```

Punkt 10 (`PaymentLifecycleSpec` zielony) = dowód, że stos działa.
Nie przechodź do 11+ bez zielonego 10.

---

## 20. Kryteria akceptacji

- `./mvnw verify` z ustawionym `BACKEND_IMAGE` przechodzi na czystej maszynie (Podman, Fedora 44).
- Stack startuje raz; Ryuk sprząta; brak wycieków kontenerów między testami.
- Każdy scenariusz przechodzi przez `api/*` (zero `given()` w `scenarios/*`).
- Każda odpowiedź błędna walidowana przez `ProblemAssert` (status + problem+json + kod + correlationId + nagłówki).
- Każda odpowiedź sukcesu z zasobu płatności/audytu walidowana przez `ResponseSpecs.*`.
- Kluczowe odpowiedzi walidowane przez `matchesJsonSchemaInClasspath(...)`.
- Audyt wyłącznie przez `Eventually` (zero bezpośrednich asercji zaraz po akcji).
- Brak importów z `apps/backend` w całym module.
- Brak literałów kodów błędów w `scenarios/*` (wyłącznie `ProblemCodes.*`).

---

## 21. Rest Assured Surface Area Learning Matrix (NOWY BLOK EDUKACYJNY)

Ten framework ma nie tylko uruchamiać testy. Ma również uczyć świadomego użycia REST Assured.
Dlatego każda klasa/interfejs poniżej ma mieć w kodzie albo praktyczne zastosowanie, albo jawny status
"znać biernie / deferred". Nie uczymy się encyklopedii klas; uczymy się problemów, które te klasy rozwiązują.

### Level 1 — absolutne must-have w pierwszych lekcjach

| Element REST Assured | Status w frameworku | Gdzie uczeń ma to zobaczyć | Problem, który rozwiązuje |
|---|---|---|---|
| `RestAssured` | używany pośrednio | `RequestSpecs.base()` przez `given()` | punkt wejścia do DSL |
| `RequestSpecification` | MUST | `RequestSpecs`, API clients | opis requestu przed wysłaniem |
| `ValidatableResponse` | MUST dopisać jako temat | lekcje API client return types | walidacja po `.then()` bez natychmiastowej ekstrakcji |
| `Response` | MUST | `createRaw()`, negative tests, `ProblemAssert` | surowa odpowiedź, gdy trzeba najpierw obejrzeć status/body |
| `ExtractableResponse<Response>` | MUST | `PaymentOrdersApi.create/authorize/capture` | zachowanie body + headers przed mapowaniem |
| `ContentType` | MUST | JSON, merge-patch, form token request | `Content-Type` i `Accept` bez string literals |
| `JsonPath` / `response.jsonPath()` | MUST | summary, audit, problem details | punktowa ekstrakcja pól bez DTO |

### Level 2 — must-have dla małego profesjonalnego test kit/frameworka

| Element REST Assured | Status w frameworku | Gdzie uczeń ma to zobaczyć | Problem, który rozwiązuje |
|---|---|---|---|
| `RequestSpecBuilder` | MUST | `RestAssuredSetup.install()` | niemutowalny szablon requestu |
| `ResponseSpecBuilder` | MUST | `ResponseSpecs` | reusable expectations bez duplikacji |
| `ResponseSpecification` | MUST | `ResponseSpecs.sensitive/problemJson/created/conditional` | wspólny kontrakt response |
| `Header` | SHOULD | osobne ćwiczenie headers | multi-value i ręczne budowanie headerów |
| `Headers` | MUST | diagnostics, `Vary`, `ETag`, `Location` | analiza wielu nagłówków naraz |
| `TypeRef<T>` | MUST | list endpoints, raw arrays | bezpieczna deserializacja `List<T>` |
| `JsonSchemaValidator` | MUST | `schema/*.json` | walidacja kontraktu struktury JSON |

### Level 3 — Senior/Expert, część wdrożona, część deferred

| Element REST Assured | Status | Decyzja dla tego projektu |
|---|---|---|
| `Filter` | MUST | `AuthFilter`, `CorrelationFilter`, opcjonalny safe logging filter |
| `OrderedFilter` | DEFERRED | dodać, gdy kolejność auth/correlation/logging zacznie mieć znaczenie |
| `RequestLoggingFilter` | DEFERRED/ostrożnie | znać; nie używać globalnie bez maskowania sekretów |
| `ResponseLoggingFilter` | DEFERRED/ostrożnie | znać; preferować log only on failure |
| `ErrorLoggingFilter` | SHOULD | używać tylko do błędów, bez tokenów i PII |
| `ResponseBuilder` | DEFERRED | tylko do zaawansowanych filtrów modyfikujących response |
| `RestAssuredConfig` | MUST | centralny config w `RestAssuredSetup` |
| `LogConfig` | MUST | logowanie tylko przy failu + blacklist headers |
| `EncoderConfig` | MUST | `merge-patch+json` bez charsetu, encoding query/path |
| `DecoderConfig` | DEFERRED | gzip/deflate/br, `Accept-Encoding`, compressed responses |
| `RedirectConfig` | DEFERRED | 301/302/303/307/308, `followRedirects(false)` |
| `ObjectMapperConfig` | DEFERRED/ostrożnie | znać; nie współdzielić mappera backendu bez powodu |
| `JsonPathConfig` | MUST | `BIG_DECIMAL` dla kwot finansowych |
| `Cookie`, `Cookies`, `DetailedCookie`, `SessionFilter` | LOW PRIORITY | tylko jeśli dojdą session/login/CSRF flows |
| `XmlPath` | LOW PRIORITY | znać biernie; obecny lab jest JSON-first |

### Strategia typów zwracanych przez API clients

Nie każda metoda klienta API powinna zwracać to samo. To jest osobny temat do nauki.

| Typ zwracany | Kiedy używać | Przykład |
|---|---|---|
| `ValidatableResponse` | gdy scenariusz ma sam pokazać kontrakt HTTP | `api.payments().createValidatable(...).statusCode(201)...` |
| `Response` | negative tests, status zależy od wariantu, custom assertions | `createRaw(...)`, `ProblemAssert.assertThat(response)` |
| `ExtractableResponse<Response>` | wewnątrz API clienta przed zmapowaniem DTO + headers | `then().spec(...).extract()` |
| `Versioned<T>` | gdy odpowiedź niesie DTO + `ETag` | create/read/lifecycle/PATCH |
| DTO | gdy endpoint read-only nie wymaga headers | proste GET bez ETagu lub metadata |
| `List<T>` / wrapper DTO | list/report endpoints | listy, summary, audit content |

**Zasada:** scenariusze biznesowe mają być czytelne, ale nie mogą ukrywać kontraktu HTTP. Jeśli helper zwraca
już gotowe DTO, musi istnieć osobna metoda `Raw` albo `Validatable`, żeby dało się testować statusy, nagłówki,
problem details i schema bez rozmontowywania helpera.

---

## 22. Advanced HTTP/REST Coverage Backlog (NOWY BLOK)

Ten framework w pierwszej iteracji skupia się na payment lifecycle, idempotency, ETag, problem details,
tenant boundary i audycie. Poniższe tematy są obowiązkowym backlogiem edukacyjnym, żeby projekt `job-learn`
pokrył szeroki zakres HTTP/REST i interview puzzles. Nie wszystko musi być implementowane od razu.
Każdy temat ma mieć docelowo: endpoint/card, testy REST Assured, pytania rekrutacyjne EN i krótkie ćwiczenie.

### 22.1 Conditional GET, cache i `304 Not Modified`

**Cel:** rozróżnić `If-Match` dla zapisu od `If-None-Match` dla odczytu.

| Element | Wymaganie edukacyjne |
|---|---|
| `ETag` | response z GET/HEAD ma wersję zasobu |
| `If-None-Match` | klient pyta, czy zasób zmienił się od znanego ETagu |
| `304 Not Modified` | brak body; klient może użyć cache |
| `Cache-Control` | dla danych wrażliwych zwykle `no-store`; dla public/status możliwe inne zasady |
| `Vary` | musi opisywać, co wpływa na reprezentację (`Authorization`, `Accept`, `If-None-Match`) |

Przykładowy test:

```java
ExtractableResponse<Response> first = given().spec(RequestSpecs.base())
        .pathParam("merchantId", merchantId)
        .pathParam("paymentOrderId", orderId)
        .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}")
        .then()
        .statusCode(200)
        .header(Headers.ETAG, matchesPattern("\"v\\d+\""))
        .extract();

String etag = first.header(Headers.ETAG);

given().spec(RequestSpecs.base())
        .header("If-None-Match", etag)
        .pathParam("merchantId", merchantId)
        .pathParam("paymentOrderId", orderId)
        .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}")
        .then()
        .statusCode(304)
        .body(is(emptyOrNullString()));
```

**Interview EN:**

> `If-Match` protects write operations from stale updates. `If-None-Match` is used for conditional reads and can return `304 Not Modified` when the client's cached representation is still valid.

### 22.2 Rate limiting: `429 Too Many Requests` + `Retry-After`

**Cel:** testować overload/abuse protection bez łamania idempotency.

| Element | Wymaganie edukacyjne |
|---|---|
| `429` | za dużo requestów w oknie czasu |
| `Retry-After` | kiedy klient może spróbować ponownie |
| problem+json | `error = rate_limited` |
| idempotency | retry po limicie nie może zrobić duplicate charge |
| scope | limit per merchant/tenant/client/IP — jawna decyzja biznesowa |

Przykładowy kontrakt:

```java
given().spec(RequestSpecs.idempotent(key))
        .pathParam("merchantId", merchantId)
        .body(body)
        .post("/api/merchants/{merchantId}/payment-orders")
        .then()
        .spec(ResponseSpecs.problemJson())
        .statusCode(429)
        .header("Retry-After", notNullValue())
        .body("error", equalTo(ProblemCodes.RATE_LIMITED));
```

Do `ProblemCodes` dodać, gdy backend to wspiera:

```java
public static final String RATE_LIMITED = "rate_limited";
```

### 22.3 `202 Accepted` i async operation status resource

**Cel:** rozróżnić synchronizowane akcje lifecycle od akcji przyjętych do przetwarzania.

Przykładowy pattern:

```http
POST /api/merchants/{m}/payment-orders/{p}/capture
HTTP/1.1 202 Accepted
Location: /api/operations/{operationId}
Retry-After: 2
```

Docelowe endpointy edukacyjne:

| Endpoint | Cel |
|---|---|
| `POST .../capture-async` | przyjęcie komendy |
| `GET /api/operations/{id}` | status operacji: `PENDING`, `SUCCEEDED`, `FAILED` |
| `GET .../history` | potwierdzenie późniejszego przejścia statusu |

REST Assured/Awaitility:

```java
Response accepted = api.payments().captureAsyncRaw(merchantId, orderId, from, key);

accepted.then()
        .statusCode(202)
        .header(Headers.LOCATION, containsString("/api/operations/"));

Eventually.awaitUntil(Duration.ofSeconds(10), () ->
        api.operations().getRaw(operationId).jsonPath().getString("status").equals("SUCCEEDED")
);
```

### 22.4 `PUT` vs `PATCH` vs action `POST` vs search `POST`

**Cel:** umieć dobrać metodę HTTP do intencji biznesowej.

| Metoda/pattern | Kiedy | Przykład w labie |
|---|---|---|
| `POST` create | serwer tworzy nowy zasób/id | `POST /payment-orders` |
| `POST` action/command | akcja biznesowa, nie zwykłe CRUD update | `POST .../{id}/authorize` |
| `POST` search | złożone filtry w body, gdy query params są niewystarczające | `POST /payment-orders/search` |
| `PUT` replace | pełna zamiana zasobu pod znanym URI | `PUT /users/{id}/profile` |
| `PATCH` partial update | częściowa zmiana pól | `PATCH .../payment-orders/{id}` metadata |
| `DELETE` | usunięcie/anulowanie zasobu, jeśli domena tak pozwala | zwykle nie payment capture/refund |

Dodać testy porównujące:

- `PATCH` z `application/merge-patch+json` → 200.
- `PATCH` z `application/json` → 415, jeśli kontrakt wymaga merge-patch.
- `PUT` częściowy bez wymaganych pól → 400.
- `POST /search` bez mutacji → 200 i brak zmiany DB/audytu mutacyjnego.

### 22.5 Pagination, sorting, filtering contract

**Cel:** list endpoints to nie tylko `statusCode(200)`.

Zakres:

| Obszar | Przypadki |
|---|---|
| pagination | `page`, `size`, first/last/empty page |
| max size | `size > max` → 400 albo clamp — jawna decyzja |
| sorting | stable ordering, invalid sort field |
| filtering | `status`, `currency`, `fromDate`, `toDate`, tenant scope |
| consistency | totalElements, content size, deterministic order |

Przykłady REST Assured:

```java
given().spec(RequestSpecs.base())
        .pathParam("merchantId", merchantId)
        .queryParam("status", "CAPTURED")
        .queryParam("currency", "PLN")
        .queryParam("page", 0)
        .queryParam("size", 20)
        .queryParam("sort", "createdAt,desc")
        .get("/api/merchants/{merchantId}/payment-orders")
        .then()
        .statusCode(200)
        .body("content.size()", lessThanOrEqualTo(20))
        .body("content.status", everyItem(equalTo("CAPTURED")))
        .body("content.currency", everyItem(equalTo("PLN")));
```

### 22.6 Bulk/batch operations

**Cel:** testować częściowe sukcesy, walidację per item i idempotency per element.

Przykładowe endpointy edukacyjne:

| Endpoint | Cel |
|---|---|
| `POST /api/merchants/{m}/payment-orders/batch` | wiele create requests w jednym żądaniu |
| `POST /api/merchants/{m}/payment-orders/bulk-cancel` | masowa akcja lifecycle |

Kontrakty do decyzji:

| Model | Kiedy | Status |
|---|---|---|
| all-or-nothing | transakcja ma przejść w całości albo wcale | 201/400/409 |
| partial success | każdy element ma własny result | 200 lub 207 ostrożnie |
| async batch | batch przyjęty, wyniki później | 202 + operation resource |

Asercje AssertJ:

```java
assertThat(results)
        .extracting("clientReference", "status", "error")
        .contains(
                tuple("A", "CREATED", null),
                tuple("B", "FAILED", "validation"),
                tuple("C", "FAILED", "duplicate_reference")
        );
```

### 22.7 API versioning i backward compatibility

**Cel:** testy kontraktowe mają wykrywać breaking changes.

Strategie do nauki:

| Strategia | Przykład |
|---|---|
| URI versioning | `/api/v1/payment-orders` |
| header versioning | `X-API-Version: 1` |
| media type versioning | `Accept: application/vnd.payment-quality.v1+json` |

Testy:

- v1 zwraca stary kontrakt.
- v2 może dodać pole, ale nie zmienia znaczenia istniejących pól.
- unknown version → 400/406.
- deprecated endpoint ma `Warning`, `Deprecation` lub `Sunset` header, jeśli projekt wprowadza politykę deprecacji.

### 22.8 OpenAPI/Swagger drift testing i documentation smells

**Cel:** dokumentacja API jest częścią kontraktu, nie marketingiem.

Review checklist:

- Czy OpenAPI opisuje wszystkie success statusy?
- Czy opisuje `401`, `403`, `404`, `409`, `412`, `415`, `422`, `428`, `429`?
- Czy `problem+json` ma ten sam shape co realny backend?
- Czy enumy w OpenAPI są zgodne z realnym DTO/schema?
- Czy `Idempotency-Key`, `If-Match`, `ETag`, `Location`, `Retry-After` są udokumentowane?
- Czy endpointy lifecycle są opisane jako command/action, a nie zwykły CRUD?

Automatyzację drift detection dodać dopiero po ustabilizowaniu OpenAPI generation.

### 22.9 Redirect semantics: `301/302/303/307/308`

**Cel:** znać zachowanie klienta HTTP, nawet jeśli payment API rzadko używa redirectów.

Zakres:

| Status | Najważniejsza lekcja |
|---|---|
| `301/302` | historycznie klienci mogą zmieniać metodę na GET |
| `303` | po POST: wynik/confirmation pod innym URI, użyj GET |
| `307/308` | metoda i body muszą zostać zachowane |

REST Assured config:

```java
given().spec(RequestSpecs.base())
        .config(RestAssuredConfig.config()
                .redirect(RedirectConfig.redirectConfig().followRedirects(false)))
        .post("/api/some-post-that-redirects")
        .then()
        .statusCode(303)
        .header(Headers.LOCATION, notNullValue());
```

### 22.10 Multipart/file upload

**Cel:** znać REST Assured poza JSON body.

Potencjalne lab endpoints:

| Endpoint | Cel |
|---|---|
| `POST /api/reconciliation-files` | upload pliku rozliczeniowego |
| `POST /api/settlement-reports/import` | import CSV/JSON report |

Testy:

- poprawny plik → 201/202.
- za duży plik → 413 Payload Too Large.
- zły media type → 415.
- malformed CSV → 400 problem+json z details per row.
- duplicate upload idempotency key → replay/conflict.

### 22.11 Cookies/session/CSRF — znać, nie mieszać z głównym JWT API

Obecny backend jest Bearer/JWT-first, więc cookies są low priority. Dodać tylko jako osobny moduł edukacyjny,
jeśli pojawi się login/session/UI-adjacent API.

Zakres bierny:

- `Cookie`, `Cookies`, `DetailedCookie`.
- `SessionFilter`.
- `SameSite`, `HttpOnly`, `Secure`.
- CSRF token: kiedy dotyczy session-based web apps, a kiedy nie dotyczy stateless bearer API.

### 22.12 SSL/TLS, proxy i URL encoding

**Cel:** senior SDET umie diagnozować problemy klienta HTTP, nie tylko backend logic.

Zakres:

| Obszar | Przykłady |
|---|---|
| SSL | self-signed cert, `relaxedHTTPSValidation()` tylko lokalnie |
| proxy | corporate proxy/debug proxy |
| URL encoding | spacja, polskie znaki, `%2F`, `+` vs `%20` |
| path vs query | kiedy RA koduje parametr automatycznie |

Ćwiczenia:

- query param z polskimi znakami.
- path variable zawierający slash jako dane — czy API w ogóle powinno to dopuszczać?
- encoded vs non-encoded request.

---

## 23. Endpoint Learning Card Template (NOWY BLOK)

Każdy endpoint w `api/*` powinien mieć kartę edukacyjną w dokumentacji lub README. Karta ma być podobna
do Swagger/OpenAPI, ale rozszerzona o perspektywę SDET: test types, risks, headers, problem details i interview traps.

```markdown
## Endpoint Card: <METHOD> <PATH>

### Purpose
Co endpoint robi biznesowo i czego NIE robi.

### Request
- Path params:
- Query params:
- Headers:
- Content-Type:
- Accept:
- Body:

### Success responses
| Status | Kiedy | Obowiązkowe headers | Body/schema |
|---|---|---|---|

### Error responses
| Status | `ProblemCodes.*` | Kiedy | Test type |
|---|---|---|---|

### Contract tests
- status
- content type
- schema
- required fields
- headers

### Business tests
- reguły domenowe
- state transition
- amount/currency/status

### Security tests
- 401
- 403
- masked 404
- tenant boundary
- ownership

### Data tests
- seed
- fixtures
- cleanup
- parallel-safety

### REST Assured focus
Jakiej klasy/metody uczymy się przy tym endpointcie?

### Interview traps
- podchwytliwe pytania
- krótka odpowiedź EN
```

Przykład skrócony dla create payment order:

```markdown
## Endpoint Card: POST /api/merchants/{merchantId}/payment-orders

### REST Assured focus
`RequestSpecification`, `RequestSpecBuilder`, `Idempotency-Key`, `.body()`, `.post()`,
`.then()`, `ValidatableResponse`, `ExtractableResponse<Response>`, `ResponseSpecs.created()`,
`JsonSchemaValidator`.

### Interview traps
- Why can POST be idempotent?
- Why first request returns 201 but replay can return 200?
- Why same idempotency key with different body returns 409?
- Why Location and ETag are useful after create?
```

---

## 24. API Test Type Taxonomy (NOWY BLOK)

Scenariusze mają mówić, jaki typ testu reprezentują. Nie każdy test REST Assured jest taki sam.

| Typ testu | Co sprawdza | Przykład w Payment Lab | Główne narzędzie |
|---|---|---|---|
| Smoke | endpoint żyje | `GET /api/status` | RA status/body |
| Sanity | krytyczna ścieżka po zmianie | create→authorize→capture | API DSL + AssertJ |
| Contract | kształt HTTP/JSON/headers | schema + headers + status | RA + JSON Schema |
| Business flow | wynik domenowy | status po capture = `CAPTURED` | DTO + AssertJ |
| Negative HTTP | protokół i błędy | 406/415/405/428 | `ProblemAssert` |
| Validation | niepoprawne dane | amount <= 0 | problem+json details |
| Security matrix | auth/roles/ownership | 401/403/masked 404 | identities + raw responses |
| State transition | dozwolone/niedozwolone przejścia | cancel captured → 422 | lifecycle DSL |
| Idempotency | replay/conflict | 201/200/409 | idempotency keys |
| Concurrency | race/stale update | same key/stale ETag | virtual threads + Awaitility |
| DB verification | final safety net | unique constraint/FK | repository/SQL layer, nie każdy API test |
| Audit/observability | ślad operacji | audit by correlationId | Eventually + audit API |
| Performance-light | budżet odpowiedzi/payload | list/summary time, N+1 suspicion | `.time()`, metrics, logs |
| E2E UI journey | zachowanie użytkownika | dashboard flow | Playwright, nie REST Assured |

**MUST:** w nazwie klasy lub sekcji scenariusza ma być jasne, czy test jest kontraktowy, biznesowy,
security, idempotency, concurrency czy audit/reliability.

---

## 25. Failure Analysis and Diagnostics (NOWY BLOK)

Pisanie testów to połowa pracy. Senior SDET musi umieć wyjaśnić, dlaczego test padł.
Każdy większy scenariusz powinien dawać wystarczające evidence: request intent, response status,
problem code, correlation id i najważniejsze headers.

### Checklist diagnozy failed API test

| Pytanie | Gdzie sprawdzić |
|---|---|
| Czy request poszedł na właściwy base URI/path? | RA failure log, `RequestSpecs` |
| Czy path/query params były poprawne? | log only on fail, test data |
| Czy `Content-Type` był zgodny z body? | request headers |
| Czy `Accept` nie powoduje `406`? | request headers |
| Czy token ma dobre `tenant_id`, `merchant_id`, `azp`, roles? | `Identity`, token factory, Keycloak realm |
| Czy `X-Correlation-ID` jest w request, response i problem body? | `CorrelationFilter`, `ProblemAssert` |
| Czy `Idempotency-Key` nie został przypadkowo użyty ponownie? | `IdempotencyKeys`, test data naming |
| Czy `If-Match` jest świeży? | `Versioned`, `ETag.version()` |
| Czy błąd jest API, auth, DB, test data, race czy async timing? | status + problem code + audit/logs |
| Czy audit był sprawdzany przez `Eventually`? | `AuditTrailSpec` |
| Czy schema failure oznacza breaking change czy zbyt restrykcyjny schema? | `schema/*.json` vs DTO/API |

### Failure classification

| Klasa błędu | Objawy | Typowa naprawa |
|---|---|---|
| API contract regression | status/header/schema się zmienił | napraw backend albo zaktualizuj kontrakt świadomie |
| Test data collision | 409 duplicate, flaky order | unique data/seed/reset/isolation |
| Auth/claims mismatch | 401/403 zamiast 200/404 | popraw identity/realm/token factory |
| Tenant boundary issue | 403 vs masked 404 niezgodne z regułą | doprecyzuj read/write boundary |
| Stale ETag | 412/428 | pobierz świeży `Versioned` albo testuj konflikt świadomie |
| Async timing | audit/event nie istnieje od razu | użyj `Eventually`, nie sleep |
| RA config issue | 415 przy merge-patch, parse error problem+json | sprawdź `EncoderConfig`, parser registration |
| Schema too strict | schema fail przy dopuszczalnym nowym polu | zdecyduj `additionalProperties` i kompatybilność |

### Safe logging rules

- NIE logować stale `Authorization`.
- NIE logować sekretów Keycloak/client secret.
- NIE logować pełnych payloadów z PII.
- Maskować `Idempotency-Key`, jeśli może być traktowany jako wrażliwy identyfikator operacji.
- Logować szczegóły tylko przy failu albo w trybie debug.
- Każdy failed error response musi pokazać `status`, `error`, `correlationId`.

---

## 26. Extension Implementation Order For Advanced HTTP Topics (NOWY BLOK)

Nie implementować wszystkich rozszerzeń naraz. Kolejność ma wspierać spiral learning.

```text
A. Dodać Rest Assured Surface Area Learning Matrix do README/framework docs.
B. Uzupełnić API client return type strategy: Raw / Validatable / Versioned / DTO.
C. Dodać endpoint cards dla istniejących payment endpoints.
D. Dodać conditional GET: If-None-Match / 304 dla read endpointów.
E. Dodać pagination/sorting/filtering contract tests dla list/summary.
F. Dodać 429 / Retry-After dopiero gdy backend ma rate limiting.
G. Dodać 202 Accepted + operation status przy pierwszym async use case.
H. Dodać API versioning/OpenAPI drift jako review/testing layer po stabilizacji OpenAPI.
I. Dodać redirects, multipart, cookies, SSL/proxy/encoding jako osobne advanced lessons, nie jako core payment lifecycle.
```

Minimalny najbliższy zestaw po zielonym `PaymentLifecycleSpec`:

1. `ValidatableResponse` lesson + helper method.
2. `Headers` diagnostics exercise.
3. Conditional GET / `If-None-Match` / `304`.
4. Pagination/filtering/sorting tests.
5. Failure analysis checklist w README.

---

## 27. Komendy

```bash
make backend-image                                          # zbuduj obraz backendu
make test                                                   # BACKEND_IMAGE=... ./mvnw verify
./mvnw -Dit.test=PaymentLifecycleSpec verify                # jeden scenariusz
./mvnw -Dit.test="PaymentLifecycleSpec+IdempotencyReplaySpec" verify  # kilka
```
