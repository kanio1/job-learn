# Playwright + TypeScript — dziewiąta iteracja rechecku źródeł (2026-08-28)

## Answer

Dziewiąty recheck wykonałem przez MCP **Exa Search** i **Exa Fetch**. Wyniki
porównałem ze wszystkimi indeksami iteracji 1–8 oraz katalogiem LinkedIn
Michala Drajny. Nowa delta pochodzi głównie z domeny ScrollTest i TestDino;
część stron znalezionych ponownie to aktualizacje lub mirrory już opisanych
materiałów.

Nie kopiuję pełnych chronionych publikacji. Notatka zachowuje canonical URL,
autora/datę, własne streszczenie oraz wnioski dla Playwright, TypeScript,
fixtures, `playwright.config.ts`, `expect`, polling, `test.step`, CI i
konstrukcji testów. Materiały AI opisuję jako praktyki autora, nie jako normę
frameworka.

## Nowa lub uzupełniona delta

### Anton Gulin

- [Your AI Model Is a Dependency: Pin It, Keep a Fallback, Re-Verify](https://www.anton.qa/blog/posts/your-ai-model-is-a-dependency) (8 lipca 2026) — model używany przez agenta traktuj tak samo jak wersję browsera, Node albo bazy: identyfikator powinien być przypięty w jednym, typowanym module konfiguracji, fallback musi być zweryfikowany na prawdziwej suite, a parity suite powinna cyklicznie uruchamiać oba modele. Oracle ma pozostać niezależny od modelu (np. wynik z seedowanych danych), a test nie może polegać na odpowiedzi „samego AI”.
- [Porting Anthropic's Skill Creator from Python to TypeScript](https://www.anton.qa/blog/posts/porting-anthropic-s-skill-creator-from-python-to-typescript) (16 kwietnia 2026) — materiał sąsiadujący z Playwright, ale przydatny dla idiomatycznego TS: oddziel wiedzę workflow (`SKILL.md`) od wykonywalnych narzędzi, waliduj strukturę wejścia, stosuj eval→improve→benchmark, zachowuj baseline i trzymaj artefakty robocze poza repo. Nie jest to bezpośrednia rada o runnerze Playwright.

### ScrollTest / Promode

- [AI Test Failure Triage for Playwright Teams](https://scrolltest.com/ai-test-failure-triage-playwright/) (9 sierpnia 2026, Day 62) — zanim agent zacznie generować setki testów, wykorzystaj artefakty istniejącej suite do klasyfikacji failure’ów. Autor proponuje cztery kontrolowane kategorie: `product_bug`, `test_bug`, `environment_issue`, `data_issue`. Z trace/screenshotu/video/logów/network/retry zbuduj mały JSON packet (test, project, browser, retry, kroki, requesty, commit, historia), zamiast wysyłać cały trace ZIP do modelu. Model ma zwrócić etykietę, confidence, evidence, ownera i `needsHumanReview`; nie podejmuje decyzji release.
- [Playwright PromptFoo Starter Suite for QA Teams](https://scrolltest.com/playwright-promptfoo-starter-suite/) (4 sierpnia 2026, Day 57) — rozdziel odpowiedzialności: Playwright sprawdza login, nawigację, API, selektory, widoczny rezultat i artefakty; PromptFoo/ewaluator sprawdza jakość odpowiedzi AI, factuality, rubric i red-team cases; CI łączy oba wyniki w politykę release. Mały, pinowany zestaw (kilka UI/API flows i reprezentatywne prompt cases) jest lepszy niż 300‑liniowy test browserowy mieszający UI z oceną języka.
- [MCP 2.0 Breaking Changes Every QA Engineer Must Know](https://scrolltest.com/mcp-2-0-breaking-changes-qa-guide/) (19 sierpnia 2026) — zmiany protokołu/SDK MCP wymagają pinowania wersji klienta i serwera, testów kompatybilności oraz migracji bez big-bang release. Dla harnessu QA ważne są jawne `protocol_version`, capabilities i możliwość in-process testu; nie zakładaj, że wersja pakietu Python, TypeScript SDK i serwera ma ten sam numer. To materiał o narzędziu AI, nie zamiennik dokumentacji Playwright.
- [DeepEval vs Ragas: What QA Engineers Should Learn](https://scrolltest.com/deepeval-vs-ragas-ai-qa-day-63/) (11 sierpnia 2026, Day 63) — ewaluacja odpowiedzi modelu i jakość retrievalu to inne ryzyka. DeepEval/Ragas mogą stanowić osobną warstwę quality gate, a Playwright/API nadal sprawdzają, czy użytkownik przejdzie uwierzytelniony workflow, otrzyma właściwy rendering i poprawny kontrakt. Nie wkładaj rubric dla LLM do deterministycznego testu UI.

### Currents.dev

- [Playwright 1.60.0 Release Updates](https://currents.dev/posts/pw-1.60.0) (11 maja 2026, aktualizacje do 19 maja) — aktualizacja runnera 1.60 wymagała nowej głównej wersji reportera Currents (`@currents/playwright` 2.0.0), a użytkownicy orchestration musieli zmienić komendę CI. Wniosek: upgrade Playwright to test kompatybilności całego toolchainu (reporter/orchestrator/config), nie tylko `package.json`; sprawdzaj release notes i uruchamiaj mały gate przed pełną suite.

### TestDino

- [Playwright AI Ecosystem 2026: MCP, Agents & Self-Healing Tests](https://testdino.com/blog/playwright-ai-ecosystem) (13 marca, aktualizacja 30 czerwca 2026) — autor porządkuje ekosystem w warstwy protocol (MCP), agents (Planner/Generator/Healer), authoring (Codegen/CLI/IDE) i tooling. Użyteczna jest sama separacja planowania, generowania i utrzymania; twierdzenia o „self-healing” i stabilności należy traktować eksperymentalnie oraz wymagać ludzkiego review oracle/locatorów.
- [Playwright Test Agents: Planner, Generator and Healer Guide](https://testdino.com/blog/playwright-test-agents) (28 lutego, aktualizacja 7 kwietnia 2026) — Planner tworzy plan w Markdown, Generator `.spec.ts`, a Healer reaguje na broken locators. Przepływ plan→code→repair powinien korzystać z istniejących fixtures, conventions, danych i trace; agent nie zastępuje testu mutacji, asercji ani izolacji. Źródło podaje wsparcie od v1.56+, więc dokładną dostępność trzeba sprawdzać w przypiętej wersji.
- [Playwright Screencast: Record Tests](https://testdino.com/blog/playwright-screencast) (14 kwietnia, aktualizacja 7 maja 2026) — video jest dodatkowym dowodem, gdy screenshot końcowy nie wyjaśnia przejścia do failure. `video: 'retain-on-failure'` ogranicza koszt retencji; programmatic `page.screencast` (według artykułu od v1.59) daje kontrolę start/stop. Trace zwykle pozostaje pierwszym artefaktem, a video powinno być włączane tam, gdzie timing wizualny ma znaczenie.
- [Playwright Locators Guide](https://testdino.com/blog/playwright-locators) (10 marca, aktualizacja 27 maja 2026) — locator jest leniwy, sprawdzany przy użyciu, auto-waituje i retry’uje. Preferuj `getByRole`, `getByLabel`, `getByText` oraz świadomie dobrany test ID; nie poprzedzaj akcji `waitForSelector`. Część twierdzeń o nowych API i usuniętych selector engines jest wersjozależna — przed migracją sprawdź oficjalny changelog dla pina repo.
- [GitHub Copilot with Playwright: Setup, MCP & Test Guide](https://testdino.com/blog/playwright-tests-with-copilot) (26 marca, aktualizacja 30 czerwca 2026) — live MCP pomaga agentowi obserwować prawdziwy accessibility tree, Skills dostarczają reguły, a `.github/copilot-instructions.md` utrwala konwencje repo. MCP/agent służy do eksploracji i debugowania; duże, krytyczne regresje powinny być zwykłymi, deterministycznymi `.spec.ts` uruchamianymi w CI.
- [What is the Accessibility Tree?](https://testdino.com/blog/accessibility-tree) (strona dynamiczna; Exa potwierdził tytuł i temat, Fetch nie zwrócił pełnego body) — źródło wyjaśnia różnicę DOM/accessibility tree i rolę locatorów semantycznych. W tej rundzie zapisuję metadata-only, bez dopisywania niezweryfikowanych szczegółów.

### Artem Bondar / Bondar Academy

- [Is Playwright MCP Worth It for Test Automation?](https://bondaracademy.com/blog/is-playwright-mcp-worth-it) (6 kwietnia 2026) — eksperyment z Copilot + MCP pokazuje, że generowanie CRUD flow wymagało wielu ręcznych poprawek: zły typ elementu, niejednoznaczny locator, kruche regex assertions i błędna nawigacja po usunięciu danych. Wniosek jest anty-hype: MCP może przyspieszyć szkic, ale test trzeba uruchomić, zawęzić locatory, sprawdzić oracle i utrzymywać jak każdy kod.

## Recheck wszystkich wskazanych autorów i stron

| Źródło | Wynik dziewiątej iteracji | Status w korpusie |
|---|---|---|
| Anton Gulin | dwie wcześniej pominięte strony: model jako dependency i portowanie skill creatora do TS | pozostałe best practices, POM, codegen, v1.60 evidence, review AI i regression museum były już opisane |
| Michal Drajna | brak nowego posta po katalogu z 28 sierpnia | custom fixtures, flake, 10k tests, Lighthouse, Quest i governed model; część to reposty |
| Angela Zelaya | brak autorskiego artykułu Playwright | reposty CLI/UI review, bez nowej treści technicznej |
| Viktor Konovalov | brak nowej delty | snapshot API, `evaluateAll`, CDPSession, accessibility tree, `addInitScript`, reporting i secrets są w iteracji 7 |
| Stefan Minchev | brak nowej delty | storageState, TOTP, API seed, `expect` vs `isVisible`, env switching i architecture discussion są w iteracji 7 |
| ScrollTest / Pramod Dutta | cztery nowe strony: failure triage, PromptFoo, MCP 2.0, DeepEval/Ragas | pozostałe serie Day 1–58, kontrakty, fixtures, polling, auth, CI i upgrade checklist były wcześniej indeksowane |
| Joseph Ward | brak nowego wpisu | mechanika Playwright, wolne UI tests i testowanie danych migracji |
| Vitaliy Potapov | brak nowej strony; fixture timeline ponownie potwierdzony | fixtures lazy/auto, scope worker/test, dependencies, overrides i worker restarts |
| Vitaliy Haradkou (Vercel) | brak jednoznacznie nowego Playwright/TS | wcześniejsze pozycje z Vercel/Hashnode/DEV pozostają jedynymi potwierdzonymi |
| Artem Bondar / Bondar Academy | jeden niezdublowany wpis o MCP; pozostałe wyniki były już w korpusie | projects, fixtures, API/POM, locators, `expect`, data-driven, storage state, CI i JSON Schema |
| Sajith Dilshan | brak nowego canonical Medium; LinkedIn to promocje | auto-waiting, auth, tsconfig/ESM, modules, hooks, errors, mocking, MFA, credentials, viewport i migracja |
| Yevhen Laichenkov | brak nowego wpisu Playwright | 17 mistakes i `test.step` decorator |
| Butch Mayhew | brak nowego materiału technicznego; newsletter post jest curation | review kodu AI, agentic QA, manual testing i newsletter są oznaczone jako kontekst |
| Currents.dev | canonical release note 1.60.0 wcześniej nieobecny; custom reporter/status są z iteracji 8 | compatibility reporter/runner, CI scale, API, HTML/blob, mocking, headless/headed i AI ecosystem |
| TestDino | sześć nowych/uzupełnionych stron | skill packs, CLI/MCP, UI Mode, trace, flaky CI, architecture, release pages i best practices są już w poprzednich indeksach |
| Level Up Coding | brak nowego niezdublowanego artykułu | custom fixtures, Browser/Context/Page, data-driven API, global setup, response assertions i MCP |

„Brak nowej delty” oznacza brak nowego wyniku w zapytaniach Exa dla publicznie
indeksowanych stron, nie dowód kompletności całego Internetu. LinkedIn, Medium,
Vercel i blogi z dynamicznym renderingiem mogą ukrywać starsze albo niezaindeksowane
posty.

## Why it matters here

1. **Najpierw risk i oracle, potem agent.** Test powinien przejść przez
   planowanie ryzyka, deterministyczny expected result, celowe zepsucie
   zachowania i review artefaktów. „AI wygenerował plik” ani „przeszedł raz” nie
   jest dowodem coverage.
2. **Polling opisuje eventual consistency.** Dla locatora używaj web-first
   `expect`; dla odczytu statusu użyj `expect.poll`; dla wąskiego
   trigger→reload→verify użyj `expect.toPass({ timeout, intervals })`. Nie
   zamieniaj tego na pętle z `waitForTimeout`.
3. **Fixture ma scope i koszt.** `test.extend` jest lazy; test/worker scope,
   dependencies, override i teardown muszą być jawne. `globalSetup` pasuje do
   prawdziwie globalnego loginu/seeda, fixture do izolowanych danych per test.
4. **Raport i status są częścią kontraktu.** Reporter musi zachować attempts,
   retry, kroki, artefakty i async flush w workerach/shardach. `failed` próby i
   `flaky` outcome nie są tym samym; nie normalizuj ich bez utraty informacji.
5. **MCP/CLI jest warstwą eksploracji.** Kontroluj originy, permissions,
   storage state i wersje. Wygenerowany flow przenieś do lokalnego POM/fixture i
   deterministycznych speców, zanim stanie się gate’em CI.

## Project impact

- W `apps/frontend/tests-pom` przygotowanie merchant/payment-order rób przez
  BFF/API fixture lub factory; browser test nie powinien tworzyć całego stanu
  przez UI.
- W `playwright.pom.config.ts` trzymaj jeden jawny kontrakt: `defineConfig`,
  projects/dependencies, `storageState`, `trace: 'on-first-retry'`, screenshot
  na failure, test IDs, timezone, `BASE_URL/TEST_ENV`, workers/retries i
  reporter. Lokalne repo przypina `@playwright/test` 1.61.0 — nie kopiuj porad
  dla 1.62.x bez celowego upgrade’u.
- POM ma wystawiać akcje i locatory; business assertions zostają w specach.
  Każdy `test.step` powinien wyjaśniać intencję użytkownika, nie maskować
  kilkuset linii workflow.
- ETag/If-Match, idempotency, status history i schema sprawdzaj API/contract
  assertions; UI niech potwierdza tylko user-visible rezultat.
- Trace/video/HAR/report mogą zawierać cookies, tokeny, OTP i PII. Używaj
  testowych kont, maskowania, retencji i uprawnień; `playwright/.auth` nie może
  trafić do VCS.

## Test impact (Playwright / TypeScript)

- **Idiomy TS:** `as const`, `satisfies`, jawne modele danych, `strict`, jeden
  punkt konfiguracji i brak `any`/ślepych castów. Dla kodu pluginów rozdziel
  typowane input/output od orkiestracji.
- **Locatory:** user-facing `getByRole`/`getByLabel`/`getByText`; test ID jako
  kontrakt, CSS/XPath dopiero jako świadomy fallback. Nie pre-waituj akcji,
  które już mają actionability checks.
- **Asercje:** `await expect(locator).toHaveText(...)` zamiast
  `textContent()` + `toBe`; `expect.poll` dla API/eventual state; `toPass` z
  krótkim timeoutem wewnętrznych asercji; `test.fail()` i expected status
  dokumentują znany, kontrolowany failure.
- **Dane i izolacja:** unikalność per test/worker, API seed, cleanup w fixture,
  osobny `BrowserContext` dla wielu ról i brak zależności przez
  `test.describe.serial`.
- **CI/debug:** headless jako default; headed/UI Mode/Inspector lokalnie;
  trace on retry, video tylko gdy pomaga, blob/merge przy shardach, mały
  release-smoke gate po zmianie wersji.

## Sources

Nowe źródła są podlinkowane przy pozycjach powyżej. Pełny wcześniejszy korpus
znajduje się w:

- [iteracja 1](playwright-typescript-practitioner-source-index-2026-08-28.md)
- [iteracja 2](playwright-typescript-practitioner-source-index-iteration-2-2026-08-28.md)
- [iteracja 3](playwright-typescript-practitioner-source-index-iteration-3-2026-08-28.md)
- [iteracja 4](playwright-typescript-practitioner-source-index-iteration-4-2026-08-28.md)
- [iteracja 5](playwright-typescript-practitioner-source-index-iteration-5-2026-08-28.md)
- [iteracja 6](playwright-typescript-practitioner-source-index-iteration-6-2026-08-28.md)
- [iteracja 7](playwright-typescript-practitioner-source-index-iteration-7-2026-08-28.md)
- [iteracja 8](playwright-typescript-practitioner-source-index-iteration-8-2026-08-28.md)
- [katalog Michala Drajny](michal-drajna-linkedin-playwright-posts.md)

Źródła normatywne, które rozstrzygają konflikty z blogami:

- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Fixtures](https://playwright.dev/docs/test-fixtures)
- [Assertions](https://playwright.dev/docs/test-assertions)
- [Configuration](https://playwright.dev/docs/test-configuration)
- [`test.step`](https://playwright.dev/docs/api/class-test#test-step)
- [Authentication](https://playwright.dev/docs/auth)

## Uncertainty / follow-up

- Exa Fetch nie pobrał pełnego body TestDino Accessibility Tree, a dla części
  dynamicznych stron zwrócił tylko shell/snippet. Wnioski z takich pozycji są
  ograniczone do metadata-only.
- Wpisy o MCP, agentach, screencast API i wersjach Playwright są wrażliwe na
  datę. Przed zastosowaniem sprawdź oficjalne release notes oraz lokalny pin
  `@playwright/test`.
- Publiczne archiwa LinkedIn/Medium i paginowane blogi nie gwarantują pełnego
  feedu; kolejny recheck powinien użyć sitemap/RSS/GitHub autora, jeśli takie
  źródło zostanie wskazane.
- Nie zmieniono kodu aplikacji ani istniejących testów; ta iteracja dodała
  wyłącznie notatkę Markdown.
