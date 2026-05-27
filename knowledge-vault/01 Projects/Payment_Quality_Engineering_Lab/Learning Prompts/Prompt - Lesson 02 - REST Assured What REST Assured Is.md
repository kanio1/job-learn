---
type: prompt
status: ready
project: Payment Quality Engineering Lab
lesson: 02
date: 2026-05-21
updated: 2026-05-25
tags:
  - prompt
  - rest-assured
  - backend-testing
  - sdet
---

# Prompt - Lesson 02 - REST Assured What REST Assured Is

```text
Jesteś moim agentem kodowania, mentorem Senior QA Automation/SDET oraz nauczycielem backend testingu.

Pracujemy w repozytorium:

/home/suso/job-learn

Projekt:

Payment Quality Engineering Lab

## Cel tej pracy

Przygotuj lub rozbuduj kolejną lekcję po REST API From Zero:

REST Assured lesson 1 - What REST Assured Is

To jest pierwsza lekcja REST Assured po zrozumieniu request/response flow. Ma nauczyć mnie od zera, czym jest REST Assured jako testowy klient HTTP, zanim przejdziemy do szczegółowej składni `given()`, `.when()`, `.then()`.

Uwaga o numeracji: to jest Lesson 02 w projekcie learning prompts, ale Lesson 1 w ścieżce REST Assured.

## Tryb pracy

To jest lekcja dydaktyczna oparta na istniejącym kodzie testów, nie sprint implementacyjny.

Nie dodawaj nowej funkcjonalności biznesowej.
Nie dodawaj Payment Order.
Nie dodawaj Kafki, GraphQL ani gRPC.
Nie refaktoruj testów produkcyjnie, chyba że użytkownik wyraźnie o to poprosi.

Możesz rozbudować vault i indeksy, jeśli lekcja lub jej linki są niepełne.

## Użyj skills

Użyj:

- `obsidian-learning-os`,
- `payment-quality-lab-orchestrator`,
- `junit6-assertj-restassured-testcraft`,
- `test-analysis-design-and-data`,
- `spring-boot4-spring7-backend-architect`, jeśli wyjaśniasz uruchomienie backendu w teście,
- `rest-api-security-oauth-testing`, tylko jeśli wspominasz o tokenach jako przyszłym kroku,
- `official-docs-and-versioned-research`, jeśli weryfikujesz wersję lub oficjalne zachowanie REST Assured.

## Najpierw sprawdź istniejące materiały

Przeczytaj:

- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Payment Gateway SDET Learning Plan.md`
- `knowledge-vault/02 Areas/Technical Learning/REST API From Zero/README.md`
- `knowledge-vault/02 Areas/Technical Learning/REST API From Zero/Merchant Request and Response Flow.md`
- `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/README.md`
- `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/README.md`
- `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/01-12 REST Assured Foundations.md`
- `knowledge-vault/02 Areas/Technical Learning/Backend Testing Review/README.md`
- `knowledge-vault/02 Areas/Technical Learning/REST API From Zero/REST REST Assured Java - Session Summary - Merchant API Tests.md`

Jeśli `Lesson 1 - Czym Jest REST Assured / What REST Assured Is` już istnieje, oceń czy ma pełną strukturę dydaktyczną. Jeśli jest niepełna, rozbuduj tę sekcję zamiast tworzyć duplikat.

Preferencja: rozbuduj istniejącą sekcję `Lesson 1` w pliku `01-12 REST Assured Foundations.md`, a nie twórz osobnego pliku, chyba że istniejąca struktura vault wyraźnie wymaga ekstrakcji.

Nie powielaj pełnego flow z Lesson 01. Lesson 01 już tłumaczy request -> security -> controller -> service -> repository -> PostgreSQL -> response. W Lesson 02 skup się na tym, czym REST Assured jest jako narzędzie testowe, co zastępuje, gdzie żyje w testach i jak czytać najprostszy test HTTP.

## Przeczytaj kod testów

Przeczytaj i powiąż lekcję z tymi plikami:

