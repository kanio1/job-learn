---
type: prompt
status: ready
project: Payment Quality Engineering Lab
lesson: 06
date: 2026-05-27
tags:
  - prompt
  - rest-assured
  - backend-testing
  - response-assertions
  - hamcrest
  - assertj
  - headers
  - error-contract
  - sdet
---

# Prompt - Lesson 06 - REST Assured Response Assertions Status Body Headers AssertJ

```text
Jesteś moim agentem kodowania, mentorem Senior QA Automation/SDET oraz nauczycielem backend testingu.

Pracujemy w repozytorium:

/home/suso/job-learn

Projekt:

Payment Quality Engineering Lab

## Cel tej pracy

Przeanalizuj i rozbuduj istniejącą lekcję REST Assured lesson 6:

Lesson 6 - Response and Basic Assertions

To jest szósta lekcja REST Assured po:

1. What REST Assured Is
2. Anatomy of given(), when(), then()
3. HTTP Method, Endpoint, Content-Type and Accept
4. Path Params, Query Params and Headers
5. Request Body, JSON, `Map.of`, DTO and Serialization

Lekcja ma nauczyć mnie intensywniej niż poprzednie lekcje: około 50% większa dawka wiedzy w jednej lekcji, nadal beginner-friendly, ale z bardziej wyraźnym kierunkiem Senior QA/SDET.

Główne pytanie lekcji:

Jak zaprojektować response assertions, które naprawdę chronią kontrakt API, a nie tylko sprawdzają, że endpoint zwrócił jakiś status?

## Nowe tempo nauki od Lesson 6

Od tej lekcji zwiększamy tempo. Każda lekcja ma zawierać:

1. Fundament od zera, żeby początkujący rozumiał składnię.
2. Dodatkowy poziom profesjonalny: ryzyka, test design, review heuristics, dane i warstwa testów.
3. Więcej powiązań z Java 25, Spring, PostgreSQL, REST Assured, AssertJ i architekturą testów.
4. Jedną małą propozycję rozszerzenia testów albo aplikacji tylko wtedy, gdy istnieje realna wartość produktowa/testowa.
5. Wyraźną decyzję: czy w tej lekcji kod aplikacji jest wystarczający, czy potrzebuje rozszerzenia.

Nie oznacza to dodawania funkcji na siłę. Intensywność ma wynikać z głębszego testowania i lepszych oracles, nie z dekoracyjnej funkcjonalności.

## Decyzja o rozszerzaniu aplikacji

Najpierw przeanalizuj, czy aplikację trzeba rozszerzać.

Domyślna decyzja dla Lesson 6: aplikacja jest wystarczająca, bo obecny backend ma już dużo typów odpowiedzi:

- `GET /api/status` zwraca prosty publiczny response,
- `POST /api/merchants` zwraca `201` i `MerchantResponse`,
- `GET /api/merchants/{id}` zwraca `200` i pojedynczego merchanta,
- `GET /api/merchants` zwraca `MerchantListResponse`,
- `POST /api/merchants/{id}/activate` i `/suspend` zwracają zmieniony status,
- błędy walidacji zwracają `400` i `ErrorResponse`,
- brak zasobu zwraca `404`,
- konflikty zwracają `409`,
- security tests mają `401` i `403`,
- `CorrelationIdFilter` ustawia `X-Correlation-ID` w response.

Nie dodawaj nowego endpointu tylko po to, żeby pokazać response assertions.

Możesz zaproponować małe rozszerzenie testów, jeśli jest edukacyjnie wartościowe, na przykład:

- dodać brakujące `.contentType(ContentType.JSON)` przy wybranych REST Assured assertions,
- dodać asercje dla `merchantId`, `createdAt`, `updatedAt` tam, gdzie response contract już je zwraca,
- dodać test headera `X-Correlation-ID`, bo aplikacja już go implementuje,
- dodać asercje `message` i `details` dla błędów walidacji.

Jeśli proponujesz takie rozszerzenie, najpierw opisz dlaczego jest warte zrobienia. Nie implementuj dużej funkcji biznesowej.

## Guardrails

Nie dodawaj Payment Order w tej lekcji.
Nie dodawaj Kafki, GraphQL ani gRPC.
Nie dodawaj PSP integration.
Nie dodawaj nowego modułu biznesowego.
Nie rób pełnej lekcji security/JWT; `401/403` pokazuj tylko jako rodzaje response, a szczegóły auth zostaw na Lesson 9.
Nie rób pełnej lekcji o listach i nested responses; listy będą Lesson 7, ale możesz pokazać minimalny kontrakt `merchants` jako zapowiedź.
Nie rób pełnej lekcji extraction/deserialization; extraction będzie Lesson 8, ale możesz pokazać różnicę między assertion a extraction.
Nie twórz nowego pliku lekcji, jeśli istniejąca sekcja Lesson 6 może zostać rozbudowana.

Preferencja: rozbuduj istniejącą sekcję Lesson 6 w:

`knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/01-12 REST Assured Foundations.md`

## Użyj skills

Użyj:

- `java-rest-api-testing-effective-java-mentor`,
- `junit6-assertj-restassured-testcraft`,
- `test-analysis-design-and-data`,
- `spring-boot4-spring7-backend-architect`, jeśli wyjaśniasz Spring controller, DTO response, error handling albo headers,
- `parallel-test-architecture-and-data-isolation`, jeśli proponujesz test data isolation dla response assertions.

Nie używaj głęboko `rest-api-security-oauth-testing`; security ma być tylko kontekstem statusów `401/403` i protected endpoints.

## Najpierw przeczytaj

Przeczytaj:

- `specs/002-merchant-registry-activation/plan.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Payment Gateway SDET Learning Plan.md`
- `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/README.md`
- `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/README.md`
- `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/01-12 REST Assured Foundations.md`
- `knowledge-vault/02 Areas/Technical Learning/REST API From Zero/Merchant Request and Response Flow.md`

Przeczytaj kod:

- `apps/backend/src/test/java/lab/paymentquality/rest/StatusRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/MerchantRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/MyMerchantRestAssuredTest.java`, jeśli istnieje, jako sandbox do ćwiczeń
- `apps/backend/src/test/java/lab/paymentquality/security/MerchantSecurityTest.java`, tylko jako kontekst statusów `401/403`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/MerchantApiTestSupport.java`
- `apps/backend/src/main/java/lab/paymentquality/foundation/status/StatusController.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantController.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantResponse.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantListResponse.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/ErrorResponse.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantExceptionHandler.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/web/CorrelationIdFilter.java`, jeśli istnieje, jako response header context

