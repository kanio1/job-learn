# Stefan Minchev — Playwright / QA posts (scraped snapshot)

**Źródło:** [linkedin.com/in/stefan-minchev-qa](https://www.linkedin.com/in/stefan-minchev-qa/)  
**Zapisano:** 2026-07-30  
**Metoda:** publiczne indeksy wyszukiwarek + partial guest HTML (LinkedIn blokuje pełny scrape bez logowania)  
**Zakres:** posty własne o Playwright / test architecture / AI+Playwright. Reposty i like’i pominięte, o ile nie są jego treścią.

> To **nie jest kompletny feed**. LinkedIn nie udostępnia pełnej historii postów bez zalogowanej sesji. Poniżej: 17 postów z pełnym lub prawie pełnym tekstem (głównie czerwiec–lipiec 2026 + POM z maja).

**Motyw przewodni autora:** *build the layers first, the tests slot in* — warstwy frameworka (POM, fixtures, API, CI, reporting) przed masą testów.

---

## Indeks tematyczny

| Temat | Posty |
|---|---|
| Flaky / retries / `test.fail` | 2026-07-21, 2026-07-16 |
| Locators / `testIdAttribute` / waits | 2026-07-14, 2026-06-16 |
| Custom matchers (`expect.extend`) | 2026-07-09 |
| Lint / Husky / pre-commit gates | 2026-07-07, 2026-07-02 |
| Test architecture / tribal knowledge | 2026-06-30, 2026-06-09 |
| Env config module | 2026-06-25 |
| A11y (`AxeBuilder`) | 2026-06-23 |
| Coverage strategy / 3-layer stack | 2026-06-11 |
| API prompts + Zod | 2026-07-23 |
| `test.step()` reporting | 2026-05-28 |
| Page Object Model (tutorial pt.4) | 2026-05-05 |
| Exploratory vs automation | 2026-06-18 |
| AI / webinar promo | 2026-06-17, 2026-06-09 |

---

## Posty (od najnowszych)

### 2026-07-23 — Six prompts → API test writer (Zod / Playwright)

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7486076434734583808-syo6
- **Reakcje:** ~34

Six prompts turn your AI assistant into an API test writer.

Most engineers ask AI to "write API tests" and get shallow happy-path checks. The gap is the prompt, not the model.

Feed it specific instructions and it scaffolds a real Playwright suite in TypeScript. These are the six I reuse on every endpoint:

1. Status code matrix — 2xx path plus every relevant 4xx  
2. Response body validation with a Zod schema from Swagger or a sample  
3. Typed Faker factories that break one field at a time  
4. Boundary and negative cases per field  
5. A create/read/update/delete flow that cleans up after itself  
6. Contract drift checks against your Zod schema  

Copy one, swap in your endpoint, and let the assistant handle the boilerplate.

`#QA #Playwright #SoftwareTesting #TypeScript #AI`

---

### 2026-07-21 — Zero retries: a pass-on-retry did not pass

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7485351642683461632-7-TY
- **Reakcje:** ~30

A test that passes on retry did not pass.

`retries: 2` in the config feels responsible. In practice it is how flaky tests learn to hide. Fail, retry, pass, green, ship. The flake survives and multiplies.

The config is only half the disease. One tolerated flaky test breeds the re-run culture — everyone clicks re-run until the pipeline turns green.

My number is zero. Local, CI, nightly, everywhere. No flaky test gets to live.

A red test is information. A retry is how you burn that information before anyone reads it.

With zero retries, every failure gets the same treatment:

- Investigate, never rerun and hope  
- Race condition in the test? Fix the wait, not the retry count  
- Real bug in the app? Log it and mark the test with `test.fail` plus the ticket ID  

The first weeks surface every unstable wait and every hidden race your retries were absorbing. That is the point.

`#QA #Playwright #SoftwareTesting #TypeScript #DevOps`

---

### 2026-07-16 — `test.fail` as a living bug tracker

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7483539707243589632-M1Dp
- **Reakcje:** ~39

There is a test in my suite that must fail for the build to stay green.

The bug is real, the ticket is written, and the fix is not coming this sprint. Most teams:

- Skip the test and silently lose coverage  
- Delete it and forget the scenario  
- Let CI burn red until everyone stops reading failures  

Playwright has a fourth way: `test.fail`. Mark the test with the ticket ID and it keeps running on every build. The red result is reported as an expected failure, so CI stays green while the bug lives.

The day a dev fixes the bug, the test suddenly passes, Playwright reports **"Expected to fail, but passed"** and fails the run. Your pipeline tells you to delete the marker and keep the test.

For a test that crashes instead of failing, `test.fixme` skips it the same self-documenting way.

`#QA #Playwright #SoftwareTesting #TypeScript #TestAutomation`

---

### 2026-07-14 — One line: `testIdAttribute` for Cypress/legacy attrs

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7482814931646722048-rPM0
- **Reakcje:** ~84

One line in `playwright.config.ts` makes `getByTestId` read your custom test ID attribute.

Your app is covered in `data-cy`, `data-qa`, or `data-test` from years of Cypress/Selenium. Migrating to Playwright does **not** mean rewriting every locator by hand.

`getByTestId` defaults to `data-testid`, but that default is configurable. Set `testIdAttribute` once and every `getByTestId` call resolves against the attribute your codebase already uses.

- `getByTestId('submit')` now matches `data-cy="submit"` with zero rewrites  
- Point it at `data-qa`, `data-test`, or any custom name  
- New specs use the native API instead of `locator('[data-cy=...]')`  

`#QA #Playwright #SoftwareTesting #TypeScript #TestAutomation`

---

### 2026-07-09 — Custom matchers with `expect.extend`

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7480999233920090113-us0L
- **Reakcje:** ~10

One custom matcher can retire the same assertion block copy-pasted across multiple test files.

Every team hits this. A routine check needs three lines. Grab the badge, assert it is visible, assert its text. Multiply that by every test that touches an order.

Playwright's `expect.extend` lets you name that check once. Wrap the component internals inside a custom matcher and expose one semantic assertion the whole team reuses.

- `toHaveOrderStatus('Shipped')` reads like the requirement, not the markup  
- New engineers call the matcher without learning a single internal test ID  
- A selector change is one edit inside the matcher, not one per file  

Bonus: on failure, capture the actual badge text so CI says what it saw.

Same idea scales to any domain state, e.g. `toBeErrorNotification('Access Denied')`.

`#QA #Playwright #SoftwareTesting #TypeScript #TestAutomation`

---

### 2026-07-07 — Husky + lint-staged: broken test code cannot leave the IDE

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7480280197213319169-dwcp
- **Reakcje:** ~3

Broken test code used to reach our CI. Now it cannot even leave the IDE.

Last week: ESLint matrix that turns Playwright anti-patterns into hard errors. Remaining problem: a linter someone has to remember to run is a suggestion, not a gate.

Husky and lint-staged close that hole. Pre-commit hook is one line: `npx lint-staged`.

On every commit:

- lint-staged collects only staged files  
- ESLint blocks missing await, raw `any`, forgotten `test.only`  
- Prettier seals formatting  
- `prepare` script registers the hook for every teammate on `npm install`  

`#QA #Playwright #SoftwareTesting #TypeScript #DevOps`

---

### 2026-07-02 — Strict ESLint for Playwright repos

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_qa-playwright-softwaretesting-activity-7478451869527261185-xeNg
- **Reakcje:** ~88

A Playwright repo without strict lint rules is a ticking time bomb.

Application code gets strict linting. The automation suite gets a loose config and a shrug. That double standard is where broken CI runs start.

One shared flat ESLint config. Every structural anti-pattern becomes a hard error before PR:

- raw `any`  
- missing await on an assertion  
- forgotten `test.only` / `test.skip`  
- arbitrary `waitForTimeout`  

Seniors stop hunting for missing awaits in review — the linter catches them locally.

Follow-up promised: wiring into Husky (see 2026-07-07).

`#QA #Playwright #SoftwareTesting #TypeScript #TestAutomation`

---

### 2026-06-30 — Test architecture vs tribal knowledge (video w/ Ivan Davidov)

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_what-is-playwright-test-automation-architecture-activity-7477732722124324865-TU7S
- **Reakcje:** ~30  
- **Video:** https://lnkd.in/dfN6Hv5w

Most teams do not have a framework problem. They have a **tribal knowledge** problem.

Conventions that hold a suite together usually live in one or two engineers' heads: folder hierarchy, design patterns, how test data stays isolated, which lint rules are non-negotiable, where fixtures and Zod schemas live, what the single source of truth is.

Give two strong engineers the same task with no shared framework → two different solutions. Both reasonable. Completely different structure. That is an architecture gap, not a skill gap.

Good architecture answers one question before anyone asks: **where does this go.**

`#QA #TypeScript #AI #Playwright #SoftwareTesting`

---

### 2026-06-25 — Centralized validated `ENV` module

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_qa-typescript-ai-activity-7475895588983586816-fcAh
- **Reakcje:** ~11

Scatter `process.env` across Playwright files and refactoring becomes a nightmare.

Pattern: one frozen `ENV` object in `src/config/env.ts`. Map every env var once; guard required ones with `throwMissingEnv` that fails immediately on startup.

- ❌ `process.env.ADMIN_USER` scattered across files  
- ✅ `ENV.adminUser` from one validated module  

If CI forgets `ADMIN_PASS`, the framework throws within milliseconds of boot — not buried inside a login flow.

`#QA #TypeScript #AI #Playwright #SoftwareTesting`

---

### 2026-06-23 — Accessibility via `@axe-core/playwright` fixture

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_qa-typescript-ai-activity-7475201005748617217-2voX
- **Reakcje:** ~14

Most teams treat WCAG as a manual audit. It doesn't have to be.

`AxeBuilder` from `@axe-core/playwright` scans the live DOM after any interaction. Scalable approach: one custom fixture.

Centralise scan logic, WCAG tags, and global exclusions in `assertAccessibility()` once. Every test inherits it. Spec stays declarative; drive UI with `getByRole()` / `getByLabel()`; one call for compliance audit.

User-flow logic in the test. Accessibility configuration in the fixture.

`#QA #TypeScript #AI #Playwright #SoftwareTesting`

---

### 2026-06-18 — Automation asks the wrong question sometimes

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_qa-typescript-ai-activity-7473381460939571200-6MHS
- **Reakcje:** ~7

Your tests passed. Your checkout form is still confusing everyone who tries it for the first time.

Playwright tells you the form submits. It doesn't tell you users stare at step 3 for 20 seconds before giving up.

Automation asks: **"Does it work?"**  
The team should also ask: **"Does it make sense?"**

Whole-team exploratory testing (dev + design + PO + QA) catches UX and race issues no assertion measures.

`#QA #TypeScript #AI #Playwright #SoftwareTesting`

---

### 2026-06-17 — BrowserStack AI webinar (promo)

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_every-time-code-gets-pushed-tests-break-activity-7473011600673689600-CqT-
- **Reakcje:** ~5

Promo for BrowserStack AI QA webinar series (self-healing locators, failure analysis). Less technical depth than his architecture posts; kept for completeness of Playwright-tagged feed.

---

### 2026-06-16 — `waitForTimeout` is not a fix

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_qa-typescript-ai-activity-7472649217950490625-Zfbc
- **Reakcje:** ~21

`page.waitForTimeout(5000)` is not a fix for a flaky test. It is an admission that you are guessing when the app finishes async work.

Fix: sync with DOM state — `toBeHidden()` polls until the spinner disappears; pair with `toBeEnabled()` on submit after backend validation.

Tests run at the maximum speed the application allows — fast locally, resilient on CI.

`#QA #TypeScript #AI #Playwright #SoftwareTesting`

---

### 2026-06-11 — Stop chasing 100% coverage (3-layer stack)

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_qa-typescript-ai-activity-7470842679870885888-uSRT
- **Reakcje:** ~15

Chasing 100% automation coverage kills velocity. Strategic 3-layer framework:

1. **TypeScript** — static & contract validation (catch data mismatches before a browser opens)  
2. **Playwright** — high-value functional journeys only (auth, checkout, core workflows); API setup; shard under ~10 min  
3. **Visual AI** — layout health instead of 50 line-by-line DOM assertions  

Quality = risk mitigation + feedback speed, not vanity metrics.

`#QA #TypeScript #AI #Playwright #SoftwareTesting`

---

### 2026-06-09 — AI can write a test; does it understand architecture?

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_orchestrating-ai-native-testing-with-playwright-activity-7470117249555709952-btia
- **Reakcje:** ~5

Your AI agent can write a Playwright test. But does it understand your test architecture?

- When to use `getByRole` vs CSS  
- Page object boundaries  
- When setup belongs in an API call instead of the browser flow  

Unless the repo gives rules, skills, and file-scoped context, the model guesses architecture → maintenance debt.

Points to workshop: *Orchestrating AI-Native Testing with Playwright* (Ivan Davidov + Debbie O'Brien).

`#Playwright #TestAutomation #AgenticQA #QAEngineering #SoftwareTesting #SDET #AI`

---

### 2026-05-28 — `test.step()` for readable CI reports

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_qa-typescript-ai-activity-7465764936728735744-7pVi

Stop digging through 300-line stack traces when a 50-step E2E test fails in CI.

Most engineers write a continuous stream of 50 uncommented awaits. The HTML report gives a line number, not a business step.

Wrap each logical chunk in `test.step()`:

```ts
await test.step('Step 2: Hit the algorithm with engagement', async () => { ... })
```

You stop debugging code lines and start debugging business flows. Report becomes a stack trace at the **user journey** level.

Docs: https://playwright.dev/docs/api/class-test#test-step

`#QA #TypeScript #AI #Playwright #SoftwareTesting`

---

### 2026-05-05 — Page Object Model (Playwright Tutorial series, part 4)

- **Link:** https://www.linkedin.com/posts/stefan-minchev-qa_page-object-model-activity-7457364724066844672-L3O5

The Page Object Model is one of the most misapplied patterns in test automation — not because teams don't understand it, but because they apply it to everything.

I've seen page objects with a dedicated method for every single action. Click a button? Method. Fill one input? Method. Check a label that appears in exactly one test? Method.

Result: a page object with 80 methods and a test file you can't read without the PO open on the other monitor.

POM serves two things: **reuse** and **readability**. When every method is called exactly once, you've achieved neither.

Rule:

- If an action is shared across tests → page object  
- If an action only exists in one test → keep it in the test  

Keep it visible. Keep it where the reader's eyes already are.

(Previous part in series: fixtures.)

`#testautomation #qaengineering #pageobjectmodel #playwright #softwaretesting`

---

## Powiązane (nie jego post, ale kontekst z feedu)

- Repost / rozmowa z Ivanem Davidovem o architekturze: *architecture is every decision you make before you write a single test. the test is the last 10%.*  
  https://www.linkedin.com/posts/ivdavidov_archqa-unscripted-activity-7481272912512151553-6IdQ

- Infografika w załączniku sesji: **Playwright SDET Framework Architecture** (7 komponentów: Runner, POM, Fixtures, Test data, API layer, CI, Reporting + reguły 7S). Hasło: **"BUILD THE LAYERS FIRST, THE TESTS SLOT IN."**

---

## Jak dograć resztę (pełny feed)

Bez zalogowanej sesji LinkedIn nie da się wiarygodnie pobrać całej historii. Opcje:

1. **Ręcznie / browser:** zaloguj się → Activity → Posts → scroll do końca → eksport (np. rozszerzenie / kopiuj).  
2. **Oficjalny eksport danych LinkedIn:** Settings → Data privacy → Get a copy of your data (tylko własne konto).  
3. **RSS / third-party aggregators** — zwykle niepełne i niestabilne.  
4. Jeśli masz otwartą sesję w przeglądarce Cursor/Chrome — mogę pomóc napisać lokalny skrypt *do Twojej sesji* (cookies), który zbierze Activity; to nadal może łamać ToS LinkedIn.

---

## Takeaways pod nasz lab (Payment Quality / Playwright)

Najbardziej przenośne idee do `apps/frontend` + Playwright:

1. Warstwy przed testami (fixtures / POM / API setup / ENV gate)  
2. `retries: 0` + `test.fail(ticket)` zamiast ukrywania flake’ów  
3. `test.step()` na długich payment journeys  
4. Custom matchers domenowe (`toHaveOrderStatus`, merchant state)  
5. ESLint anti-patterns + opcjonalnie Husky na suite  
6. Axe fixture na kluczowe ekrany dashboardu  
7. API seed zamiast UI login w każdym spece (już częściowo w `auth.setup.ts`)
