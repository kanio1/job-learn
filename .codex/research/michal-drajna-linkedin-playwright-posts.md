# Michał Drajna — posty i artykuły na LinkedIn o Playwright (2024-08 → 2026-08)

> Zebrano: 2026-08-28. Źródła: profil publiczny `linkedin.com/in/michaldrajna-qa` (web_fetch), artykuły LinkedIn Pulse (web_fetch), wyniki wyszukiwania Firecrawl API (`/v2/search`) oraz publiczne strony pojedynczych postów.
>
> Uwaga techniczna: Firecrawl scrape nie obsługuje `linkedin.com` („we do not support this site"), więc pełne treści pobrano przez `web_fetch` na publicznych URL-ach (działają bez logowania), a wyszukiwarka Firecrawl posłużyła do odkrycia listy postów i krótkich opisów. Posty udostępnione (reposts) zawierają komentarz Drajny + link do artykułu źródłowego.
>
> Zasięg: ostatnie 2 lata licząc od 2026-08-28, czyli 2024-08-28 → 2026-08-28. Posty/artykuły spoza zakresu oznaczono na końcu sekcji.

Profil: https://www.linkedin.com/in/michaldrajna-qa/ — Senior QA Engineer (Bloomreach), „Playwright Enthusiast 🎭", 10,9k followers, 407 postów, 12 artykułów.

---

## 1. Artykuły (LinkedIn Pulse / Articles) — pełne treści

### 1.1 Playwright v1.62.0 🎭 — 2026-07-26
- URL: https://www.linkedin.com/pulse/playwright-v1620-michal-drajna-md9ff
- Reakcje: 126 · komentarze: 2

Treść: Playwright v1.62.0 live! Po WebAuthn w v1.61 przychodzi przebudowany Component Testing, natywne WebP, wbudowany MCP server i większa kontrola nad async/retries.

1. **Next-Gen Component Testing (Stories & Galleries)** — architektura wokół stories i galerii. Stories izolują komponenty z mockowanymi props/state/providers; `mount('components/Expandable/Stateful')` wraca typowany Locator; `component.update(newProps)` / `component.unmount()` w teście.
2. **Cancel Operations with AbortSignal** — `AbortSignal` w akcjach/nawigacjach/oczekiwaniach/asercjach, np. `page.getByRole('button').click({ signal })`.
3. **Native WebP Visual Testing** — `toHaveScreenshot()`/`page.screenshot()` wspierają `.webp` (lossless golden, lossy debug z `quality`).
4. **Isolated Retries (`retryStrategy: 'isolated'`)** — retry pojedynczo na końcu suity w jednym workerze.
5. **Bundled Playwright MCP Server & CLI** — `npx playwright mcp`, `npx playwright cli`.

Inne: passkeys w storage state, `scroll: "none"`, `apiResponse.timing()`, `reporter.preprocess()`, koniec Debian 11.

### 1.2 Playwright v1.61.0 — 2026-06-15
- URL: https://www.linkedin.com/pulse/playwright-v1610-michal-drajna-dsief
- Reakcje: 213 · komentarze: 2

1. **Native WebAuthn Passkeys (`browserContext.credentials`)** — wirtualny authenticator: `context.credentials.create('example.com', {...})` + `install()`, automatycznie odpowiada na `navigator.credentials.create()/get()`.
2. **Direct Web Storage Access** — `page.localStorage.setItem(...)`, `page.sessionStorage.items()` zamiast `page.evaluate`.
3. **Smarter Video Management** — tryby `retain-on-first-failure`, `retain-on-failure-and-retries`.
4. **WebSocket Tracing & HAR Logs** — WebSocket w HAR i Traces.
5. Inne: Ubuntu 26.04, `expect.soft.poll()`, `-G` skrót dla `--grep-invert`, `apiResponse.securityDetails()`, `apiResponse.serverAddr()`.

### 1.3 🎭 Playwright v1.60.0 just dropped — here's what's hot! 🚀 — 2026-05-12
- URL: https://www.linkedin.com/pulse/playwright-v1600-just-dropped-heres-whats-hot-michal-drajna-7ohie
- Reakcje: 199 · komentarze: 2

1. **HAR Recording in Tracing** — `context.tracing.startHar({ path, content: 'embed', mode: 'minimal', urlFilter })` + `await using` (auto-cleanup).
2. **New `drop()` API** — `page.locator('#dropzone').drop({ files: [...] })` / `drop({ data: { 'text/plain': ... } })`, prawdziwe DOM events.
3. **Aria Snapshots AI-Ready** — `toMatchAriaSnapshot({ boxes: true })` dodaje bounding boxy `[box=120,340,80,32]`.
4. **`test.abort()`** — czyste przerwanie testu z fixtures/hooks.
5. Bonusy: `browser.on('context')`, mirror lifecycle events, `toHaveCSS()` pseudo (`::before/::after`), `getByRole()` description, `locator.highlight({ style })`.
6. Breaking: usunięto `Locator.ariaRef()`, opcje `handle`/`logger`, `videosPath`/`videoSize`.
7. Bundled: Chromium 148.0.7778.96, Firefox 150.0.2, WebKit 26.4.

### 1.4 🚀 Playwright 1.59: Why CLI trace analysis for agents is an absolute game-changer! — 2026-04-07
- URL: https://www.linkedin.com/pulse/playwright-159-why-cli-trace-analysis-agents-absolute-michal-drajna-hyuwf
- Reakcje: 183 · komentarze: 18

Playwright 1.59 = CLI-native debugging i analiza trace'ów dla AI agentów. Od „Copy as Prompt" do autonomicznego śledztwa: agent (np. Claude Code) konsumuje cały kontekst trace'a przez CLI — nawiguje po krokach, przegląda HTML w momencie faila, sieć, logi, snapshoty before/after bez otwierania przeglądarki.

Nowe komendy: `npx playwright test --debug=cli` (agent podłącza się do działającego testu) oraz `npx playwright trace open` (grep po akcjach, badanie stanu strony w danym momencie). Wniosek: redukcja MTTR, diagnoza flaków w nocy, self-healing lokatorów przez PR agenta.

### 1.5 Playwright Release v1.59.0 🎭 — 2026-04-01
- URL: https://www.linkedin.com/pulse/playwright-release-v1590-michal-drajna-4ptyf
- Reakcje: 500 · komentarze: 9

1. **Screencast API** — `page.screencast.start({ path })` / `stop()`, akcje z adnotacjami, rozdziały, overlaye HTML, streaming klatek JPEG do modeli vision.
2. **Agentic Video Receipts** — agenci nagrywają „dowód pracy" (walkthrough z rozdziałami) do szybkiego review przez człowieka.
3. **Browser Interoperability (`browser.bind()`)** — jedna przeglądarka współdzielona przez playwright-cli, @playwright/mcp i klientów.
4. **Observability Dashboard** — `playwright-cli show`, `PLAYWRIGHT_DASHBOARD=1`.
5. **CLI Debugging & Trace Analysis dla agentów** — `--debug=cli`, `trace open`.
6. QoL: `await using`, `page.ariaSnapshot()`, `locator.normalize()`, `page.pickLocator()`, `setStorageState()` reset, trace mode `retain-on-failure-and-retries`, UI Mode (tylko zmienione testy).
7. Browsery: Chromium 147.0.7727.15, Firefox 148.0.2, WebKit 26.4.

### 1.6 Playwright + AI: Revolúcia v testovaní alebo len drahý pomocník? — 2026-03-30 (słowacki)
- URL: https://www.linkedin.com/pulse/playwright-ai-revol%C3%BAcia-v-testovan%C3%AD-alebo-len-drah%C3%BD-pomocn%C3%ADk-drajna-xnkhf
- Reakcje: 44 · komentarze: 1

1. AI jako „power-user" asystent, nie zamiennik — boilerplate i selektory OK, ale nie bez zrozumienia.
2. Narzędzia: Cursor & Copilot (top), ZeroStep (natural language `ai('click on the login button')` — koszt i zależność), natywne AI w Playwright.
3. Największy strach: stabilność i halucynacje self-healing testów — walidacja musi zostać u człowieka.
4. Bezpieczeństwo: nie karmić publicznych modeli firmowym kodem; lokalne LLM wymagają mocy.
5. Wniosek: AI to „brutálny urýchľovač" (generowanie danych/scenariuszy/debug), nie srebrna kula dla strategii testowania.

---

## 2. Posty (activity) — pełne treści

### 2.1 How to Create Custom Fixtures in Playwright TypeScript — 2026-08-28 (36 min temu)
- URL: https://www.linkedin.com/posts/michaldrajna-qa_how-to-create-custom-fixtures-in-playwright-activity-7499020460131766273-l642
- Źródło: levelup.gitconnected.com (Level Up Coding)

„Why custom fixtures make Playwright fundamentally superior to legacy test runners. 🛠️ Coming from frameworks where global hooks like beforeEach control state, Playwright's dependency-injected fixture model can feel like magic. But once you lock down the design pattern, you'll never go back. 🎭 Check out this comprehensive guide on Level Up Coding to see how custom TypeScript fixtures eliminate global state, prevent race conditions in parallel CI runs, and keep your test suite lightning-fast."

Hashtagi: #Playwright #TypeScript #SoftwareTesting #SDET #CleanCode #TestArchitecture #QAAutomation #LevelUpCoding #DevOps

### 2.2 Why Your Playwright Tests Are Lying to You — 2026-08-27 (1d)
- URL: https://www.linkedin.com/posts/michaldrajna-qa_why-your-playwright-tests-are-lying-to-you-activity-7498658155678244865-tiNA
- Źródło: medium.com/@ARaffaeSQA — „A Practical Guide to Killing Flakiness for Good"
- Reakcje: 14 · komentarze: 1

„Playwright auto-waits. So why are your tests still flaking? 📊 Switching to Playwright eliminates many timing issues, but modern app complexity creates a whole new class of flakiness. 🎭 If your suite passes in isolation but breaks under parallel CI execution, the issue usually isn't the framework. It's environment drift, optimistic UI renders, and shared data dependencies."

Hashtagi: #Playwright #SoftwareTesting #SDET #QAAutomation #TestArchitecture #DevOps #CICD #FlakyTests

### 2.3 What if learning Playwright felt like playing a video game? — 2026-08-26 (2d)
- URL: https://www.linkedin.com/posts/michaldrajna-qa_what-if-learning-playwright-felt-like-playing-activity-7498295694269730817-WAL8
- Reakcje: 115 · komentarze: 8

„What if learning Playwright felt like playing a video game? 🎮 Reading dry documentation and sifting through GitHub gists can make learning a new testing framework feel like a chore. 🎭 Enter Playwright Quest — an interactive online game that guides you through modern E2E automation step-by-step. Instead of passive watching, you solve real-world coding puzzles, tackle tricky selectors, and level up your test automation skills directly in your browser."

### 2.4 Lighthouse Performance Audits in Playwright — 2026-08-25 (3d)
- URL: https://www.linkedin.com/posts/michaldrajna-qa_lighthouse-performance-audits-in-playwright-activity-7497929859579514880-uw70
- Źródło: scrolltest.com/playwright-lighthouse-performance-audits
- Reakcje: 24

„Automating Lighthouse Audits with Playwright. 🎭 Want to automatically audit Core Web Vitals, Accessibility, and SEO on every deployment? Scrolltest published a complete guide on pairing Playwright with Lighthouse for automated non-functional testing."

Hashtagi: #Playwright #Lighthouse #CoreWebVitals #SoftwareTesting #SDET #PerformanceTesting #Accessibility #WebDev #Scrolltest

### 2.5 Playwright MCP Gives an AI Agent a Browser. I Gave Mine a Governed Model of the Application. — 2026-08 (3w)
- URL: https://www.linkedin.com/posts/michaldrajna-qa_playwright-mcp-gives-an-ai-agent-a-browser-activity-7490356957200031744-CmT9
- Autor źródła: Daniel Dahlin (medium.com/@daniel.dahlin)
- Reakcje: 21 · komentarze: 1

„Beyond the MCP Hype: How to build AI test generation that actually scales. 📈 The Model Context Protocol gives AI agents direct access to browser automation frameworks like Playwright. 🎭 But without strict boundaries, agents quickly produce duplicate Page Objects, fragile selectors, and unmaintainable test logic. Daniel Dahlin presents a blueprint for shifting from raw agentic crawling to Model-Governed Test Generation. Discover how encoding application domain rules before the agent writes code preserves code quality, prevents drift, and makes agent output truly production-ready."

Hashtagi: #Playwright #ModelContextProtocol #AIAgents #SoftwareTesting #SDET #TestArchitecture #AgenticQA #SoftwareEngineering

### 2.6 Getting Started with Claude Code and Playwright CLI: A Step-by-Step Guide — 2026-07 (1mo)
- URL: https://www.linkedin.com/posts/michaldrajna-qa_getting-started-with-claude-code-and-playwright-activity-7488141317873651712-X7Ac
- Źródło: dev.to/aswani25
- Reakcje: 34

„Stop burning millions of tokens streaming raw HTML into your AI agent context. 💸 When using terminal-based AI agents to drive browser automation, sending massive DOM trees on every single step destroys your context window and spikes API costs. 🎭 In his latest step-by-step guide, Aswani Kumar breaks down how to pair Claude Code with Microsoft's Playwright CLI. By storing DOM state locally as YAML snapshots and using reference-based interactions, you keep your agent sessions cheap, fast, and remarkably long-running."

Hashtagi: #Playwright #ClaudeCode #AIAgents #SoftwareTesting #SDET #BrowserAutomation #DevOps #QAAutomation

### 2.7 Book review: „Build an End-to-End Testing Suite with Playwright" — 2026-07 (1mo)
- URL: https://www.linkedin.com/posts/michaldrajna-qa_playwright-testautomation-qualityassurance-activity-7487429785019682817-Tvzb
- Autorzy książki: Butch Mayhew i Debbie O'Brien
- Reakcje: 75 · komentarze: 3

Recenzja egzemplarza przedpremierowego. Kluczowe kategorie: **The Prompt > Review > Run Workflow** (bezpieczne włączanie AI w tworzenie testów), **Building a Stable Environment** (przewidywalne dane testowe, świadome zarządzanie autoryzacją), **Evidence-Driven Debugging** (HTML reports + Trace Viewer zamiast zgadywania). Poleca QA/SDET/devom; przedpremierowo na Amazon.

Hashtagi: #Playwright #TestAutomation #QualityAssurance #AI #SoftwareTesting #TechBooks

### 2.8 Playwright v1.62.0 (post) — 2026-07 (1mo)
- URL: https://www.linkedin.com/posts/michaldrajna-qa_playwright-v1620-activity-7487065429266509824-igX4
- Reakcje: 126 · komentarze: 2

„Playwright v1.62.0 is live! 🎭" (post z linkiem do artykułu, patrz 1.1)

### 2.9 Playwright vs. Vibium: The last 6 months — 2026-06/07 (1mo)
- URL: https://www.linkedin.com/posts/michaldrajna-qa_playwright-and-vibium-the-last-6-months-activity-7482716770005602305-6joG
- Źródło: beththetester.com/2026/06/26/playwright-and-vibium-the-last-6-months (Beth Marshall)
- Reakcje: 30

„Playwright vs. Vibium: 50+ Releases in 180 Days. ⚡ Trying to build a future-proof automation stack right now feels like shooting at a moving target. Thankfuly, Beth Marshall did the heavy lifting of auditing the latest technical arcs. Whether you need the hyper-fast, deterministic test execution runner of Playwright or the intent-based, agentic backend flexibility of Vibium, this article cuts through the noise to show you exactly how both tools have evolved to anchor the modern DevOps pipeline."

Hashtagi: #Playwright #Vibium #SoftwareTesting #AgenticQA #MCP #ModelContextProtocol #BrowserAutomation #SDET #DevOps

### 2.10 Release v1.60.0 · microsoft/playwright — 2026-05 (3mo)
- URL: https://www.linkedin.com/posts/michaldrajna-qa_release-v1600-microsoftplaywright-activity-7459686607286312960-vh4m
- Reakcje: 160 · komentarze: 4

„Playwright v1.60.0 is here! 🎭 While v1.59 was the 'Agent' release, v1.60 focuses on Precision & Network Stability. If you are dealing with complex file uploads, deep network analysis, or AI-driven testing, this update is for you. 🚀
High-Impact Highlights: Native HAR Recording (tracing.startHar() + await using), True Drag-and-Drop (locator.drop()), Spatial AI Snapshots (ariaSnapshot boxes option), Smart Test Aborts (test.abort()).
DevEx & Reporting: Zipped Reports (playwright show-report na .zip), Trace Viewer pretty-print dla JSON/Form, CSS Pseudo-elements (toHaveCSS() pseudo).
⚠️ Breaking Changes: usunięto Locator.ariaRef() i videosPath."

Hashtagi: #Playwright #TestAutomation #WebDev #QA #SoftwareEngineering #AI #NetworkTesting

### 2.11 Why We Chose Playwright Over Cypress — 2026-04 (4mo)
- URL: https://www.linkedin.com/posts/michaldrajna-qa_why-we-chose-playwright-over-cypress-qa-activity-7445368215813304320-Saak
- Źródło: qawolf.com/blog/why-qa-wolf-chose-playwright-over-cypress (QA Wolf)
- Reakcje: 59 · komentarze: 2 (jeden krytyczny: „Sounds like another AI slop slogan")

„Why the world's biggest QA-as-a-Service provider bet everything on Playwright. 🎭 When you're responsible for the test suites of hundreds of companies, 'good enough' doesn't cut it. QA Wolf breaks down exactly why they migrated from Cypress to Playwright — citing native parallelism, multi-tab support, and superior speed. ⚡ If you're still wrestling with Cypress workarounds, this is the sign you've been looking for."

Hashtagi: #Playwright #Cypress #SoftwareTesting #QA #TestAutomation #WebDev #SoftwareEngineering #QAWolf

---

## 3. Posty znalezione przez wyszukiwarkę Firecrawl (tytuł + opis, bez pełnej treści)

### 3.1 Release v1.56.0 · microsoft/playwright
- URL: https://www.linkedin.com/posts/michaldrajna-qa_release-v1560-microsoftplaywright-activity-7381008999925837824-2wFU
- Opis: „Playwright v1.56 is here. Meet Playwright Agents — your new AI-powered teammates: planner — explores your app and drafts a Markdown..."

### 3.2 How to test POST API Requests with Playwright TypeScript
- URL: https://www.linkedin.com/posts/michaldrajna-qa_how-to-test-post-api-requests-with-playwright-activity-7441068710024343552-3pyo
- Opis: „Sajith Dilshan breaks down how to leverage Playwright for POST API requests."

### 3.3 Release v1.58.0 · microsoft/playwright
- URL: https://www.linkedin.com/posts/michaldrajna-qa_release-v1580-microsoftplaywright-activity-7420438648669569024-GENw
- Opis: „Playwright v1.58.0 finally dropped! If you live in Playwright reports, traces, and UI mode, this release is for you. What's new..."

### 3.4 Playwright in Pictures: Why Workers Restart?
- URL: https://www.linkedin.com/posts/michaldrajna-qa_playwright-in-pictures-why-workers-restart-activity-7472592081392283648-t16c
- Opis: „Visualizing the process boundary: Why Playwright workers restart. If you've ever had a test pass locally but bottleneck your test runner..."

### 3.5 17 Playwright Testing Mistakes You Should [Avoid]
- URL: https://www.linkedin.com/posts/michaldrajna-qa_17-playwright-testing-mistakes-you-should-activity-7441763177178112000-B0yp
- Opis: „Are you still writing Playwright tests like it's 2021? The framework has evolved, but many of us are still carrying over habits from legacy tools." (autor: Yevhen Laichenkov)

### 3.6 playwright-labs/reporter-slack — Better Playwright Alerts, Straight to Slack
- URL: https://www.linkedin.com/posts/michaldrajna-qa_playwright-labsreporter-slack-rich-slack-activity-7469659578377355265-1myZ
- Opis: „Ditch the fragile, homemade webhook scripts. Vitali Haradkou's latest article shows how to..." (Rich Slack alerts)

### 3.7 TIL: Playwright step decorator for better test reporting
- URL: https://www.linkedin.com/posts/michaldrajna-qa_til-playwright-step-decorator-for-better-activity-7445749444337233920-en2O

### 3.8 Advanced Playwright Authentication: A Multi-Role Fixture for Scalable E2E Testing
- URL: https://www.linkedin.com/posts/michaldrajna-qa_advanced-playwright-authentication-a-multi-role-activity-7440397017412972545-mkko
- Opis: „Mastering Multi-Role Testing in Playwright. Tired of wrestling with authentication states? Faizan Ahmed's latest guide on Multi-Role..." (storage state / fixtures)

### 3.9 How I Built an AI Agent That Writes Playwright Tests From a GitHub Issue
- URL: https://www.linkedin.com/posts/michaldrajna-qa_how-i-built-an-ai-agent-that-writes-playwright-activity-7468576097983520769-J6bS
- Opis: „...I use code coverage and TDD skills. And they generates when AI touch the code. And it's working smoothly :)"

### 3.10 How to Use Playwright CLI Skill for Agentic Testing
- URL: https://www.linkedin.com/posts/michaldrajna-qa_how-to-use-playwright-cli-skill-for-agentic-activity-7429484533424660480-AKDR
- Opis: „AI agents just got a PhD in Playwright. In this brilliant demo, Debbie O'Brien shows how the new Playwright CLI empowers coding agents to..." (~6 mies. temu)

### 3.11 Handling Multi-User Flows in Playwright the Right Way
- URL: https://www.linkedin.com/posts/michaldrajna-qa_handling-multi-user-flows-in-playwright-the-activity-7331589760680882176-Fsoj

### 3.12 Vibe testing with Playwright (+ AI)
- URL: https://www.linkedin.com/posts/michaldrajna-qa_vibe-testing-with-playwright-activity-7327603517295108096-pl_5
- Opis: „Vibe testing with Playwright + AI? Yes, please. I've always loved Playwright for its speed, reliability, and dev-friendly API." (~1 rok temu, 2025)

### 3.13 Playwright — budúcnosť test automatizácie? (wideo, YouTube)
- URL: https://www.youtube.com/watch?v=tz7dfBxkYDA
- Opis: „Playwright je nový, moderný testovací framework od spoločnosti Microsoft. Od jeho prvej verzie vydanej v roku 2017..."

---

## 4. Poza zakresem 2 lat (starsze niż 2024-08-28)

- **Cypress vs Playwright vs WebdriverIO** — https://www.linkedin.com/posts/michaldrajna-qa_cypress-vs-playwright-vs-webdriverio-the-activity-7114508990591422465-pLnL („2y edited", granica zakresu)
- **Thank you for inviting me to speak at TestCrunch 2024** — https://www.linkedin.com/posts/michaldrajna-qa_thank-you-for-inviting-me-to-speak-at-testcrunch-activity-7153379796989669376-xv-k (początek 2024; wystąpienie „Playwright: The Future of Test Automation?", TestCrunch 27.03.2024)
- **Playwright new release 1.45 🎭** (Pulse) — https://www.linkedin.com/pulse/playwright-new-release-145-michal-drajna-j8cae (2024-06-27)
- **Playwright Elevates Locator Handling in Version 1.44** (Pulse) — https://www.linkedin.com/pulse/playwright-elevates-locator-handling-version-144-michal-drajna-mvglf (2024-05-14)
- **Streamlining test runs with Playwright's new --last-failed CLI option** (Pulse) — https://www.linkedin.com/pulse/streamlining-test-runs-playwrights-new-last-failed-cli-michal-drajna-l3nte (2024-05-09)
- **How to Test SMS OTP Authentication with Playwright without Using a Paid Service** (Pulse) — https://www.linkedin.com/pulse/how-test-sms-otp-authentication-playwright-without-using-drajna (2023-01-13)
- **Sending a GraphQL POST Request with Playwright** (Pulse) — https://www.linkedin.com/pulse/sending-graphql-post-request-playwright-michal-drajna (2023-01-12)

---

## 5. Komentarze i aktywność poboczne (wzmianki o Playwright)

- Komentarz 6d temu: „Gratulujem 👏👏👏" (pod postem Petra Škody o nagrodzie TestCon)
- Komentarz 1mo: „I still find real value here, but it requires heavy filtering." (pod postem Juraja Zábsky o LinkedIn)
- Post BrowserStack Meetup Amsterdam (polubiony): talk „Just Enough Context: Teaching Claude to Test with Playwright"
- Newsletter: https://www.linkedin.com/newsletters/michal-drajna-7333099244930662404 („Michal Drajna | LinkedIn", serie o release'ach Playwright)

---

## 6. Podsumowanie tematyczne (2024-08 → 2026-08)

| Temat | Posty/artykuły |
|---|---|
| Release'y Playwright (v1.56–v1.62) | 1.1, 1.2, 1.3, 1.4, 1.5, 2.8, 2.10, 3.1, 3.3 |
| Playwright + AI / agenci / MCP / CLI | 1.4, 1.5, 1.6, 2.5, 2.6, 2.7, 3.9, 3.10, 3.12, 5 |
| Flaki, stabilność, architektura testów | 2.1, 2.2, 2.4, 3.4, 3.5, 3.8 |
| Porównania (Cypress, Vibium) | 2.9, 2.11, 3.12, 4 |
| Nauka / edukacja | 2.3, 3.13, 1.6 |

**Uwagi o kompletności:** LinkedIn publicznie pokazuje tylko ok. 10 ostatnich postów bez logowania; wyszukiwarka Firecrawl odnalazła dodatkowe posty (sekcja 3) wg tytułu/opisu. Pełna historia 407 postów wymagałaby zalogowanej sesji (auth-wall) — poza zasięgiem narzędzi bez poświadczeń. Sekcja 3 zawiera posty, których pełna treść nie została pobrana (URL dostępny do dalszego scrapowania przez web_fetch).