Jeśli któryś plik nie istnieje, nie zgaduj. Zaznacz obserwację i użyj istniejących plików.

## Sprint Learning Matrix dla tej lekcji

Uwzględnij:

| Sekcja | Odpowiedź |
|---|---|
| Business capability | Brak nowej funkcji; uczymy się chronić kontrakt odpowiedzi istniejących endpointów |
| Previous knowledge refresh | `given/when/then`, method/endpoint/content type, path/body z Lessons 2-5 |
| New learning focus | Response assertions: status, content type, body fields, error shape, headers, oracle strength |
| Java 25 focus | `record` jako response DTO, `List<Map<String,Object>>` po extraction, typowanie vs dynamic JSON paths |
| Spring focus | `ResponseEntity`, controller return type, `@RestControllerAdvice`, `ErrorResponse`, response headers |
| SQL/PostgreSQL focus | Response odzwierciedla zapisany stan i lifecycle; nie robimy głębokiego SQL, ale pamiętamy o DB-backed truth |
| REST Assured focus | `.statusCode(...)`, `.contentType(...)`, `.body(path, matcher)`, `.header(...)`, Hamcrest matchers |
| AssertJ focus | Kiedy użyć AssertJ po `.extract()`, np. dla listy lub bardziej złożonego oracle |
| Security/Keycloak focus | Tylko response statusy `401/403` jako kontrakt odmowy; szczegóły JWT później |
| Test design focus | Oracle strength, positive/negative assertions, error contract, false green tests |
| Test data focus | Unikalny `merchantReference`, stan `DRAFT/ACTIVE/SUSPENDED`, brak zależności od cudzych danych |
| Test layers | REST Assured contract tests, security boundary tests, optional filter/header test |
| Vault output | Rozbudowana Lesson 6 w istniejącym lesson-packu oraz prompt dla Lesson 6 |
| Interview story | I assert status, headers and meaningful body fields so the test protects the API contract, not just endpoint availability |

## Obowiązkowa struktura rozbudowanej Lesson 6

Rozbuduj Lesson 6 tak, aby miała sekcje:

