# Playwright + TypeScript — dziesiąta iteracja rechecku źródeł (2026-08-28)

## Answer

Dziesiąty recheck wykonałem przez MCP **Exa Search** i **Exa Fetch**. Dodatkowo
użyłem mapowania sitemap Firecrawl wyłącznie do znalezienia canonical URL-i;
treść merytoryczna została sprawdzona przez Exa Fetch albo oznaczona jako
`metadata-only`. Nową deltę porównałem z indeksami iteracji 1–9 oraz
katalogiem wpisów LinkedIn Michala Drajny.

Największe luki uzupełnione w tej iteracji:

- kolejne dni serii ScrollTest (setup, interakcje, API, auth, iframe/popup,
  visual, dane, reporter, CI, mobile, sharding i architektura),
- nowe artykuły Anton Gulin o eval-driven development, niezależnym oracle i
  offline scoringu agentów,
- nowe wpisy Vitaliya Potapova o scope `beforeAll`, fixture naprawiającym test,
  `test.step`, cache sieciowym, metadanych raportu i mockowaniu requestów
  serwerowych,
- canonical release note Currents dla Playwright 1.60,
- reprezentatywne strony TestDino o CI, parallelism, raportach, tagowaniu,
  adnotacjach i sharding.

Nie kopiuję pełnych chronionych publikacji. Poniżej zapisuję canonical URL,
autora/datę, własne streszczenie i zastosowanie do Playwright/TypeScript.

## Nowa lub uzupełniona delta

### Anton Gulin

