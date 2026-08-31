# Playwright + TypeScript — trzecia iteracja: zamknięcie luk (2026-08-28)

## Wynik

Trzecia iteracja znalazła dwa istotne, wcześniej pominięte zbiory: blog
**Artema Bondara** oraz serię **Vitaliya Potapova**. Poszerzyła też katalog
**Butcha Mayhewa** o drugą stronę. To katalog URL-i i własnych streszczeń,
nie repozytorium pełnej treści cudzych artykułów.

## Artem Bondar — potwierdzony blog autorski

Strona [Bondar Academy blog](https://bondaracademy.com/blog) przypisuje niżej
wymienione wpisy Artemowi Bondarowi. Z mapy domeny wynika następujący komplet
znalezionych **relewantnych dla Playwright/TypeScript** artykułów:

| Data | Artykuł | Wartość dla praktyki |
| --- | --- | --- |
| 2026-07-13 | [Mock API responses](https://bondaracademy.com/blog/playwright-mock-api-response) | `page.route()` + `route.fulfill()`; mocking organizować tak, aby nie zacierał kontraktu E2E. |
| 2026-07-07 | [Projects configuration](https://bondaracademy.com/blog/playwright-projects-configuration-guide) | Projekty to także smoke/regression, dependencies i ustawienia per-project, nie tylko browser matrix. |
| 2026-06-29 | [Test PDF](https://bondaracademy.com/blog/how-to-test-pdf-in-playwright) | Assertions dla pobranego pliku i jego tekstu. |
| 2026-06-22 | [Read Excel](https://bondaracademy.com/blog/how-to-read-excel-files-in-playwright) | Dane tabelaryczne i typy TypeScript w testach data-driven. |
| 2026-06-15 | [Storage state](https://bondaracademy.com/blog/playwright-storage-state-authentication) | Jednorazowy auth setup i ponowne użycie sesji. |
| 2026-06-01 | [Data-driven testing](https://bondaracademy.com/blog/data-driven-testing-playwright) | Parametryzacja przez tablice zamiast kopiowania scenariuszy. |
| 2026-05-18 | [AI agents review](https://bondaracademy.com/blog/playwright-ai-agents-review) | Weryfikacja planner/generator/healer w realnym projekcie. |
| 2026-05-12 | [Codegen recorder](https://bondaracademy.com/blog/playwright-codegen-test-recorder) | Codegen jest szkicem: trzeba uzupełnić oracles, poprawić locatory i uprościć wygenerowany kod. |
| 2026-04-27 | [GitHub Actions](https://bondaracademy.com/blog/playwright-github-actions-setup) | Workflow, reporter GitHub i diagnostyka CI. |
| 2026-04-20 | [Fix flaky tests](https://bondaracademy.com/blog/how-to-fix-playwright-flaky-tests) | Race conditions i kruche locatory jako odrębne przyczyny flaków. |
| 2026-04-10 | [Page Object Model](https://bondaracademy.com/blog/what-is-page-object-model-in-test-automation) | Reuse metod bez przeinżynierowania obiektów strony. |
| 2026-04-03 | [JSON schema API testing](https://bondaracademy.com/blog/json-schema-testing-playwright) | Schemat jako kontrakt odpowiedzi API, zamiast ręcznie sprawdzać każdy klucz. |
| 2026-03-30 | [Fixtures](https://bondaracademy.com/blog/how-to-use-playwright-fixtures) | `beforeEach` kontra DI fixture, dependencies i worker scope. |
| 2026-03-27 | [Copy as Prompt](https://bondaracademy.com/blog/playwright-copy-as-prompt) | Kontekst failure jako wejście do narzędzia AI w debugowaniu. |
| 2026-03-23 | [Generate API tests with AI](https://bondaracademy.com/blog/generate-api-tests-with-ai-playwright) | HAR/API jako droga do szybszych testów, gdy UI nie jest przedmiotem weryfikacji. |
| 2026-03-18 | [getByRole](https://bondaracademy.com/blog/how-to-use-getbyrole-in-playwright) | Semantyczne role i granice użycia alternatywnych locatorów. |
| 2025-04-28 | [Automate API](https://bondaracademy.com/blog/how-to-atuomate-api-using-playwright) | Playwright API + TypeScript/custom framework. |
| 2024-08-15 | [Not waiting for elements](https://bondaracademy.com/blog/playwright-not-waiting-for-elements) | Diagnoza synchronizacji zamiast przypadkowego wydłużania timeoutu. |
| 2024-07-30 | [Locator best practices](https://bondaracademy.com/blog/playwright-locators-best-practices) | Atrybuty widoczne dla użytkownika, Locator Picker i odporność na zmianę UI. |
| 2024-07-16 | [Timeout 30000ms](https://bondaracademy.com/blog/playwright-timeout-30000ms-exceeded) | Rodzaje timeoutów, poprawa locatora i traktowanie wolnego endpointu osobno. |
| 2024-07-09 | [Expect assertions](https://bondaracademy.com/blog/how-to-use-playwright-expect-assertions) | Generic vs locator assertions oraz stabilne assertion style. |
| 2024-02-24 | [toBe vs toEqual](https://bondaracademy.com/blog/tobe-vs-toequal-assertion-playwright) | Różnica identity i deep equality. |
| 2024-01-29 | [Do not force](https://bondaracademy.com/blog/do-not-force-playwright) | `force: true` omija actionability i często maskuje realny problem UI. |

## Vitaliy Potapov — „Playwright in Pictures”

Mapa [serii](https://vitalets.github.io/posts/playwright-in-pictures/) ujawniła
trzy wpisy:

- [How Fixtures Work](https://vitalets.github.io/posts/playwright-in-pictures/how-fixtures-work/)
  (25 czerwca 2026) — najlepsze nowe źródło o lifecycle fixture: `test.extend`,
  lazy/auto fixtures, overrides, graf zależności i teardown w odwrotnej kolejności.
  Test scope daje świeżą instancję na test; worker scope oznacza jedną instancję
  **na worker**, a nie globalnie na cały run.
- [Why Workers Restart](https://vitalets.github.io/posts/playwright-in-pictures/why-worker-restarts/)
  — wpis z tej samej serii o granicach procesu workera; ważny dla zrozumienia,
  czemu współdzielenie stanu oraz retry wpływają na fixture lifecycle.
- [Fully Parallel](https://vitalets.github.io/posts/playwright-in-pictures/fully-parallel/)
  — wizualne ujęcie parallelismu; uzasadnia izolację danych na worker/test.

## Butch Mayhew — dodatkowe pozycje z drugiej strony katalogu

Odczyt [page 2](https://playwrightsolutions.com/page/2/) potwierdził kolejne,
niezapisane wcześniej artykuły:

- [Use testInfo.retry for flaky environments](https://playwrightsolutions.com/how-to-use-playwrights-testinforetry-to-deal-with-flakey-environments/)
  — retry-aware zachowanie powinno być jawne i ograniczone do znanej niestabilności.
- [Run failures only from the last run](https://playwrightsolutions.com/how-to-run-failures-only-from-the-last-playwright-run/)
  — targeted rerun przez `--last-failed`.
- [Indent list reporter](https://playwrightsolutions.com/custom-playwright-indent-list-reporter/)
  — czytelność outputu CLI jako element diagnostyki.
- [Feature-map framework](https://playwrightsolutions.com/tracking-automated-ui-testing-using-a-feature-map-with-playwright/)
  — śledzenie pokrycia i planu UI testów.
- [Codegen assertions in Playwright 1.40](https://playwrightsolutions.com/playwright-release-1-40-includes-ability-to-create-assertions-through-codegen-tool/)
  — codegen pomaga zacząć, ale generated assertion nadal wymaga review.
- [API test automation: CI/CD](https://playwrightsolutions.com/the-definitive-guide-to-api-test-automation-with-playwright-part-16-adding-ci-cd-through-github-actions/)
  — CI jako część skalowania API suite.
- [API test automation: tags](https://playwrightsolutions.com/the-definitive-guide-to-api-test-automation-with-playwright-part-15-adding-test-tags-to-get-targeted-feedback/)
  — tagowanie dla szybkiego, celowanego feedbacku.
- [Realistic data with Faker](https://playwrightsolutions.com/how-to-quickly-get-realistic-data-with-faker-for-playwright-tests/)
  — dane unikalne ograniczają współdzielony stan i flakiness.
- [apiRequestContext disposed](https://playwrightsolutions.com/how-to-fix-apirequestcontext-fetch-request-context-disposed/)
  — lifecycle API request context jest zasobem, nie globalnym singletonem.

## Co pozostaje negatywnym wynikiem

Wyszukiwanie Firecrawl potwierdziło profil/aktywnność, ale nie dostarczyło
samodzielnego publicznego artykułu z treścią i byline dla **Angeli Zelayi**,
**Viktora Konovalova** ani **Stefana Mincheva**. Dla Stefana istnieje
[wywiad ArchQA](https://idavidov.eu/playwright-test-architecture-stefan-minchev)
i LinkedIn post o architekturze, ale nie są to zweryfikowane artykuły jego
autorstwa do zarchiwizowania. Nie przypisuję im więc cudzych treści.

## Zastosowanie w repozytorium

Nowo zebrane materiały potwierdzają lokalne zasady: używać semantycznych
locatorów i web-first `expect`; fixture jako dependency injection z właściwym
scope; projects/dependencies + `storageState` dla auth; `test.step` dla
raportowalnych etapów biznesowych; oraz retry/polling jako precyzyjny mechanizm
obserwacji, nie substytut deterministycznych danych i synchronizacji.

## Granica kompletności

Katalog Bondar Academy oraz wskazane trzy wpisy Potapova zostały zmapowane w
tej sesji. Butch Mayhew ma dalszą paginację, więc jego całość nadal nie jest
udowodniona. Pełne teksty artykułów nie są zapisane z przyczyn praw autorskich.
