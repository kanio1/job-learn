---
type: prompt
status: completed
project: Payment Quality Engineering Lab
lesson: 01
date: 2026-05-21
completed_date: 2026-05-21
read: true
tags:
  - prompt
  - completed
  - read
  - rest-api-from-zero
  - merchant-registry
  - sdet
---

# Prompt - Lesson 01 - REST API Request Response Flow

```text
Jesteś moim agentem kodowania, mentorem Senior QA Automation/SDET oraz nauczycielem backend testingu.

Pracujemy w repozytorium:

/home/suso/job-learn

Projekt:

Payment Quality Engineering Lab

## Cel tej pracy

Przygotuj lub rozbuduj pierwszą praktyczną lekcję:

REST API From Zero - Merchant Request and Response Flow

To jest pierwsza praktyczna lekcja przed wejściem w REST Assured. Ma nauczyć mnie od zera, co dzieje się z requestem i responsem w realnym kodzie Merchant Registry.

## Tryb pracy

To jest lekcja oparta na istniejącym kodzie, nie sprint implementacyjny.

Nie dodawaj nowej funkcjonalności biznesowej.
Nie dodawaj Payment Order.
Nie dodawaj Kafki, GraphQL ani gRPC.

Możesz rozbudować vault i indeksy, jeśli lekcja lub jej linki są niepełne.

## Użyj skills

Użyj:

- `obsidian-learning-os`,
- `payment-quality-lab-orchestrator`,
- `spring-boot4-spring7-backend-architect`,
- `junit6-assertj-restassured-testcraft`,
- `test-analysis-design-and-data`,
- `rest-api-security-oauth-testing`, jeśli wchodzisz w JWT/Spring Security,
- `postgres18-data-architecture-and-risk`, jeśli wyjaśniasz zapis do PostgreSQL.

## Najpierw sprawdź istniejące materiały

Przeczytaj:

- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Payment Gateway SDET Learning Plan.md`
- `knowledge-vault/02 Areas/Technical Learning/REST API From Zero/Merchant Request and Response Flow.md`
- `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/README.md`
- `knowledge-vault/02 Areas/Technical Learning/Backend Testing Review/README.md`
- `docs/architecture/payment-gateway-roadmap-analysis.md`

Jeśli lekcja już istnieje, oceń czy ma pełną strukturę dydaktyczną. Jeśli jest niepełna, rozbuduj ją zamiast tworzyć duplikat.

## Przeczytaj kod aplikacji

Przeczytaj i powiąż lekcję z tymi plikami:

- `apps/frontend/app/pages/admin/merchants.vue`
- `apps/frontend/server/api/merchants/index.post.ts`
- `apps/frontend/server/utils/backendApi.ts`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverter.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantController.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/CreateMerchantRequest.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantResponse.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantService.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/Merchant.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/MerchantReference.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/DisplayName.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/infrastructure/JpaMerchantRepository.java`
- `apps/backend/src/main/resources/db/migration/merchant/V1__create_merchants.sql`
- `apps/backend/src/test/java/lab/paymentquality/rest/MerchantRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/MerchantSecurityTest.java`

## Sprint Learning Matrix dla tej lekcji

W lekcji uwzględnij:

| Sekcja | Odpowiedź |
|---|---|
| Business capability | Create merchant request/response flow |
| Previous knowledge refresh | brak, to pierwsza praktyczna lekcja |
| New learning focus | HTTP request -> backend -> response -> assertion |
| Java 25 focus | record/DTO, methods, exceptions na poziomie rozpoznawania |
| Spring focus | controller, DTO binding, validation, service delegation |
| SQL/PostgreSQL focus | repository zapisuje encję do tabeli `merchants` |
| REST Assured focus | REST Assured jako testowy klient HTTP, jeszcze bez głębokiej składni |
| Security/Keycloak focus | JWT/Bearer token i Spring Security jako brama requestu |
| Test design focus | happy path, validation error, duplicate error, auth denial |
| Test data focus | unikalny merchant reference |
| Test layers | REST API test, domain test, repository test, security test |
| Vault output | rozbudowana lekcja REST API From Zero |
| Interview story | how I trace an API request through backend layers |

## Obowiązkowa struktura lekcji

Lekcja musi mieć sekcje:

1. Tytuł PL + EN
2. Gdzie ta lekcja znajduje się w ścieżce nauki
3. Po co testerowi/SDET ta wiedza
4. Co już powinienem wiedzieć przed tą lekcją
5. Intuicyjne wyjaśnienie od zera
6. Słowniczek pojęć
7. Minimalny przykład request/response
8. Wyjaśnienie przykładu linia po linii
9. Bardziej profesjonalny przykład z obecnego repo
10. Jak ten temat pojawia się w obecnym kodzie aplikacji
11. Jakie ryzyko testowe ten temat pomaga zrozumieć
12. Jakie testy można z tego zaprojektować
13. Test design: EP, BVA, negative scenarios, data setup
14. Ćwiczenia praktyczne: zrozumienie, czytanie kodu, projektowanie testu, pytanie reviewera
15. Wskazówki do ćwiczeń
16. Odpowiedzi / przykładowe rozwiązania
17. Typowe błędy początkujących
18. Zasada jakości: KISS, layering, test design
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

UI -> Nuxt Server API -> Spring Security -> Controller -> DTO -> Validation -> Service -> Domain -> Repository -> PostgreSQL -> Response

Dodaj osobny diagram:

REST Assured -> Spring Security -> Controller -> DTO -> Service -> Repository -> PostgreSQL -> assertions

## Styl

Pisz po polsku, beginner-friendly, ale z perspektywą Senior QA/SDET.

Nie zakładaj, że znam Spring, REST Assured, JPA, DTO, JWT ani SQL.

Każdy termin wyjaśnij prostym językiem i powiąż z realnym plikiem repo.

## Weryfikacja jakości

Na końcu sprawdź:

1. Czy lekcja zaczyna od podstaw?
2. Czy nie tworzy duplikatu?
3. Czy ma ćwiczenia, wskazówki i odpowiedzi?
4. Czy linkuje realne pliki repo?
5. Czy ma perspektywę SDET?
6. Czy mówi o ryzykach testowych?
7. Czy wskazuje następną lekcję: REST Assured lesson 1 - What REST Assured Is?
8. Czy zaktualizowano indeks, jeśli było to potrzebne?

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