1. Tytuł PL + EN
2. Gdzie ta lekcja znajduje się w ścieżce nauki
3. Co zmieniamy w tempie nauki od Lesson 6
4. Po co testerowi/SDET response assertions
5. Co już powinienem wiedzieć przed tą lekcją
6. Sprint Learning Matrix
7. Intuicyjne wyjaśnienie response jako kontraktu wyjścia
8. Słowniczek pojęć
9. Minimalny przykład REST Assured status + body
10. Wyjaśnienie przykładu linia po linii
11. Profesjonalny przykład z obecnego repo: create merchant response
12. Status code assertions: `200`, `201`, `400`, `401`, `403`, `404`, `409`
13. Content-Type assertions: kiedy i dlaczego sprawdzać `ContentType.JSON`
14. Body assertions: JSON path, field names, Hamcrest matchers
15. Error response assertions: `error`, `message`, `details`
16. Header assertions: `X-Correlation-ID` jako kontrakt observability
17. REST Assured Hamcrest vs AssertJ po extraction
18. Response DTO: `MerchantResponse`, `MerchantListResponse`, `ErrorResponse`
19. Test design: strong oracle vs weak oracle
20. Jakie testy z repo są gotowe dla tej lekcji
21. Czy warto rozszerzyć testy albo aplikację teraz
22. Ćwiczenia praktyczne
23. Wskazówki do ćwiczeń
24. Odpowiedzi PL i EN do ćwiczeń
25. Typowe błędy początkujących
26. Perspektywa Senior QA Automation/SDET
27. Pytania, które powinienem sobie zadać podczas review
28. Mini quiz kontrolny z odpowiedziami
29. Pytania rekrutacyjne po angielsku + przykładowe odpowiedzi
30. Powiązane pliki w repo
31. Powiązane notatki w vault
32. Co przerobić następnie
33. Zapamiętaj
34. Weryfikacja jakości tej lekcji

## Tematy beginner-friendly, ale intensywniejsze

Wyjaśnij prosto:

- co to jest response,
- dlaczego `statusCode(201)` nie wystarcza,
- czym różni się status code od response body,
- czym różni się response body od request body,
- co robi `.then()` w REST Assured,
- co robi `.statusCode(...)`,
- co robi `.contentType(ContentType.JSON)`,
- co robi `.body("field", equalTo(value))`,
- co to jest JSON path w REST Assured,
- co to jest Hamcrest matcher,
- kiedy użyć `equalTo`, `notNullValue`, `hasItem`, `startsWith`, `matchesPattern`,
- kiedy nie sprawdzać całego JSON stringa,
- czym jest `MerchantResponse`,
- czym jest `ErrorResponse`,
- dlaczego error body jest częścią kontraktu,
- dlaczego header `X-Correlation-ID` jest testowalny,
- kiedy wystarczy REST Assured matcher, a kiedy warto zrobić extraction i AssertJ,
- jak odróżnić false green od dobrego oracle.

## Powiąż z obecnym repo

Użyj realnych przykładów:

- `StatusRestAssuredTest#statusEndpointSupportsFoundationOnlyHttpSmokeCheck` jako prosty status/body smoke contract,
- `MerchantRestAssuredTest#createReadListActivateAndSuspendMerchant` jako główny happy path response oracle,
- `MerchantRestAssuredTest#createValidationAndDuplicateErrors` jako error body `400/409`,
- `MerchantRestAssuredTest#notFoundMalformedAndInvalidTransitionErrors` jako `400/404/409` i error code/message,
- `MerchantRestAssuredTest#suspendValidAndInvalidTransitions` jako state-dependent response assertions,
- `MerchantSecurityTest` tylko jako statusy `401/403/200`,
- `MerchantResponse` jako success response DTO,
- `MerchantListResponse` jako wrapper listy, ale bez głębokiej Lesson 7,
- `ErrorResponse` i `MerchantExceptionHandler` jako error contract,
- `CorrelationIdFilter` jako header response contract.

## Wartości response, które warto omówić

Omów przykładowe pola success response:

- `merchantId` - generowany identyfikator, zwykle `notNullValue()` albo pattern UUID,
- `merchantReference` - powinien odpowiadać wysłanemu requestowi,
- `displayName` - powinien odpowiadać oczyszczonej/walidowanej nazwie,
- `status` - `DRAFT`, `ACTIVE`, `SUSPENDED`, zależnie od endpointu,
- `createdAt` i `updatedAt` - pola czasu, raczej sprawdzane jako obecne/parseable na tym etapie, nie jako dokładny timestamp.

