---
type: prompt
status: ready
project: Payment Quality Engineering Lab
lesson: 05
date: 2026-05-26
tags:
  - prompt
  - rest-assured
  - backend-testing
  - request-body
  - json
  - dto
  - serialization
  - sdet
---

# Prompt - Lesson 05 - REST Assured Request Body JSON Map DTO Serialization

```text
Jesteś moim agentem kodowania, mentorem Senior QA Automation/SDET oraz nauczycielem backend testingu.

Pracujemy w repozytorium:

/home/suso/job-learn

Projekt:

Payment Quality Engineering Lab

## Cel tej pracy

Przeanalizuj i rozbuduj istniejącą lekcję REST Assured lesson 5:

Lesson 5 - Request Body, JSON, `Map.of`, DTO and Serialization

To jest piąta lekcja REST Assured po:

1. What REST Assured Is
2. Anatomy of given(), when(), then()
3. HTTP Method, Endpoint, Content-Type and Accept
4. Path Params, Query Params and Headers

Lekcja ma nauczyć mnie od zera, jak REST Assured wysyła body requestu i jak Java object/mapa/DTO zmienia się w JSON.

Główne pytanie lekcji:

Jak bezpiecznie, czytelnie i testowalnie zbudować request body dla `POST /api/merchants`?

To nadal jest lekcja dydaktyczna oparta na istniejącym kodzie, nie sprint implementacyjny.

## Decyzja o rozszerzaniu aplikacji

Najpierw przeanalizuj, czy aplikację trzeba rozszerzać.

Domyślna decyzja: nie rozszerzaj aplikacji.

Obecne repo ma już wystarczające przykłady dla tej lekcji:

- `POST /api/merchants` wymaga JSON body,
- `MerchantRestAssuredTest#createMerchant` używa `.contentType(ContentType.JSON)` i `.body(createMerchantBody(...))`,
- `MerchantApiTestSupport#createMerchantBody` buduje mapę pól requestu,
- `CreateMerchantRequest` jest Spring request DTO jako Java `record`,
- `MerchantController#create` ma `@RequestBody CreateMerchantRequest`,
- istnieją pozytywne i negatywne testy walidacji body.

Jeśli uznasz, że aplikacji nie trzeba rozszerzać, wpisz to w lekcji wprost. Lesson 5 ma uczyć na istniejącym Merchant create flow, a nie dodawać nowy endpoint.

## Guardrails

Nie dodawaj nowej funkcjonalności biznesowej.
Nie dodawaj Payment Order.
Nie dodawaj Kafki, GraphQL ani gRPC.
Nie refaktoruj produkcyjnie testów.
Nie twórz nowego pliku lekcji, jeśli istniejąca sekcja Lesson 5 może zostać rozbudowana.
Nie dodawaj nowego endpointu tylko po to, żeby pokazać DTO albo serializację.
Nie wchodź głęboko w security/JWT. `operatorRequest(port)` traktuj tylko jako helper requestu z autoryzacją.
Nie rób pełnej lekcji o validation/domain rules; wspomnij je tylko tam, gdzie body powoduje `400`.

Preferencja: rozbuduj istniejącą sekcję Lesson 5 w:

`knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/01-12 REST Assured Foundations.md`

## Użyj skills

Użyj:

- `obsidian-learning-os`,
- `payment-quality-lab-orchestrator`,
- `junit6-assertj-restassured-testcraft`,
- `test-analysis-design-and-data`,
- `spring-boot4-spring7-backend-architect`, jeśli wyjaśniasz `@RequestBody`, DTO binding, `record`, validation albo controller mapping.

Nie używaj `rest-api-security-oauth-testing` głęboko. Jeśli wspominasz tokeny, tylko jako kontekst `operatorRequest(port)`.

## Najpierw przeczytaj

Przeczytaj:

- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Payment Gateway SDET Learning Plan.md`
- `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/README.md`
- `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/README.md`
- `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/01-12 REST Assured Foundations.md`
- `knowledge-vault/02 Areas/Technical Learning/REST API From Zero/Merchant Request and Response Flow.md`

Przeczytaj kod:

- `apps/backend/src/test/java/lab/paymentquality/rest/MerchantRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/MyMerchantRestAssuredTest.java`, jeśli istnieje, jako disabled sandbox do ćwiczeń
- `apps/backend/src/test/java/lab/paymentquality/security/MerchantSecurityTest.java`, tylko jako dodatkowe przykłady `.contentType(ContentType.JSON)` i `.body(...)`, bez głębokiego security
- `apps/backend/src/test/java/lab/paymentquality/testsupport/MerchantApiTestSupport.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/CreateMerchantRequest.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantController.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantExceptionHandler.java`, jeśli istnieje, jako kontekst `400 validation`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantResponse.java`, tylko dla rozróżnienia request DTO vs response DTO

Jeśli któryś plik nie istnieje, nie zgaduj. Zaznacz obserwację i użyj istniejących plików.

## Sprint Learning Matrix dla tej lekcji

Uwzględnij:

| Sekcja | Odpowiedź |
|---|---|
| Business capability | Brak nowej funkcji; uczymy się budowy body dla istniejącego create merchant request |
| Previous knowledge refresh | Method, endpoint, `Content-Type`, path/query/header z poprzednich lekcji |
| New learning focus | Request body, JSON, `Map.of`, helper mapy, DTO/record, serialization |
| Java 25 focus | `Map.of`, `Map.copyOf`, `LinkedHashMap`, `record CreateMerchantRequest`, immutability, method chaining |
| Spring focus | `@RequestBody`, DTO binding, `@Valid`, Bean Validation, `CreateMerchantRequest` jako request contract |
| SQL/PostgreSQL focus | Brak głębokiego SQL; successful body może prowadzić do zapisu merchanta, ale lekcja skupia się na HTTP body contract |
| REST Assured focus | `.contentType(ContentType.JSON)`, `.body(...)`, serializacja mapy/obiektu do JSON, kiedy użyć mapy vs DTO/helper |
| Security/Keycloak focus | Tylko kontekst: `operatorRequest(port)` przygotowuje authorized request; auth będzie osobną lekcją |
| Test design focus | Czy body ma wymagane pola, poprawne nazwy pól, wartości boundary/invalid i czy oracle sprawdza response/error |
| Test data focus | Unikalny `merchantReference`; payload valid/invalid/duplicate |
| Test layers | REST API contract/validation tests plus domain tests dla reguł szczegółowych, nie UI test |
| Vault output | Rozbudowana Lesson 5 w istniejącym lesson-packu oraz prompt dla Lesson 5 |
| Interview story | I build request bodies explicitly and choose the simplest stable payload representation for the test |

## Obowiązkowa struktura lekcji

Rozbuduj Lesson 5 tak, aby miała sekcje:

1. Tytuł PL + EN
2. Gdzie ta lekcja znajduje się w ścieżce nauki
3. Po co testerowi/SDET ta wiedza
4. Co już powinienem wiedzieć przed tą lekcją
5. Intuicyjne wyjaśnienie od zera
6. Słowniczek pojęć
7. Minimalny przykład REST Assured
8. Wyjaśnienie przykładu linia po linii
9. Bardziej profesjonalny przykład z obecnego repo
10. Jak ten temat pojawia się w obecnym kodzie testów
11. Jakie ryzyko testowe ten temat pomaga zrozumieć
12. Jakie testy można z tego zaprojektować
13. Test design: required fields, field names, valid/invalid payloads, oracle
14. Ćwiczenia praktyczne: body, JSON, `Map.of`, helper, DTO, serialization
15. Wskazówki do ćwiczeń
16. Odpowiedzi / przykładowe rozwiązania
17. Typowe błędy początkujących
18. Zasada jakości: simplest stable payload, explicit field names, no raw JSON unless needed
19. Perspektywa Senior QA Automation/SDET
20. Pytania, które powinienem sobie zadać podczas pracy
21. Mini quiz kontrolny
22. Pytania rekrutacyjne po angielsku + przykładowe odpowiedzi
23. Powiązane pliki w repo
24. Powiązane notatki w vault
25. Co przerobić następnie
26. Zapamiętaj

## Tematy beginner-friendly

Wyjaśnij prosto:

