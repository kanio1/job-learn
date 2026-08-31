# Playwright + TypeScript — druga iteracja: uzupełnione źródła (2026-08-28)

## Cel

Druga iteracja sprawdza dokładnie luki z pierwszego indeksu. Wynik jest
**katalogiem z autorskimi streszczeniami i linkami**, a nie kopią pełnych
tekstów chronionych artykułów. Źródła z pierwszej iteracji pozostają w
[`playwright-typescript-practitioner-source-index-2026-08-28.md`](playwright-typescript-practitioner-source-index-2026-08-28.md).

## Nowe źródła potwierdzone przez Firecrawl

### Anton Gulin

- [Playwright Best Practices: 14 Rules AI Agents Get Wrong (2026)](https://www.anton.qa/blog/posts/playwright-best-practices)
  (17 czerwca 2026) — łączy role/label locators, web-first assertions,
  izolację, API-driven setup, reuse auth, trace na pierwszym retry,
  równoległość i cienkie POM-y. Ostrzega przed hard waits, warunkami `if`
  maskującymi błędy, nadmiarem test-id i assertions ukrytymi w POM.
- [Should Page Objects Assert? Where Test Assertions Belong](https://www.anton.qa/blog/posts/where-test-assertions-belong)
  — assertion biznesowa powinna zostać w scenariuszu; POM nie powinien
  przejmować widocznych oracle.
- [The Modern Page Object Model: Less Shared Code, Easier Changes](https://www.anton.qa/blog/posts/modern-page-object-model)
  — POM daje małe API nad ekranem, zamiast stawać się wspólnym frameworkiem.
- [Test Retries Hide Real Bugs](https://www.anton.qa/blog/posts/test-retries-hide-real-bugs)
  — retry służy do diagnostyki, nie do ukrywania problemu synchronizacji lub danych.

### Joseph Ward
- [Why Simple UI Tests Become Slow](https://josephward.tech/2026-06-30-why-simple-ui-tests-become-slow)
  — analiza, czemu proste akcje/iteracje po tabeli mogą mieć nieproporcjonalny koszt.
- [Looking Behind Playwright's Magic](https://josephward.tech/2026-07-07-looking-behind-playwrights-magic-edited)
  — wykryty wpis Playwright; treść nie była dostępna, więc bez przypisywania szczegółowych tez.

### ScrollTest / Pramod Dutta

- [Day 6: Fixtures — Dependency Injection That Eliminates Boilerplate](https://scrolltest.com/21-day-playwright-day-6-fixtures-dependency-injection/)
  (15 sierpnia 2026) — potwierdzony autor Pramod Dutta; fixture ma typowane
  zależności, cleanup po `use()` i pozwala testowi deklarować potrzebny setup.
- [Day 2: Locator Strategies — Why getByRole Wins](https://scrolltest.com/21-day-playwright-day-2-locator-strategies-getbyrole/)
  — user-facing locator jako kontrakt stabilniejszy od XPath/CSS.
- [Day 3: Assertions That Actually Catch Bugs — expect() Deep Dive](https://scrolltest.com/21-day-playwright-day-3-assertions-expect-deep-dive/)
  — auto-retrying assertions, soft assertions i wąsko dobrane timeouty.
- [Day 5: Page Object Model — Structure Tests That Scale](https://scrolltest.com/21-day-playwright-day-5-page-object-model-structure/)
  — POM, układ testów oraz antywzorce.
- [Day 13: Debugging — Trace Viewer, UI Mode, and Inspector](https://scrolltest.com/21-day-playwright-day-13-debugging-trace-viewer-ui-mode/)
  (22 sierpnia 2026) — trace/UI Mode/Inspector jako dowód przy diagnozie.
- [Day 19: Advanced Patterns — Retry, Tags, Parameterization, and Hooks](https://scrolltest.com/21-day-playwright-day-19-advanced-patterns-retry-tags/)
  (28 sierpnia 2026) — retries, tagowanie, parametryzacja i hooks.
- [Playwright Global Setup and Teardown with TypeScript](https://scrolltest.com/playwright-global-setup-teardown-typescript-day-58/)
  (22 sierpnia 2026) — one-time setup/cleanup; porównywać z setup projects, nie zastępować ich mechanicznie.
- [Playwright TypeScript Checklist](https://scrolltest.com/playwright-typescript-checklist/)
  — lokatory użytkownika, biznesowe oracles, fixtures, trace, dane i CI.

### Butch Mayhew / Playwright Solutions

Strona [Butch Mayhew](https://playwrightsolutions.com/author/butch/) ujawniła
paginowany katalog. Pierwsza strona zawiera między innymi:

- [Load a Custom Test Fixture or Setup Projects When Running Code Generator](https://playwrightsolutions.com/how-to-load-a-custom-test-fixture-or-setup-projects-when-running-playwright-test-code-generator/)
  — codegen ma wejść w istniejącą architekturę fixture/setup projects, a nie tworzyć finalny test.
- [How to Run a Specific Spec File Sequentially](https://playwrightsolutions.com/how-to-run-a-specific-spec-file-playwright-tests-sequentially/)
  — ograniczony wyjątek od domyślnej równoległości dla współdzielonego stanu.
- [Trace Viewer — Copy as Playwright API Request](https://playwrightsolutions.com/tip-playwright-copy-as-playwright-api-request-button/)
  — trace/report jako źródło odtwarzalnego żądania API w debugowaniu.
- [Combine Playwright HTML Reports](https://playwrightsolutions.com/how-to-combine-playwright-html-reports-after-running-multiple-playwright-commands/)
  — raportowanie dla uruchomień z różnymi workerami/profilami.
- [A Few Thoughts on Flaky Tests](https://playwrightsolutions.com/a-few-thoughts-on-flakey-tests-playwright-solutions/)
  — flakiness jako problem budowania, utrzymania i monitorowania, nie pojedynczego retry.

### Uzupełnienia na wcześniej sprawdzonych stronach

- [Currents: component testing](https://currents.dev/posts/playwright-component-testing)
  — konfiguracja CT z Vite/React/aliasami; nie mieszać jej bez potrzeby z E2E.
- [Currents: measure code coverage](https://currents.dev/posts/how-to-measure-code-coverage-in-playwright-tests)
  — Chromium coverage i konfiguracja; coverage nie zastępuje oracle.
- [Currents: Selenium → Playwright migration](https://currents.dev/posts/migrating-from-selenium-to-playwright-the-complete-guide)
  — async/await, context isolation oraz test-/worker-scoped fixtures.
- [Sajith Dilshan: logic versus configuration](https://medium.com/@sajith-dilshan/%EF%B8%8F-from-chaos-to-control-a-senior-qa-engineers-guide-to-decoupling-logic-from-configuration-in-3e5ffba7291f)
  — konfiguracja środowiska nie powinna mieszać się z logiką scenariusza.
- [TestDino: 17 best practices](https://testdino.com/blog/playwright-best-practices)
  — user-facing locators, isolation, API setup i reporting.
- [TestDino: timeout guide](https://testdino.com/blog/playwright-timeout)
  — rozróżnia test/expect/fixture timeout oraz zaleca najwęższy scope.

## Autorzy nadal niewystarczająco potwierdzeni

Firecrawl nie dostarczył autorskiego wpisu technicznego z byline i tekstem dla:
Angeli Zelayi, Viktora Konovalova, Stefana Mincheva, Vitaliya Potapova i Artema
Bondara. Dla Stefana znaleziono [wywiad ArchQA](https://idavidov.eu/playwright-test-architecture-stefan-minchev),
ale nie jest to wpis jego autorstwa. Dla Artema znaleziono kursy/wideo, a nie blog
techniczny w zadanym zakresie. Nie przypisuję im cudzych treści.

## Niekompletność pozostała jawna

- Blog Antona i ScrollTest mają więcej wpisów niż pozycje powyżej; wybrano tylko
  materiały bezpośrednio związane z Playwright/TypeScript/praktykami testów.
- Katalog Butcha jest paginowany (`page/2` i dalej); pierwsza strona została
  pobrana, więc nie jest to pełne archiwum jego publikacji.
- `currents.dev/posts/` jako indeks zwraca 404, lecz indywidualne URL-e działają.

## Wpływ na ten projekt

Nowe źródła wzmacniają istniejące konwencje `tests-pom`: fixture zamiast
globalnego hooka dla zależności testu; retry dla diagnostyki, nie jako lekarstwo;
POM dla akcji/lokatorów, zaś biznesowe oracles w specach.
