# Playwright + TypeScript — szósta iteracja: recheck pozostałych źródeł (2026-08-28)

## Zakres i zasady

Ta runda użyła MCP **Exa Search** oraz **Exa Fetch** i porównała wyniki z
indeksami iteracji 1–5. Poniżej są nowe albo wcześniej znalezione, lecz dotąd
nieopisane pozycje dotyczące Playwright, TypeScript, fixture, konfiguracji,
asercji, polling, `test.step`, danych testowych i konstrukcji testów.

Nie kopiuję pełnych treści artykułów. Zapisuję canonical URL, autorstwo, datę
(gdy źródło ją podało) i własne streszczenie. LinkedIn reposty, mirror pages i
materiały innych autorów są jawnie oznaczone.

## Nowe i uzupełnione materiały

### Anton Gulin

- [Playwright Just Shipped the Fix For Flaky Tests I Built 3 Years Ago](https://www.anton.qa/blog/posts/playwright-just-shipped-the-fix-for-flaky-tests-i-built-3-years-ago)
  (23 kwietnia 2026) — porównuje własny podział Planner/Generator/Healer z
  Playwright Test Agents. Wniosek praktyczny: generator powinien działać na
  pisemnej specyfikacji, typowanych fixture i deterministycznych locatorach,
  a trace/video/screencast dostarczać dowodu do ludzkiego review.
- [Playwright Codegen: The Complete Guide](https://www.anton.qa/blog/posts/playwright-codegen-complete-guide)
  (3 czerwca 2026) — Codegen jest szkicem, nie gotowym testem. Nagrywaj też
  asercje, potem przeglądaj locatory, usuń zbędne akcje i przenieś powtarzalne
  zachowanie do małych POM/fixture.
- [Playwright vs Cypress vs Selenium in 2026](https://www.anton.qa/blog/posts/playwright-vs-cypress-vs-selenium-in-2026)
  (21 maja 2026) — wybór narzędzia powinien uwzględniać browser matrix,
  równoległość, jakość artefaktów i ryzyko produktu, nie tylko składnię.
  Agent może pomagać, ale człowiek nadal ocenia dowód i ryzyko.
- [Create Video Receipts for AI Agents with Playwright Screencast API](https://www.anton.qa/blog/posts/create-video-receipts-for-ai-agents-with-playwright-screencast-api)
  (17 kwietnia 2026) — `page.screencast.start/stop` i adnotacje tworzą
  odtwarzalny dowód sesji agenta. Dla lokalnego repo to obserwability pattern,
  nie powód do włączania ciężkiego nagrywania w każdym udanym teście.
- [Playwright CLI v0.1.10 Brings Spec-Driven Testing Skills](https://www.anton.qa/blog/posts/playwright-cli-v0-1-10-brings)
  (30 kwietnia 2026) — opisuje przepływ plan → generate → heal oraz indeksowane
  żądania CLI. Skill jest read-only i wymaga własnego mechanizmu uruchamiania
  po zmianie specyfikacji; wpis nie zastępuje dokumentacji CLI.
- [Playwright MCP v0.0.73: Browser Paths via Environment Variables](https://www.anton.qa/blog/posts/playwright-mcp-v0-0-73)
  (2 maja 2026) — ścieżki browsera i kanały powinny być parametryzowane przez
  środowisko CI, a nie hard-code. Dotyczy MCP/containerów, nie zmienia
  wersji `@playwright/test` w tym repo.
- [How to Test MCP Servers Before They Break Your CI](https://www.anton.qa/blog/posts/mcp-server-testing-production-checklist)
  (6 maja 2026) — proponuje trzy warstwy: discovery narzędzi, test zachowania
  i audyt uprawnień/danych. To osobna kontrola integracji MCP, nie asercja UI.

Pozostałe odnalezione strony Antona, które nie dodają nowej techniki do
wcześniejszego indeksu: [POM Playwright 2026](https://www.anton.qa/blog/posts/page-object-model-in-playwright-with-typescript-complete-guide),
[tutorial dla początkujących](https://www.anton.qa/blog/posts/playwright-tutorial-for-beginners-your-first-test-in-10-minutes),
[migracja z Selenium](https://www.anton.qa/blog/posts/how-to-migrate-from-selenium-to-playwright-in-2026-complete-guide),
[10 strategii naprawy flaków](https://www.anton.qa/blog/posts/how-to-fix-flaky-tests-in-playwright-10-battle-tested-strategies),
[drag-and-drop w MCP](https://www.anton.qa/blog/posts/drag-and-drop-automation),
[native drag-and-drop w MCP](https://www.anton.qa/blog/posts/native-drag-and-drop-automation-arrives-in-playwright-mcp-what-v0-0-71-changes),
[browser_run_code_unsafe](https://www.anton.qa/blog/posts/browser_run_code_unsafe),
[MCP ecosystem](https://www.anton.qa/blog/posts/the-mcp-ecosystem-just-collapsed-into-playwright) i
[testowanie MCP w produkcji](https://www.anton.qa/blog/posts/mcp-server-testing-production-checklist).
Pierwsze dwa opisują natywne drag-and-drop, `browser_run_code_unsafe` podkreśla
ryzyko wykonywania dowolnego kodu, a wpis o MCP porządkuje role CLI/MCP/agentów.
Są zindeksowane, lecz ich główne zalecenia pokrywają materiały z iteracji 2–5.

### Angela Zelaya — znalezione reposty, bez przypisywania cudzej treści

Exa znalazł publiczne wpisy Angeli, ale każdy z poniższych jest oznaczony jako
reshare. Nie traktuję ich jako jej autorskiego artykułu:

- [Creating a Playwright framework with AI](https://www.linkedin.com/posts/angela-zelaya-b185b218b_creating-a-playwright-framework-with-ai-activity-7468275913273614336-YtfR)
  (4 czerwca 2026) — repost wpisu Michala Drajny o szybkim szkielecie frameworka.
- [API Automation CRUD with Playwright MCP + Claude](https://www.linkedin.com/posts/angela-zelaya-b185b218b_api-testing-with-llmclaude-and-playwright-activity-7381674496044314624-YVjh)
  (8 października 2025) — repost Kailasha Pathaka; wskazuje API-first i MCP,
  ale nie zawiera niezależnego materiału Angeli.
- [How to test POST API requests with Playwright](https://www.linkedin.com/posts/angela-zelaya-b185b218b_how-to-test-post-api-requests-with-playwright-activity-7442587994257018880-gS_b)
  (25 marca 2026) — repost materiału Michala/Sajitha o przenoszeniu walidacji
  na warstwę API.
- [Playwright TypeScript Part 12: CI/CD with GitHub Actions](https://www.linkedin.com/posts/angela-zelaya-b185b218b_playwright-typescript-part-12-playwright-activity-7355607798014980097-Ho5w)
  (28 lipca 2025) — repost wideo Alexa Khvastovicha.
- [Can agentic AI really be tested?](https://www.linkedin.com/posts/angela-zelaya-b185b218b_can-agentic-ai-really-be-tested-my-unpopular-activity-7467552016983306240-1Ome)
  (2 czerwca 2026) — repost rozważań Randy’ego Rice’a o granicy między
  testowaniem a monitoringiem agentów.

### Viktor Konovalov — nowe autorskie tipy LinkedIn

- [Use `test.step()` for readable reports](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7464933432901627904-MnqV)
  (26 maja 2026) — kroki powinny reprezentować działania biznesowe. Nie
  zwiększają stabilności same w sobie, ale skracają analizę faila w CI.
- [Why a successful click does nothing](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-qa-testautomation-activity-7444805097760514048-JvNc)
  (31 marca 2026) — widoczność i clickability nie dowodzą gotowości systemu;
  po akcji czekaj na sygnał sieciowy, zmianę stanu lub potwierdzenie backendu.
- [Why Playwright suites slowly become fragile](https://www.linkedin.com/posts/viktorkonovalovqa_why-playwright-test-suites-slowly-become-activity-7433964541861945344-xiRW)
  (1 marca 2026) — typowe przyczyny to UI użyte do kontroli API, ciężkie
  `beforeEach`, layoutowe locatory i mieszanie smoke z regression.
- [Visible does not always mean ready](https://www.linkedin.com/posts/viktorkonovalovqa_qa-testautomation-playwright-activity-7441888340368596992-69XA)
  (23 marca 2026) — gotowość produktu może wymagać zniknięcia loadera,
  odpowiedzi sieci lub innego obserwowalnego warunku.
- [Stop asserting implementation details](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-tip-stop-asserting-implementation-activity-7452056973652635648-iV9_)
  (20 kwietnia 2026) — E2E sprawdza doświadczenie użytkownika, API kontrakt,
  a unit/component wewnętrzną logikę; nie przenoś implementation oracle do UI.
- [Stop branching for UI states](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-tip-stop-branching-for-ui-states-activity-7455340077267787776-sjNE)
  (29 kwietnia 2026) — `if/else` odczytane w jednym momencie może wybrać
  błędną gałąź. Najpierw zsynchronizuj stan, potem asertywnie sprawdź oczekiwany
  wariant zamiast „zgadywać”, co pojawi się na ekranie.
- [Control time with the Clock API](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7479783782863187970-XmM0)
  (6 lipca 2026) — countdown, expiry i debounce testuj przez kontrolowany
  zegar, nie przez realne minuty i `waitForTimeout`.
- [Build self-healing locators with a Locator Factory](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7484857211710685184-RjXN)
  (20 lipca 2026) — można warstwować semantyczne strategie locatora, lecz
  ciche fallbacki muszą nadal failować, gdy element lub zachowanie nie istnieje;
  automatyczne „healing” nie może maskować regresji.
- [Use `locator.and()` when one condition is not enough](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7495004026032463872-OxTh)
  (17 sierpnia 2026) — łącz dwa znaczące locatory (`role` + `title`, test ID
  + label) zamiast budować kruchy CSS. Dla relacji wewnątrz kontenera użyj
  chaining/filter.

### Stefan Minchev — nowe lub wcześniej wykryte, dotąd nieopisane posty

- [50-step E2E: use `test.step()` instead of 300-line stack traces](https://www.linkedin.com/posts/stefan-minchev-qa_qa-typescript-ai-activity-7465764936728735744-7pVi)
  (28 maja 2026) — nazwane kroki biznesowe sprawiają, że raport pokazuje
  intencję, która zawiodła, a nie tylko numer linii.
- [Configure `getByTestId` with a custom attribute](https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7482814931646722048-rPM0)
  (14 lipca 2026) — `use.testIdAttribute` pozwala przejść z istniejącego
  `data-cy`/`data-qa` na API `getByTestId`; dla nowych testów nadal preferuj
  role/label, gdy są dostępne.
- [Pin the timezone in `playwright.config.ts`](https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7491149838202580992-Lp84)
  (6 sierpnia 2026) — `timezoneId` stabilizuje daty renderowane w browserze;
  jeśli expected date powstaje w Node, ustaw także `TZ` procesu.
- [Link test steps to the test-management system](https://www.linkedin.com/posts/stefan-minchev-qa_qa-softwaretesting-testautomation-activity-7452291324638052352-UeIv)
  (21 kwietnia 2026) — zamiast utrzymywać kruche, ręczne instrukcje w TCMS,
  trzymaj GIVEN/WHEN/THEN w kodzie przez `test.step` i dodaj tylko stabilny
  identyfikator integracyjny (np. Qase).
- [Your AI agent needs repository architecture](https://www.linkedin.com/posts/stefan-minchev-qa_orchestrating-ai-native-testing-with-playwright-activity-7470117249555709952-btia)
  (9 czerwca 2026) — model powinien dostać reguły repo dla locatorów, POM,
  fixture, API setup i danych; sam prompt nie definiuje granic architektury.
- [Centralize and validate `process.env`](https://www.linkedin.com/posts/stefan-minchev-qa_qa-typescript-ai-activity-7475895588983586816-fcAh)
  (25 czerwca 2026) — jeden zamrożony, typowany moduł env powinien sprawdzić
  wymagane wartości przy starcie, zamiast rozsiewać `process.env.X!` po specach.
- [Move repeated `beforeEach` setup into fixtures](https://www.linkedin.com/posts/stefan-minchev-qa_playwright-fixtures-activity-7453000985473445888-4EhF)
  (23 kwietnia 2026) — fixture ma setup → `use()` → teardown, jest współdzielona
  przez pliki i kompozycyjna; tworzenie danych przez API oraz cleanup należą do
  lifecycle fixture, nie do duplikowanych hooków.
- [Thirteen lines that pass for the wrong reason](https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7495498494972096512-iIar)
  (18 sierpnia 2026) — kolejność rejestracji route/interception względem
  `goto` ma znaczenie; poprawny składniowo test może nie testować tego, co mówi
  jego nazwa.
- [Close popup races with `Promise.all`](https://www.linkedin.com/posts/stefan-minchev-qa_qa-typescript-ai-activity-7463239833541308417-2c4n)
  (21 maja 2026) — zarejestruj `context.waitForEvent('page')` równocześnie z
  triggerem w `Promise.all`, zanim kliknięcie otworzy nową kartę.
- [Choose the smallest proof before opening a browser](https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7496223286117285888-8HXd)
  (20 sierpnia 2026) — typ/contract/unit check powinien przejąć to, czego nie
  trzeba dowodzić w przeglądarce; E2E zostaw dla realnego user journey.
- [Six prompts for an API test writer](https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7486076434734583808-syo6)
  (23 lipca 2026) — dobry szablon API obejmuje status matrix, Zod schema,
  typed Faker, boundary/negative cases, CRUD cleanup i contract drift. AI nie
  zastępuje review tych oracle.

### Michal Drajna — uzupełnienia katalogu postów LinkedIn

Jego osobny katalog znajduje się w
[michal-drajna-linkedin-playwright-posts.md](michal-drajna-linkedin-playwright-posts.md).
Exa odnalazł także następujące publiczne posty, których nie było w tamtym
zestawieniu albo były tylko pośrednio zasygnalizowane:

- [Playwright E2E Testing Cheatsheet](https://www.linkedin.com/posts/michaldrajna-qa_playwright-e2e-testing-cheatsheet-activity-7366737567368597504-Tdpi)
  (28 sierpnia 2025) — mapa organizacji testów, locatorów, flaków, wydajności,
  debugowania i anti-patternów; traktuj ją jako checklistę, nie normatywną API.
- [Playwright v1.61.0: Native Passkey testing](https://www.linkedin.com/posts/michaldrajna-qa_playwright-v1610-activity-7472239939796025345-KSIz)
  (15 czerwca 2026) — post wskazuje WebAuthn i Web Storage; szczegóły wersji
  są już opisane w katalogu artykułów release.
- [From zero to an enterprise-ready suite with AI](https://www.linkedin.com/posts/michaldrajna-qa_creating-a-playwright-framework-with-ai-activity-7468217482009403392-G3yx)
  (4 czerwca 2026) — repost materiału Cakehursta Ryana; nie jest niezależnym
  projektem ani artykułem Michala.
- [How to migrate 300+ tests to Playwright](https://www.linkedin.com/posts/michaldrajna-qa_good-infrastructure-genai-how-we-migrated-activity-7426550553515704322-oJAc)
  (9 lutego 2026) — repost playbooka Skai Engineering; istotne jako sygnał
  migracji infrastruktury, ale autorstwo źródłowe jest zewnętrzne.
- [Accessibility beyond the green checkmark](https://www.linkedin.com/posts/michaldrajna-qa_playwright-accessibility-testing-what-axe-activity-7451905690887094272-dBpf)
  (20 kwietnia 2026) — repost Davida Mello o ograniczeniach axe; wspiera
  zasadę, by a11y oracle łączyć z testem doświadczenia użytkownika.
- [MCP servers for test automation](https://www.linkedin.com/posts/michaldrajna-qa_mcp-servers-for-test-automation-4-practical-activity-7455160192867983360-nVi8)
  (29 kwietnia 2026) — repost Michała Ślęzaka o łączeniu Playwright, systemu
  plików i narzędzi projektowych przez MCP.

### Vitaliy Potapov

- [Introducing Playwright-magic-steps](https://medium.com/@vitaliypotapov/introducing-playwright-magic-steps-simplify-your-test-automation-workflow-b8911116c16b)
  (18 lipca 2024) — komentarze w kodzie są transformowane do `test.step()`;
  rozwiązanie działa inaczej dla ESM i CommonJS, więc trzeba świadomie ocenić
  koszt hookowania loadera przed użyciem w repo.
- [Playwright in Pictures: How Fixtures Work — Medium mirror](https://medium.com/@vitaliypotapov/playwright-in-pictures-how-fixtures-work-6695b7f6b192)
  (30 czerwca 2026) — mirror wpisu z `vitalets.github.io`; wizualizuje
  `test.extend`, setup/teardown, auto fixtures oraz test/worker scope. Nie
  liczę go jako drugiej publikacji.
- [Why I Prefer BDD over SDD for Agentic Development](https://vitalets.github.io/posts/bdd-agentic-workflow/)
  (10 lipca 2026) — BDD/`Given-When-Then` ma być guardrailem przed generowaniem
  kodu przez agenta; decyzja o BDD powinna wynikać z domeny, nie z chęci
  dodania kolejnej warstwy składni.
- [Supercharge E2E Tests with Playwright-Network-Cache](https://medium.com/@vitaliypotapov/supercharge-your-e2e-tests-with-playwright-network-cache-4f2fa331c30e)
  — proponuje cache odpowiedzi sieciowych jako alternatywę dla ciężkiego
  mockowania. Sprawdź izolację, świeżość danych i wpływ na oracle przed użyciem.
- [Setting up subpath import aliases in TypeScript](https://medium.com/@vitaliypotapov/setting-up-subpath-import-aliases-in-a-typescript-project-3ee027b75f1d)
  — poprawne `rootDir`/`outDir` i mapowanie subpath importów ma znaczenie dla
  typowanych fixture/POM; alias nie może ukrywać cyklicznych zależności.

### Vitali Haradkou

Indeks [Hashnode archive](https://vitalicset.hashnode.dev/archive) oraz
[Playwright series](https://vitalicset.hashnode.dev/series/playwright-labs)
potwierdzają pozycje widoczne w publicznym archiwum. Nowe albo dotąd
nieopisane pozycje (część to warianty publikacji z innych domen):

- [Testing Angular Components by Properties with Playwright](https://dev.to/vitalicset/testing-angular-components-by-properties-with-playwright-33jj)
  (2 kwietnia 2026) — custom selector `angular=` odpytuje komponent po
  `@Input`, signals i stanie, zamiast po klasach CSS. Wymaga Angular DevTools
  API/dev build; nie jest ogólnym zamiennikiem user-facing locatorów.
- [Stop Writing Custom Slack Notifications — Use a Reporter](https://dev.to/vitalicset/your-playwright-tests-deserve-better-slack-notifications-53a1)
  (30 kwietnia 2026) — reporter powinien implementować adapter Playwright,
  a template i webhook muszą być konfigurowane w `playwright.config.ts`, nie w
  shellowym skrypcie rozrastającym się do drugiego systemu.
- [Option<T> and Result<T, E> in TypeScript](https://vitalicset.hashnode.dev/option-t-and-result-t-e-in-typescript-the-missing-error-handling-primitives)
  (20 lutego 2026) — `Option`/`Result` modelują brak wartości i błąd jawnie.
  W testach mogą ograniczyć `undefined`/wyjątki w helperach, lecz dodatkowa
  biblioteka nie jest potrzebna, jeśli prosty typ domenowy wystarcza.
- [Playwright & Prometheus: Send metrics in real time](https://dev.to/vitalicset/playwright-prometheus-send-your-metrics-in-real-time-1aej)
  (22 grudnia 2023) — custom reporter może wysyłać metryki runu do Prometheus.
  To obserwowalność, a nie zastępstwo dla raportu i trace.
- [Real Docker Containers in Playwright Tests — Zero Boilerplate](https://dev.to/vitalicset/real-docker-containers-in-playwright-tests-zero-boilerplate-4ml7)
  (27 marca 2026) oraz [email reports with React Email](https://dev.to/vitalicset/sending-beautiful-playwright-test-reports-via-email-using-shadcnui-and-react-email-1khd)
  (31 marca 2026) są mirrorami/aliasami pozycji zapisanych w iteracji 5;
  nie liczę ich podwójnie.

### ScrollTest / Promode / Pramod Dutta

- [Playwright Fixtures and Hooks Tutorial](https://scrolltest.com/playwright-fixtures-hooks-day-6/)
  (14 czerwca 2026) — hook zostaje lokalny, gdy setup dotyczy jednej specy,
  a fixture jest właściwa dla typowanej, współdzielonej capability. Lifecycle
  fixture to setup → `use()` → teardown po teście.
- [Playwright Configuration TypeScript: Day 32](https://scrolltest.com/playwright-configuration-typescript-day-32/)
  (24 lipca 2026) — rozdziel ustawienia runnera od `use`; `defineConfig`
  zapewnia typowanie. Projekty, retry, artefakty, timeout i workers powinny
  tworzyć czytelny kontrakt wykonania, nie przypadkowy dump opcji.
- [Playwright Test Data Management: Day 45](https://scrolltest.com/playwright-test-data-management-typescript-day-45/)
  (7 sierpnia 2026) — typed factories, unikalność per test/worker, API setup,
  cleanup i jawne granice fixture kontra test eliminują data races, których
  sama izolacja browser context nie rozwiązuje.
- [Playwright Projects TypeScript Guide](https://scrolltest.com/playwright-projects-typescript/)
  (20 lipca 2026) — projekty modelują browser/device, auth, smoke/regression,
  dependencies i grep; nazwa projektu powinna mówić, co bezpiecznie uruchamia.
- [Multi-Environment Configuration: Dev, Staging, Prod](https://scrolltest.com/playwright-multi-environment-configuration/)
  (18 lipca 2026) — jeden typowany lookup środowiska zasila `baseURL`, API,
  retries i flagi; spec nie zna URL-a. Sekrety pozostają poza plikiem config.
- [Playwright API Mocking TypeScript Guide: Day 33](https://scrolltest.com/playwright-api-mocking-typescript-day-33/)
  (25 lipca 2026) — `page.route`, `route.fetch`, typed mock fixtures i HAR
  pozwalają rozdzielić contract/API tests, kontrolowane UI tests i kilka
  cienkich smoke E2E.
- [Playwright Screenshots and Video Recording in TypeScript](https://scrolltest.com/playwright-screenshots-video-recording-typescript-day-53/)
  (17 sierpnia 2026) — screenshot elementu/full-page, maskowanie PII, video
  na retry i trace tworzą failure evidence pack; nie przechowuj w nim sekretów.
- [Playwright Multi-User Testing with TypeScript: Day 44](https://scrolltest.com/playwright-multi-user-testing-typescript-day-44/)
  (6 sierpnia 2026) — osobne `BrowserContext`/storage state i typowane fixture
  są potrzebne dla maker-checker, RBAC i przepływów dwóch użytkowników.
- [Playwright Clock API: Fake Timers in TypeScript](https://scrolltest.com/playwright-clock-api-typescript-day-56/)
  (20 sierpnia 2026) — zamrażaj/przesuwaj browser clock dla OTP, expiry i
  debounce; nie testuj czasu produkcyjnego przez wielosekundowe sleeps.
- [Playwright Debugging TypeScript: Day 31](https://scrolltest.com/playwright-debugging-typescript-day-31/)
  (23 lipca 2026) — najpierw sklasyfikuj locator/assertion/timing/data/env/product
  failure, dopiero potem zmieniaj kod; trace/UI mode/Inspector są narzędziami
  diagnozy, nie wymówką do podnoszenia timeoutu.
- [Global Setup and Teardown: Seeding and Health Checks](https://scrolltest.com/playwright-global-setup-teardown/)
  (1 lipca 2026) — globalSetup jest właściwy dla pracy procesowej, lecz setup
  project z dependencies daje fixture, retry i trace. Wybór musi wynikać ze
  scope oraz potrzeby obserwowalności.
- [Playwright Page Object Model: Day 5](https://scrolltest.com/playwright-page-object-model-day-5/)
  (13 czerwca 2026) — małe typowane POM-y i component objects mogą być
  dostarczane przez fixture; assertion biznesowy pozostaje w teście.
- [Playwright API Testing: Day 8](https://scrolltest.com/playwright-api-testing-day-8/)
  (16 czerwca 2026) — użyj `request` do szybkiego setup/cleanup i walidacji
  post-condition, a browser zachowaj dla realnego user-visible journey.

### Joseph Ward

- [Testing the Data, Not Just the Migration](https://josephward.tech/2026-08-12-test-the-data-not-just-the-migration/)
  (12 sierpnia 2026) — przed pisaniem testów zbadaj rzeczywiste rozkłady,
  nulls, outliers i relacje danych. Test fixture powinien reprezentować realne
  ryzyko danych, nie tylko idealny przykład ze specyfikacji. Blog index nadal
  pokazuje tylko trzy wcześniejsze wpisy stricte Playwright.

### Currents.dev

- [Playwright + Feature Flags: Advanced Test Isolation](https://currents.dev/posts/playwright-feature-flags)
  (19 czerwca 2026) — flagi żyją poza BrowserContext; przechwytuj ich SDK
  przez `page.route`, deklaruj stan jako fixture i izoluj identity per worker,
  zamiast mutować wspólną flagę w `beforeAll`.
- [How to Adopt Playwright the Right Way](https://currents.dev/posts/how-to-adopt-playwright-the-right-way)
  (19 stycznia 2026) — mierz MTTR, lead time i zaufanie do suite; zaczynaj od
  małego zakresu, buduj stabilne dane/auth i dopiero potem skaluj CI.
- [How Playwright Tests Leak Data](https://currents.dev/posts/playwright-avoid-data-leak)
  (2 czerwca 2026) — trace, video, screenshot, HAR i logi mogą zawierać
  tokeny, cookies, PII i body odpowiedzi. Zakres artefaktów, maskowanie,
  retencja i access control są częścią konfiguracji testu.
- [Playwright CI at Scale: GitHub and GitLab](https://currents.dev/posts/playwright-ci-at-scale-github-gitlab)
  (9 czerwca 2026) — trzy poziomy równoległości (workers, `fullyParallel`,
  shards) mogą oversubscribe runner. Zacznij od kilku shardów i niskiego worker
  count, agreguj artefakty i mierz long-tail, nie tylko liczbę testów.
- [Playwright Anti-Patterns: What to Watch For](https://currents.dev/posts/playwright-anti-patterns)
  (27 maja 2026) — usuń hard waits, shared mutable state, nadmiarowe UI setup
  i szerokie retry; stan przenieś do właściwie scoped fixture.
- [What Breaks When the Suite Grows from 20 to 500 Tests](https://currents.dev/posts/what-breaks-when-your-test-suite-grows-from-20-to-500-tests)
  (8 kwietnia 2026) — worker-scoped schema/container lub API-created data może
  izolować backend, ale nadal trzeba kontrolować external dependencies,
  async workflows, retries i konfigurację globalną.

### TestDino

- [Playwright Fixtures: Setup, Scope & Test Helpers](https://testdino.com/blog/playwright-fixtures)
  (aktualizacja 22 kwietnia 2026) — fixture dostarcza zasób przez `use()` i
  automatycznie sprząta; scope jest decyzją o lifecycle, a nie dekoracją testu.
- [Playwright Assertions: `expect()` Guide](https://testdino.com/blog/playwright-assertions)
  (aktualizacja 8 czerwca 2026) — web-first locator assertions retry, generic
  assertions sprawdzają już rozwiązaną wartość, `expect.soft` zbiera błędy,
  a `poll`/`toPass` służą odpowiednio callbackowi i powiązanemu blokowi.
- [Playwright E2E Testing Setup Guide 2026](https://testdino.com/blog/playwright-e2e-testing)
  (2 marca 2026) — TypeScript, semantic locators, web-first assertions,
  trace-based debugging i rozdzielenie unit/API/E2E dają lepszy feedback niż
  automatyzowanie wszystkiego przez browser.
- [Playwright 1.59 Release Guide](https://testdino.com/blog/playwright-release-guide)
  (22 kwietnia 2026, aktualizacja 30 czerwca) — omawia screencast,
  `browser.bind`, trace CLI, `await using` i agenty; wersję trzeba porównać z
  lokalnym pinem przed użyciem.
- [Playwright CLI: Commands and Setup](https://testdino.com/blog/playwright-cli)
  (aktualizacja 17 maja 2026) — odróżnia `npx playwright` od osobnego
  `@playwright/cli`; snapshoty na dysku ograniczają kontekst agenta, lecz nie
  zastępują Playwright Test runnera.
- [Playwright 1.60: API Drop, HAR Tracing & Diagnostics](https://testdino.com/blog/playwright-1-60-release)
  (2026) — indeks release’u wskazuje `locator.drop`, `tracing.startHar`, ARIA
  boxes, `test.abort` i `errorContext`; traktuj jako release note, nie jako
  rekomendację aktualizacji repo do wersji innej niż 1.61.0.

### Level Up Coding / Mohammad Faisal Khatri

- [Authentication Setup in Playwright TypeScript](https://levelup.gitconnected.com/how-to-use-authentication-setup-in-playwright-typescript-f54bc68356f4)
  (31 lipca 2026) — setup project zapisuje `storageState`, a projekty zależne
  ładują je bez powtarzania logowania; plik auth musi być izolowany i poza VCS.
- [PUT, PATCH and DELETE API Requests](https://levelup.gitconnected.com/how-to-test-a-put-patch-and-delete-api-request-using-playwright-typescript-2cd61e313b10)
  (6 maja 2026) — `request` fixture obsługuje CRUD API, status i body;
  mutujące testy powinny kończyć się kontrolowanym cleanupem.
- [POST API Requests with Playwright TypeScript](https://levelup.gitconnected.com/how-to-test-post-api-requests-with-playwright-typescript-5a210ed2f500)
  (21 lutego 2026) — body może pochodzić z obiektu, JSON file albo typed Faker;
  dane generuj tak, by przypadki negatywne i powtarzalność były jawne.
- [Multiple Environments in Playwright TypeScript](https://levelup.gitconnected.com/playwright-typescript-multiple-environments-a-complete-real-world-guide-4173bb136d68)
  (20 lipca 2026) — projects, `baseURL`, env vars i dotenv pozwalają zmieniać
  środowisko bez if/else w specach; credentials i feature flags pozostają
  konfigurowalne per environment.
- [Network Interception with Playwright TypeScript](https://levelup.gitconnected.com/network-interception-with-playwright-typescript-0dd32195848b)
  (11 sierpnia 2026) — monitoruj, mockuj, modyfikuj i opóźniaj requests przez
  route; dla stabilności rozdziel kontrolowane UI mocks od cienkich real E2E.

### Sajith Dilshan / Medium

- [Playwright API Interception & Mocking](https://medium.com/@sajith-dilshan/playwright-api-interception-mocking-a-senior-qa-engineers-guide-to-stable-and-scalable-ui-07704f7c18a8)
  (30 kwietnia 2026) — `page.route()` izoluje UI od niestabilnego backendu;
  interception nie powinna zastąpić kilku kontraktowych testów real API.
- [Window Size vs Viewport Size](https://medium.com/@sajith-dilshan/understanding-window-size-vs-viewport-size-in-playwright-complete-guide-e3cdfacc634a)
  (18 kwietnia 2026) — viewport opisuje obszar strony, a window obejmuje
  chrome; konfiguruj device/viewport świadomie dla responsive i screenshotów.
- [From Selenium to Playwright: Migration Journey](https://medium.com/@sajith-dilshan/from-selenium-to-playwright-a-senior-qa-engineers-migration-journey-77125ae54c05)
  (7 marca 2026) — migracja zyskuje na auto-wait, native protocol,
  interception i prostszej paralelizacji, lecz wymaga przebudowy setupu i
  danych, nie mechanicznej zamiany locatorów.
- [How to Automate MFA in Playwright](https://medium.com/@sajith-dilshan/how-to-automate-mfa-in-playwright-3e75f6b6301a)
  (28 lutego 2026) — MFA testuj przez kontrolowany testowy kanał OTP/TOTP,
  service account lub authenticator; sekrety trzymaj w secret managerze, nigdy
  w fixture, trace ani `storageState` commitowanym do repo.
- [Secure Credential Management in Playwright](https://medium.com/@sajith-dilshan/secure-credential-management-in-playwright-0cf75c4e2ff4)
  (31 stycznia 2026) — lokalny `.env` i sekrety CI powinny być rozdzielone,
  walidowane przy starcie i maskowane w logach/artefaktach.

Profil Medium pokazuje także wpisy o auto-waiting, hooks, annotations,
fixtures, `tsconfig` i modułach, które są już w indeksach 1 i 4. LinkedIn
reshare’y tych samych artykułów nie są liczone drugi raz.

### Artem Bondar / Bondar Academy

- [How to Install Playwright in VS Code (2026)](https://bondaracademy.com/blog/how-to-install-playwright-in-vs-code)
  (4 maja 2026) — instalacja Node LTS/VS Code i scaffold projektu; wybór
  TypeScript daje typowanie configu i refaktoryzację. To materiał startowy,
  nie nowy wzorzec fixture.

[Bondar Academy blog index](https://bondaracademy.com/blog) potwierdza, że
  wcześniejsza iteracja 3 zmapowała jego relewantne pozycje o fixture,
  projects, API, POM, locators, assertions, data-driven testing, storage state
  i CI. W szóstej rundzie nie znaleziono kolejnego niezdublowanego artykułu
  o tych tematach.

### Yevhen Laichenkov

[Posts index](https://elaichenkov.github.io/posts/) i tag Playwright nadal
  prowadzą do dwóch wcześniej opisanych wpisów: [17 Playwright Testing
  Mistakes](https://elaichenkov.github.io/posts/17-playwright-testing-mistakes-you-should-avoid)
  oraz [step decorator](https://elaichenkov.github.io/posts/til-playwright-step-decorator).
  Nie znaleziono nowego artykułu Playwright/TypeScript.

### Butch Mayhew / Playwright Solutions

- [AI can write Playwright tests fast](https://www.linkedin.com/posts/butchmayhew_ai-can-write-playwright-tests-fast-in-activity-7492931581888827392-uiNI)
  (11 sierpnia 2026) — jakość repo determinuje wynik agenta: słabe assertions,
  hard waits i ogromne specy pojawią się szybko. Model pracy to Prompt →
  Review → Run, z Arrange/Act/Assert i gotowymi reporterami/trace.
- [Playwright is moving toward agentic QA](https://www.linkedin.com/posts/butchmayhew_orchestrating-ai-native-testing-with-playwright-activity-7470086051827580928-GCGE)
  (9 czerwca 2026) — AI może przyspieszyć execution, ale człowiek pozostaje
  odpowiedzialny za ryzyko, architekturę i jakość fixture/POM/API/data.
- [AI tests pass for the wrong reasons](https://www.linkedin.com/posts/butchmayhew_the-scariest-ai-testing-finding-this-week-activity-7479882663286382592-P-dI)
  (6 lipca 2026) — zielony test nie dowodzi poprawnego oracle; po zmianie UI
  sprawdzaj, czy test nadal obserwuje właściwe zachowanie.
- [Expectation-Driven Development](https://www.linkedin.com/posts/butchmayhew_aiinqa-softwaretesting-aitesting-activity-7467202064004132864-qEuX)
  (1 czerwca 2026) — zdefiniuj oczekiwania przed implementacją i wymagaj
  dowodu, lecz odróżniaj deterministyczne assertions od probabilistycznych
  evals dla AI.
- [Confidence is the new coverage](https://www.linkedin.com/posts/butchmayhew_confidence-is-the-new-coverage-youll-activity-7485018111390138368-vO2a)
  (20 lipca 2026) — coverage/pass rate nie zastępują pytania, czy system
  działa; dla deterministycznej aplikacji zachowaj klasyczne assertions,
  a evals stosuj tam, gdzie wynik jest niedeterministyczny.
- [AI-generated 20 test cases with playwright-cli](https://www.linkedin.com/posts/butchmayhew_i-asked-ai-to-generate-20-test-cases-using-activity-7423012944092893184-Txqq)
  (30 stycznia 2026) — wygenerowane przypadki bez strategii danych, auth i
  skalowalnego frameworka są tylko punktem startowym.
- [Shard Playwright tests instead of running sequentially in CI](https://www.linkedin.com/posts/butchmayhew_running-your-playwright-tests-sequentially-activity-7419161443394211841--ahx)
  (19 stycznia 2026) — zacznij od 2–4 shardów, mierz rozkład czasu i nie
  zakładaj, że statyczny podział zawsze będzie równy.
- [Spend more time fixing tests than writing new ones](https://www.linkedin.com/posts/butchmayhew_playwright-testing-workshop-activity-7425176562519515136-ATtJ)
  (5 lutego 2026) — problemy z flaky, wolnymi testami są zwykle problemem
  projektu: locatorów, stanu i architektury. Zalecany kierunek to semantyczne
  locatory, izolacja, równoległość i świadome retry, a nie kolejne sleep.
- [Playwright CLI `show` for live agent sessions](https://www.linkedin.com/posts/butchmayhew_15-days-thats-how-long-it-took-the-playwright-activity-7429162688762519553-XOqQ)
  (16 lutego 2026) — obserwacja i przejęcie sesji agenta pomagają debugować
  agentic browser workflows; to narzędzie operacyjne, nie assertion.

Profil [Butcha](https://playwrightsolutions.com/author/butch/) nadal jest
dynamiczny i paginowany. Tutoriale T.J. Mahera oraz reposty innych autorów
zwracane przez Exa nie zostały przypisane Butchowi.

## Synteza praktyk

1. **Locator i oracle:** używaj role/label/test ID zgodnie z kontraktem UI;
   `locator.and()`/`filter()` zwiększa precyzję, ale fallback/self-healing nie
   może ukrywać braku elementu. Assertions mają sprawdzać wynik użytkownika,
   nie strukturę DOM.
2. **Synchronizacja:** actionability i web-first assertions są domyślną
   ścieżką. `expect.poll` służy bezefektownemu odczytowi eventual state,
   `expect.toPass` powtarza wąski trigger → wait → verify block, a
   `test.step` nadaje raportowi sens biznesowy. Unikaj `waitForTimeout` i
   ręcznych pętli sleep.
3. **Fixture i dane:** typowane fixture mają jasno wybrany test/worker scope,
   cleanup po `use()` i unikalne dane. API/setup project służy do szybkiego
   przygotowania, browser do user-visible journey.
4. **`playwright.config.ts`:** `defineConfig`, projects, dependencies,
   `testIdAttribute`, env profiles, workers/retries, artefakty i timezone
   powinny tworzyć jeden jawny kontrakt wykonania. Nie rozrzucaj `process.env`
   po specach.
5. **TypeScript:** `import type`, `strict`, małe typy opcji POM, discriminated
   unions/`Result` tam, gdzie poprawiają kontrakt, oraz centralny moduł env są
   lepsze niż `any`, `!` i casty maskujące błąd.
6. **AI i dowód:** agent generuje szkic; człowiek weryfikuje dane, assertions,
   izolację, artefakty i rzeczywiste zachowanie. Zielony run nie jest dowodem,
   jeśli test może przechodzić z niewłaściwego powodu.

## Wpływ na Payment Quality Engineering Lab

- Nie zmieniam kodu aplikacji ani istniejących testów na podstawie samych
  blogów. Wnioski są zgodne z obecnym POM: projekty auth, izolacja danych,
  `test.step`, `expect.poll`, `toPass` i artefakty po failure.
- Najbardziej bezpośrednie punkty do przyszłego review to: `testIdAttribute`
  tylko jako migracyjny most, walidacja env przy starcie, rozdzielenie
  production/staging, maskowanie trace/HAR oraz kontrolowany Clock API dla
  testów zależnych od czasu.
- Nie rekomenduję dodawania `playwright-magic-steps`, network cache, custom
  locator engine ani reporterów bez osobnej decyzji o zależności, scope,
  bezpieczeństwie artefaktów i zgodności z Playwright 1.61.0.

## Metoda, kompletność i ograniczenia

- Użyto MCP **Exa Search** do discovery oraz **Exa Fetch** do weryfikacji
  canonical URL, dat, byline i treści nowych stron.
- Recheck nie jest dowodem kompletności całego Internetu. Publiczne LinkedIn,
  Medium i paginowane archiwa mogą ukrywać wpisy, a Exa może zwracać mirror,
  repost lub stronę indeksu zamiast źródła pierwotnego.
- Pozycje „brak nowej” oznaczają brak nowego, jednoznacznie autorskiego,
  relewantnego URL-u w dostępnych wynikach tej rundy.
- Pełne teksty cudzych publikacji nie są kopiowane; plik zawiera linki i
  zwięzłe, własne streszczenia.

## Źródła normatywne

- [Playwright Best Practices](https://playwright.dev/docs/best-practices) — locatory, izolacja, web-first assertions.
- [Playwright Fixtures](https://playwright.dev/docs/test-fixtures) — DI, scope i teardown.
- [Playwright Assertions](https://playwright.dev/docs/test-assertions) — locator assertions, `poll`, `toPass`.
- [Playwright Configuration](https://playwright.dev/docs/test-configuration) — runner options, projects i `use`.
- [Playwright Test API: `test.step`](https://playwright.dev/docs/api/class-test#test-step) — raportowalne kroki.

W razie konfliktu bloga z dokumentacją lub lokalnym pinem `@playwright/test`
**1.61.0** pierwszeństwo ma dokumentacja oficjalna i kod repozytorium.