- co to jest request body,
- dlaczego `POST /api/merchants` potrzebuje body,
- czym jest JSON,
- co robi `.body(...)` w REST Assured,
- dlaczego `.contentType(ContentType.JSON)` musi iść z JSON body,
- co to jest serializacja Java object/map -> JSON,
- co to jest `Map.of`,
- czym różni się `Map.of` od helpera `createMerchantBody(...)`,
- dlaczego helper używa `LinkedHashMap` i `Map.copyOf`,
- co to jest DTO,
- co to jest Java `record`,
- co robi Spring `@RequestBody`,
- co robi `@Valid` na DTO,
- czym różni się request DTO `CreateMerchantRequest` od response DTO `MerchantResponse`,
- kiedy lepsza jest mapa, kiedy helper, kiedy DTO/record,
- dlaczego ręczny JSON string jest ryzykowny,
- dlaczego literówka w nazwie pola, np. `displayNAme`, może prowadzić do mylącego testu.

## Powiąż z obecnym repo

Użyj realnych przykładów:

- `MerchantRestAssuredTest#createMerchant` jako główny przykład `.contentType(ContentType.JSON)` i `.body(createMerchantBody(...))`,
- `MerchantRestAssuredTest#createReadListActivateAndSuspendMerchant` jako happy path payload i response oracle,
- `MerchantRestAssuredTest#createValidationAndDuplicateErrors` jako invalid payloads i duplicate payload,
- `MerchantApiTestSupport#createMerchantBody` jako helper budujący mapę body,
- `CreateMerchantRequest` jako Spring request DTO/record,
- `MerchantController#create` jako `@RequestBody` i `ResponseEntity.status(HttpStatus.CREATED)`,
- `MerchantExceptionHandler` jako `400 validation` dla niepoprawnego body,
- `MyMerchantRestAssuredTest` jako sandbox, jeśli istnieje, szczególnie do wykrywania literówek w body/asercjach.

## Diagramy

Dodaj diagram Mermaid pokazujący:

REST Assured `.body(...)` -> Java Map/DTO -> JSON serialization -> HTTP request body -> Spring `@RequestBody` -> `CreateMerchantRequest` -> validation -> response

Dodaj prosty diagram porównujący:

raw JSON string vs `Map.of` vs helper mapy vs DTO/record

z punktu widzenia czytelności, ryzyka literówki, reużywalności i kiedy używać.

## Styl

Pisz po polsku, beginner-friendly, ale z perspektywą Senior QA/SDET.

Nie zakładaj, że znam:

- request body,
- JSON,
- serializację,
- DTO,
- Java record,
- `Map.of`,
- `Map.copyOf`,
- `LinkedHashMap`,
- `@RequestBody`,
- `@Valid`,
- Bean Validation.

Każdy termin wyjaśnij prostym językiem i przypnij do realnego pliku repo.

Nie przeciążaj lekcji szczegółami Jacksona, pełną walidacją domenową, security, extraction ani DB constraints. To są osobne lekcje.

## Weryfikacja jakości

Na końcu sprawdź:

1. Czy lekcja zaczyna od podstaw?
2. Czy nie tworzy duplikatu?
3. Czy jasno tłumaczy request body, JSON, `.body(...)` i serialization?
4. Czy pokazuje związek `.contentType(ContentType.JSON)` z `.body(...)`?
5. Czy używa realnych przykładów z repo?
6. Czy nie wymaga rozszerzania aplikacji?
7. Czy wskazuje, które testy są już gotowe dla tej lekcji?
8. Czy ma ćwiczenia, wskazówki i odpowiedzi?
9. Czy mówi o ryzyku raw JSON stringów i literówek w field names?
10. Czy wyjaśnia mapę vs helper vs DTO/record?
11. Czy wskazuje następną lekcję: PayU-like Business Flow jako accelerated response-contract sprint?

## Format odpowiedzi końcowej

Odpowiedz w strukturze:

1. Co przeanalizowałem
2. Czy Lesson 5 jest już opisana i czego jej brakuje
3. Czy aplikacja jest wystarczająca dla Lesson 5
4. Czy testy dla Lesson 5 są gotowe i na które patrzeć
5. Jakie zagadnienia będą w Lesson 5
6. Co nowego w REST Assured, Java 25, Spring, DB/security
7. Jaki prompt przygotowałem i gdzie
8. Czy rekomenduję rozszerzać lekcję teraz, czy dopiero przy wykonaniu promptu
9. Co przerobić jako następne
```
