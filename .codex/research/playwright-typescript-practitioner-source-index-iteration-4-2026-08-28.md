# Playwright + TypeScript — audyt luk po trzeciej iteracji (2026-08-28)

## Wynik

W repozytorium były już trzy indeksy tego samego badania, w tym plik nazwany
„trzecia iteracja”. Ta runda **nie powiela** ich pozycji: dokumentuje materiały,
które pozostawały poza zebranym indeksem, oraz precyzuje granice kompletności.

Nie zapisuję pełnej treści cudzych publikacji. Jest to materiał chroniony
prawem autorskim. Zamiast tego poniżej są trwałe URL-e, autorstwo, zwięzłe
autorskie streszczenia i zastosowanie w projekcie.

## Co było jeszcze niepokryte

### Butch Mayhew / Playwright Solutions

Strona autorska potwierdza, że Butch publikuje dalej; odkrycie pierwszej i
drugiej strony archiwum nie było pełnym katalogiem. Te istotne wpisy nie były
wcześniej zapisane:

- [Playwright-Cli-Select: quick targeted test runs](https://playwrightsolutions.com/tool-playwright-cli-select-for-quick-targeted-test-runs-via-cli/)
  — narzędzie ma usprawniać wybór kilku testów do uruchomienia lokalnego;
  selektywny run jest wsparciem diagnozy, nie substytutem pełnego CI.
- [Login test with TOTP 2FA](https://playwrightsolutions.com/playwright-login-test-with-two-factor-authentication-2fa-enabled/)
  — testowanie drugiego czynnika wymaga kontrolowanego, testowego kanału
  kodów; nie wolno wkładać sekretu TOTP do fixture ani `storageState` w repo.
- [Diagnose a missing video in the HTML report](https://playwrightsolutions.com/playwright-solutions-challenge-debug-and-figure-out-why-the-video-recording-isnt-in-the-html-report/)
  — artefakty są dowodem awarii; konfigurację video/traces należy odczytywać
  razem z trybem retain i warunkami retry.
- [Run only changed tests in GitHub Actions](https://playwrightsolutions.com/update-v1-46-is-it-possible-to-run-only-playwright-tests-that-changed-in-github-actions-on-a-pull-request/)
  — `--only-changed=<ref>` uwzględnia specy importujące zmienione zależności.
  To optymalizacja szybkiego feedbacku; nie jest wystarczającym gate'em przed
  merge, zwłaszcza przy zmianie środowiska lub danych.
- [Test infinite scrolling](https://playwrightsolutions.com/how-do-you-scroll-to-the-bottom-of-an-infinite-scrolling-page-in-a-playwright-test/)
  — warunek końca scrollowania musi być obserwowalny (liczba rekordów,
  wyczerpanie danych albo konkretny loader), a nie oparty na stałym śnie.

### ScrollTest / Pramod Dutta

Wcześniej odnotowano serię „21 days”, ale poniższe wpisy wprost domykają
tematy zgłoszone w zadaniu:

- [Custom matchers with `expect.extend`](https://scrolltest.com/playwright-custom-matchers-expect-extend/)
  — matcher może opakować powtarzalny, domenowy oracle. Jego nazwa ma mówić o
  obserwowalnym zachowaniu; nie należy przez matcher ukrywać wieloetapowej
  nawigacji ani mutacji danych. Gdy wartość jest eventual, retry powinno
  należeć do `expect.poll`, nie do ręcznej pętli.
- [UI Mode: Time Travel Debugging](https://scrolltest.com/playwright-ui-mode-typescript-day-57/)
  — `test.step` czyni istotne etapy scenariusza czytelnymi w raporcie. Jest to
  uzasadnienie dla kroków biznesowych, nie dla dekorowania każdego wywołania
  POM.
- [Authentication storage state](https://scrolltest.com/playwright-authentication-storage-state-skip-login/)
  — setup project zapisuje stan, a zależny projekt go konsumuje. Przy wielu
  rolach stan powinien być oddzielny; pliki uwierzytelnienia nie trafiają do
  kontroli wersji.
- [Infinite scroll and lazy loading](https://scrolltest.com/playwright-infinite-scroll-lazy-loading/)
  — polluj konkretny sygnał postępu, zamiast stosować `waitForTimeout`.
- [Framework capstone](https://scrolltest.com/playwright-typescript-framework-day-21/)
  — POM-y, custom fixtures, API-driven setup i semantyczne lokatory są
  warstwami jednego testu, a nie niezależnym „frameworkiem nad Playwright”.

## Uzupełnienia znalezione przez MCP Exa

### Anton Gulin — brakujące pozycje bez dublowania wcześniejszego indeksu

- [Reuse one Page Object method for success and failure cases](https://www.anton.qa/blog/posts/reuse-page-object-method-success-and-failure)
  (29 lipca 2026) — jedno działanie POM może obsłużyć wynik pozytywny i
  negatywny przez mały, typowany obiekt opcji. To zmniejsza duplikację bez
  przenoszenia biznesowych assertions z testu do POM.
- [Playwright v1.60 turns test failures into evidence](https://www.anton.qa/blog/posts/playwright-v1-60-evidence-first-testing)
  (14 maja 2026) — trace/HAR i inne artefakty mają umożliwić rekonstrukcję
  awarii w CI. Dla tego repo najważniejsza jest zasada: artefakty zbierać przy
  failure, a nie permanentnie dla każdego udanego testu.
- [How to test passkey login in Playwright](https://www.anton.qa/blog/posts/test-passkey-login-playwright)
  (26 czerwca 2026) — Playwright 1.61 pozwala testować WebAuthn przez wirtualny
  authenticator. To jest test protokołu i UI, nie zastępstwo dla testu
  fizycznego klucza/urządzenia.

### Sajith Dilshan — dwa pominięte artykuły

- [Fixture vs lazy object creation](https://medium.com/@sajith-dilshan/fixture-vs-lazy-object-creation-in-playwright-avoiding-hidden-performance-traps-b147673ef900)
  (7 lutego 2026) — fixture jest właściwa, gdy test deklaruje zależność i jej
  lifecycle; lazy creation ma sens tylko wtedy, gdy zależność może w ogóle nie
  być potrzebna. Nie należy tworzyć kosztownych zasobów „na wszelki wypadek”.
- [Playwright annotations: a practical guide](https://medium.com/@sajith-dilshan/playwright-annotations-a-practical-guide-for-qa-engineers-f1c723fc47f7)
  (15 maja 2026) — używać `skip`/`fixme`/tagów do jawnej polityki wykonania,
  nigdy do milczącego ukrycia regresji. To uzupełnia, a nie zastępuje opisowe
  `test.step`.

### Pozostałe potwierdzone luki

- [Playwright test data management: Day 19](https://scrolltest.com/playwright-test-data-management-day-19/)
  — unikalne dane i cleanup są warunkiem bezpiecznego `fullyParallel`; fixture
  nie rozwiązuje współdzielenia danych sama z siebie.
- [Fixing Playwright tests with AI](https://testdino.com/blog/fixing-playwright-tests-with-ai)
  — automatyczna zmiana testu musi być weryfikowana przez zachowanie/oracle;
  „zielony test” po automatycznej naprawie nie jest sam w sobie dowodem.
- [Yevhen Laichenkov: `playwright-expect`](https://github.com/elaichenkov/playwright-expect)
  — istnieje zewnętrzna biblioteka assertion. Nie jest potrzebna temu projektowi:
  wbudowane `expect` Playwrighta już obsługuje wymagane web-first assertions i
  polling, a dodanie zależności nie przynosi uzasadnionej wartości.

### Korekta atrybucji

W poprzednich indeksach wpis
[`testInfo.retry`](https://playwrightsolutions.com/how-to-use-playwrights-testinforetry-to-deal-with-flakey-environments/)
został zaliczony do Butcha Mayhewa. Strona Playwright Solutions wskazuje
**Sergeia Gapanovicha** jako autora. Pozycja pozostaje wartościowym materiałem,
ale nie jest publikacją Butcha i nie powinna być liczona w jego katalogu.

## Weryfikacja nazw bez własnego bloga

Wyniki wyszukiwania nadal wskazują na publiczne posty LinkedIn dla
[Viktora Konovalova](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7489942200819015680-t7Pt)
i [Stefana Mincheva](https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7482814931646722048-rPM0),
ale nie na publiczne, samodzielne artykuły z dostępną treścią i byline. Dla
Angeli Zelayi nie znaleziono materiału autorskiego w tym zakresie. Nie należy
zatem przedstawiać wcześniejszej listy jako „wszystkich postów” tych trojga
autorów — byłoby to nieweryfikowalne.

## Rekomendowana hierarchia praktyk

Blogi są dobrym źródłem doświadczeń, ale zachowanie API musi wynikać z
oficjalnej dokumentacji Playwright:

1. [Best practices](https://playwright.dev/docs/best-practices): role/label,
   izolacja i web-first assertions.
2. [Fixtures](https://playwright.dev/docs/test-fixtures): typowane DI,
   poprawny scope i teardown po `use()`.
3. [Assertions](https://playwright.dev/docs/test-assertions): locator assertion
   dla UI; `expect.poll` wyłącznie do krótkiego, bezefektowego odczytu, który
   ma dojść do wartości; `toPass` tylko do retry całego powiązanego bloku z
   jawnym timeoutem.
4. [Configuration](https://playwright.dev/docs/test-configuration): projekty,
   zależności setupu, artefakty i różne limity dla CI/lokalnie.

## Dopasowanie do Payment Quality Engineering Lab

Lokalny `apps/frontend/playwright.pom.config.ts` już realizuje większość
zalecenia: projektowe logowanie Keycloak, `storageState`, zależności projektów,
limit workerów wynikający z czterech seeded worlds oraz artefakty tylko po
awarii. `tests-pom` ma też kontrolowane przykłady `test.step`, `expect.poll` i
`toPass`.

Nie rekomenduję zmiany kodu z samego tego badania. Ewentualne przyszłe użycie
`expect.extend` powinno przejść przez review: tylko gdy scala powtarzalny,
domenowy oracle i nie maskuje akcji, lokatorów ani asercji biznesowych.

## Metoda i ograniczenia

- Użyto MCP **Exa Search** (live crawl) do sprawdzenia pozostałych luk,
  potwierdzenia autorstwa i dat oraz dostępnego MCP Firecrawl z wcześniejszych
  iteracji. Exa nie zastępuje autorskich archiwów: przy braku dostępnej
  publicznie strony autora wynik pozostaje niepełny.
- Autor page Butcha potwierdza kolejne publikacje, lecz nie jest kompletną,
  łatwo paginowaną mapą całego historycznego archiwum; dlatego nie twierdzę,
  że lista obejmuje wszystkie jego teksty.
- Poprzednie, zweryfikowane katalogi są w
  [`iteration 1`](playwright-typescript-practitioner-source-index-2026-08-28.md),
  [`iteration 2`](playwright-typescript-practitioner-source-index-iteration-2-2026-08-28.md)
  i [`iteration 3`](playwright-typescript-practitioner-source-index-iteration-3-2026-08-28.md).
