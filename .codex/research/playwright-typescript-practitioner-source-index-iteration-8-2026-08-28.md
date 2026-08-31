# Playwright + TypeScript — ósma iteracja rechecku źródeł (2026-08-28)

## Answer

Ta iteracja użyła MCP **Exa Search** i **Exa Fetch**. Wyniki porównałem z
indeksami iteracji 1–7 oraz z katalogiem postów Michala Drajny. Nowa delta to
przede wszystkim materiały znalezione wcześniej tylko jako URL/metadane albo
opublikowane po ostatnim indeksie: architektura AI Antona Gulina, debuggingowy
Day 13 na ScrollTest, własne reportery Currents oraz trzy materiały TestDino o
skillach, CLI i MCP.

Nie kopiuję pełnych chronionych artykułów. „Treść” w tej notatce oznacza
canonical URL, autorstwo/datę, własne streszczenie i praktyczne wnioski. Reposty,
mirrory, strony dynamiczne i materiały promocyjne są oznaczone jawnie.

## Nowa lub uzupełniona delta

### Anton Gulin

- [AI Test Automation Architecture: The 3-Layer System](https://www.anton.qa/blog/posts/ai-test-automation-architecture-3-layer-system) (13 maja 2026) — produkcyjna automatyzacja AI wymaga trzech warstw: **orchestration** (jakie ryzyko i dane testujemy), **execution** (realny run w CI z kontrolą stanu, cleanupem, retry i izolacją workerów) oraz **evidence** (trace, screenshot, log, video/HAR, storage state). Przed wpuszczeniem testu do suite sprawdź scope, dane, stan przeglądarki, CI, artefakty i możliwość ludzkiego wyjaśnienia failure. Generowanie kodu nie zastępuje decyzji o ryzyku ani oracle.

To rozszerza wcześniejsze zasady Gulina o brakującą bramkę dowodową. W praktyce
agent może szkicować test, ale release gate powinien wymagać sprawdzonego faila,
stabilnego setupu i artefaktu diagnostycznego.

### ScrollTest / Pramod Dutta

- [Day 13: Debugging — Trace Viewer, UI Mode, and Inspector](https://scrolltest.com/21-day-playwright-day-13-debugging-trace-viewer-ui-mode/) (21/22 sierpnia 2026) — strona była już wykryta w iteracji 2, lecz dopiero teraz zweryfikowałem treść przez Fetch. `trace: 'retain-on-failure'` daje timeline, snapshoty DOM, request/response, console i mapowanie do źródła; `npx playwright test --ui` służy do interaktywnego time-travel i filtrowania; `--debug`/Inspector pozwala przejść akcję po akcji. Zalecany triage: trace → failing action → DOM before/after → network → console, zamiast `console.log` i `waitForTimeout`.

### Currents.dev

- [Playwright Custom Reporters: Build Your Own](https://currents.dev/posts/playwright-custom-reporters-build-your-own) (30 czerwca 2026) — reporter ma jawny lifecycle (`onBegin`, test/step begin/end, `onEnd`, `onExit`) i działa w głównym procesie runnera. Przy parallelism/sharding trzeba agregować wyniki bez założenia kolejności, nie gubić asynchronicznych zapisów i kończyć pracę dopiero po `onEnd`. Najpierw ustal właściciela, schemat danych, retencję i zachowanie przy błędzie; mały reporter Slack może stać się nieutrzymywalnym systemem historii flaków. Rozważ rozszerzenie natywnego HTML/blob albo platformę raportową, zanim dodasz bazę, funkcję i dashboard.
- [What does “skipped” mean — test statuses across JavaScript runners](https://currents.dev/posts/test-status-translation-guide) (31 lipca 2026) — dla Playwright rozróżnia status próby (`passed`, `failed`, `timedOut`, `skipped`, `interrupted`) od outcome testu (`expected`, `unexpected`, `skipped`, `flaky`). `test.fail()` sprawia, że „fail” może być expected, a flakiness wynika z porównania wielu attempts. Dashboardy powinny przechowywać attempt-level status i dopiero potem normalizować go między runnerami.
- [Playwright 1.60](https://currents.dev/posts/pw-1.60) — Exa znalazł odnośnik release-note, lecz Fetch zwrócił `CRAWL_NOT_FOUND`; nie traktuję go jako zweryfikowanego źródła. Wersję runnera należy sprawdzać w oficjalnych release notes, nie w tym wpisie.

### TestDino / Pratik Patel

- [Playwright Skill: Train Your AI Agent to Write Better Tests](https://testdino.com/blog/playwright-skill) (13 lutego, aktualizacja 27 lutego 2026) — repozytorium `testdino-hq/playwright-skill` grupuje ponad 70 przewodników w pakiety `core`, `playwright-cli`, `pom`, `ci` i `migration`. Każdy przewodnik ma „when to use”, „avoid when”, quick reference i pełny pattern w TS/JS. To użyteczny format progressive disclosure dla człowieka i agenta: najpierw reguły locatorów, assertions/waiting, fixtures i config, potem CI, POM i migracja. Skill nie zwalnia z review zgodności z lokalnym pinem Playwright.
- [Playwright CLI and MCP: Key Differences and Integration with AI Agents](https://testdino.com/blog/playwright-cli-vs-mcp) (25 lutego, aktualizacja 27 marca 2026) — MCP odsyła bogaty accessibility/DOM context do modelu, natomiast CLI zapisuje snapshoty YAML i refy na dysku, więc agent pobiera tylko potrzebny fragment. Autor raportuje około czterokrotnie mniejsze zużycie tokenów dla CLI w typowym przepływie; traktuję to jako obserwację artykułu, nie gwarancję benchmarku. CLI jest warstwą eksploracji/generowania, `npx playwright test` pozostaje deterministycznym runnerem CI.
- [Playwright MCP Explained: Setup, Config & Real-World Examples](https://testdino.com/blog/playwright-mcp) (19 sierpnia 2026) — nowy, dynamiczny wpis o `@playwright/mcp@latest`, konfiguracji klienta oraz flagach `--headless`, `--browser`, `--caps`, `--isolated`, `--storage-state`, proxy i `--config`. Najważniejsza praktyka: ograniczaj originy/permissions, wersjonuj konfigurację MCP i używaj MCP do eksploracji, a krytyczne ścieżki koduj jako `.spec.ts` z deterministycznymi asercjami.
- [How to install Playwright MCP on Claude Code](https://testdino.com/blog/playwright-mcp-installation/) (11 grudnia 2025) — historyczna instrukcja `claude mcp add playwright npx @playwright/mcp@latest`, restartu klienta i testu połączenia. Zachowuję ją jako starszy materiał instalacyjny; nazwa pakietu i opcje muszą być sprawdzone względem aktualnej dokumentacji Microsoftu.
- [Learn Playwright in 2026: The Complete Roadmap](https://testdino.com/blog/learn-playwright) (20 marca, aktualizacja 24 marca 2026) — roadmapa prowadzi od async/await i struktury `playwright.config.ts`, przez locatory, `expect`, POM, API, CI, sharding i trace, do MCP/agentów. To materiał edukacyjny, nie nowy idiom; potwierdza, że fixtures/config/CI powinny być uczone przed warstwą AI.

### Butch Mayhew

- [600 subscribers — AI in QA Newsletter](https://www.linkedin.com/posts/butchmayhew_softwaretesting-qa-aiinqa-activity-7490440181498560512-uEgt) (4 sierpnia 2026) — post curation/newsletter, bez nowego API Playwright. Potwierdza jedynie rolę regularnego filtrowania sygnału; komentarze Antona Gulina i Vitaliya Potapova są reakcjami, nie ich autorstwa. Nie liczę tego jako niezależnego artykułu technicznego.

## Recheck wszystkich wskazanych autorów i stron

| Źródło | Wynik ósmej iteracji | Co pozostaje w korpusie |
|---|---|---|
| Anton Gulin | znaleziono nową architekturę 3-layer | best practices, POM, codegen, review AI i regression museum z wcześniejszych iteracji |
| Michal Drajna | brak nowszego, jednoznacznie autorskiego wpisu po katalogu z 28 sierpnia | custom fixtures, flake, 10k tests, Lighthouse, Quest i governed model; część to reposty |
| Angela Zelaya | brak autorskiego artykułu Playwright; tylko reposty CLI/UI review | wpisy są sygnałami narzędzi, nie treścią autorską |
| Viktor Konovalov | brak nowej delty po wpisach snapshot/evaluateAll/CDP/a11y/initScript/reporting | pełne streszczenia tych postów są w iteracji 7 |
| Stefan Minchev | brak nowej delty | storageState, TOTP, API seed, `expect` vs `isVisible`, env switching i architecture discussion |
| ScrollTest / Pramod Dutta | Day 13 był wcześniej URL-only; teraz treść zweryfikowana | kontrakty, API+UI, fixtures, trace, retries, contexts, auth, performance, mobile, frames, a11y i upgrade checklist |
| Joseph Ward | brak nowego wpisu Playwright/TS | trzy wpisy o mechanice Playwright, kosztach UI testów i testowaniu danych migracji |
| Vitaliy Potapov (`vitalets.github.io`) | brak nowego artykułu po rechecku archiwum | fixtures, workers, fully-parallel i BDD workflow |
| Vitaliy Haradkou (Vercel blog) | brak jednoznacznie autorskiej nowej treści | wcześniejsze indeksy; nie znaleziono dodatkowego Playwright/TS |
| Artem Bondar / Bondar Academy | brak niezdublowanej strony; wyszukane data-driven, codegen, CI i JSON Schema były już w iteracji 3 | projects, fixtures, API/POM, locators, assertions, data-driven, storage state i CI |
| Sajith Dilshan | LinkedIn promuje już zindeksowane Medium | auto-waiting, auth, tsconfig/ESM, modules, hooks, errors, mocking, MFA, credentials, viewport i migracja |
| Yevhen Laichenkov (`elaichenkov.github.io`) | brak nowego wpisu | 17 mistakes i `test.step` decorator |
| Currents.dev | trzy nowe/uzupełnione wpisy; `pw-1.60` niedostępny przez Fetch | reporters, status/outcome, HTML/blob, CI scale, mocking, headless/headed, AI ecosystem i last-failed |
| TestDino | cztery nowe strony nieobecne w poprzednich indeksach | skill packs, CLI/MCP boundary, MCP config i roadmapa |

„Brak nowej delty” oznacza brak nowej strony wykrytej przez zapytania Exa z
filtrami autora/domeny, a nie dowód, że autor niczego nie opublikował. Publiczny
LinkedIn, Medium i paginowane archiwa mogą ukrywać treści za logowaniem lub
dynamicznym renderingiem.

## Why it matters here

Najbardziej praktyczne połączenie nowej i wcześniejszej wiedzy jest następujące:

1. **Risk → deterministic test → evidence.** Agent/recorder może pomóc w
   szkicu, lecz test musi mieć nazwane ryzyko, kontrolowane dane, oracle,
   sprawdzony negatywny przypadek i trace/report możliwy do odczytania bez
   ponownego uruchamiania.
2. **Fixtures są granicą DI i cleanupu.** `test.extend` ma dostarczać lazy,
   typowane POM/API/data fixtures; test utrzymuje business assertions, a
   fixture `use()`/teardown sprząta stan.
3. **Synchronizacja jest semantyczna.** Preferuj web-first `expect`,
   `expect.poll` dla eventual state, `expect.toPass` dla małego
   trigger→wait→verify oraz nazwane `test.step`. `isVisible()` i stały sleep nie
   są substytutem oczekiwania.
4. **Raport jest częścią kontraktu runnera.** Przy shardach/workerach reporter
   musi zachować attempt, retry, status, artefakty i async flush; `flaky` jest
   outcome, nie pojedynczym statusem.
5. **MCP/CLI to exploration layer.** Ograniczaj kontekst/origin/permissions,
   nie zapisuj sekretów w snapshotach, a wygenerowane flow przenieś do
   wersjonowanych `.spec.ts` z lokalnymi conventions.

## Project impact

- W `apps/frontend/tests-pom` utrzymuj jeden rozszerzony `test` z typowanymi
  fixture; setup merchant/payment-order rób przez BFF/API, a UI zostaw dla
  user-visible lifecycle.
- W `playwright.pom.config.ts` trzymaj jawne `projects`, dependencies,
  `storageState`, `trace: 'on-first-retry'`, `testIdAttribute`, timezone,
  `BASE_URL/TEST_ENV`, workers/retries i reporter. Nie przenoś bez sprawdzenia
  porad dla 1.62.x do lokalnego pina `@playwright/test` 1.61.0.
- ETag/If-Match, idempotency, status history i schema sprawdzaj przez API/
  contract assertions; UI asercją potwierdza efekt widoczny dla użytkownika.
- Artefakty mogą zawierać cookies, tokeny, OTP i PII. Używaj testowych kont,
  maskowania, retencji i uprawnień; `playwright/.auth` nie trafia do VCS.
- Przy flaku najpierw otwórz trace i sklasyfikuj locator/timing/data/env/product,
  dopiero potem zmieniaj timeout, retry albo kod aplikacji.

## Test impact (Playwright / TypeScript)

- **Locatory:** `getByRole`, `getByLabel`, `getByText` i kontraktowe test IDs;
  `locator.evaluateAll()` tylko do jednego, świadomego odczytu kolekcji w
  browser context.
- **Asercje/polling:** `await expect(locator).toHaveText(...)`,
  `await expect.poll(readState).toEqual(...)`; nie mieszaj chwilowego booleana
  z oczekiwaniem na stan.
- **Fixtures/config:** `defineConfig`, projects/dependencies, `storageState`,
  `request` fixture, worker/test scope i cleanup; `globalSetup` tylko gdy jego
  scope jest rzeczywiście globalny.
- **Konstrukcja testu:** Arrange przez API/factory, krótki user journey,
  assertion rezultatu, `test.step` dla intencji i artefakty przy failure. Nie
  umieszczaj dużych workflow ani business oracle w POM.
- **CI/debug:** headless jako default, headed/UI Mode/Inspector lokalnie,
  trace on retry, blob/merge przy shardach, mały smoke gate po upgrade oraz
  klasyfikacja `expected`/`unexpected`/`flaky`.

## Sources

Źródła nowej delty są podlinkowane przy każdej pozycji. Pełny wcześniejszy
korpus i linki do pozostałych artykułów znajdują się w:

- [iteracja 1](playwright-typescript-practitioner-source-index-2026-08-28.md)
- [iteracja 2](playwright-typescript-practitioner-source-index-iteration-2-2026-08-28.md)
- [iteracja 3](playwright-typescript-practitioner-source-index-iteration-3-2026-08-28.md)
- [iteracja 4](playwright-typescript-practitioner-source-index-iteration-4-2026-08-28.md)
- [iteracja 5](playwright-typescript-practitioner-source-index-iteration-5-2026-08-28.md)
- [iteracja 6](playwright-typescript-practitioner-source-index-iteration-6-2026-08-28.md)
- [iteracja 7](playwright-typescript-practitioner-source-index-iteration-7-2026-08-28.md)
- [katalog Michala Drajny](michal-drajna-linkedin-playwright-posts.md)

Źródła normatywne, używane do rozstrzygania konfliktów z blogami:

- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Fixtures](https://playwright.dev/docs/test-fixtures)
- [Assertions](https://playwright.dev/docs/test-assertions)
- [Configuration](https://playwright.dev/docs/test-configuration)
- [`test.step`](https://playwright.dev/docs/api/class-test#test-step)
- [Authentication](https://playwright.dev/docs/auth)

## Uncertainty / follow-up

- Exa Fetch nie pobrał `https://currents.dev/posts/pw-1.60` (`CRAWL_NOT_FOUND`),
  a strona TestDino MCP zwróciła stronę dynamiczną bez pełnego body. Wnioski z
  tych dwóch pozycji są ograniczone do metadanych/snippetów i nie mają statusu
  oficjalnej dokumentacji.
- LinkedIn/Medium i paginowane blogi nie wystawiają gwarantowanego kompletnego
  feedu publicznego. Kolejna iteracja powinna użyć sitemap/RSS/GitHub autora,
  jeśli zostanie wskazany, zamiast powtarzać identyczne zapytania webowe.
- Daty są datami podanymi przez Exa/page metadata; przy rozbieżności widocznej
  na stronie zachowuję datę publikacji i zaznaczam aktualizację.
- W repozytorium zmieniono wyłącznie tę notatkę Markdown; nie modyfikowano
  aplikacji ani istniejących testów.
