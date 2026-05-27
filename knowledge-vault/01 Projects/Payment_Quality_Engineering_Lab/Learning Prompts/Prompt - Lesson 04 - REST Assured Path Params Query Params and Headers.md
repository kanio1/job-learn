---
type: prompt
status: ready
project: Payment Quality Engineering Lab
lesson: 04
date: 2026-05-25
tags:
  - prompt
  - rest-assured
  - backend-testing
  - path-params
  - query-params
  - headers
  - sdet
---

# Prompt - Lesson 04 - REST Assured Path Params Query Params and Headers

```text
Jesteś moim agentem kodowania, mentorem Senior QA Automation/SDET oraz nauczycielem backend testingu.

Pracujemy w repozytorium:

/home/suso/job-learn

Projekt:

Payment Quality Engineering Lab

## Cel tej pracy

Rozbuduj istniejącą lekcję REST Assured lesson 4:

Lesson 4 - Parametry Wejścia / Path Params, Query Params and Headers

To jest czwarta lekcja REST Assured po:

1. What REST Assured Is
2. Anatomy of given(), when(), then()
3. HTTP Method, Endpoint, Content-Type and Accept

Lekcja ma nauczyć mnie od zera, że dane wejściowe requestu API mogą trafić do różnych kanałów:

- path params,
- query params,
- headers,
- oraz że body jest osobnym kanałem, który będzie głębiej w Lesson 5.

To nadal jest lekcja dydaktyczna oparta na istniejącym kodzie, nie sprint implementacyjny.

## Decyzja o rozszerzaniu aplikacji

Najpierw przeanalizuj, czy aplikację trzeba rozszerzać.

Domyślna preferencja: nie rozszerzaj aplikacji.

Obecne repo ma już realne przykłady:

- path params: `GET /api/merchants/{id}`, `POST /api/merchants/{id}/activate`, `POST /api/merchants/{id}/suspend`,
- headers: `Authorization` przez REST Assured `.auth().oauth2(token)` w helperach oraz `X-Correlation-ID` w `CorrelationIdFilter`,
- query params: brak aktualnego merchant filtering contract, więc query params traktuj jako składnię i projektowy future topic, nie jako powód do dodania endpointu.

Jeśli uznasz, że aplikacji nie trzeba rozszerzać, wpisz to w lekcji wprost: mamy wystarczające przykłady path/header, a query param będzie omówiony edukacyjnie bez implementacji.

## Guardrails

Nie dodawaj nowej funkcjonalności biznesowej.
Nie dodawaj Payment Order.
Nie dodawaj Kafki, GraphQL ani gRPC.
Nie refaktoruj produkcyjnie testów.
Nie twórz nowego pliku lekcji, jeśli istniejąca sekcja Lesson 4 może zostać rozbudowana.
Nie implementuj filtrowania `/api/merchants?status=ACTIVE` tylko po to, żeby mieć query param.
Nie wchodź głęboko w JWT ani Keycloak. `Authorization` traktuj jako przykład headera, a auth zostaw do osobnej lekcji.

Preferencja: rozbuduj istniejącą sekcję Lesson 4 w:

`knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/01-12 REST Assured Foundations.md`

## Użyj skills

Użyj:

- `obsidian-learning-os`,
- `payment-quality-lab-orchestrator`,
- `junit6-assertj-restassured-testcraft`,
- `test-analysis-design-and-data`,
- `spring-boot4-spring7-backend-architect`, jeśli wyjaśniasz `@PathVariable`, `@RequestParam`, `@RequestHeader`, controller mapping albo filter.

Nie używaj `rest-api-security-oauth-testing` głęboko. Jeśli wspomnisz tokeny, tylko jako kontekst headera `Authorization` i helperów `operatorRequest(port)` / `requestWithToken(port, token)`.

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
- `apps/backend/src/test/java/lab/paymentquality/security/MerchantSecurityTest.java`, tylko jako kontekst headera Authorization i 401/403
- `apps/backend/src/test/java/lab/paymentquality/testsupport/MerchantApiTestSupport.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantController.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/web/CorrelationIdFilter.java`, jeśli istnieje
- `apps/backend/src/test/java/lab/paymentquality/merchant/internal/web/MerchantControllerTest.java`, jeśli istnieje, jako unit-level kontekst malformed UUID

Sprawdź, czy w produkcyjnym kodzie istnieją `@RequestParam` albo `@RequestHeader`. Jeśli nie ma ich dla Merchant Registry, zaznacz to w lekcji i nie udawaj, że query/header controller mapping już istnieje.

## Sprint Learning Matrix dla tej lekcji

Uwzględnij:

| Sekcja | Odpowiedź |
|---|---|
| Business capability | Brak nowej funkcji; uczymy się, gdzie request przenosi dane wejściowe |
| Previous knowledge refresh | Request intent z Lesson 3: method, endpoint, headers formatu, body, expected status |
| New learning focus | Path params, query params i headers |
| Java 25 focus | `String id`, `UUID.randomUUID()`, `UUID.fromString(...)`, method chaining, static imports |
| Spring focus | `@PathVariable`, `@GetMapping("/{id}")`, `@PostMapping("/{id}/activate")`, zapowiedź `@RequestParam` i `@RequestHeader` |
| SQL/PostgreSQL focus | Brak głębokiego SQL; GET by id i list korzystają z DB, ale lekcja skupia się na HTTP contract |
| REST Assured focus | `.pathParam(...)`, placeholder `{id}`, wariant `.get("/path/{id}", id)`, `.queryParam(...)`, `.header(...)`, auth helper context |
| Security/Keycloak focus | Tylko kontekst: `Authorization` jest headerem ustawianym przez `.auth().oauth2(token)` w helperach; głęboka lekcja auth później |
| Test design focus | Czy wartość jest identyfikatorem zasobu, filtrem/kontrolą kolekcji czy metadanym requestu |
| Test data focus | Utworzony merchant id dla path param; brak danych biznesowych dla correlation id; query params tylko edukacyjnie obecnie |
| Test layers | REST API contract test plus selective security/header tests, nie UI test |
| Vault output | Rozbudowana Lesson 4 w istniejącym lesson-packu oraz prompt dla Lesson 4 |
| Interview story | I decide where request data belongs: path identifies resources, query filters collections, headers carry metadata |

## Obowiązkowa struktura lekcji

Rozbuduj Lesson 4 tak, aby miała sekcje:

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
13. Test design: resource identity, filtering, metadata, expected status, oracle
14. Ćwiczenia praktyczne: rozpoznawanie path, query, headers i expected status
15. Wskazówki do ćwiczeń
16. Odpowiedzi / przykładowe rozwiązania
17. Typowe błędy początkujących
18. Zasada jakości: right input channel, contract clarity, no fake query support
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

- co to jest path param,
- co oznacza placeholder `{id}`,
- czym różni się `.pathParam("id", id)` od `.get("/api/merchants/{id}", id)`,
- co to jest query param,
- dlaczego query param zwykle filtruje lub steruje listą,
- dlaczego obecne repo nie wymaga implementacji query params,
- co to jest header,
- dlaczego `Authorization` jest headerem,
- dlaczego `X-Correlation-ID` jest headerem technicznym,
- dlaczego `merchantId` dla `GET /api/merchants/{id}` jest path paramem,
- dlaczego `not-a-uuid` powinno prowadzić do `400`,
- dlaczego poprawny, ale nieistniejący UUID powinien prowadzić do `404`,
- dlaczego kanał danych wejściowych jest częścią API contract.

## Powiąż z obecnym repo

Użyj realnych przykładów:

- `MerchantRestAssuredTest#createReadListActivateAndSuspendMerchant` jako `GET /api/merchants/{id}`, activate i suspend,
- `MerchantRestAssuredTest#notFoundMalformedAndInvalidTransitionErrors` jako `400` malformed UUID i `404` unknown UUID,
- `MerchantRestAssuredTest#suspendValidAndInvalidTransitions` jako path param przy status action,
- `MerchantSecurityTest` jako kontekst headera `Authorization`, bez głębokiej lekcji JWT,
- `MerchantApiTestSupport#requestWithToken` jako helper, który ustawia auth header przez `.auth().oauth2(token)`,
- `MerchantController` jako produkcyjne mapowanie `@GetMapping("/{id}")`, `@PostMapping("/{id}/activate")`, `@PathVariable String id`, `UUID.fromString(id)`,
- `CorrelationIdFilter` jako przykład `X-Correlation-ID`, jeśli istnieje,
- `MyMerchantRestAssuredTest` jako sandbox do ćwiczeń, jeśli istnieje.