- [Eval-Driven Development for AI Agent Skills](https://www.anton.qa/blog/posts/eval-driven-development-for-ai-agent-skills) (19 kwietnia 2026) — opisuje umiejętność agenta jak oprogramowanie, które potrzebuje testów: przypadki `should-trigger` i `should-not-trigger`, porównanie uruchomienia z umiejętnością i bez niej oraz iterację `Create → Evaluate → Optimize → Benchmark → Install`. Dla testów Playwright jest to wzorzec testowania samego harnessu: nie wystarczy, że agent wygeneruje plik, trzeba zmierzyć, czy dobrał właściwy fixture, locator i oracle.
- [How to Create Custom OpenCode Skills](https://www.anton.qa/blog/posts/how-to-create-custom-opencode-skills-step-by-step-guide) (12 kwietnia 2026) — intake interview zbiera trigger, jakość i niezmienne reguły; później workflow ma baseline, ewaluację, optymalizację i benchmark. Przeniesienie na TS: trzymaj konwencje w typowanym module/fixture, a opis workflow oddziel od kodu wykonawczego.
- [How to Implement AI in QA](https://www.anton.qa/blog/posts/how-to-implement-ai-in-qa) — zasada „AI wykonuje pracę, fixed check/oracle decyduje o wyniku”. Agent może eksplorować, naprawiać locator albo analizować trace, ale nie może sam wystawić sobie `pass`. W specu pozostaw deterministyczne `expect` i niezależny expected result.
- [I Ate My Own Dog Food: How I Benchmarked AI Skills](https://www.anton.qa/blog/posts/i-ate-my-own-dog-food-how-i-benchmarked-ai-skills-and-proved-eval-driven-development-works) (15 kwietnia 2026) — autor uruchamia evals na własnym projekcie zamiast walidacji wyłącznie ręcznej. Wniosek dla repo: zmiany w opisie skill/fixture powinny mieć mały regresyjny zestaw promptów i znany baseline.
- [How to Score Your AI Test Agents: Offline Evaluation with Trajectories](https://www.anton.qa/blog/posts/score-ai-test-agents-offline-evaluation) (10 czerwca 2026) — zapis trajektorii (obserwacja, decyzja, akcja, kod) można odtworzyć offline i ocenić poprawność, relewancję i edge-case coverage bez żywego API. To dobry wzorzec dla review testów generowanych przez AI; zielony run nie jest jeszcze oceną jakości testu.
- [You Were Handed Testing and No One Trained You](https://www.anton.qa/blog/posts/handed-testing-field-guide) — praktyczny field guide: nie śpij `waitForTimeout`, czekaj na sygnał; web-first assertion retryuje, aż pojawi się wiersz/spinner/stan. Przykład preferuje `getByRole` oraz `await expect(...)`, bo race condition naprawia się warunkiem, nie dłuższą pauzą.

### ScrollTest / Promode i Pramod Dutta

#### Dodatkowe dni serii 21-Day Playwright

- [Day 1 — Installation and First Test](https://scrolltest.com/21-day-playwright-day-1-installation-first-test/) (9 sierpnia 2026, Pramod Dutta) — `npm init playwright@latest`, pierwszy test z `getByRole` i `expect`, tryby headless/headed/UI/debug. Punkt wyjścia powinien pozostać prosty; konfigurację rozszerzaj dopiero, gdy pojawi się realna potrzeba.
- [Day 4 — Page Interactions](https://scrolltest.com/21-day-playwright-day-4-page-interactions-click-fill/) (12 sierpnia 2026) — `click`, `fill`, `selectOption`, upload, keyboard i mouse korzystają z actionability auto-wait. `force` omija te zabezpieczenia, więc powinien być wyjątkiem z udokumentowanym powodem.
- [Day 7 — API Testing with Request Context](https://scrolltest.com/21-day-playwright-day-7-api-testing-request-context/) (15 sierpnia 2026) — wbudowany fixture `request` umożliwia CRUD i hybrydę API+UI. Seed przez API skraca setup UI, a UI pozostaje sprawdzeniem user-visible rezultatu.
- [Day 9 — Authentication and Storage State](https://scrolltest.com/21-day-playwright-day-9-authentication-storage-state/) (17 sierpnia 2026) — osobny setup project loguje użytkownika raz i zapisuje `storageState`; zależności projektów zapewniają kolejność. Dla wielu ról używaj osobnych plików stanu, a `playwright/.auth` ignoruj w VCS.
- [Day 10 — Iframes, Popups, Dialogs and Tabs](https://scrolltest.com/21-day-playwright-day-10-iframes-popups-dialogs-tabs/) (18 sierpnia 2026) — `frameLocator` dla iframe, `page.on('dialog')` przed akcją wywołującą dialog i `page.waitForEvent('popup')` przed kliknięciem. To wzorzec „zarejestruj listener przed triggerem”, a nie ślepe oczekiwanie po fakcie.
- [Day 11 — Visual Regression](https://scrolltest.com/21-day-playwright-day-11-visual-regression-testing/) (19 sierpnia 2026) — `toHaveScreenshot`, screenshot elementu, maskowanie dynamicznych obszarów i kontrola `maxDiffPixelRatio`. Snapshot powinien być deterministyczny; aktualizuj go świadomie po review, nie przez automatyczne akceptowanie każdej różnicy.
- [Day 12 — Parallel Execution and Sharding](https://scrolltest.com/21-day-playwright-day-12-parallel-execution-sharding) (20 sierpnia 2026) — `fullyParallel`, ograniczenie `workers` w CI i matrix `--shard=...`; każdy test powinien tworzyć własne dane i nie zależeć od kolejności. Podane czasy 100 testów są przykładem autora, nie obietnicą dla każdej suite.
- [Day 14 — Test Data Factories and Faker](https://scrolltest.com/21-day-playwright-day-14-test-data-factories-faker/) (22 sierpnia 2026) — `@faker-js/faker`, `TestDataFactory` oparty o `APIRequestContext` i fixture z cleanupem po `use`. Praktyka jest trafna, ale przykładowe `any` należy zastąpić modelem domenowym i `satisfies`/jawnym typem.
- [Day 15 — Custom Reporters, Allure and Slack](https://scrolltest.com/21-day-playwright-day-15-custom-reporters-allure-slack) (23 sierpnia 2026) — built-in list/dot/json/junit/html oraz `onTestEnd`/`onEnd` dla własnego reportera. W shardach trzeba agregować wynik, czekać na asynchroniczny flush i redagować sekrety; status `flaky` nie powinien zniknąć pod samym `passed`.
- [Day 16 — CI/CD GitHub Actions](https://scrolltest.com/21-day-playwright-day-16-ci-cd-github-actions-pipeline) (24 sierpnia 2026) — matrix shardów, `npm ci`, instalacja browserów, artefakty `if: always()` i merge reportów. Liczbę workerów/shardów dobieraj pomiarem CPU, czasu i kosztu, nie zasadą „zawsze cztery”.
- [Day 17 — Mobile Emulation and Viewport](https://scrolltest.com/21-day-playwright-day-17-mobile-testing-emulation-viewport) (25 sierpnia 2026) — projekty Pixel/iPhone/iPad, viewport, geolokalizacja/permissions, dark mode i screenshot. Emulacja jest osobnym projektem konfiguracji; nie mieszaj jej z domyślnym desktop smoke bez czytelnego tagu.

#### Uzupełniające wpisy CI, POM i architektury

- [Playwright CI GitHub Actions — Day 12](https://scrolltest.com/playwright-ci-github-actions-day-12/) (20 czerwca 2026, Promode) — clean machine jest prawdziwym testem setupu: `npm ci`, cache browsera, traces/reports/artifacts, sekrety z CI i jawna polityka retry. Lokalny profil/cookies nie mogą być ukrytym warunkiem sukcesu.
- [Playwright CI Sharding with TypeScript — Day 18](https://scrolltest.com/playwright-ci-sharding-typescript-day-18) (26 czerwca 2026, Promode) — rozróżnia worker, project i shard; suite dzielona na niezależne joby wymaga merge reportów oraz zachowania trace/screenshot dla konkretnego shardu. Najpierw zmierz nierównowagę shardów i izolację danych.
- [Playwright Framework Architecture — Day 20](https://scrolltest.com/playwright-framework-architecture-day-20) (28 czerwca 2026, Promode) — runner options są na poziomie config, a test options w `use`; typed fixtures, proste POM-y, helpery danych/API i jawne artefakty odpowiadają na pytania „gdzie trafia nowy test?” i „jak go uruchomić w CI?”. Framework ma czynić domyślną ścieżkę poprawną, nie budować drugiego frameworka nad Playwright.
- [Playwright Page Object Model — Day 14](https://scrolltest.com/playwright-page-object-model-day-14/) (22 czerwca 2026, Promode) — POM ukrywa locatory, małe akcje i znaczące page waits, ale nie ukrywa celu testu, business assertions ani danych. Component objects i fixtures są lepsze niż monolityczna klasa „aplikacja”.
- [Playwright Docker GitHub Actions CI/CD Pipeline](https://scrolltest.com/playwright-docker-github-actions-ci-cd-pipeline/) (5 maja 2026, aktualizacja 29 czerwca 2026, Promode) — pinuj obraz/Docker i wersję browsera, używaj `npm ci`, artefaktów i trace; drift Ubuntu/browsera jest częstszą przyczyną różnic niż sam locator. Cache powinien być mierzalny i unieważniany przy zmianie lockfile.

### Currents.dev

- [Playwright 1.60.0 Release Updates](https://currents.dev/posts/pw-1.60.0) (11 maja 2026, aktualizacje do 19 maja) — upgrade runnera 1.60 wymagał `@currents/playwright` 2.0.0; użytkownicy orchestration musieli zmienić komendę CI, a pozostali przede wszystkim reporter. Upgrade testuj jako cały toolchain (runner, reporter, orchestrator, config), zaczynając od małego smoke gate.

### TestDino

Poniższe strony zostały odnalezione w sitemapie i zweryfikowane przez Exa. Są
to własne streszczenia praktyk, nie rekomendacje wersji bez sprawdzenia
oficjalnego changelogu.

- [Playwright in GitLab CI](https://testdino.com/blog/playwright-in-gitlab-ci) (9 maja 2026, Savan Vaghani) — kontener, powtarzalna instalacja, artefakty i joby równoległe. Obraz powinien przypinać kompatybilny browser, a raport/trace publikować także po failure.
- [Playwright Parallel Execution](https://testdino.com/blog/playwright-parallel-execution) (14 marca 2026, aktualizacja 16 marca, Jashn Jain) — workers są procesami OS; pliki mogą biec równolegle, lecz testy w pliku domyślnie sekwencyjnie. `fullyParallel` i rozmiar CI dobieraj po izolacji danych oraz saturacji CPU.
- [How to Make Playwright Tests Faster](https://testdino.com/blog/playwright-slow-tests) (17 grudnia 2025, aktualizacja 26 marca 2026, Pratik Patel) — reuse sesji, sensowne parallelism i usunięcie zbędnych waits/selectorów. Marketingowe procenty czasu traktuj jako hipotezę do pomiaru na własnym pipeline.
- [Playwright Reporting Metrics](https://testdino.com/blog/playwright-reporting-metrics) (28 października 2025, aktualizacja 11 kwietnia 2026, Vishwas Tiwari) — mierz pass/fail, flakiness, duration, retries, środowisko i PR. Sam HTML report nie zastępuje trendu z wielu runów.
- [Grouping Playwright Tests](https://testdino.com/blog/grouping-playwright-tests) (2 marca 2026, aktualizacja 6 marca, Dhruv Rai) — `test.describe`, tagi i `--grep` pomagają zbudować smoke/regression slices i czytelny raport. Tagi powinny opisywać misję/ryzyko, nie służyć do ukrywania czerwonych testów.
- [Playwright HTML Reporter](https://testdino.com/blog/playwright-html-reporter) (6 stycznia 2026, aktualizacja 12 lutego, Savan) — built-in list/dot/json/html pokazują duration, retries i steps; przy dużej skali potrzebna jest agregacja oraz retencja artefaktów.
- [Playwright Annotations](https://testdino.com/blog/playwright-annotations) (28 lutego 2026, aktualizacja 2 marca, Dhruv Rai) — `skip`, expected failure i metadata sterują zachowaniem runnera. Używaj semantycznych adnotacji zamiast warunkowych hacków i zawsze dokumentuj właściciela/warunek znanego failure.
- [Playwright Sharding](https://testdino.com/blog/playwright-sharding) (26 stycznia 2026, aktualizacja 30 stycznia, Pratik) — worker przyspiesza jeden job, shard rozkłada suite na wiele jobów; matrix CI i merge reportów muszą zachować identyfikator shardu oraz artefakty.
- [Playwright Framework Setup](https://testdino.com/blog/playwright-framework-setup) (31 października 2025, aktualizacja 2 czerwca 2026, Pratik) — struktura frameworka powinna obejmować config, fixtures, POM, dane i raportowanie, ale nie ukrywać intencji testu za nadmiarowymi wrapperami.

#### Dodatkowa inwentaryzacja TestDino (metadata-only lub strony dynamiczne)

Mapa `https://testdino.com` zwróciła 121 URL-i. Poniższe canonicale są
nową/uzupełniającą listą do dalszego czytania; Exa zwrócił dla części jedynie
shell/snippet, więc nie dopisuję szczegółów, których nie da się zweryfikować:

- [Playwright Visual Testing](https://testdino.com/blog/playwright-visual-testing)
- [Playwright MCP Visual Testing](https://testdino.com/blog/playwright-mcp-visual-testing)
- [Playwright Authentication](https://testdino.com/blog/playwright-authentication)
- [Playwright Custom Reporter](https://testdino.com/blog/playwright-custom-reporter)
- [AI Write Playwright Tests](https://testdino.com/blog/ai-write-playwright-tests)
- [Playwright Tests with Antigravity](https://testdino.com/blog/playwright-tests-with-antigravity)
- [Playwright Tests with Cline](https://testdino.com/blog/playwright-tests-with-cline)
- [Playwright MCP Cursor](https://testdino.com/blog/playwright-mcp-cursor)
- [Playwright Tests in Azure](https://testdino.com/blog/playwright-tests-in-azure)
- [Playwright Test Management](https://testdino.com/blog/playwright-test-management)
- [Playwright Debugging Guide](https://testdino.com/blog/playwright-debugging-guide)
- [Playwright Automation](https://testdino.com/blog/playwright-automation)
- [Claude Code with Playwright](https://testdino.com/blog/claude-code-with-playwright)
- [Accessibility Tree](https://testdino.com/blog/accessibility-tree)
- [Playwright Types of Software Testing](https://testdino.com/blog/types-of-software-testing)

### Vitaliy Potapov (Vitali(y)ets)

- [“Fix with AI” Button in Playwright HTML Report](https://vitalets.github.io/posts/playwright/fix-with-ai-html-report) (13 stycznia 2025) — test-scope, auto fixture po `use()` sprawdza `testInfo.error`; prompt tworzy tylko wtedy, gdy próby się skończyły, i dołącza error, snippet testu oraz ARIA snapshot. Automatyzacja pomaga w triage, ale nie powinna samodzielnie zmieniać kodu ani oznaczać testu jako poprawionego.
- [Global Cache: Make Playwright BeforeAll Run Once for All Workers](https://vitalets.github.io/posts/playwright/global-cache-beforeall) (12 sierpnia 2025) — `beforeAll` wykonuje się raz na worker i może uruchomić się ponownie po restarcie workera; nie jest globalnym singletonem runu. Global setup pasuje do wspólnego loginu/schema seed, a fixture do izolowanych danych per test.
- [Introducing Playwright-magic-steps](https://vitalets.github.io/posts/playwright/playwright-magic-steps) (18 lipca 2024) — komentarze `// step: ...` są transformowane do `test.step()`, co poprawia raport bez ręcznego zagnieżdżania. To wygoda oparta o transformację/loader; przed użyciem sprawdź ESM/CommonJS, debugowanie i koszt dodatkowej zależności.
- [Supercharge Your E2E Tests with Playwright-Network-Cache](https://vitalets.github.io/posts/playwright/playwright-network-cache) (8 października 2024) — cache odpowiedzi na filesystemie może przyspieszyć i ustabilizować testy z zewnętrznym API, jako alternatywa dla ręcznych mocków/HAR. Ustal TTL, maskowanie sekretów i osobny test live-contract, by cache nie ukrywał driftu.
- [Show Metadata in Playwright HTML Report](https://vitalets.github.io/posts/playwright/show-metadata-html-report) (19 grudnia 2024) — autor wykorzystuje `metadata` z revision/CI linkiem do raportu. Wpis wskazuje rozbieżność dokumentacji i typów w v1.49; typy aktualnie przypiętej wersji są ważniejsze niż przykład z bloga.
- [Request-Mocking-Protocol](https://vitalets.github.io/posts/testing/request-mocking-protocol) (13 marca 2025) — `page.route` nie przechwytuje fetch wykonywanego po stronie serwera (np. RSC/Next.js); autor proponuje osobny proxy/protocol dla deterministycznego server-side mock. Kontrakt danych powinien pozostać jawny, a browser test nie powinien udawać, że zweryfikował prawdziwy upstream.

### Artem Bondar / Bondar Academy

Recheck nie znalazł nowej strony poza opisanym w iteracji 9 wpisem [Is
Playwright MCP Worth It for Test Automation?](https://bondaracademy.com/blog/is-playwright-mcp-worth-it).
Pozostałe canonicale o fixtures, locators, `expect`, storage state, API/POM,
CI i JSON Schema są już w poprzednich indeksach; sitemap nie odsłoniła nowej
wersji tych materiałów.

## Recheck wszystkich wskazanych autorów i stron

| Źródło | Wynik dziesiątej iteracji | Status |
|---|---|---|
| Anton Gulin | sześć nowych wpisów o evalach, oracle, offline trajectories i web-first waits | dodane powyżej; wpisy z iteracji 9 pozostają w korpusie |
| Michal Drajna | brak nowego publicznie indeksowanego wpisu po katalogu LinkedIn z 28 sierpnia | katalog custom fixtures, flake, 10k tests, Lighthouse, Quest i governed model pozostaje aktualny |
| Angela Zelaya | brak nowego autorskiego artykułu Playwright/TS | znalezione reposty/komentarze nie dodają canonicalnej treści |
| Viktor Konovalov | brak nowej delty | snapshot API, `evaluateAll`, CDPSession, accessibility tree, init script i reporting są w iteracji 7 |
| Stefan Minchev | brak nowej delty | storageState, TOTP, API seed, `expect` vs `isVisible`, env switching i architecture są w iteracji 7 |
| ScrollTest / Pramod Dutta / Promode | 15 nowych lub wcześniej nieopisanych stron | seria Day 1/4/7/9/10/11/12/14/15/16/17 oraz CI/POM/Docker/architecture |
| [Joseph Ward](https://josephward.tech) | brak nowego Playwright/TS wpisu | mechanika Playwright, wolne UI tests i testowanie danych migracji są już opisane |
| Vitaliy Potapov | sześć nowych stron z mapy bloga | fixture triage, worker-scope, magic steps, cache, metadata i server mock |
| [Vitaliy Haradkou (Vercel)](https://blog-vitaliharadkous-projects.vercel.app) | brak jednoznacznie nowego wpisu Playwright/TS | wcześniejsze Vercel/Hashnode/DEV wpisy pozostają jedynymi potwierdzonymi |
| Artem Bondar / Bondar Academy | brak nowej delty po MCP wpisie z iteracji 9 | projects, fixtures, API/POM, locators, `expect`, data-driven, storage i CI są pokryte |
| [Sajith Dilshan](https://medium.com/@sajith-dilshan) | brak nowego canonical Medium; wyniki LinkedIn to promocje | auto-waiting, auth, tsconfig/ESM, hooks/errors, mocking, MFA i viewport są w poprzednich indeksach |
| [Yevhen Laichenkov](https://elaichenkov.github.io/) | brak nowego wpisu | `17 mistakes` i `test.step` decorator są w korpusie |
| Butch Mayhew | brak nowego materiału technicznego | review kodu AI, agentic QA, manual testing i newsletter są oznaczone jako kontekst |
| Currents.dev | canonical release note 1.60.0 | dodane powyżej; reporter/CI/API/HTML/blob/mocking były wcześniej |
| TestDino | 9 stron merytorycznie streszczonych + 15 metadata-only | sitemap 121 URL-i; wcześniejsze best-practices, releases, UI Mode, trace i AI wpisy pozostają w iteracjach 1–9 |
| [Level Up Coding](https://levelup.gitconnected.com/) | brak nowego niezdublowanego artykułu | custom fixtures, Browser/Context/Page, API/data-driven, global setup i response assertions są pokryte |

„Brak nowej delty” oznacza brak nowego wyniku w publicznie indeksowanych
zapytaniach Exa i sitemapach w dniu 2026-08-28, a nie dowód kompletności
całego Internetu. LinkedIn, Medium, Vercel i strony dynamiczne mogą ukrywać
starsze albo niezaindeksowane posty.

## Why it matters here

Wspólny wzorzec z nowych źródeł jest spójny z lokalną suite: izoluj dane,
czekaj na sygnał domenowy, trzymaj oracle poza agentem i traktuj config,
fixture, reporter oraz artefakty jako jeden kontrakt wykonania. To ogranicza
flakiness bez ukrywania realnych błędów produktu.

### Fixtures i setup

- `test.extend` jest lazy; fixture wykonuje się dopiero, gdy jest użyte. Jawnie
  oznaczaj `scope: 'test'`/`'worker'`, `auto`, dependencies i teardown po `use()`.
- `beforeAll` nie oznacza „raz na cały run”. Po restarcie workera setup może się
  powtórzyć; dane per-test powinny być izolowane, a globalny seed/auth powinien
  mieć osobny kontrakt i idempotencję.
- API `request`/factory jest właściwym miejscem na seed merchant/payment-order;
  browser ma sprawdzać user-visible rezultat, nie budować całej bazy kliknięciami.
- Auto fixture do AI triage może dołączyć kontekst failure, lecz nie może
  zmieniać oracle ani automatycznie commitować „naprawy”.

### `playwright.config.ts` i CI

- Rozdziel runner options (`projects`, `workers`, `fullyParallel`, `reporter`,
  `webServer`) od test options w `use` (`baseURL`, `storageState`, trace,
  screenshot, video, locale/timezone). Typy configu są źródłem prawdy dla pina.
- `storageState` trzymaj poza VCS; osobne role dostają osobne state files.
  CI uruchamiaj na clean machine z `npm ci`/lockfile i kompatybilnym browserem.
- Sharduj dopiero po sprawdzeniu izolacji i rozkładu czasu; merge blob/HTML
  reportów, trace i artefakty zachowuj z identyfikatorem joba/shardu.
- Po aktualizacji Playwright sprawdź reporter/orchestrator (przypadek Currents
  1.60) oraz uruchom mały smoke gate przed pełną suite.

### `expect`, polling i `test.step`

- Locator assertion (`await expect(locator).toBeVisible()`, `toHaveText`,
  `toHaveCount`) jest web-first i retryuje do timeoutu. Nie pobieraj raz
  `textContent()` i nie rób z niego ręcznej asercji, jeśli UI jest eventual.
- `expect.poll` pasuje do funkcji zwracającej status/API value; `expect.toPass`
  do krótkiego trigger→reload→verify z kontrolowanymi `intervals`. Wewnętrzny
  timeout asercji powinien być krótszy niż zewnętrzny polling timeout.
- `waitForTimeout` jest diagnostyką, nie synchronizacją. Czekaj na sygnał:
  locator state, response, URL, zniknięcie spinnera albo domenowy status.
- `test.step` opisuje intencję użytkownika i ułatwia trace/report. Magic-step
  transform może skrócić kod, ale dodatkowy loader należy przypiąć i testować;
  nie ukrywaj w kroku setek linii ani business oracle.

## Test impact (Playwright / TypeScript)

### Idiomatyczny TypeScript i konstrukcja testu

- Modele danych trzymaj jawnie (`type`/`interface`, `as const`, `satisfies`),
  włącz `strict` i zastępuj przykładowe `any` typem domenowym. Config, fixture,
  factory i POM powinny mieć małe, jednoznaczne interfejsy.
- Preferuj `getByRole`, `getByLabel`, `getByText` i świadomie zdefiniowany
  `getByTestId`; CSS/XPath jest fallbackiem związanym z implementacją.
- POM ukrywa locatory/akcje, ale spec zachowuje scenariusz, dane i assertions.
  Każdy test ma własne dane, niezależny oracle i może działać w dowolnej kolejności.
- AI/MCP/CLI stosuj do eksploracji, szkicu i triage. Do gate CI przenieś wynik
  do zwykłego, reviewowanego `.spec.ts` z deterministycznym `expect`.

## Project impact

- W `apps/frontend/tests-pom` przygotowuj merchant/payment-order przez BFF/API
  fixture lub factory; nie duplikuj setupu przez UI.
- W `playwright.pom.config.ts` utrzymuj jeden jawny kontrakt dla projects,
  dependencies, auth state, trace/screenshot/video, retries/workers/reportera
  oraz `BASE_URL`/`TEST_ENV`. Repo przypina Playwright 1.61.0 — porady dla
  1.60/1.62 traktuj jako wersjozależne.
- ETag/If-Match, idempotency, status history i schema sprawdzaj w API/contract
  assertions; UI potwierdza tylko wynik widoczny dla użytkownika.
- Trace, video, HAR i HTML mogą zawierać cookies, tokeny, OTP oraz PII; używaj
  kont testowych, maskowania, retencji i ograniczonego dostępu.

## Sources

Pełny wcześniejszy korpus znajduje się w:

- [iteracja 1](playwright-typescript-practitioner-source-index-2026-08-28.md)
- [iteracja 2](playwright-typescript-practitioner-source-index-iteration-2-2026-08-28.md)
- [iteracja 3](playwright-typescript-practitioner-source-index-iteration-3-2026-08-28.md)
- [iteracja 4](playwright-typescript-practitioner-source-index-iteration-4-2026-08-28.md)
- [iteracja 5](playwright-typescript-practitioner-source-index-iteration-5-2026-08-28.md)
- [iteracja 6](playwright-typescript-practitioner-source-index-iteration-6-2026-08-28.md)
- [iteracja 7](playwright-typescript-practitioner-source-index-iteration-7-2026-08-28.md)
- [iteracja 8](playwright-typescript-practitioner-source-index-iteration-8-2026-08-28.md)
- [iteracja 9](playwright-typescript-practitioner-source-index-iteration-9-2026-08-28.md)
- [katalog Michala Drajny](michal-drajna-linkedin-playwright-posts.md)

Źródła normatywne, które rozstrzygają konflikty z blogami:

- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Fixtures](https://playwright.dev/docs/test-fixtures)
- [Assertions](https://playwright.dev/docs/test-assertions)
- [Configuration](https://playwright.dev/docs/test-configuration)
- [`test.step`](https://playwright.dev/docs/api/class-test#test-step)
- [Authentication](https://playwright.dev/docs/auth)
- [Test retries and trace](https://playwright.dev/docs/test-retries)
- [Parallelism and sharding](https://playwright.dev/docs/test-parallel)

## Uncertainty / follow-up

- Część TestDino (visual/MCP/auth/accessibility) renderuje dynamiczny shell;
  zachowuję tylko URL i temat, bez niezweryfikowanych szczegółów.
- Wpisy o MCP, agentach, screencast i wersjach runnera są datowane i szybko
  się starzeją. Przed wdrożeniem sprawdź release notes przypiętej wersji.
- `metadata` w HTML report, magic steps, network cache i request-mocking
  protocol są rozszerzeniami/eksperymentami; wymagają spike'a, security review
  i testu degradacji, zanim trafią do głównej suite.
- Firecrawl sitemap służył do inwentaryzacji, nie do kopiowania tekstu. Exa
  Search/Fetch nie gwarantuje pełnego archiwum LinkedIn, Medium, Vercel ani
  stron z paywallem; „wszystkie” oznacza wszystkie publicznie znalezione i
  niezdublowane w tym rechecku.
- Ta iteracja zmienia wyłącznie tę notatkę Markdown; nie zmieniono kodu
  aplikacji ani istniejących testów.