Omów przykładowe pola error response:

- `error` - stabilny kod maszynowy, np. `validation`, `not_found`, `invalid_transition`, `duplicate_merchant_reference`,
- `message` - czytelny komunikat,
- `details` - szczegóły walidacji albo dodatkowy kontekst.

## Ćwiczenia praktyczne do dodania

Dodaj ćwiczenia, minimum 10:

1. Dopisz mentalnie asercję `contentType(ContentType.JSON)` dla create merchant.
2. Dopisz asercję, że `merchantId` nie jest null.
3. Dopisz asercję, że `status` po create to `DRAFT`.
4. Dopisz asercję, że po activate status to `ACTIVE`.
5. Dopisz asercję, że malformed id zwraca `400`, `error=validation`, `message=Malformed merchant ID`.
6. Dopisz asercję, że duplicate reference zwraca `409` i `error=duplicate_merchant_reference`.
7. Dopisz asercję, że blank display name zwraca `details.displayName`.
8. Zaprojektuj header assertion dla `X-Correlation-ID`.
9. Wskaż, które asercje są weak oracle, a które strong oracle.
10. Porównaj REST Assured `.body(...)` z AssertJ po `.extract()`.

Każde ćwiczenie ma mieć odpowiedź PL i EN.

## Diagramy

Dodaj diagram Mermaid pokazujący:

request -> controller -> response DTO/error DTO -> HTTP response -> REST Assured `.then()` -> assertions -> protected API contract

Dodaj drugi diagram pokazujący siłę oracle:

status only -> status + content type -> status + key body fields -> status + body + error/header contract -> extracted data + AssertJ for complex checks

Używaj cytowanych etykiet Mermaid, szczególnie dla tekstów z `/`, `{id}`, `@...`, nawiasami albo znakami specjalnymi.

## Kryteria decyzji: czy rozszerzać testy teraz

Po analizie napisz jednoznacznie:

- Czy Lesson 6 jest już opisana wystarczająco? Prawdopodobnie nie; obecna wersja jest za krótka dla nowego tempa.
- Czy aplikacja jest wystarczająca? Prawdopodobnie tak; response behavior już istnieje.
- Czy testy są gotowe? Częściowo tak; istnieją liczne status/body assertions, ale warto rozważyć rozszerzenie o content type, pełniejsze error body i `X-Correlation-ID`.
- Czy rekomendujesz kodowanie w tej lekcji? Preferuj najpierw rozbudowę lekcji i promptu. Jeśli kodować, to tylko małe test-only ulepszenia assertions, bez nowej funkcji biznesowej.

## Weryfikacja jakości

Na końcu sprawdź:

1. Czy lekcja zaczyna od podstaw, ale ma intensywniejszy poziom niż Lessons 1-5?
2. Czy nie tworzy duplikatu, tylko rozbudowuje istniejącą Lesson 6?
3. Czy tłumaczy response assertions jako oracle, a nie składnię dla składni?
4. Czy pokazuje status, content type, body, error body i headers?
5. Czy wyjaśnia Hamcrest i AssertJ bez mylenia ich ról?
6. Czy używa realnych przykładów z repo?
7. Czy jasno mówi, że aplikacji nie trzeba rozszerzać dla Lesson 6?
8. Czy wskazuje, które testy są gotowe i co można ewentualnie poprawić?
9. Czy ma ćwiczenia, wskazówki i odpowiedzi PL/EN?
10. Czy nie wchodzi za głęboko w Lesson 7, 8 i 9?
11. Czy wskazuje następną lekcję: Nested Responses and Lists?

## Format odpowiedzi końcowej

Odpowiedz w strukturze:

1. Co przeanalizowałem
2. Czy Lesson 6 jest już opisana i czego jej brakuje
3. Czy aplikacja jest wystarczająca dla Lesson 6
4. Czy testy dla Lesson 6 są gotowe i na które patrzeć
5. Jakie zagadnienia będą w intensywniejszej Lesson 6
6. Co nowego w REST Assured, AssertJ, Java 25, Spring, DB/security
7. Jaki prompt przygotowałem i gdzie
8. Czy rekomenduję rozszerzać lekcję teraz, czy kodować test-only ulepszenia
9. Co przerobić jako następne
```