- `apps/backend/src/test/java/lab/paymentquality/rest/StatusRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/MerchantRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/MyMerchantRestAssuredTest.java`, jako disabled sandbox do samodzielnego przepisywania testów
- `apps/backend/src/test/java/lab/paymentquality/security/MerchantSecurityTest.java`, tylko jako zapowiedź auth tests, bez wchodzenia głęboko w JWT
- `apps/backend/src/test/java/lab/paymentquality/testsupport/MerchantApiTestSupport.java`, jeśli istnieje i pomaga wyjaśnić helpery
- `apps/backend/src/test/java/lab/paymentquality/testsupport/TestJwtSupport.java`, tylko jeśli wspominasz o tokenach jako przyszłym kroku
- `apps/backend/src/main/java/lab/paymentquality/shared/web/StatusController.java`, jeśli istnieje i jest używany przez status test
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantController.java`, jako przykład realnego API testowanego później

Jeśli któryś plik nie istnieje, nie zgaduj. Zaznacz to w lekcji jako obserwację i użyj istniejących plików.

## Sprint Learning Matrix dla tej lekcji

W lekcji uwzględnij:

| Sekcja | Odpowiedź |
|---|---|
| Business capability | Brak nowej funkcji; uczymy się testowego klienta HTTP na istniejącym backendzie |
| Previous knowledge refresh | Request/response flow z Merchant Registry |
| New learning focus | REST Assured jako programowalny klient HTTP w teście JUnit |
| Java 25 focus | Static import, method chaining, fluent API na poziomie rozpoznawania |
| Spring focus | Test uruchamia aplikację na porcie testowym i wysyła realny HTTP request |
| SQL/PostgreSQL focus | Brak głębokiego SQL; DB pojawi się później przy testach merchant flow |
| REST Assured focus | Czym jest REST Assured, co zastępuje, co sprawdza, czego jeszcze nie robi magicznie |
| Security/Keycloak focus | Tylko zapowiedź: tokeny i 401/403 będą osobną lekcją |
| Test design focus | Smoke/API contract, status code vs body assertion, oracle testowy |
| Test data focus | Brak danych biznesowych dla `/api/status`; później unikalny merchant reference |
| Test layers | REST API smoke test, REST API contract test, nie UI test |
| Vault output | Rozbudowana lekcja REST Assured lesson 1 |
| Interview story | I use REST Assured to test backend APIs directly without relying on UI |

## Granica względem Lesson 01

Lesson 02 może krótko przypomnieć:

- request vs response,
- status vs body assertion,
- REST Assured jako klient HTTP.

Lesson 02 nie powinna ponownie szczegółowo tłumaczyć:

- pełnego Merchant Registry request/response flow,
- DTO/domain/repository/PostgreSQL flow,
- pełnej security matrix 401/403,
- test design BVA/EP dla merchant reference.

Te tematy są już w Lesson 01 albo wrócą później w dedykowanych lekcjach.

## Obowiązkowa struktura lekcji

Lekcja musi mieć sekcje:

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
13. Test design: smoke, contract, oracle, negative thinking preview
14. Ćwiczenia praktyczne: zrozumienie, czytanie testu, projektowanie assertion, pytanie reviewera
15. Wskazówki do ćwiczeń
16. Odpowiedzi / przykładowe rozwiązania
17. Typowe błędy początkujących
18. Zasada jakości: KISS, readable assertions, no premature helpers
19. Perspektywa Senior QA Automation/SDET
20. Pytania, które powinienem sobie zadać podczas pracy
21. Mini quiz kontrolny
22. Pytania rekrutacyjne po angielsku + przykładowe odpowiedzi
23. Powiązane pliki w repo
24. Powiązane notatki w vault
25. Co przerobić następnie
26. Zapamiętaj

## Diagramy

Dodaj lub popraw diagram Mermaid pokazujący:

JUnit test method -> REST Assured -> HTTP request -> Spring Boot app -> HTTP response -> assertions -> test result

Dodaj osobny diagram mentalny:

given -> when -> then jako zapowiedź następnej lekcji, bez głębokiej składni

## Styl

Pisz po polsku, beginner-friendly, ale z perspektywą Senior QA/SDET.

Nie zakładaj, że znam REST Assured, JUnit, fluent API, static import, Spring Boot test, random port ani assertions.

Każdy termin wyjaśnij prostym językiem i powiąż z realnym plikiem repo.

Nie przeciążaj lekcji składnią. To jest lekcja „czym jest REST Assured”, nie pełny kurs DSL.

## Weryfikacja jakości

Na końcu sprawdź:

1. Czy lekcja zaczyna od podstaw?
2. Czy nie tworzy duplikatu?
3. Czy jasno mówi, że REST Assured jest klientem testowym, nie częścią aplikacji?
4. Czy wyjaśnia różnicę między testem API a testem UI?
5. Czy ma ćwiczenia, wskazówki i odpowiedzi?
6. Czy linkuje realne pliki repo?
7. Czy ma perspektywę SDET?
8. Czy mówi o ryzykach testowych i oracle testowym?
9. Czy wskazuje następną lekcję: Anatomy of `given()`, `when()`, `then()`?
10. Czy wykorzystuje `MyMerchantRestAssuredTest` jako miejsce ćwiczeń, ale nie wymusza implementacji nowej funkcji?
11. Czy zaktualizowano indeks, jeśli było to potrzebne?

## Format odpowiedzi końcowej

Odpowiedz w strukturze:

1. Co utworzyłem lub rozbudowałem
2. Ścieżka do pliku lekcji
3. Jak wybrałem tę lekcję z roadmapy
4. Jak lekcja wpisuje się w kolejność nauki
5. Jakie ćwiczenia dodałem
6. Jakie pliki repo powiązałem
7. Jakie indeksy zaktualizowałem
8. Co przerobić jako następne
```
