# Playwright + TypeScript — siódma iteracja: recheck źródeł (2026-08-28)

## Zakres i zasady

Ta runda użyła MCP **Exa Search** oraz **Exa Fetch**. Wyniki porównałem z
indeksami iteracji 1–6 oraz z osobnym katalogiem postów Michala Drajny. Poniżej
zapisuję nowy albo dotąd nieopisany delta dotyczący Playwright, TypeScript,
idiomatycznych locatorów i asercji, custom fixtures, `playwright.config.ts`,
pollingu, `test.step`, danych, CI i konstrukcji testów.

Nie kopiuję pełnych cudzych publikacji. Każda pozycja ma canonical URL,
autorstwo/datę (gdy źródło je podało) i własne streszczenie. Reposty, mirrory,
strony indeksów oraz źródła wtórne są jawnie oznaczone.

## Nowe i uzupełnione materiały

### Anton Gulin

- [Review AI-generated tests with seven checks](https://www.anton.qa/blog/posts/review-ai-generated-tests-seven-checks) (12 sierpnia 2026) — przed przyjęciem testu nazwij ryzyko użytkownika, celowo zepsuj zachowanie, sprawdź końcowy rezultat, zmień dane, przeczytaj komunikat błędu, uruchom ponownie i zdecyduj, czy test zatrzyma złą wersję. Sam fakt, że wygenerowany plik przechodzi, nie jest dowodem wartości.
- [Your Regression Suite Is a Museum](https://www.anton.qa/blog/posts/regression-suite-museum) (19 sierpnia 2026) — audytuj stare testy przez historię failów, chronione ryzyko, eksperyment z `--grep-invert`, rozróżnienie „kroki vs rezultat” oraz odwrócenie oracle. Test, który przechodzi także po odwróceniu oczekiwania, powinien zostać usunięty lub przepisany.

### Michal Drajna — świeże posty i wskazane źródła kanoniczne

- [How to Create Custom Fixtures in Playwright TypeScript](https://www.linkedin.com/posts/michaldrajna-qa_how-to-create-custom-fixtures-in-playwright-activity-7499020460131766273-l642) (28 sierpnia 2026; repost Level Up Coding) — przypomnienie, że fixture DI zastępuje kopiowane `beforeEach`, dostarcza typowane POM/dane tylko testom, które ich żądają, i utrzymuje cleanup w jednym miejscu.
- [Why Your Playwright Tests Are Lying to You](https://www.linkedin.com/posts/michaldrajna-qa_why-your-playwright-tests-are-lying-to-you-activity-7498658155678244865-tiNA) (27 sierpnia; repost Abdur Raffae Masood) — flaki zwykle wynikają z race’ów DOM, animacji, współdzielonego stanu, sieci, przeciążenia CI i locatorów związanych z layoutem; retry nie zastępuje diagnozy.
- [What Actually Breaks When Your Playwright Suite Hits 10,000 Tests](https://medium.com/@ARaffaeSQA/what-actually-breaks-when-your-playwright-suite-hits-10-000-tests-6e7f15279369) (29 lipca; autor źródłowy Abdur Raffae Masood) — skalowanie ogranicza izolacja, organizacja repo, realna równoległość i czas informacji zwrotnej. Unikalne dane, API setup i organizacja wg własności są ważniejsze niż samo zwiększenie liczby workerów. DEV jest [mirrorem](https://dev.to/araffaesqa/what-actually-breaks-when-your-playwright-suite-hits-10000-tests-55g2).
- [Lighthouse Performance Audits in Playwright](https://scrolltest.com/playwright-lighthouse-performance-audits/) (5 sierpnia; repost ScrollTest) — Michal wskazał testy Lighthouse jako warstwę Core Web Vitals, a nie zamiennik testów funkcjonalnych. Szczegóły techniczne są w sekcji ScrollTest.
- [What if learning Playwright felt like playing a video game?](https://www.linkedin.com/posts/michaldrajna-qa_what-if-learning-playwright-felt-like-playing-activity-7498295694269730817-WAL8) (26 sierpnia) — Playwright Quest uczy locatorów i E2E przez interaktywne zadania; to materiał edukacyjny, nie nowy wzorzec produkcyjnego frameworka.
- [Playwright MCP gives an AI agent a browser](https://www.linkedin.com/posts/michaldrajna-qa_playwright-mcp-gives-an-ai-agent-a-browser-activity-7490356957200031744-CmT9) (sierpień; repost Daniela Dahlina) — model-governed generation wymaga reguł domeny przed generowaniem kodu, inaczej agent tworzy duplikaty POM, kruche selektory i drift. To repost, więc nie przypisuję autorstwa Michalowi.

### Viktor Konovalov

- [Snapshots aren’t just for UI](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-api-activity-7487397613378363392-7Egr) (27 lipca 2026) — snapshot API pomaga wykrywać drift stabilnych pól konfiguracji, feature flags i dużych payloadów. Usuń ID, timestampy i tokeny przed zapisaniem wzorca; snapshot uzupełnia, a nie zastępuje, targeted assertions i schema validation.
- [Use `locator.evaluateAll()` in one browser call](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7482327981252591616-q1yt) (13 lipca) — przy tabelach/listach zbieraj i transformuj dane w jednym wywołaniu w przeglądarce, zamiast wykonywać osobne `textContent()` dla każdego elementu.
- [Use CDPSession when the public API is not enough](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7489942200819015680-t7Pt) (3 sierpnia) — CDP jest uzasadnione dla emulacji sieci, metryk i niskopoziomowych możliwości Chrome, lecz publiczne API jest stabilniejszym domyślnym wyborem. Sesja ma zakres pojedynczego cyklu życia targetu i nie powinna być globalnym uchwytem.
- [Validate the accessibility tree, not the DOM](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7472539768522940418-yMME) (16 czerwca) — `getByRole()`/`getByLabel()` testują powierzchnię użytkownika i są odporniejsze na refaktory DOM. CSS/test ID są fallbackiem, gdy aplikacja nie ma stabilnej semantyki.
- [Run `page.addInitScript()` before the app starts](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7470361720436183040-N_HK) (10 czerwca) — feature flags, localStorage, session data i kontrolowane API przeglądarki ustawiaj przed skryptami aplikacji; ustawienie ich po `goto()` może przegapić odczyt startowy.
- [Compare Playwright HTML, Allure and QAit reports](https://www.linkedin.com/posts/viktorkonovalovqa_day3reportcomparison-activity-7492837359609192448-K0mf) (11 sierpnia) oraz [secrets-management experiment](https://www.linkedin.com/posts/viktorkonovalovqa_day4secrets-activity-7495385292724596736-ib3O) (18 sierpnia) — raport ma być oceniany po użyteczności diagnostycznej i kosztach utrzymania, a sekrety powinny być szyfrowane, wstrzykiwane runtime i maskowane w logach/trace. Drugi wpis ma disclosure komercyjny; traktuję go jako doświadczenie, nie normę.

### Stefan Minchev

- [One login test plus `storageState`](https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7490425075612049409-pXAh) (4 sierpnia 2026) — setup project loguje raz, zapisuje `storageState`, a projekty zależne przywracają sesję. Zachowaj jeden prawdziwy test formularza logowania i ignoruj `playwright/.auth` w VCS.
- [Automate the second factor instead of disabling it](https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7493686570198142978-GBRT) (13 sierpnia) — TOTP generuj z testowego sekretu w setupie, a SMS/e-mail pobieraj przez kontrolowane API. Nie używaj kodu jednorazowego wpisanego na stałe ani konta prawdziwego użytkownika; sekret pozostaje w CI secret managerze.
- [Seed the order through the request fixture](https://www.linkedin.com/posts/stefan-minchev-qa_seed-data-via-api-activity-7492961792541851648-oLlI) (11 sierpnia) — test strony zamówienia powinien dostać dane przez API i sprzątnąć je w fixture; pełny checkout pozostaje jednym osobnym testem UI.
- [`isVisible()` is not a waiting assertion](https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7488613133301792768-wV3t) (30 lipca) — `isVisible()` zwraca stan chwilowy, więc może pominąć późno renderowany element i dać fałszywie zielony test. Gdy element ma się pojawić, użyj `await expect(locator).toBeVisible()`; boolean czytaj tylko po świadomym ustaleniu stanu.
- [Switch environments with one typed variable](https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7487889010883801088-yhnc) (28 lipca) — `TEST_ENV` wybiera zestaw `.env.<env>` i wspólny `.env`; `cross-env` zapewnia przenośność skryptów, a `.env.example` jest jedynym plikiem przeznaczonym do commitowania.
- [I Made Stefan Minchev Defend Every Decision in Our Playwright Framework](https://dev.to/idavidov13/i-made-stefan-minchev-defend-every-decision-in-our-playwright-framework-2dfa) (24 sierpnia; Ivan Davidov) — rozmowa podkreśla, że folder tree, fixture, data isolation, lint rules i konwencje są architekturą przed specami. Scaffold jest nośnikiem decyzji, nie czymś do bezmyślnego kopiowania.

### Angela Zelaya — recheck potwierdza reposty, nie niezależne artykuły

- [So good! — Claude Code + Playwright CLI](https://www.linkedin.com/posts/angela-zelaya-b185b218b_getting-started-with-claude-code-and-playwright-activity-7490742116910870528-06b9) (5 sierpnia 2026) — repost Michala/Aswaniego o lokalnych snapshotach YAML i ograniczaniu kontekstu agenta.
- [Playwright CLI UI Review and `generate-locator`](https://www.linkedin.com/posts/angela-zelaya-b185b218b_giving-ui-reviews-to-coding-agents-playwright-activity-7455966334925910016-Lt1E) (1 maja) — repost ogłoszenia Playwright o review UI, drop/clipboard i stabilnych refach.

Oba wpisy są wartościowymi sygnałami o narzędziach, ale nie są autorskimi
artykułami technicznymi Angeli.

### ScrollTest / Promode i Pramod Dutta — nowe strony znalezione w rechecku

- [Playwright Contract Testing with TypeScript — Day 46](https://scrolltest.com/playwright-contract-testing-typescript-day-46/) (8 sierpnia 2026) — oddziel kontrakt (shape, typy, enumy, nullability) od API behavior i E2E. Zod/AJV walidują JSON runtime, a osobny projekt `contracts` może być dependency przed `chromium-e2e`.
- [API Contract Testing: REST + UI in One Test](https://scrolltest.com/api-contract-testing-playwright-rest-ui-one-test/) (14 maja) — hybrydowy przepływ: API tworzy dane i sprawdza schemat, browser potwierdza user-visible rendering. Skraca setup, ale nie usuwa potrzeby osobnych API/contract checks.
- [TypeScript for Playwright Testing](https://scrolltest.com/typescript-for-playwright-testing/) (17 maja) — typy są dokumentacją fixture/POM i wykrywają błędy przed runtime. W praktyce preferuj `strict`, jawne modele danych i typowany config zamiast `any`/castów.
- [Playwright Suite Lying? 3 Signs](https://scrolltest.com/playwright-suite-lying-tests/) (11 sierpnia) — zielony run może ukrywać słabe assertions, fake data i brak error-state coverage. Sprawdzaj, czy test failuje po usunięciu oczekiwanego rezultatu.
- [Playwright Retries and Flaky Tests — Day 42](https://scrolltest.com/playwright-retries-flaky-tests-day-42/) (3 sierpnia) — rozsądny default to brak retry lokalnie, jedno retry w CI i trace na pierwszym retry. Retry ma dać dowód do triage, nie maskować błędny locator.
- [Flaky Test Audit Template](https://scrolltest.com/playwright-flaky-test-audit-template/) (9 sierpnia) — wybierz testy z największą liczbą retry, przypisz ownera i evidence link, rozdziel product/test/data/infra failure i nie zwiększaj retry przed klasyfikacją.
- [Browser Contexts: Test Isolation in TypeScript — Day 52](https://scrolltest.com/playwright-browser-contexts-typescript-day-52/) (16 sierpnia) — `BrowserContext` izoluje cookies, storage, permissions i cache; jest właściwą granicą dla multi-user/RBAC i równoległych scenariuszy.
- [Authentication — Day 10](https://scrolltest.com/playwright-authentication-day-10/) (18 czerwca) — UI login testuj raz, a pozostałe specy zaczynaj ze sprawdzonym `storageState` lub API login. Role i auth files muszą być izolowane per projekt/worker i poza VCS.
- [Performance Testing with TypeScript — Day 47](https://scrolltest.com/playwright-performance-testing-typescript-day-47/) (9 sierpnia) — mierz page/API/resource timing i stabilne budżety w osobnym projekcie; Playwright jest single-user performance gate, nie load-testingiem.
- [Lighthouse Performance Audits in Playwright](https://scrolltest.com/playwright-lighthouse-performance-audits/) (5 sierpnia) — Lighthouse może audytować stan po loginie i interakcji przez CDP. Wymaga stałego portu debugowania i osobnego, kontrolowanego przebiegu; nie jest powodem do ręcznego uruchamiania Chromium w zwykłych testach E2E.
- [iFrames in TypeScript — Day 50](https://scrolltest.com/playwright-iframes-frames-typescript-day-50/) (14 sierpnia) — `frameLocator()` jest scoped locator zamiast mutowalnego `switchTo`; czekaj na właściwą granicę dokumentu i używaj semantycznych locatorów wewnątrz frame.
- [Tags and Annotations — Day 48](https://scrolltest.com/playwright-tags-annotations-typescript-day-48/) (11 sierpnia) — tag wybiera test, annotation wyjaśnia lub zmienia zachowanie (`skip`, `fail`, `slow`, `fixme`), a project wybiera browser/env. Ustal małą taksonomię i nie używaj tagów jako ukrytego mechanizmu retry.
- [Network Interception — Day 8](https://scrolltest.com/21-day-playwright-day-8-network-interception-mock-api/) (17 sierpnia, aktualizacja serii) — `page.route()` mockuje odpowiedzi, symuluje 5xx, blokuje zasoby i pozwala czekać na konkretny response. Rejestruj route przed `goto()`/akcją, a mocki równoważ realnymi contract tests.
- [Mobile Testing and Device Emulation — Day 49](https://scrolltest.com/playwright-mobile-testing-typescript-day-49/) (13 sierpnia) — descriptor urządzenia ustawia UA, viewport, DPR, touch i mobile behavior; sam resize nie dowodzi doświadczenia telefonu. Emulacja nie zastępuje testu na realnym urządzeniu.
- [Date Pickers and Calendars](https://scrolltest.com/playwright-date-picker-calendar-testing/) (11 lipca) — najpierw `fill('yyyy-mm-dd')` dla natywnego inputu; gdy popup jest jedyną drogą, nawiguj miesiącami i zamrażaj „today” przez Clock API zamiast sleepów.
- [Toast Notifications and Timing](https://scrolltest.com/playwright-toast-notification-testing/) (23 lipca) — używaj `getByRole('status'/'alert')` i web-first `toBeVisible`/`toBeHidden`; dla krótkich toastów kontroluj request/timer i nie ścigaj ich przez `waitForTimeout`.
- [Keyboard Navigation and Accessibility](https://scrolltest.com/playwright-keyboard-navigation-accessibility/) (13 sierpnia) — testuj focus, Tab/Shift+Tab/Enter/Space, role i accessibility tree; axe-core jest dodatkiem, nie dowodem pełnej użyteczności klawiaturą.
- [Allure Reporting](https://scrolltest.com/playwright-allure-reporting-complete-guide/) (10 sierpnia) — Allure uzupełnia natywny HTML o trendy, severity i TMS; można rejestrować oba reportery, ale trzeba pilnować kosztu/retencji artefaktów.
- [Release Notes Checklist](https://scrolltest.com/playwright-release-notes-checklist/) (27 czerwca) — każdą zmianę runnera, browsera, trace, loadera, fixture lub asercji zamieniaj na mały test ryzyka przed upgrade’em.
- [Playwright 1.62 Upgrade Checklist](https://scrolltest.com/playwright-1-62-upgrade-checklist/) (1 sierpnia) i [1.62 Regression Checklist](https://scrolltest.com/playwright-1-62-regression-checklist/) — najpierw zamroź baseline, wersję Node/browser, skipped tests i raporty; potem sprawdź component fixtures, AbortSignal, WebP, retry strategy, CI cache i tsconfig.
- [Release Smoke Suite for 1.62.1](https://scrolltest.com/playwright-release-smoke-suite-1621/) (3 sierpnia) — mały gate (kilkanaście testów, wszystkie browser projects, trace on first retry) ma odpowiadać za zaufanie do platformy testowej, nie zastępować produktu smoke.
- [Browser AI Testing: 1.62.1 Deep Dive](https://scrolltest.com/playwright-browser-ai-testing/) (15 sierpnia) — MCP/CLI, WebP, isolated retries i browser bump powinny wejść przez kontrolowany rollout; aktualizacja engine może zmienić screenshoty i zachowanie.
- [Playwright vs Selenium in 2026](https://scrolltest.com/playwright-vs-selenium-2026/) (13 sierpnia) — porównuj narzędzia przez browser matrix, natywną paralelizację, multi-tab, CI i ryzyko produktu, nie przez popularność pojedynczego API.

### Currents.dev — nowe strony i repozytorium skill

- [AI Skill: Playwright Best Practices](https://currents.dev/posts/playwright-best-practices-skill) (3 lutego 2026) oraz [dokumentacja skill](https://docs.currents.dev/ai/agent-skill-playwright-best-practices) — skill dostarcza agentowi progressive disclosure dla locatorów, assertions/waiting, fixtures, danych, debugowania, a11y, mobile, CI, security i performance. Repozytorium [playwright-best-practices-skill](https://github.com/currents-dev/playwright-best-practices-skill) jest źródłem technicznym, ale nadal wymaga lokalnego review i zgodności z pinem Playwright.
- [The `--last-failed-file` flag](https://currents.dev/posts/playwright-re-run-only-failed-tests) (22 czerwca) — w sharded GitHub Actions zapisuj `.last-run.json` we wskazanym pliku; cache key powinien uwzględniać `run_id`, shard i `run_attempt`, a restore/save wykonywać się osobno z `if: always()`.
- [HTML Reporter at Scale](https://currents.dev/posts/playwright-html-reporter-why-it-breaks-down-at-scale) (24 kwietnia) — HTML jest świetny do jednego runu i małej suite, lecz nie pokazuje trendów flakiness, korelacji workerów ani historii. Blob/merge i zewnętrzna analityka są decyzją operacyjną, nie powodem do porzucenia trace.
- [State of Playwright AI Ecosystem](https://currents.dev/posts/state-of-playwright-ai-ecosystem-in-2026) (2 marca) — MCP to constrained execution, agents to planner→generator→healer, a trace/report/video to evidence. MCP nie dostarcza strategii; permissions, sekrety i oracle pozostają odpowiedzialnością zespołu.
- [When Tests Should Run Headless vs Headed](https://currents.dev/posts/when-tests-should-run-headless-vs-headed-in-playwright) (25 lutego) — headless CI/headed debug to dobra reguła pracy, ale losowe przełączanie trybu nie jest diagnozą. `channel: 'chromium'` może ujednolicić pełny browser między headed i headless kosztem zależności systemowych.
- [The Network Mocking Playbook](https://currents.dev/posts/the-playwright-network-mocking-playbook) (1 kwietnia) — mocking jest spektrum od real integration do static route; mock debt rośnie, gdy kontrakty zewnętrzne zmieniają się bez testu realnego. Ustal, co mockujesz, co kontraktujesz i co uruchamiasz w sandboxie.

### TestDino

- [17 Playwright Best Practices](https://testdino.com/blog/playwright-best-practices) (wcześniej zindeksowane; aktualizacja 12 sierpnia 2026) — cele coverage, user-facing locators, nazwy testów/kroków, izolacja, web-first assertions, accessibility tree, API setup, reset DB, route mocks, struktura repo, debug, `test.abort`, parallel/sharding, flake control, AI review i centralny reporting.
- [Playwright UI Mode](https://testdino.com/blog/playwright-ui-mode) (16 lipca, aktualizacja 3 sierpnia) — UI Mode daje time-travel, DOM/network snapshots, filtrowanie testów i szybki feedback; służy do lokalnej diagnozy, nie zastępuje headless CI ani asercji.
- [Flaky Test Debugging](https://testdino.com/blog/playwright-flaky-test-debugging) (18 lipca) — źródło jest częściowo dynamiczne, lecz kieruje do trace, race-condition i CI triage. Traktuję je jako uzupełnienie procesu, nie jako dowód konkretnej recepty.
- [Headless vs Headed](https://testdino.com/blog/headless-vs-headed) (18 marca, aktualizacja 8 czerwca) — headless jest domyślny i tańszy dla CI; headed jest narzędziem debugowania. Pełny Chromium przez `channel: 'chromium'` zmniejsza różnicę binariów.
- [Trace Viewer](https://testdino.com/blog/playwright-trace-viewer) (13 lutego, aktualizacja 3 czerwca) — czytaj timeline, snapshots, network, console i attachments; `trace on-first-retry` daje dobry kompromis diagnostyka/koszt, a dane w trace trzeba traktować jak potencjalne sekrety.
- [Why Tests Pass Locally but Fail in CI](https://testdino.com/blog/playwright-test-failure) (18 lutego, aktualizacja 21 maja) — wyszczególnia async wait, concurrency, environment/resource, network, order, timezone i random data; nie podnoś timeoutu bez nazwania przyczyny.
- [Performance Testing Using Playwright](https://testdino.com/blog/playwright-performance-testing) (19 maja) — mierz navigation timing, Web Vitals, resource timing i flow duration, ale nie nazywaj tego load testem. Budżety powinny być stabilne i oddzielone od domyślnego E2E gate.

### Level Up Coding / Mohammad Faisal Khatri

- [Custom Fixtures in Playwright TypeScript](https://levelup.gitconnected.com/how-to-create-custom-fixtures-in-playwright-typescript-a-complete-practical-guide-4fa8b2fc2c82) (24 lipca 2026) — `test.extend` centralizuje POM, API client i dane; `as const satisfies` utrzymuje dokładne wartości i shape. Fixture jest lazy, ma setup → `use()` → teardown i nie powinna przenosić business assertions z testu.
- [Browser vs BrowserContext vs Page](https://levelup.gitconnected.com/playwright-browser-vs-browsercontext-vs-page-complete-guide-with-examples-b6c771c8d371) (22 lipca) — Browser to proces, Context to izolowana sesja, Page to karta. W zwykłych testach używaj managed fixtures; ręczne `chromium.launch()` zostaw multi-context/performance utilities.
- [Data-driven API Testing — Part 1](https://levelup.gitconnected.com/data-driven-api-testing-in-playwright-typescript-part-1-cc4f7deaf74a) (23 maja) — wspólny test + tablica typowanych payloadów zamiast kopiowania testów; dane muszą mieć unikalność i jawny expected result.
- [Data-driven API Testing — Part 2](https://levelup.gitconnected.com/data-driven-api-testing-in-playwright-typescript-part-2-5d6e84f5da45) (2 czerwca) — oddziel JSON data od logiki, zbuduj model i helper/dataprovider; waliduj strukturę oraz response, a plik danych traktuj jako wersjonowany kontrakt.
- [GET API Requests](https://levelup.gitconnected.com/how-to-test-get-api-requests-with-playwright-typescript-7caf7bf790b2) (8 stycznia) — `request.get()` z `params` i status/body assertions jest szybszą warstwą dla endpointu niż browser.
- [Global Setup for API Testing](https://levelup.gitconnected.com/global-setup-in-playwright-typescript-for-api-testing-ce7abe0173cf) (23 lutego) — `globalSetup` może seedować dane raz, lecz przy fixture/retry/observability trzeba rozważyć setup project. W ESM `module/moduleResolution: NodeNext` wpływa na sposób wskazania pliku setup.
- [Verify Response Data](https://levelup.gitconnected.com/how-to-verify-response-data-in-playwright-typescript-4b288ad46616) (10 stycznia) — warstwy assertions: status, top-level shape, required properties, typy i wartości; schema check chroni kontrakt, ale nie zastępuje business invariant.
- [Build a Local AI QA Engineer with Playwright MCP](https://levelup.gitconnected.com/build-your-own-local-ai-qa-engineer-with-docker-ollama-librechat-and-playwright-mcp-1a254fab91d0) (17 lipca) — lokalny agent ogranicza wyciek kodu i koszt, lecz nadal potrzebuje permission boundary, repo conventions i ludzkiego review.
- [Vibe Testing with Playwright MCP](https://levelup.gitconnected.com/vibe-testing-with-playwright-mcp-testing-ux-with-ai-agents-6b1be03b388a) (24 kwietnia) — AI może wspierać usability/visual/accessibility exploration, ale wynik probabilistyczny powinien być oddzielony od deterministycznych assertions.

### Vitaliy Potapov i Vitali Haradkou

- [Angular-aware Playwright Selectors](https://vitalicset.hashnode.dev/angular-aware-playwright-selectors-query-components-by-input-signals-and-state) (2 kwietnia 2026) — canonical Hashnode page opisuje custom `angular=` selector engine, który odczytuje `@Input`, signals i component state przez Angular DevTools. To selektor domenowy dla Angulara, nie uniwersalny zamiennik `getByRole`. DEV jest [mirrorem/aliasem](https://dev.to/vitalicset/testing-angular-components-by-properties-with-playwright-33jj).
- Recheck [Vercel blogu Vitaliego](https://blog-vitaliharadkous-projects.vercel.app/blog) nie znalazł nowego, jednoznacznie autorskiego wpisu Playwright/TypeScript poza pozycjami już zindeksowanymi. Hashnode/DEV aliases nie są liczone ponownie.
- Recheck [Vitaliaya Potapova](https://vitalets.github.io/posts/) potwierdził, że fixtures, workers, fully-parallel i BDD workflow były już opisane w iteracji 6; nie znaleziono nowego artykułu Playwright w tej rundzie.

### Sajith Dilshan / Medium

- [Playwright Auto-Waiting](https://www.linkedin.com/posts/sajith-dilshan_playwright-testautomation-qualityassurance-activity-7489870161777152000-hWVW) (3 sierpnia 2026) to promocja jego canonical [Medium article](https://medium.com/@sajith-dilshan/playwright-auto-waiting-the-secret-behind-stable-and-reliable-test-automation-bd3987a3156e), który był już w indeksie. Recheck potwierdza actionability, web-first assertions i brak hard waits, ale nie dodaje nowej treści.
- [TypeScript Module Systems](https://www.linkedin.com/posts/sajith-dilshan_typescript-module-systems-explained-commonjs-activity-7474649871942307840-HVje) (22 czerwca) promuje już opisany artykuł o CommonJS/ESM i `NodeNext`.
- [Scalable Authentication beyond `globalSetup`](https://www.linkedin.com/posts/sajith-dilshan_playwright-testautomation-qaengineering-activity-7484796747332931584-bgzX) (20 lipca) promuje już opisany artykuł o setup projects + `storageState`; nie dubluję canonical Medium.

### Artem Bondar / Bondar Academy

- [Course refresh announcement](https://www.linkedin.com/posts/artem-bondar_hey-guys-just-fyi-i-recently-refreshed-activity-7490476134573412352-rkQH) (4 sierpnia 2026) — kurs Playwright został zaktualizowany, a sekcja Advanced była jeszcze planowana. To komunikat o stanie materiału, nie nowy artykuł o fixture/config/expect.
- Recheck [Bondar Academy bloga](https://bondaracademy.com/blog) nie znalazł nowego, niezdublowanego wpisu ponad wcześniejsze pozycje o projects, fixture, API, POM, locatorach, assertions, data-driven, storage state i CI.

### Joseph Ward i Yevhen Laichenkov

- [Joseph Ward blog](https://josephward.tech/blog/) nadal pokazuje trzy relewantne wpisy Playwright: `Looking Behind Playwright’s Magic`, `Why Simple UI Tests Become Slow` oraz `Testing the Data, Not Just the Migration` (opisane w iteracji 6). Nie znaleziono nowego wpisu.
- [Yevhen Laichenkov posts](https://elaichenkov.github.io/posts/) nadal mają dwa relewantne artykuły: [17 Playwright Testing Mistakes](https://elaichenkov.github.io/posts/17-playwright-testing-mistakes-you-should-avoid) i [step decorator](https://elaichenkov.github.io/posts/til-playwright-step-decorator). Brak nowego wpisu Playwright/TypeScript.

### Butch Mayhew / Playwright Solutions

- [Do not stop reading agent-written code](https://www.linkedin.com/posts/butchmayhew_softwaretesting-aiinqa-qa-activity-7490115981323935744-S_1o) (3 sierpnia 2026) — AI może bezpiecznie pomagać przy małym, niskiego ryzyka projekcie, ale enterprise software wymaga czytania kodu, review i dowodu.
- [Playwright and AI in QA: Beyond Test Generation](https://www.linkedin.com/posts/butchmayhew_orchestrating-ai-native-testing-with-playwright-activity-7478073686026207234-DD7I) (1 lipca) — wygenerowany test musi pasować do CI, fixture, danych, locator drift, retry i triage; „pass once” nie oznacza „belongs in framework”.
- [Manual testing is still valuable](https://www.linkedin.com/posts/butchmayhew_playwright-testautomation-qaengineering-activity-7473717480301977600-X6nL) (19 czerwca) — nowoczesny QA łączy TypeScript, UI/API, CI, fixture, POM, dane i świadome użycie AI. To post edukacyjny, bez nowego API.

## Synteza siódmej iteracji

1. **Oracle przed generowaniem:** odwróć oczekiwanie, celowo zepsuj produkt i nazwij ryzyko użytkownika. Zielony test bez sprawdzonego faila nie jest coverage.
2. **Warstwy dowodu:** API/contract powinny sprawdzać shape i status, a browser tylko user-visible journey. Snapshoty są dodatkowym alarmem driftu; targeted assertions sprawdzają wartości i invariants.
3. **Fixture i izolacja:** `test.extend`, lazy DI, `use()`/teardown, unikalne dane i osobne BrowserContext ograniczają race’y. `globalSetup` jest decyzją o scope, nie automatycznym zamiennikiem fixture.
4. **Synchronizacja:** `expect(locator)` czeka i retry’uje; `expect.poll` pasuje do eventual state, `expect.toPass` do wąskiego trigger → wait → verify, a `test.step` opisuje intencję. `isVisible()`, `waitForTimeout` i wczesny `if/else` nie powinny udawać synchronizacji.
5. **Konfiguracja jako kontrakt:** `defineConfig`, projects/dependencies, `storageState`, `testIdAttribute`, timezone, `BASE_URL/TEST_ENV`, workers/retries i artefakty muszą być jawne i typowane.
6. **Platforma testowa ma release gate:** przy upgrade’ach sprawdź browser binary, Node/tsconfig, fixture, reporters, trace, retry strategy, screenshots i CI cache; lokalny pin repo (w tym projekcie `@playwright/test` 1.61.0) ma pierwszeństwo przed poradą dla 1.62.x.
7. **Dowód i bezpieczeństwo:** trace/video/HAR/reporty mogą zawierać tokeny, cookies, OTP i PII. Maskowanie, retencja, uprawnienia i testowe konta są częścią jakości testów.
8. **AI z granicami:** agent dostaje repo conventions, jedne drzwi do fixture/POM, kontrakt danych i permission boundary; człowiek zatwierdza risk, oracle, izolację i triage.

## Wpływ na Payment Quality Engineering Lab

- W `tests-pom` przygotowanie merchant/payment-order rób przez BFF/API fixture, a browser zostaw dla journeys i widocznych stanów lifecycle.
- W specach importuj jeden rozszerzony `test` z typowanymi fixture; asercje biznesowe trzymaj w specach, a POM ogranicz do akcji i locatorów.
- Dla `playwright.pom.config.ts` utrzymuj projects, storage states, trace-on-retry, test IDs, timezone i env profile jako jeden jawny kontrakt. Nie kopiuj ustawień 1.62.x bez zgodności z lokalnym pinem 1.61.0.
- Weryfikuj ETag/If-Match, idempotency i status history przez API/contract assertions, a UI testem potwierdzaj tylko efekt użytkownika. Dane mutujące muszą mieć cleanup i unikalność per test/worker.
- Przy flakach najpierw otwórz trace i sklasyfikuj locator/timing/data/env/product; dopiero później zmieniaj timeout lub retry.

## Metoda, kompletność i ograniczenia

- Użyto MCP **Exa Search** do discovery i **Exa Fetch** do weryfikacji canonical URL, dat, autorstwa oraz treści kandydatów.
- „Nowe” oznacza nieobecne w indeksach iteracji 1–6 albo wcześniej tylko zasygnalizowane bez pobranej treści. Mirrory/aliasy są wskazane, lecz nie liczone podwójnie.
- Publiczny LinkedIn, Medium i paginowane archiwa nie gwarantują pełnej historii; auth-wall może ukrywać starsze posty. Nie twierdzę, że indeks jest dowodem kompletności całego Internetu.
- Nie kopiuję pełnych tekstów publikacji; notatka zawiera canonical URL, datę/autora i zwięzłe streszczenie.

## Źródła normatywne

- [Playwright Best Practices](https://playwright.dev/docs/best-practices) — locatory, izolacja i web-first assertions.
- [Fixtures](https://playwright.dev/docs/test-fixtures) — DI, scope, dependencies i teardown.
- [Assertions](https://playwright.dev/docs/test-assertions) — locator assertions, `poll`, `toPass`.
- [Configuration](https://playwright.dev/docs/test-configuration) — runner, projects i `use`.
- [`test.step`](https://playwright.dev/docs/api/class-test#test-step) — raportowalne kroki.
- [Authentication](https://playwright.dev/docs/auth) — `storageState` i izolacja stanu logowania.

W razie konfliktu bloga z dokumentacją lub lokalnym kodem repozytorium pierwszeństwo
ma dokumentacja oficjalna oraz przypięta wersja `@playwright/test`.