## Diagramy

Dodaj diagram Mermaid pokazujący:

HTTP request -> path param / query param / headers / body -> Spring mapping/filter/security -> response -> assertions

Dodaj diagram obecnego repo:

REST Assured test -> GET/POST `/api/merchants/{id}` -> `@PathVariable String id` -> `UUID.fromString` -> valid id / malformed id / unknown id -> 200 / 400 / 404

## Styl

Pisz po polsku, beginner-friendly, ale z perspektywą Senior QA/SDET.

Nie zakładaj, że znam:

- path params,
- query params,
- headers,
- `@PathVariable`,
- `@RequestParam`,
- `@RequestHeader`,
- `Authorization`,
- `X-Correlation-ID`,
- `UUID`.

Każdy termin wyjaśnij prostym językiem i przypnij do realnego pliku repo, jeśli istnieje.

Nie przeciążaj lekcji szczegółami JWT, walidacji domenowej, extraction ani pełnej obsługi query filtering. To będą późniejsze lekcje lub przyszłe funkcje.

## Weryfikacja jakości

Na końcu sprawdź:

1. Czy lekcja zaczyna od podstaw?
2. Czy nie tworzy duplikatu?
3. Czy wyjaśnia path params, query params i headers?
4. Czy jasno mówi, że query params są obecnie edukacyjne, bo brak kontraktu filtrowania w repo?
5. Czy pokazuje różnicę między path identity, query filtering i header metadata?
6. Czy używa realnych przykładów z repo?
7. Czy nie wymaga rozszerzania aplikacji?
8. Czy ma ćwiczenia, wskazówki i odpowiedzi?
9. Czy mówi o ryzyku złego kanału wejścia?
10. Czy wyjaśnia `400` malformed id vs `404` not found?
11. Czy wskazuje następną lekcję: Request Body, JSON, `Map.of`, DTO and Serialization?

## Format odpowiedzi końcowej

Odpowiedz w strukturze:

1. Co rozbudowałem
2. Ścieżka do pliku lekcji
3. Czy trzeba było rozszerzać aplikację i dlaczego
4. Jak lekcja wpisuje się w kolejność nauki
5. Na jakie testy mam spojrzeć
6. Jakie zagadnienia są do nauki
7. Co nowego w REST Assured, Java 25, Spring, DB/security
8. Jakie ćwiczenia dodałem
9. Jaki prompt przygotowałem i gdzie
10. Co przerobić jako następne
```
