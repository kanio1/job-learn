# LinkedIn Posts — Playwright SDET

Liczba postów: 131

## 1. A test that passes on retry did not pass.`retries: 2` in the config feels responsible. In practice i…

### Tekst postu

A test that passes on retry did not pass.`retries: 2` in the config feels responsible. In practice it is how flaky tests learn to hide. Fail, retry, pass, green, ship. The flake survives and multiplies.The config is only half the disease. One tolerated flaky test breeds the re-run culture, everyone clicks re-run until the pipeline turns green.My number is zero. Local, CI, nightly, everywhere. No flaky test gets to live.A red test is information. A retry is how you burn that information before anyone reads it.With zero retries, every failure gets the same treatment:🔸 Investigate, never rerun and hope🔸 Race condition in the test? Fix the wait, not the retry count🔸 Real bug in the app? Log it and mark the test with test.fail plus the ticket IDThis is not free. The first weeks surface every unstable wait and every hidden race your retries were absorbing. That is the point. You fix them once, and green starts meaning something for the whole team.

### Tekst z obrazka

QA ARCHITECTURE
A flaky pass is not a pass
® ® ® playwright.config.ts
1 // playwright.config.ts
2 export default defineConfig({
3 // No second chances, a red test means something is wrong
EVERYWHERE
| « [eetriesTa),
J HE
7 // The workflow when a test goes red:
9 // 1. Investigate, never rerun and hope
16
11 // 2. Race condition in the test? Fix the wait, not the retry count
1
1 // 3. Real bug in the app? Log it, mark the test with test.fail + ticket
14
15 // 4. Now green means something, for every engineer on the team
Stefan Minchev

---

## 2. There is a test in my suite that must fail for the build to stay green.The bug is real, the ticket i…

### Tekst postu

There is a test in my suite that must fail for the build to stay green.The bug is real, the ticket is written, and the fix is not coming this sprint. Most teams handle that moment in one of three ways.🚩 Skip the test and silently lose the coverage🚩 Delete it and forget the scenario ever existed🚩 Let CI burn red until everyone stops reading the failuresPlaywright has a fourth way: test.fail. Mark the test with the ticket ID and it keeps running on every build. The red result is reported as an expected failure, so CI stays green while the bug lives.Here is the part I love. The day a dev fixes the bug, the test suddenly passes, Playwright reports "Expected to fail, but passed" and fails the run. Your pipeline literally tells you to delete the marker and keep the test.The suite becomes a living bug tracker. No lost coverage, no stale tickets, no human memory required.For a test that crashes instead of failing, test.fixme skips it the same self-documenting way.How does your team keep CI green while a known bug waits for its fix?

### Tekst z obrazka

QA ARCHITECTURE
The test that must fail to keep CI
®®e cart-coupons.spec. ts
// The bug is real and tracked, the fix is not coming this sprint
test('cart total with two stacked coupons', async ({ page }) = {
// Tell Playwright this test is EXPECTED to fail, with the ticket ID
| 4 [EEEFRre, a1rA-1025, totals double the second coupon’);
await page.goto('https: //exanple.con/cart');
await page.getByRole('button’, { name: 'Apply coupons’ }).click();
// This assertion hits the real bug today, so the test goes red
await expect (page. getByTestId(" total"). [POREVETALEM $96.00");
Bb;
| 13 // white the bug tives, red counts as an expected failure, CI stays green
// The day a dev fixes it, the test passes, Playwright reports
// 'Expected to fail, but passed’ and fails the run,
// CI itself tells you to delete the marker and keep the test
Stefan Minchev

---

## 3. One custom matcher can retire the same assertion block copy-pasted across multiple test files.Every …

### Tekst postu

One custom matcher can retire the same assertion block copy-pasted across multiple test files.Every team hits this. A routine check needs three lines. Grab the badge, assert it is visible, assert its text. Multiply that by every test that touches an order.Playwright's `expect.extend` lets you name that check once.You wrap the component internals inside a custom matcher and expose one semantic assertion the whole team reuses (see image).🔸 `toHaveOrderStatus('Shipped')` reads like the requirement, not the markup🔸 New engineers call the matcher without learning a single internal test ID🔸 A selector change is one edit inside the matcher, not one per file across the suiteBonus. On failure, capture the actual badge text so your CI log says what it saw, not a vague "text didn't match".Same idea scales to any domain state. A toast becomes `toBeErrorNotification('Access Denied')`. Your assertion library starts speaking your product's language.Are you still forcing your team to copy-paste raw element chains for routine checks, or have you wrapped your domain rules into semantic custom matchers?

### Tekst z obrazka

© Unleashing Quality
with Playwright Automation
PLAYWRIGHT ARCHITECTURE
Custom matchers that read like requirements
import { test, expect as baseExpect, type Locator } from @playwright/test
type Orderstatus = Processing | ‘Shipped | Delivered
// define the matcher once, the whole team imports this expect
export const expect = dbaseExpectextend({ | Derie once
async toHaveOrderstatusllocator. Locator, expected: Orderstatus, options?: { timeout. number 1) {
const badge = locator getByTestid order status badse)
let pass = true
let actual = expected
ry {
// Forward the caller's timeout, default 5000
await baseExpecHbadae) toHaveTexi(expected, { timeout. options? timeout 72 5000 ))
} cateh {
// on kaiure. capture what the badge actually showed
pass - False
actual = (await badge textContentO2riml) 22 (no badse Found)
}
return {
pass,
name. ‘toHaveOrder Status,
expected,
actual,
// phrase the message For both expect and expectnot
message: () => thisisNot
2? Expected order NOT to show status "Hexpected)”
Expected order status “Hexpected)’ but badge showed “#Hactual)",
}
)
»
// the payol: one line that reads like the business rule
test(order i= shipped, async ({ page J) => {
const orderCard = page getByRole(listitem)fiter({ hasText. ‘Order #4092 })
await expect{orderCard)toHaveOrderstatus( shipped) | READS LIKE THE RULE
»n

---

## 4. Broken test code used to reach our CI. Now it cannot even leave the IDE.Last week I shared the ESLin…

### Tekst postu

Broken test code used to reach our CI. Now it cannot even leave the IDE.Last week I shared the ESLint matrix that turns Playwright anti-patterns into hard errors. One problem remained: a linter someone has to remember to run is a suggestion, not a gate.Husky and lint-staged close that hole. The entire pre-commit hook is one line: `npx lint-staged`.Here is what fires on every commit:🔸 lint-staged collects only the staged files, so commits stay fast🔸 ESLint blocks the missing await, the raw `any`, the forgotten `test.only`🔸 Prettier seals the formatting before the diff even exists🔸 The `prepare` script registers the hook for every teammate on npm installNo discipline required. The gate runs whether you remember it or not.I packed the full setup into a step-by-step guide, every command executed and verified on a clean project before publishing. Link is in the comments 👇The first 100 copies are free, so early readers can grab it before I decide on pricing. Once those are claimed, the offer changes.Do you gate commits locally, or is CI your first line of defense?

### Tekst z obrazka

& Unleashing Quality
with Playwright Automation
PLAYWRIGHT ARCHITECTURE
The one-line pre-commit quality gate
// the entire hook, husky/pre-commit
px Int-staged> | THE WHOLE HOOK
// package son, hooks auto-register on npm install
‘scripts: { ‘prepare" husky" },
// run the tools only on Fies staged For this commit
Yint-staged". i
// every staged spec, page object, and helper
"tis": Leslint --Fix', ‘prettier —write’],
// conkigs and docs stay Formatted too
“*{jsonymiyarmimd)" [prettier —write”]
}

---

## 5. A Playwright repo without strict lint rules is a ticking time bomb.Application code gets strict lint…

### Tekst postu

A Playwright repo without strict lint rules is a ticking time bomb.Application code gets strict linting. The automation suite gets a loose config and a shrug. That double standard is exactly where broken CI runs start.So I make the framework self-policing with one shared flat ESLint config. Every structural anti-pattern becomes a hard error before the code reaches a pull request:🚩 A raw `any` escaping type safety🚩 A missing await on an assertion🚩 A forgotten `test.only` or `test.skip`🚩 An arbitrary `waitForTimeout` burning CI budgetPush one of these and the build breaks instantly. No debate, no "we will fix it later".The payoff is review time. Seniors stop hunting for missing awaits and loose types, because the linter catches them locally before anyone opens the diff.Next up I will talk about wiring this config into Husky, so every rule fires automatically in a pre-commit hook.Is your framework self-policing these anti-patterns, or is your team catching missing awaits the hard way inside broken CI runs?

### Tekst z obrazka

© with Playwright Automation
PLAYWRIGHT ARCHITECTURE
The self-policing ESLint matrix
{
Laierent Etat eens); "pleat reports ede Nes) Ty
files: ['**/*.ts'], 5
oliwinn: € slimes, “Bomba phoistiog os Gestiot, sraktia sy
rules: {
‘prettier/prettier': ‘error’,
‘@typescript-eslint/no-explicit-any': ‘error’,
mE es py
{ argsIgnorePattern: '* °, varsignorePattern: Hn,
(Bypbscrint-er1ihtfhorimply-fenction?s. anvort)
“layuright/missing-plaright- await: ‘cror’,
een
PEE Sr GA
ir oi ond at
Suieight ne bud rics Sarat,
(iui ei le eee
(ere
ia
}

---

## 6. Scatter `process.env` calls across multiple Playwright test files and refactoring becomes a nightmar…

### Tekst postu

Scatter `process.env` calls across multiple Playwright test files and refactoring becomes a nightmare.Most teams do not think twice about it. A test needs the base URL, so they write `process.env.BASE_URL!`. Another needs admin credentials, so they write `process.env.ADMIN_PASS!` right there in the spec.Three months later, someone renames `ADMIN_PASS` to `ADMIN_PASSWORD` in the CI config. Half the suite runs fine. The other half crashes with `Cannot read properties of undefined` buried inside a login flow.There is a better pattern: one centralized, validated config module.Create a frozen `ENV` object in `src/config/env.ts`. Map every environment variable exactly once and guard required ones with a `throwMissingEnv` helper that fails immediately on startup.🚩 `process.env.ADMIN_USER` scattered across multiple files✅ `ENV.adminUser` imported from one validated moduleIf CI forgets to inject `ADMIN_PASS`, the framework throws within milliseconds of boot. Not after a full login sequence. Not buried in a stack trace. Right at the gate.You also get IDE autocomplete on every `ENV` property and a single file to update when variables change.Are your environment variables scattered blindly across your specs, or do you validate them at the gate with a centralized configuration module?

### Tekst z obrazka

&) Unleashing Quality
with Playwright Automation
| © I srelcanfigfenvats

export const ENV = {
baseUrl : process. env. BASE_URL || 'https:// example. com’,
adminUser: process. env. ADMIN_USER 7? (EhrowMissingEny (‘ADMIN_USER'),
adminPass: process. env. ADMIN_PASS 22 \ throwMissingEnv (‘ADMIN_PASS'

} as const;

function throwMissingEnv (name: string): string { ] FAIL FAST
throw new Ervor (*X Missing required variable. [${name}]; |!

}

// login. spec.ts

import { ENV } from "../config/env';

test (‘Authenticate via secure confi’, async ({ page }) => {
// X Anti-pattern: process. env.BASE_URL!
// / Validated, typed config:
await page. goto (ENV. baseUrl);
await page. getByLabel (‘Username’). fill (ENV.adminUser);
await page. getByLabel (Password). fill (ENV.adminPass );
await page. getByRole ('button’, { name: ‘Sign In’ }). click ();

1h;

---

## 7. Most teams treat WCAG compliance as a manual audit item. It doesn't have to be.`AxeBuilder` from `@a…

### Tekst postu

Most teams treat WCAG compliance as a manual audit item. It doesn't have to be.`AxeBuilder` from `@axe-core/playwright` scans the live DOM state after any interaction. Your Playwright tests are already navigating real pages, filling real forms, and clicking real buttons. Adding accessibility coverage at that point costs one line.The scalable way to do it is a single custom fixture.Centralise the scan logic, the WCAG tags, and the global exclusions in `assertAccessibility()` once. Every test inherits it. When compliance rules change, you update one file instead of forty.Your spec stays clean and declarative. You drive the UI with `getByRole()` and `getByLabel()` exactly like a real user. When you want to audit compliance, one call is all it takes.User-flow logic in the test. Accessibility configuration in the fixture. You're scanning against the same accessibility tree screen readers actually use.Does your Playwright suite currently catch accessibility regressions automatically, or is WCAG compliance still a manual checklist item before releases?

### Tekst z obrazka

&) Unleashing Quality
with Playwright Automation
import { test as base, expect } from '@ playwright / test’;
import AxeBuilder from '@axe-core/ playwright;
// 1. Centralize compliance rules into a global fixture
export const test = base.extend <{ assertAccessibility: () > Promise <void> }> ({
assertAccessibility: async ({ page }, use) => {
const runScan = async, () => {
const scanResults = await new AxeBuilder ({ page })
¢ .withTags (['wcag2a’, "weag2aa’, 'weagllan']) ————————> WCAG RULES
ES exclude (".legacy- third -party-widget')
~analyze (); 1
expect (scanResults. violations). toEqual (11); oe
Yi
await use (runScan);
bi
1/2. The test: clean and declarative
test (‘verify accessibility compliance’, async ({ page, assertAccessibility }) => {
await page. goto ('/account/settings');
await expect (page. getByRole (‘heading’, { name: ‘Profile Settings’ })). toBeVisible );
await @sserthccessibility D> ove ea
hi 7

---

## 8. Your tests passed. Your checkout form is still confusing everyone who tries it for the first time.Pl…

### Tekst postu

Your tests passed. Your checkout form is still confusing everyone who tries it for the first time.Playwright tells you the form submits. It doesn't tell you users stare at step 3 for 20 seconds before giving up.That's not a bug. No script will ever catch it.This is what whole-team exploratory testing is for.Not because automation is broken. Because automation asks the wrong question.It asks: "Does it work?"Your team should also be asking: "Does it make sense?"When a developer, designer, and product owner sit down with the same staging build:The designer notices the micro-interaction that feels weirdly sluggishThe developer spots the console flooding with 400s nobody is handlingThe product owner flags the copy that's grammatically fine but sends users the wrong wayThe QA engineer finds the race condition behind the double-click on the submit buttonSome of those are real bugs your scripts never reached. Some are UX issues no assertion can measure. None of that lives in a test report.How often does your full product team actually use the app together before a release?

### Tekst z obrazka

& Unleashing Quality
with Playwright Automation
WHAT AUTOMATION SEES WHAT YOUR TEAM SEES
O—@—F0 USERS
Cl REPORT Step 1 Step2 | Step 3 he
form. submit V4 Billing Address
Eien El-p
checkout. step3 v 1S WRONG:
= Ee ir tails
api. response V Review & Confirm
A BACK ii
email. send Vv BuTToN ie?
ALL TESTS PASSED WORKS. DOESN'T MAKE SENSE.
NE
_———
Automation tells you it works. Your team tells you if it makes sense.
+ Follow me Save this §
JN for more n this post [J Like © Comment ¢# Share

---

## 9. `page.waitForTimeout(5000)` is not a fix for a flaky test. It is an admission that you are guessing …

### Tekst postu

`page.waitForTimeout(5000)` is not a fix for a flaky test. It is an admission that you are guessing when your application finishes async work.Every hardcoded sleep you scatter across your test suite slows it down and leaves the race condition buried, waiting for a CI runner that runs just a bit slower than your local machine.The fix is to let Playwright sync with DOM state directly.`toBeHidden()` polls the DOM continuously. It proceds the exact millisecond the spinner disappears, not 5000 ms later regardless of whether it is gone.Pair it with `toBeEnabled()` on submit buttons that unlock after backend validation, and you eliminate an entire class of timing-related flakiness.Your tests run at the maximum speed the application allows, fast locally and resilient on CI runners.How many hardcoded sleeps are currently holding your test suite's speed hostage?

### Tekst z obrazka

&) Unleashing Quality
with Playwright Automation
test ("Handle dynamic UL rendering without flaky hardcoded sleeps’, async ({ page }) => {
await page. goto (‘/analytics');
ll Trigger an asynchronous backend data crunching operation
await page. getByRole ‘button’, { name: ‘Compile Global Metrics }).click();
JI" X The Lazy Fix: await page. waitForTimeout (5000);
/1 The Battle-Tested Move: Sync directly with the DOM state deterministically
1] Playwright polls the DOM automatically, proceeding the exact millisecond it's hidden
await expect (page. getByTestTd (‘processing spinner’). toBebidden ({ timenst: 15000 }); } el
JI Plaguright's auto-waiting will now safely click the button the moment it becomes actionable
await page. getByRole (‘button’, { name: ‘Export CSV'}). click ();
await expect (page. getByRole (‘alert’). toBeVisible(); [/
bs
EE
+ Follow me Save this §
IN for more n this post ["] Like & Comment ¢# Share

---

## 10. Chasing 100% test coverage is quietly killing your engineering velocity.If your primary metric for Q…

### Tekst postu

Chasing 100% test coverage is quietly killing your engineering velocity.If your primary metric for QA success is hitting 100% automation coverage, you aren't fighting bugs. You are building a massive, flaky monster that your developers will eventually learn to ignore.Trying to assert every single UI element, error message, and obscure edge case through browser automation leads straight to three things:🔴 False sense of security - brittle tests that only check if a button exists do not prove your app is healthy. They just prove nobody moved the button.🔴 High maintenance, low value - you burn 80% of your time maintaining tests for edge cases that hit 0.01% of users.🔴 CI/CD Bottlenecks - Your regression suite takes 45 minutes to run, killing developer deployment velocity.The modern QA stack is not about automating everything. It is about automating smartly.Successful teams use a strategic 3-Layer Framework:Layer 1: TypeScript for Static & Contract ValidationStop using heavy browser instances to test data structures or missing properties. Leverage strict TypeScript interfaces to catch API contract changes and data mismatches right in the IDE or at the unit/component level before a browser even opens.Layer 2: Playwright for High-Value Functional JourneysRuthlessly restrict your E2E browser automation to critical revenue-generating flows (e.g., Authentication, Checkout, Core Workflows). Keep these tests blazing fast, bypass the UI for test setup using Playwright’s native API capabilities, and shard execution to keep pipeline times under 10 minutes.Layer 3: Visual AI for Layout HealthStop writing 50 line-by-line DOM element assertions to check if text, colors, and images are rendered correctly. Replace them with a single Visual AI baseline check. Let machine learning distinguish between dynamic data updates and actual visual degradation or broken CSS.Quality is about risk mitigation and feedback speed, not vanity metrics.Save the chart below to help convince your stakeholders it is time to delete low value tests and focus on speed.

### Tekst z obrazka

&) Unleashing Quality
with Playwright Automation
100% COVERAGE vs MODERN QA
THE 100% COVERAGE TRAP STRATEGIC MODERN GA |
\Y,
' 7
S yi 1%
SSE HA |
% Bloated Playwright suites Vv Critical user journeys |
X Slow CI/CD pipelines V' Strict TypeScript pattems
X Flaky low-value tests V' Al-driven observability
% High maintenance overhead 10-minute feedback loops
£06 Fotowime Rn idles ils Like © Comment Share

---

## 11. Stop burning CI credits on a checkout flow that was doomed before it even started.I learned this one…

### Tekst postu

Stop burning CI credits on a checkout flow that was doomed before it even started.I learned this one the expensive way. A downstream service was down, and every checkout test still spun up a browser, walked half the flow, then timed out. Multiply that by retries and parallel shards and one bad deploy quietly ate a big chunk of our CI minutes for the day.The fix was embarrassingly small. Before Playwright boots a single browser, hit a health endpoint and fail the whole suite in one cheap request.🔸 `test.beforeAll` calls `request.get('/api/health')`🔸 `expect(response.ok()).toBeTruthy()` with a loud abort messageIf the core service is offline, the suite stops in under 200 milliseconds instead of grinding through heavy UI sessions until every test hits its timeout (see image).That one gate protects your cloud bill, kills false-alarm flakes, and keeps the feedback loop fast for the whole team.🚩 The anti-pattern is letting the suite spin up multi-tab browser sessions and attempt full UI checkouts while a payment gateway is entirely down. You get cascade timeouts and burned credits for zero signal.A failing dependency is an environment problem, not a test failure. Catch it at the door.Do you use fail-fast API gates in your automation pipeline, or do you let your tests run to their maximum timeout threshold when downstream environments crash?

### Tekst z obrazka

&) Unleashing Quality
with Playwright Automation
|
|
test. beforeAll (async. ({ request }) => {
Const response = await (request. get (/api/ health); 11 |
expect (response.ok(), ‘Critical backend dependency is offline! Aborting suite). toBeTruthy ();
i i} |
test (‘Execute expensive multi-step checkout pipeline’, async ({ page 3) => { |
await page. goto ('/checkout'); |
CRITiAL |
const premiumBanner = page. getByRole ‘region’, { name: ‘Premium Perks’ 3); PATH |
await expect (premiumBanner. getByTestTd (‘unlock-status'). toHaveText ('FOLLOW_FOR_MORE TIPS");
await, expect (premiumBanner.. getByRole ‘button’, { name: ‘Subscription Fee’ 13). toaveText (‘4 Repost’);
bi
EE
+ Follow me Save this -
IN for more n this post ["] Like & Comment ¢# Share

---

## 12. You need to revoke a user's access mid-session and prove the block works without restarting the brow…

### Tekst postu

You need to revoke a user's access mid-session and prove the block works without restarting the browser.Here is the challenge. Test an Admin changing a regular user's permission settings, then immediately verify that user loses access. No browser restart. No logout. No login dance.Most engineers reach for the slow path. Log in as the Admin in one session. Make the change. Click logout. Log back in as the User through the UI. Assert the result.That sequence is brittle and painfully slow. Every extra login round trip inflates your CI runtime and adds another step.Playwright already solves this. You can run two isolated browser contexts inside a single test, each loading its own authenticated state.🔸 One context boots as the Admin from `state/admin.json`🔸 A second context boots as the User from `state/user.json`The Admin changes the permission. The User hits the protected route in parallel. You assert the access change in real time (see image).No logout. No re-login. Two roles with fully isolated cookies and storage, living side by side in one test.This is how you catch permission sync bugs the instant they happen. And you slash a multi-minute multi-role flow down to seconds.How do you approach multi-role testing in your E2E suites? Do you orchestrate multiple active browser contexts at once, or do you stick to sequential login and logout UI flows?

### Tekst z obrazka

&) Unleashing Quality
with Playwright Automation
test (‘Admin revokes access and user is blocked instantly’, async ({ browser }) => {
const adminContext = await browser. newContext ({ storageState: ‘state /admin. json J}
const adminPage = await adminContext. newPage();
const userContext = await browser. newContext ({ storageState : ‘state Juser. json’ 1); Key
const userPage = await userContext. newPage(); ———— HVE
await adminPage. goto ('/admin /users/42');
await adminPage. getByRole ( ‘button’, { name: ‘Revoke premium access’ }).click(); [|
await expect (adminPage. getByTestId ( ‘access -status')). toHaveText (‘Standard’); °°
await userPage. goto (/premium /insights');
await expect. soft (userPage. getByTestId ('pagunll-alert’)). toBeVisible();
await expect. soft (userPage. getByRole ( ‘button’, { name: ‘Upgrade to premium’ 1). toBeVisible();
await adminContext. close ();
i . D Flow:
A i)
hi
+ Follow me Save this -
JN for more n this post [J Like @ Comment Share

---

## 13. Stop digging through 300-line stack traces when a 50-step E2E test fails in CI.This is the question …

### Tekst postu

Stop digging through 300-line stack traces when a 50-step E2E test fails in CI.This is the question that separates script writers from framework architects. When a massive end-to-end flow blows up midway, how do you pinpoint the failure without manually tracing every await before the crash?Most engineers write the test as a continuous stream of 50 uncommented commands. The HTML report fires back a generic line number, and now you are reverse engineering the application state.🚩 50 awaits in a row with no logical boundaries🚩 The report gives you a line number, not a business stepPlaywright's `test.step()` fixes this completely.Wrap each logical chunk of the flow inside a `test.step()` block (see image). Each step gets a clear, human-readable label inside your HTML report. When a step fails, the report points to the exact business intent that went under, not a raw line of code.`await test.step('Step 2: Hit the algorithm with engagement', async () => { ... })`This is a deliberate design choice. You stop debugging code lines and start debugging business flows. Your test report becomes a stack trace at the user journey level, not the syntax level.How do you break down long E2E user journeys? Do you rely on modular `test.step()` blocks to keep your reports clean, or do you prefer chopping them into tiny, disconnected test files?

### Tekst z obrazka

&) Unleashing Quality
with Playwright Automation
| test (‘Complex multi-step user onboarding Flow’, async ({ page 1) => {
await test. step (‘Step 1: Locate high-value content’, async () = {
await page. goto ('/feed’);
await expect (page. getByRole (‘heading', { name : ‘Top Tech Tips" })). toBeVisible ();
Bn;
| await (Rest. step (‘Step 2: Hit the algorithm with engagement?) async () => { J rhe
await page. getByRole ‘button’, { name: ‘Like Post’ }).click();
await page. getByTestTd('repost-action-btn'). click ();
await expect (page. getByTestTd ("engagement- counter). toHaveTet (‘Over 9000'); If
Bs a me
| await test. step (‘Step 3: Secure future enlightenment’, async () => {
await page. getByRole (‘button’, { name: ‘Follow Creator’ }). click();
| await expect (page. getByRole (‘alert’). toHaveText ("You are now enlightened |");
bs
|B;
——_—— ee ——
+ Follow me Save this §
£& for more n this post [J Like @ Comment Share

---

## 14. Stop debugging multi-field pages one assertion at a time.You know the loop. Test fails. Fix the one …

### Tekst postu

Stop debugging multi-field pages one assertion at a time.You know the loop. Test fails. Fix the one broken field. Rerun. Find the next broken field. Fix it. Rerun again.With hard `expect()` chains on independent UI elements, every failure stops execution immediately. You are debugging one issue at a time when five might be broken simultaneously. Your CI is lying to you by design.Playwright's `expect.soft()` changes this completely.Soft assertions collect every failure and report them all at the end of the test run. Execution continues even when an assertion fails partway through.🚩 Avoid this pattern on independent fields:🚩 `await expect(page.locator('.checkout-summary > div:nth-child(2)')).toHaveText('Engagement Fee')`A dev adds one extra wrapper div and your entire test bleeds red for the wrong reason.Here is the soft approach with semantic locators (see image).Every soft assertion runs. Every failure is logged. You fix them all in one shot instead of one rerun at a time.This radically slashes debugging time and keeps your CI feedback loop fast.`expect.soft()` is not a workaround. It is a deliberate design choice when you need to audit every field on a page in a single pass.How do you handle multi-field validations in your automation framework? Do you let them run soft, or do you prefer breaking them out into isolated micro-tests?

### Tekst z obrazka

&) Unleashing Quality
with Playwright Automation
test (‘Validate checkout Summary fields without halting on first failure’,
async ({ page }) => {
await page. goto ('/checkout /summary’);
const summaryRegion = page. getByRole ('region’, { name: ‘Order Summary’ }); + SCOPE
Bi AA ht 1!
await expect. soft (summaryRegion. getByRole ‘cell’, { name: ‘Engagement Fee 1). foHaveTest (‘1 Repost’);
await expect. soft (summaryRegion. get ByTest Td ('like-button-status')) . doHaveText (Clicked);
await expect. soft (summaryRegion. getByRole ("heading { mame: ‘Framework Vale’ 1). doHaveText ( Priceless’);
Hs
SOFT ASSERT
+ Follow me Save this :
£§ for more n this post ["] Like & Comment ¢# Share

---

## 15. This is the classic async race condition trap interviewers use to filter out junior Playwright candi…

### Tekst postu

This is the classic async race condition trap interviewers use to filter out junior Playwright candidates.You click a link. A new tab opens. You try to interact with it.In CI, your test flakes with a timeout. Locally it passes every time.The culprit is execution order.Most engineers write it like this:🚩 Click fires before the event listener registers🚩 New tab opens before Playwright starts watchingBy the time `context.waitForEvent('page')` is called, the browser has already opened the tab and the event is gone. The promise times out.The fix is one JavaScript concept applied deliberately: register the listener and fire the trigger at the same time.`Promise.all` locks both into the same tick. `context.waitForEvent('page')` registers first, then the click fires. The race condition is permanently closed.In the snippet, the listener and the click are wrapped together inside `Promise.all`. The new page reference drops out of the destructured result. You can assert the URL, read headings, or run the full verification flow from there.This pattern works for any multi-tab scenario. External billing portals. PDF preview tabs. OAuth pop-up windows. If your app opens it, you own it.CI runs slower than your local machine. That timing gap is exactly where this race condition lives.Now you can answer it like a senior.

### Tekst z obrazka

&) Unleashing Quality
with Playwright Automation
import { test, expect } from '@playwright/test';
test (“Handle popups and new tabs without race conditions’, async ({ context, page}) =>{
await page . goto ('/dashboard');
// The “Architect” Move: Orchestrate the listener and the trigger simultaneously
oy Const Drewhage] = awit (Promise all [ I
MOVE context. waitForEvent (‘page), <— >
page. getByRole('link’, { name: ‘View Invoice (PDF) }). click(),
iH
| // Safely interact with the newly opened tab
| await expect (newPage) . toHaveURL (/.*\/inveice\/123/);
| await expect (newPage. locator (*h1')). toContainText (‘Invoice Details’);
Be
EEE —————
+ Follow me Save this -
0H Sore Rn this post of Like © Comment Share

---

## 16. Your status check passes, but the data contract is broken underneath.`expect(response.ok()).toBeTrut…

### Tekst postu

Your status check passes, but the data contract is broken underneath.`expect(response.ok()).toBeTruthy()` only tells you the server didn't crash. It says nothing about whether the data your frontend depends on actually matches the agreed contract.If the backend silently converts a required field to `null`, or returns extra unmapped keys, your 200 OK assertion still passes. Your frontend breaks in production instead.This is the Schema Sniper pattern, modernized for Zod 4.Define your contract with `z.strictObject`. Declare each field using top-level Zod 4 format functions like `z.uuid()` and `z.email()`. Then call `userProfileSchema.parse(await response.json())` right after the request. That single line validates the shape and returns a fully typed object.`z.strictObject` fails the test immediately if the backend adds a rogue field or silently changes a type. No more hunting through obscure UI errors to trace a silent API drift.Are you validating the structural schema of your API responses, or just trusting the status code?

### Tekst z obrazka

&) Unleashing Quality
with Playwright Automation
import { 2 } from ‘zd’;
const user Profile Schema = @-skrictObject «)
Elda,
id: zouuid(), «e—— TOP-LEVEL FORMAT
email: z.email(),
vole: z. enum ([ ‘admin’, ‘member']),
preferences: z. strictObject ({
theme: 2. string (),
notifications: z.boolean(),
b,
bi
Const response = await page. request. get ("/api/users/me’);
const user = userProfileSchema. parse (await response. json); schema |]
SNIPER 00
expect (user. role). toBe (‘admin’);
+ Follow me n Save this :
IN Hbre this post ls Like © Comment ¢ Share

---

## 17. Stop writing while loops to wait for a database update. You’re over-engineering your own stress.We'v…

### Tekst postu

Stop writing while loops to wait for a database update. You’re over-engineering your own stress.We've all been there: you trigger an action like placing an order, but the backend needs a few seconds to process it.The old way usually looks like a messy while loop with `page.waitForTimeout(1000)` inside. It's brittle, hard to read, and leads to flaky tests whenever the environment runs slow.The Playwright way is `expect.poll`. It's a built-in "smart waiter" that retries your logic until a condition is met, with configurable intervals and a clear timeout.Here's how it works: create the order, then pass an async callback to `expect.poll` that fetches the order list and returns the status of your specific order. Add a backoff strategy via `intervals`, a `timeout`, and a clear `message` for failures. Chain `.toBe('COMPLETED')` at the end. That's it.No custom retry logic. No guessing how long to sleep. `expect.poll` handles the polling, the intervals, and the assertions in a single readable block.🚩 Anti-pattern to avoid: the "sleep and hope" method calls `page.waitForTimeout(5000)`, then checks the status in a separate assertion.

### Tekst z obrazka

5 [ 17 8

// 1. Create the order
const response = await page. request. post ('/api/orders’, {

data: { item: 'Mechanical Keyboard’, quantity: 1 }
bs;
const { orderId } = await response. json();
// 2. # The “Smart Waiter" (expect. poll)
await Expect. poll) (async () => {

const ListResponse = await page.request.get ('/api/orders');

const orders = await ListResponse. json();

// Find our specific order and return its status

const order = orders. find (

) (0: { id: string; status: string }) => o.id === orderId

return order?. status;
bhi

message: ‘Order ${orderTd} should eventually be COMPLETED,

intervals: [1_000, 2_000, 5.000], —————————5 BACKOFF STRATEGY

timeout: 15.000, //
1). toBe (COMPLETED);

L. KEY ASSERTION

---

## 18. I tested a New Year's Eve countdown in the middle of May without touching the server clock.Time-depe…

### Tekst postu

I tested a New Year's Eve countdown in the middle of May without touching the server clock.Time-dependent UI is one of the hardest things to test reliably. Expiring banners, countdown timers, session timeouts, "Early Bird" pricing cutoffs. They all depend on the clock, and the clock is not your friend.Most teams do one of two things when they need to test time-sensitive logic.🚩 They call page.waitForTimeout(10000) and literally wait for the timer to expire🚩 They SSH into the Staging server and manually shift the system clockBoth are painful. One bloats your suite runtime. The other terrifies everyone who shares that environment.Playwright has a built-in answer: the Clock API.With page.clock.install() you freeze time at any arbitrary point. Then page.clock.fastForward() jumps the clock forward by exactly the amount you need, instantly, no real waiting required.Here is what it looks like in practice. I freeze the clock to 10 seconds before midnight on New Year's Eve, confirm the discount timer reads 00:10, then jump 11 seconds into the future and assert the UI marks the offer as expired and disables the buy button.The whole thing runs in milliseconds.This also works for throttling setInterval callbacks, validating JWT expiry, testing cron-driven UI updates, and any other logic that relies on Date.now(). If it depends on the clock, page.clock owns it.Are you still hacking your server clock to test dates, or have you started using the Playwright Clock API?

### Tekst z obrazka

import { test, expect } from '@ playwright /test';
test ('Fast-forward time to test expiring discount banners’, async ({ page 3) = {
11. The "Time Traveler” move: Freeze time to 10 seconds before expiry
1] No more waiting for “real” time or hacking the server clock
await page. clock. install ({ time: new Date ('2025-12-31723:59:50') });
await page. goto (*/shop');
I Verify the banner is still active
await expect (page. locator ('. discount-timer')). toContainText ('00:10');
// 2. Jump 11 seconds into the future instantly
TIME JUMP
await page. clock. fast Forward ('00:11'); —L re
//'3. The clock has struck midnight. Verify ih correctly.
await expect (page. locator ('. discount~ timer')). toContainText (‘Expired’);
await expect (page. locator ('. buy-row-btn')). toBeDisabled (); 1
Ds i

---

## 19. Stop relying on 'perfect data' to test your UI.Most teams spend 10 minutes seeding a database just t…

### Tekst postu

Stop relying on 'perfect data' to test your UI.Most teams spend 10 minutes seeding a database just to verify if a single banner shows up. Then 10 more minutes cleaning state when the test breaks halfway.🚩 Manual test users🚩 Dirty staging environments🚩 Flaky waits for data to syncThis is test data fatigue. It kills velocity and confidence.There is a smarter way. Hijack the network and decide exactly what your UI sees.Playwright gives you page.route plus route.fulfill. Intercept the API call, fetch the real response, modify it on the fly, return whatever scenario you need.In the snippet, a standard user response gets rewritten with subscription PREMIUM and a list of VIP features. The dashboard renders premium without touching the database.Want to test an expired subscription? Mock /api/v1/billing/status and return expired.Want a 500 error? Return an error response in milliseconds.Want an empty state? Return [] and watch the UI react.

✅ No database seeding✅ No test data fatigue✅ No staging dependency✅ Deterministic, reproducible scenariosYour tests stop being data-dependent and start being state-driven. That is the difference between writing tests and architecting them.Do you prefer mocking the network for UI tests, or do you still believe in full end-to-end database integration for every scenario?

### Tekst z obrazka

test (‘Test Premium UT features by hijacking the network’, async ({ page }) => {
// 1. The "Hidden Gem": Intercept the profile API call
await page.route ('#¢ /api [user profile’, asyne (route) => { | CRITICAL sTEP
Const response = await route. fetch();
const json = await response. json();
// 2. Modify data on the fly — turn a ‘Standard’ user into a 'VIP'
await route. fulfill ({
json: {
.. json, //
subscription: ‘PREMIUM’, !!
features: ['ai_tools’, ‘unlimited exports]
}
)
bs
// 3. Navigate +o the dashboard
await page. goto ('/dashboard’);
// The UT now renders as if we are a VIP, regardless of the real DB state
await expect (page. getByTestId ('premium-badge')). toBeVisible() ;
bs

---

## 20. The Page Object Model is one of the most misapplied patterns in test automation. Not because teams d…

### Tekst postu

The Page Object Model is one of the most misapplied patterns in test automation. Not because teams don't understand it, because they apply it to everything.➡ This is part 4 of my Playwright Tutorial series. Last week I covered fixtures. Today: the Page Object Model.I've seen page objects with a dedicated method for every single action. Click a button? Method. Fill one input? Method. Check a label that appears in exactly one test? Method.The result is a page object with 80 methods and a test file you can't read without having the page object open on the other monitor.POM serves two things: reuse and readability. When every method is called exactly once, you've achieved neither.The rule I follow is simple:🔸 If an action is shared across tests, it belongs in the page object.🔸 If an action only exists in one test, it belongs in the test.Keep it visible. Keep it where the reader's eyes already are.Follow along for the next post in the Playwright Tutorial series.Where do you draw the line between what goes in the page object and what stays in the test?

---

## 21. Your UI test suite should protect user journeys, not mirror your ticket board.That difference decide…

### Tekst postu

Your UI test suite should protect user journeys, not mirror your ticket board.That difference decides whether automation helps the team or slows it down.I've seen this pattern too many times:A ticket gets built.A UI test gets added.Another ticket gets built.Another UI test gets added.It feels responsible.It is not.It is like checking every room in a building by walking the entire building each time you replace one light bulb.You can do it, but soon the inspection takes longer than the work.The cost shows up fast:1. Every small rule change becomes another brittle UI scenario2. CI slows down3. Flaky tests start hiding real failures4. The team stops trusting the suiteThat is not quality engineering.That is test debt with a nice report.The better approach is simple:✅ Business logic: API and integration tests.✅ Critical flows: E2E UI tests.✅ Ticket edge cases: test them at the lowest layer that proves the risk.A good E2E suite should answer: "Can our users still complete the core things?"It should not answer: "Did we automate the ticket board?"Where do you draw the line between API/integration coverage and E2E UI coverage?

---

## 22. Your beforeEach works. Until you need the same setup in 4 different test files.➡ This is part 3 of m…

### Tekst postu

Your beforeEach works. Until you need the same setup in 4 different test files.➡ This is part 3 of my Playwright Tutorial series. Last week I covered tags. Today: fixtures.Think of a fixture as a helper you attach to your test. It creates what the test needs, hands it over, and cleans up when the test is done.Every fixture has three parts:1. Setup: create the test data (via API, database, or UI)2. use(): pass whatever you created to the test, the test runs here3. Teardown: everything written after use() runs automatically when the test finishesThat last part is the one most people miss. Cleanup is built in. No afterEach. No risk of forgetting it.Here is how this looks in practice. The fixture calls the API to create an order. The test receives the order ID, already in the database, ready to use. When the test finishes, the fixture deletes the order automatically.✅ Your test gets clean data. Your database stays clean after.And because a fixture lives in a shared file, you can import it into any test file in the project. Write the setup once. Use it everywhere.Three things that make fixtures better than hooks for this:1. Reusable across any test file, not just the one they are defined in2. Teardown is guaranteed, it runs even if the test fails3. They compose, a fixture can use other fixtures as inputThat third point matters more than it sounds. Your order fixture can use Playwright's built-in request fixture. Another fixture can build on top of yours. You add layers instead of duplicating setup.Follow along for the next post in the Playwright Tutorial series.What test data do you currently set up and tear down manually that a fixture would handle for you?

### Tekst z obrazka

#### Obraz 1 (`image20.png`)

fixtures ts
type Fixtures = {
orderId: string;
Yi
export const test = base.extend<Fixtures>({
orderTd: async ({ request }, use) => {
/1 Setup: create an order via APT
const response = await request.post("/api/orders”, {
data: { product: "Laptop", quantity: 1},
bi
const { id } = await response.json();
await use(id); // hand the order ID to the test
// Teardoun: runs automatically after the test finishes
await request.delete(’/api/orders/${id}');
h
Bb;

#### Obraz 2 (`image21.png`)

test(*order appears in the dashboard", async ({ page, orderld }) => {
await page.goto(" /orders/S{orderld}');
await expect(page.getByText(*Laptop)). toBevisible();

<

---

## 23. Writing detailed, step-by-step manual test cases in Jira, TestRail, or any TCMS your team uses in 20…

### Tekst postu

Writing detailed, step-by-step manual test cases in Jira, TestRail, or any TCMS your team uses in 2026 is a massive waste of engineering time.Step 1: Open the browser.Step 2: Navigate to the login page.Step 3: Enter a valid email address.Step 4: Click "Sign In".Expected result: User is redirected to the dashboard.Your developer just renamed that button to "Log In".Your test case is already wrong. Your TCMS doesn't know. Nobody updated it.This is what detailed manual test documentation looks like in practice. It's outdated the moment someone merges a PR. I've watched QA engineers spend half a sprint writing test cases that nobody read. Not developers. Not product owners. Not even the QA engineer who wrote them two months later.The documentation exists. The confidence is fake.Here's the thing: you don't need to stop having test steps in your TCMS.You need to stop writing them there.In one of my current project, our Playwright tests look like this:test step('GIVEN the user is on the checkout page', ...)test step('WHEN the user applies a discount code', ...)test step('AND confirms the order', ...)test step('THEN order confirmation is displayed', ...)These steps are visible in our TCMS. Automatically. Because the test is linked directly in the code.One annotation. That's it.Take Qase as an example. Two lines at the top of your test:qase suite('Checkout Flow') // <-- This is the suite name in the TCMSqase id(123) // <-- This is the test case ID in the TCMSYour test case is registered. Your steps are synced. When the flow changes, the developer updates the code. The TCMS reflects it.You write the steps once. In the place where they actually run.Not once in TestRail or wherever your team manages tests. Then again in your automation file. Then again when someone renames a button.The steps live in the code. Your TCMS just shows them.The integration already exists. Most teams just don't use it.When did you last update a test case in your TCMS and your Playwright test separately because someone changed a label?

---

## 24. 500 tests. You only need to verify one feature. You run all 500. There is better way.Without tags, e…

### Tekst postu

500 tests. You only need to verify one feature. You run all 500. There is better way.Without tags, every pipeline run is all-or-nothing. That's not a strategy.➡ This is part of my Playwright Tutorial series.No tags means no control. You either run everything or you grep test names or files and hope for the best.Tags give you a surgical option.These are the three conventions worth adopting:🔸 @smoke - critical paths only. Runs on every commit.🔸 @regression - full feature coverage. Runs before release.🔸 @slow - tests that take over 30 seconds. Excluded from fast pipelines.You can also combine them.Run smoke OR regression:npx playwright test --grep "@smoke|@regression"Exclude slow tests entirely:npx playwright test --grep-invert @slowA test suite without tags is just a list of files. Tags turn it into a tool.Follow along for the next post in the Playwright Tutorial series.What tag conventions does your team use, or are you still running the full suite every time?

### Tekst z obrazka

Playwright Tutorial Series
Playwright Tagging: From "All-or-
Nothing" to Surgical Control
Master tags to isolate tests, speed up CI/CD, and gain
precise execution control.
Adopt the "Big Three" Conventions
® @smoke ® @regression & @slow
Critical paths Full coverage Tests over 30s
(every commit) (releases)
Tags provide a surgical option to filter, group, and selectively
execute tests based on priority and feature area.
Modern Syntax for Clean Code
Apply tags via the details object in "test" or ‘test.describe’
blocks for better reporting.
// Single Tag
test('login', { tag: '@smoke' }, async ({ page }) => {});
// Multiple Tags
test('checkout', { tag: ['@regression', '@checkout'] },
async ({ page }) => {});
// Describe Block (Inherited)
test.describe('Admin', { tag: '@admin' }, () => {
test('create user', async ({ page }) => {}); // Has @admin
bi
Filter Execution with CLI Grep
Use "--grep’ for logical OR/AND runs and *--grep-invert’ to
skip specific subsets.
# Run Smoke OR Regression
npx playwright test --grep "@smoke|@regression"
# Exclude Slow Tests
npx playwright test --grep-invert @slow
A NotebookLM

---

## 25. If your Playwright tests have the same 10 lines of setup code copied into every single test, you're …

### Tekst postu

If your Playwright tests have the same 10 lines of setup code copied into every single test, you're doing it the wrong way.Playwright gives you 4 hooks to handle setup and teardown cleanly.➡ This is part of my Playwright Tutorial series.Hooks are functions that Playwright runs automatically at specific points in your test suite. You don't call them manually. You define them once and Playwright handles when they fire.There are 4 of them. Each one has a specific job. Pick the wrong one and your tests start bleeding into each other.1. beforeAllRuns once before all tests in the block. Too expensive to repeat per test? Put it here.test.beforeAll(async ({ request }) => {await request.post('/api/users', {data: { name: 'Test User', email: 'test@example.com' },});});2. afterAllRuns once after all tests finish. Whatever beforeAll created, afterAll cleans up.test.afterAll(async ({ request }) => {await request.delete('/api/users/test@example.com');});3. beforeEachRuns before every single test. Use it to create data specific to that test, not shared with others.let orderId: string;test.beforeEach(async ({ request }) => {const res = await request.post('/api/orders', {data: { product: 'Laptop', quantity: 1 },});orderId = (await res.json()).id;});4. afterEachRuns after every single test. Use it to clear per-test state so the next test starts clean.test.afterEach(async ({ request }) => {await request.delete(`/api/orders/${orderId}`);});🔸 The rule: shared setup belongs in beforeAll/afterAll. Per-test setup belongs in beforeEach/afterEach.Getting this wrong is one of the fastest ways to introduce flaky tests.Follow along for the next post in the Playwright Tutorial series.Which hook do you reach for most in your Playwright projects?

### Tekst z obrazka

#### Obraz 1 (`image23.png`)

beforeAlL
Runs once before all tests in the block. Too expensive to repeat per test? Put it
here.
test.beforeAll (async ({ request }) => {

await request.post('/api/users’, {

data: { name: 'Test User’, email: 'test@example.com' },

Bi

hn;

#### Obraz 2 (`image24.png`)

afters
afterall
Runs once after all tests finish. Whatever beforeAll created, afterAll cleans up.
| test.afterAlL(async ({ request }) => {
await request.delete('/api/users/test@exanple.con');
5g

#### Obraz 3 (`image25.png`)

beforeEach
Runs before every single test. Use it to create data specific to that test, not
shared with others.
Tet orderId: string;
test.beforeEach(async ({ request }) => {

const res = await request.post('/api/orders’, {

data: { product: ‘Laptop’, quantity: 1},

bh;

orderId = (await res.json()).id;
bn;

#### Obraz 4 (`image26.png`)

oe aterEach
afterkach
Runs after every single test. Use it to clear per-test state so the next test starts
clean.
test.afterEach(async ({ request }) => {
await request.delete("/api/orders/${orderId}");
i

---

## 26. lesson ABOUT ARRAYS AND OBJECTS IN TYPESCRIPT

### Tekst postu

lesson ABOUT ARRAYS AND OBJECTS IN TYPESCRIPT

---

## 27. Every course I took taught me XPath. Nobody mentioned it was the worst locator you could write.I spe…

### Tekst postu

Every course I took taught me XPath. Nobody mentioned it was the worst locator you could write.I spent months writing //div[@class='btn-submit'] and thinking that was just how automation worked.Nobody told me there was a clear hierarchy.Here it is, best to worst:✅ 1. Accessibility locators (getByRole, getByLabel) - what real users interact with✅ 2. Text locators (getByText, getByPlaceholder) - tied directly to visible content✅ 3. data-testid - explicit, controlled, decoupled from styling🟨 4. CSS selectors - fragile when design changes❌ 5. XPath - forbidden. Not a last resort. Off the table entirely.The reason this order works: the higher you go, the less likely it breaks when the app changes.Accessibility locators survive a full redesign. XPath breaks when someone adds a wrapper div.

---

## 28. Ivan Davidov

### Tekst postu

Ivan Davidov

if your E2E tests take forever, it is usually because you do everything through the browserlogging in, opening menus, filling forms, clicking save. all that just to create the data your test actually needsthat is like driving to the store to buy a pen so you can write your shopping list. the test has not even started yetthere is a faster way. hit the backend API directlySetup your test in the following way:1️⃣ Arrange: use request to inject the data in milliseconds2️⃣ Act: open the page like a user would3️⃣ Assert: check the user actually sees the databonus, wrap each phase in its own test.step with GIVEN, WHEN, THEN names. your report reads like a sentence instead of a stack traceinstead of 10 clicks and 5 page loads, one POST call. the data exists so you can move on to the important part - testing✅ fewer UI steps, less flakiness✅ setup finishes in milliseconds, not seconds✅ you test the feature, not the data entry formjoke aside, every UI step in your Arrange phase is one more place your test can die before it tests anythingcode below - create a product via API, then check the UI displays ithow many login forms did your suite fill out today? be honest, I will wait

### Tekst z obrazka

_[GIF — OCR pominięty]_

---

## 29. many test suite i have seen starts the same way. open the app, type the email, type the password, cl…

### Tekst postu

many test suite i have seen starts the same way. open the app, type the email, type the password, click login, wait for the redirect. multiply that by 50 tests and you are burning real time on a screen that has nothing to do with what you are actually testing.the fix is a setup stage. Playwright calls it a "project dependency". it runs once before your actual tests even start.here is how it works1. hit the login API directly and log in from the UI2. validate the response (i used Zod for schema validation) or the success of the login3. save your cookies and local storage to a session file and your token as env variable4. every other test loads that file and token and skips the login screenyour tests start exactly where the user lands after login. the login screen never touches your test results again. plus you get a token for that user for all API requests.in my setup i used a custom API abstraction, Page Object Model as fixtures, Zod for response validation, and dotenv for environment variables. but you don't need any of that. Playwright has built-in support for storage state and API requests out of the box. the pattern works the same.so, session file or fresh login before every test? be honest, I will wait.

### Tekst z obrazka

_[GIF — OCR pominięty]_

---

## 30. 90% of your Playwright tests prove Playwright works, not that your app worksyou write a test. it cli…

### Tekst postu

90% of your Playwright tests prove Playwright works, not that your app worksyou write a test. it clicks a button, waits for a spinner, asserts the next page shows up. green. ship it. another flawless release, most probablycongrats. you just proved Playwright can click and the DOM can render. Microsoft already tested that part, for free, and they even wrote the docs.that "green" suite is checking that your car doors open. nobody asked if the car actually driveswhat you did NOT test? is the total correct. did the discount apply. did the right person get charged. is the order in the database, or just glowing on the screen for the demo.we got addicted to asserting mechanics because mechanics are easy. toBeVisible() never asks a hard question. it nods, goes green, and lets everyone go home earlythe button being clickable is not the risk. the wrong number showing up after you click it is the risk. but sure, the button rendered, great work teamjoke aside, run the gut check on your suite:🚩 assertions on visibility, existence, URLs🚩 assertions on values, totals, state, datafirst pile bigger? congratulations, you built a very expensive way to confirm the internet still loads pagesthe fix is not more tests. it is meaner assertions. check the number, not the pixels

### Tekst z obrazka

_[GIF — OCR pominięty]_

---

## 31. you click a link, a new tab opens, and your test needs to assert something inside it. easy, right?th…

### Tekst postu

you click a link, a new tab opens, and your test needs to assert something inside it. easy, right?then you write it like this and watch it fail half the time```await page.getByText("Open Support").click()const newPage = await page.waitForEvent("popup")```the problem is timing. the click fires and the tab opens before you even start listening for the popup event. by the time waitForEvent runs, the event already came and went.so you sit there registering a listener for something that already happened. the test hangs until it times out.the fix is to start listening BEFORE the click, and run both at the same time.```const [newPage] = await Promise.all([page.waitForEvent("popup"),page.getByText("Open Support").click(),])```Promise.all kicks off both operations together. the listener is armed first, then the click triggers the tab. no gap for the event to slip through.now newPage is the handle to the second tab. you treat it like any other page.`await expect(newPage).toHaveTitle("Support Center")`one detail people miss. waitForEvent("popup") catches both new browser tabs and window.open popups. same pattern for both.this is the same shape you use for downloads and file choosers too. arm the listener, then trigger it, inside one Promise.all.how do you handle new tabs in your suite today?

### Tekst z obrazka

_[GIF — OCR pominięty]_

---

## 32. if you are testing functional logic like a signup or newsletter form, you don't need to wait for hig…

### Tekst postu

if you are testing functional logic like a signup or newsletter form, you don't need to wait for high resolution marketing images to loadbut by default, Playwright waits for the page to fully load. that includes every banner, hero image, and background assetthis slows your tests down. and if those images come from a slow CDN or third party, it makes them flaky toothe fix is simple. use page.route to intercept and abort requests you don't care about`await page.route('**/*.{png,jpg,jpeg,svg,gif}', route => route.abort())`that one line tells Playwright to immediately kill any image request before it even startsyour page still renders. your buttons, forms, and text are all there. you just skip the heavy stuff that does not matter for your test- faster test execution- less flakiness from slow or unreliable CDNs- cleaner test focus on what actually mattersthis works for more than images. you can abort analytics scripts, tracking pixels, video preloads, or any request pattern that is not relevant to the behavior you are validatingtry it in your suite and share how much time it saves you

### Tekst z obrazka

_[GIF — OCR pominięty]_

---

## 33. waitForTimeout(5000) burns 5 full seconds even when your state was ready in 200ms. and it still flak…

### Tekst postu

waitForTimeout(5000) burns 5 full seconds even when your state was ready in 200ms. and it still flakes when the backend is slow and needs 5500ms.a backend status flips from "Pending" to "Shipped" after a queue picks it up. you need to wait, but you do not know how long. so a fixed sleep is always wrong in one direction or the other.use expect.poll. it runs your check on a loop, returns the moment the condition passes, and only fails if the timeout hits. no fixed sleep, no guessing the magic number.the snippet below ships an order through the UI, then polls the API until the status reads "Shipped". the assertion and the retry logic live in one place.how do you usually wait for the backend response or async state?

### Tekst z obrazka

_[GIF — OCR pominięty]_

---

## 34. 1 minute #Playwright tip to stop copy pasting the same test helpermost page helpers start clean. the…

### Tekst postu

1 minute #Playwright tip to stop copy pasting the same test helpermost page helpers start clean. then you need one that skips the newsletter signup. so you copy it. then one that does not accept terms. copy again. now you maintain four helpers that do almost the same thing.optional and default params fix this.add a ? to a param and it becomes optional. callers can leave it out and it arrives as undefined. give a param a value with = and it becomes a default. skip it and you get the fallback.one helper, many call styles.registerUser(page, email, pass) just works. need to subscribe? pass true. testing the path where terms are declined? pass false. the same function handles all of it.below is the helper. subscribeToNewsletter is optional, acceptTerms defaults to true.how many near duplicate helpers are hiding in your framework right now?hasztaghasztag

### Tekst z obrazka

_[GIF — OCR pominięty]_

---

## 35. 1 minute #Playwright trick to catch all bugs in a single test runstandard assertions stop your test …

### Tekst postu

1 minute #Playwright trick to catch all bugs in a single test runstandard assertions stop your test the moment something fails. one broken check and you never find out about the other problems on the same page.soft assertions change this. they log the failure but let the test keep going.use expect.soft instead of expect. that is the only change.below is an example. we validate product names and prices in a list. if "Mouse" shows $30 instead of $25, the test logs it and moves on to check "Keyboard" too.at the end you see every failure at once. no more fixing one thing and rerunning just to find the next.how do you handle multiple checks in one test case?hasztaghasztag

### Tekst z obrazka

_[GIF — OCR pominięty]_

---

## 36. your Playwright locators have a priority order, and most suites ignore itwalk into the average test …

### Tekst postu

your Playwright locators have a priority order, and most suites ignore itwalk into the average test repo and you find a pile of CSS selectors and XPath. they break every time a dev renames a class or drops in a wrapper div.Playwright gives you a ranking. start at the top, only move down when the one above does not fit.1. getByRolethe default for almost everything. it finds elements the way a user and a screen reader do, by role and accessible name.✅ `getByRole('button', { name: 'Submit' })`one locator that checks behavior and accessibility at once. it shrugs off the DOM refactors that snap CSS paths.2. getByLabelfor form fields, go after the label the user actually reads. not the id, not the wrapper class.✅ `getByLabel('Email')`restyle the input or move it across the page, the label still points at it.3. getByPlaceholderno label on the field? fall back to the placeholder. still text the user can see on screen.✅ `getByPlaceholder('Search orders')`4. getByTextfor the non-interactive stuff like headings and banners, match the words on screen.✅ `getByText('No users found')`use it to assert what the user reads, not how the markup is nested.5. getByTestIdthe escape hatch. when nothing user-facing is stable or unique, add a data-testid and lock onto it.✅ getByTestId('checkout-cta')an explicit contract that does not care about styling or position.below those live the raw selectors. treat the two of them very differently.CSS is a last resort, not a habit.🔸 `page.locator('.btn.btn-primary')`when nothing user-facing is unique, a CSS selector will do the job. just know it chains to your styling, so a class rename can break it. reach for it only when getBy cannot.XPath is a hard no.🚩 `page.locator('//div[2]/form/button')`it chains to your DOM shape. reorder one element and the path shatters. unreadable, and it tests how the page was built, not what the user does.small syntax win. when these live in a Page Object, write them as getters, not constructor properties.✅ `get submit() { return this.page.getByRole('button', { name: 'Submit' }) }`less boilerplate than a readonly field you assign in the constructor. and it is safe because Playwright locators are lazy. nothing resolves until you act on it, so rebuilding the locator on each access costs nothing.getByRole first, getByTestId last. CSS only as a last resort, XPath never.

### Tekst z obrazka

ROLE FIRST, XPATH NEVER

1 import { Page, Locator } from "@playwright/test"

2

3 export class ShopPage {

4 constructor (private page: Page) {}

5

6 get submit (): Locator {

100, — ‘this. page. getByRole ( “button”, { name: “Submit” })

9 email (): Locator {

10 i this. page. getByLabel (“Email”) PRIORITY

ak! ORDER

12 get search(): Locator {

13 a return this. page. getByPlaceholder ("Search orders")

14

15 get notice (): Locator {

16 5 return this. page. getByText (“No users found")

17

18 get checkout (): Locator {

ok return this. page. getByTestId (“checkout-cta")

20

21 get css(): Locator { " 3 LAST

22 return this. page. Locator (". btn. btn- primary”

For 3 ar ; a RESORT

h(): Locat

2 get xpath (): Locator 8 > FORBIDDEN

CLA ToCATOR
STRATEGY.

---

## 37. testing scenarios like empty states or server errors usually means you need to set up specific data …

### Tekst postu

testing scenarios like empty states or server errors usually means you need to set up specific data first. modifying the database or writing seed scripts just to see how the UI reacts?that is slow. and sometimes risky. and always unnecessarythere is a simpler way. use `page.route` to intercept the API call and return whatever response you wantin the example below, we mock the /api/users endpoint to return an empty list. then we check that the "No users found" message shows upno database setup. no cleanup after. the test controls exactly what the frontend seesit works great, and here is why:- you skip the database setup entirely- you get the exact same response every single run- you never risk deleting or corrupting real test datathis pattern works for more than empty states. you can mock 500 errors, partial responses, slow connections, or any scenario that would be painful to reproduce with real datado you prefer mocking API responses for edge cases, or do you seed real data in the database?

### Tekst z obrazka

MOCK THE API, SKIP THE DB
es Er =
import { test, expect } from '@playwright/test';
test (‘Verify "No users found" when the user API returns an empty list’, async ({
page,
H={
// 1. Intercept the network request to the users API
await page. route('x+/api/users¥', async (route) => {
1 2. Mock the response with an empty list of users MOCK
await route. fulfill (
status: 200, V4
contentType: ‘application/json’,
json: [1,
3); ——
1b;
77 3. Navigate to the page that loads the table
await page. goto (‘https://idavidov.eu/users');
I // 4. Assertions
se await expect (page. getByText (No users found')).toBeVisible ();
1H; smo)

---

## 38. 1 minute #Playwright trick to make failing tests tell you exactly where they brokea long end to end …

### Tekst postu

1 minute #Playwright trick to make failing tests tell you exactly where they brokea long end to end test is one big wall of actions. ☑ search☑ add to cart☑ checkout☑ payall stacked on top of each other.then it fails. the report points at one assertion and stops there. was it the search? or the checkout at the end? you have no clue.so you scroll through 40 lines of trace trying to find the part that actually broke.test.step() fixes this. you wrap related actions in a named step. that is the whole change.now the report shows a clean breakdown. each step has a name and expands to show what ran inside it.when something fails you see which step went red first. you know the phase before you read a single locator.you also get documentation for free. a new person reads the step names and follows the flow without parsing every line.below is a checkout flow split into three steps. same test, much better report.how do you keep your long e2e tests readable?hasztaghasztag

### Tekst z obrazka

import { test, expect } from "@playwright/test";
test (“Checkout flow”, async ({ page }) => {
@ await test.step("Search for product”, async 0) => {
await page. goto ("/products");
await page. getByPlaceholder ("Search"). fill ("Arch@A T-Shirt"); \ 1 NAMED
await page. getByRole ("button”, { name: “Search” }).click(); STEP
await expect (page. getByText (“ArchQA T-Shirt")).toBeVisible ();
oH
@ await test.step ("Add to cart”, async () => {
await page. getByRole (“button”, { name: "Add to Cart” }).click();
await expect (page.getByTestId (“cart-count")). toHaveText (“1”);
1H;
@ await test.step ("Complete checkout”, async () => {
await page. getByRole ("button”, { name: "Pay Now" }).click();
15.0008 expect (page. getByText (“Order Confirmed"). toBeVisible(); CLEAR
ni REPORTS

---

## 39. 5 Playwright setup tricks i wish someone handed me on day onethis is part 1 of my 33 tricks series. …

### Tekst postu

5 Playwright setup tricks i wish someone handed me on day onethis is part 1 of my 33 tricks series. configuration and core setup. the stuff you touch every day but nobody bothers to teach.1. stop retyping the same assertion - you write the visibility check 40 times a day. make a VS Code snippet for it instead.map a prefix to the line and let `$1` drop your cursor inside:`"exv" => await expect($1).toBeVisible()`type exv, hit tab, move on.2. one npm script per tag - stop trying to remember grep flags. write them down once in package.json.`"test:smoke": "playwright test --grep @smoke"`now the whole team runs npm run test:smoke and gets the same slice.3. keep secrets out of the repo - read them from process.env. the ! tells TypeScript the value is there at runtime.`.fill(process.env.API_PASSWORD!)`no passwords sitting in your git history.4. tag your tests, skip the folder gymnastics - smoke, regression, slow, they can all live in the same file. tag once and filter whenever you want.`test("Login", { tag: "@smoke" }, async ({ page }) => { ... })`5. skip with a reason, not silence - a mobile-only or feature-flagged test should say why it skipped. one line does it.`test.skip(!isMobile, "Mobile only")`the next person to read the report knows exactly what happened.none of this shows up in the getting started guide. but six months in, this is what keeps your suite sane.which of these 5 are you already doing?

### Tekst z obrazka

CONFIGURATION & CORE SETUP

] Playwright

The defaults nobody bothers to teach you.
USER SNIPPETS 01 NPM SCRIPTS 02
Stop typing the same Tag-aware run scripts
assertions One npm script per tag. No more remembering
Drop a VS Code snippet for the line you write grep flags.
= pEme—— ||
Secrets stay out of the 03 Filter without folder 04
repo gymnastics
Read from process.env, fail loudly with the Tag once, filter forever. Smoke, regression,
non-null bang. slow, all live together.
Skip with a reason, not silence 05
Mobile-only or feature-flagged tests skip themselves with a one-liner.

idavidov.eu INTELLIGENT QUALITY

---

## 40. 1 minute Playwright tip to keep secrets out of your test codei still open test files and find this`c…

### Tekst postu

1 minute Playwright tip to keep secrets out of your test codei still open test files and find this`const password = "superSecret123"`it feels harmless. one test, one string, you tell yourself you will clean it up laterbut the second you commit it, that password lives in your git history forever. anyone who clones the repo can read it, and rotating the secret later does not wipe the old commitsso move it out. put your secrets in a .env file at the project root`API_PASSWORD=superSecret123`then read it from process.env inside the test`await page.getByLabel("Password").fill(process.env.API_PASSWORD!)`the ! tells TypeScript the value exists at runtime so it stops flagging it as possibly undefinedhere is the part that trips most people up. Playwright does not read your .env file on its own. you load it once at the top of playwright.config.ts`import dotenv from "dotenv"``dotenv.config()`and the step everyone forgets. add .env to your .gitignore. otherwise you just moved the secret into a new file and committed that one instead✅ no secrets sitting in your test code or git history✅ every engineer keeps their own local .env✅ CI injects the real values as environment variableshow do you manage secrets in your test suite today?

### Tekst z obrazka

STOP HARDCODING SECRETS
————
import { test, expect } from "@playwright/test";
// .env file: APT_PASSWORD=superSecret123 <— GITIGNORE IT
test (“login with env credentials", async ({ page }) => {
await page. goto ("/login");
// never hardcode secrets
await
await page. getByRole ("button", { name: "Log in" }).click();
1 await expect (page. getByText ("Welcome")). toBeVisible () ;
1;

---

## 41. 1 minute Playwright tip to run only the tests you need with tagsyour suite has 200 tests. you change…

### Tekst postu

1 minute Playwright tip to run only the tests you need with tagsyour suite has 200 tests. you changed one login field. you do not need to run checkout or billing to know if login still worksbut most teams still run everything. every PR. every time. CI minutes pile up and failures get harder to readtag your tests once. run a slice when you need itadd a tag in the test options:`test("Login flow", { tag: "@smoke" }, async ({ page }) => { ... })`then pick what runs from the CLI:`npx playwright test --grep @smoke`need smoke plus auth? use a regex:npx playwright test --grep "@smoke|@auth"wire those into npm scripts and your team gets one command for each slice. smoke before merge. regression on nightly. api tests without opening a browser tab✅ run 12 tests instead of 200 when you only touched login✅ same tags work in CI and on your laptop✅ one naming convention, no duplicate test fileshow do you split your suite today? tags, folders, or still running everything?

### Tekst z obrazka

TAG ONCE, RUN WHAT You NEED
— —
import. { test, expect } from “@playwright/test";
- el
SZ dest(" Login Flow", { tag: "@smoke” }, async ({ page 1)» {
await page.goto("/login");
await page.getByLabel ("Email"). fill ("user@example.com"); sais
await page.getByLabel (* Password"). ill ("password 123); @
await page.getByRole (“button { name: “Log in” }).click();
await expect (page). toHaveURL("/dashboard");
bs
test ("Change avatar", { tag: "@regression” }, async ({ page }) = {
await page.goto ("/profile"); regression
await page.getByLabel ("Upload Avatar"). setTnputFiles (*./fistures avatar pg’); @
await expect (page.getByAltText (“User Avatar") toBeVisible ();

---

## 42. multi-language apps do not give you the right to drop accessibility locators, so use this 5 minute P…

### Tekst postu

multi-language apps do not give you the right to drop accessibility locators, so use this 5 minute Playwright trickwhen i start talking about accessibility locators, i get the same question a lot. what about apps with multiple languages?most engineers panic because their trusted english text locators would break the moment they switch the testing languageso they abandon accessibility locators and fallback to the dark ages of dom manipulationthey start writing locators like this🚩 page.locator('.form-group .btn.btn-primary .submit-btn')🚩 page.locator('//div[@class="login-container"]/div[2]/form/button')this is how flaky pipelines are born, and confidence in your testing is gonea dev adds one extra wrapper div for a layout tweak and your entire test suite is brokenthere is a much cleaner way in Playwright. your framework just needs a single source of truthkeep it simple. load the correct language json file at runtime using an environment variablethen pass that dynamic dictionary right back into your accessibility locators✅ page.getByRole('button', { name: i18n.submitBtn })playwright inserts the correct string automatically. doesn't matter if it's english today and french next sprintyou keep the user-centric accessibility locators and skip the brittle dom pathsdo not compromise your architecture just because the text changesadding a new language to the framework takes under a minutehow do you handle multi-language locators in your suite today?

### Tekst z obrazka

1 LOCATOR, ANY LANGUAGE
®@ // BJ locales/en.json -> { "submitBtn": "Submit" }
SURESF  // BI locales/fr.json — { “submitBtn": "Soumettre" }
// BI locales/es.json — { "submitBtn": “Enviar” }
®@ // BI config/il8n.ts
import en from '../locales/en.json';
RESOWE  inoort fr from '../locales/fr.json';
import es from '../locales/es.json';
const locales = { en, fr, es } as const;
type Supportedlang = keyof typeof locales;
const currentlang = (process.env.RUN_LANG as SupportedLang) I] ‘en’;
export const i18n = locales [currentlang];
[©] // BI pages/LoginPage.ts
import { Locator, Page } from '@playwright/test';
PAGEOBECT import { i18n } from './../config/il8n';
export class LoginPage {
constructor (private readonly page: Page) {}
// X name: 'Submit' breaks on /fr or [es
// V resolves at runtime via RUN_LANG
get submitButton (): Locator {
return this. page. getByRole( button’, { name: 1);
} A LE A ee aes
} 3) BRITTLE PATHS

---

## 43. 1 minute Playwright trick to catch silent API contract drift before it hits productionhow many times…

### Tekst postu

1 minute Playwright trick to catch silent API contract drift before it hits productionhow many times has a backend dev quietly removed or renamed a field in an API response? no ticket. no heads upyour tests still pass because they only check status codes. you ship green. then production breaks because the frontend was reading a field that no longer existsif you are using Playwright, the answer is Zodyou define a schema once with z.strictObject, then derive the typescript type from it with zOutput. one source of truth for both runtime validation and compile-time typesnow the test parses the body against the schema right after the status check. rename a key or sneak in an extra field that strictObject did not expect, the test fails immediately with a clear validation error✅ catch contract changes the moment they land✅ no manual type definitions to keep in sync✅ reuse the same schema across api tests and runtime guardsthis is what end-to-end type safety actually looks likehow do you usually catch silent api changes in your test suite?

### Tekst z obrazka

ONE SCHEMA, ZERO DRIFT
—
// schemas/user.ts
import { z } from 'zod/v4';
import type { output as zOutput } from 'zod/v4';
® export const UserSchema = z.strictObject ({
email: z.email 0), STRICT
username: z.string(), SCHEMA
token: z.string(),
bi
export type User = zOutput<typeof UserSchema>;
// tests/api/login.spec.ts
@ test('login returns valid user’, async ({ apiRequest }) => {
const { body, status } = await apiRequest<User>({
method: ‘POST’,
url: 'api/users/login’,
body: { email, password },
bs;
expect (status). toBe (200); CATCHES
1! expect (UserSchema. parse (body). toBeTruthy (); | DRIFT
1;

---

## 44. 1 minute Playwright trick to perform visual regression tests for UI with dynamic datavisual regressi…

### Tekst postu

1 minute Playwright trick to perform visual regression tests for UI with dynamic datavisual regression tests are great until your UI has dynamic data. a clock that ticks every second or a randomly generated transaction id is enough to break a snapshotso your test fails on the first run, then again, then again. you give up and disable ityou don't have to. Playwright has a `mask` option built right into `toHaveScreenshot`pass an array of locators and Playwright paints a pink box over those elements before the screenshot is captured. the rest of the page stays pixel perfect for comparisonin the example below, the dashboard has a `current-time` element and a `transaction-id` that change every run. both get masked. the snapshot stays stablethis works for any flaky region. live counters, charts, user avatars, "updated 3 minutes ago" badges. mask once and sleep at night like a babywhat is your strategy for dynamic data in visual tests?

### Tekst z obrazka

MASK FLAKES — STABLE SNAPS
import { test, expect } from '@playwright/test’;
test (‘Visual Check of Dashboard’, async ({ page }) => {
await page.goto('/dashboard’);
// Mask dynamic elements like dates or random IDs
I" await expect (page) (EoHaveScreenshob) dashboard. png’, {
page.getByTestId ('current-time'),
page.getByTestld ('transaction-id’), MASKED
gS
§ a>
b;

---

## 45. 5 minute Playwright fix to stop instantiating page objects in every test. if you look at any Playwri…

### Tekst postu

5 minute Playwright fix to stop instantiating page objects in every test. if you look at any Playwright test file written by a junior, or even a mid level engineer, you will find out that it always starts the same way```const loginPage = new LoginPage(page)const dashboardPage = new DashboardPage(page)const settingsPage = new SettingsPage(page)```three lines of boilerplate before you even get to the actual test logic. multiply that by 100 tests and you can imagine the painthe cleanest fix is to extend the Playwright test and let it hand you the page objects automaticallyyou declare your fixtures once. then every spec just destructures whatever pages it needsthe wins are immediateNO more new() calls in your specsless code per testbetter readabilityeasier refactors when a page object signature changesthe test reads like a sentence again. you focus on what the test does, not on how to wire up the dependenciesin the snippet below i extended the base test with two fixtures, loginPage and dashboardPage. each one returns a fresh instance bound to the current page. the spec just asks for what it needs and Playwright takes care of the restthis is what I call great DX. as a summary - keep implementation details as far from the test layer as possibledo you even use POM, and if yes, how you handle it?

### Tekst z obrazka

import { test as base } from ‘@playwright/test’;
import { LoginPage } from ‘'./pages/LoginPage’;
import { DashboardPage } from './pages/DashboardPage’ ;
@ type MyFixtures = {
loginPage: LoginPage ; FixTure
3 dashboard Page: Dashboard Page ; TYPES
@ export const test = base.extend < MyFixtures>({
loginPage: async ({ page }, use) => {
¥, await use(new LoginPage (page)); wits up
dashboardPage: async ({ page }, use) => { ONCE
es Dashboard Page (page);
5) Fe
@ / in spec file:
// test('login’, async (({ loginPage }) => {...}) 0 BOILERPLATE

---

## 46. 1 minute Playwright trick to grab data from an API response triggered by a UI actionyou click a butt…

### Tekst postu

1 minute Playwright trick to grab data from an API response triggered by a UI actionyou click a button in the ui. it fires an api request behind the scenes. the backend sends back something you actually need later in the test, like the id of the resource that was just createdwhat most people do? they fire a second api call after the ui action to fetch what was created. slower than it should be. sometimes the data is not ready yet. sometimes the wrong record gets pickedthere is a cleaner way. run the UI action and the response listener at the same time with `Promise.all`put the page object call (the trigger) and `page.waitForResponse` (the listener) inside the same `Promise.all` array. the listener subscribes first, the ui action sends the request, the response comes back, both promises resolve at oncethen you destructure the response and pull whatever you need out of it- no extra api call after the ui finishes- you grab the real backend payload from the same network round trip- the id is available the moment the ui is donea common use case is cleanup. you save the id in a shared variable, then `test.afterAll` deletes the record via the api. no leftover data in the database. no broken state for the next runhow do you usually pull backend data out of your ui tests?

### Tekst z obrazka

import { test, expect } from '@playwright/test’;
import { deleteArticle } from '../helpers/articles’;
test. describe (‘Test Article Creation’, () => {
let articleId: string;
const title = ‘My Article’;
const description = ‘short summary’;
const body = ‘article body’;
const tags = [‘playwright’];
test (‘Promise All’, { tag: ‘@smoke’ ({ page, articlePage }) => {
const [, response] = await (Promise. allD([
articlePage . publishArticle (title, description, body, tags), PARA
page. wait ForResponse (  #* /api /articles/'),
hE
const responseBody = await response. json();
articleId = responseBody.id;
await expect (page. getByRole( heading’, { name: title })). toBeVisible();
bh;
11 test. afterall (* Cleanup: Delete the created article’, async O => {
if (articleId) await deleteArticle (articleTd);
Hs
bs

---

## 47. It’s strange how the easiest ideas are usually the ones people get wrong. It’s either because people…

### Tekst postu

It’s strange how the easiest ideas are usually the ones people get wrong. It’s either because people think they're too simple to care about, or they just weren't explained very well in the first place.Since I almost always talk about fundamentals and architecture, I prepared the following Cheatsheet, trying to show with practical examples the main REST API Responses you can receive. For Success Status Codes (2xx), I show when they are returned and what you should verify from the response bodyFor Client-Side Failures (4xx), I show what are the common causes and I provide a practical example scenariosFor Server-Side Failures (5xx), I show what they means and which should be the testing angleI highly recommend to go through the tables. I bet you will either learn something or catch a mistake I might made

### Tekst z obrazka

Decoding HTTP Status Codes:
| i f Guid
A Developer's Quick Reference Guide
HTTP status codes act as the primary communication language between a web server
and a client, indicating the result of an attempted action. Mastering these codes
enables developers to debug integrations and build resilient applications.
2
The Success Range (2xx)
d These codes confirm that the server successfully received, understood, and
[= eo] accepted the request from the client.
[Code | Name | Whonts Retumed What to Verity
200 ok Successful GET, PUT, PATCH | Response body contains the expected data
Response body contains the new resource
201 Created Successful POST ie so et
Async operations (background jobs, | Response confirms request acceptance;
202 Accepted batch processing) check for job ID or status URL for polling
Successful DELETE, or updates that | Response body is empty; do not attempt to
[20 | No Content return no body parse it
Ts C3 ——
~/ Client-Side Failures (4xx)
\ese errors occur when the request is incorrect, malformed, or lacks t
Bl — Th hen thy isi iformed, or lacks the
I= necessary permissions, requiring the client to change the request before retrying.
Malformed JSON, wrong data Sending “price” "abc" instead of a
@00 EE types, missing required fields number
: Missing or invalid authentication Calling an endpoint without an
| wo | Lnaiinerized token Authorization header
3 Valid auth, but insufficient Aregular user trying to access an
403 Forbidden permissions ‘admin-only endpoint
Resource doesn't exist, orwrong | GET /apl/users/99999 when that user
| wo | an [i doesn't exist
Method Not = Sending a DELETE to an endpoint that
Creating a user with an email that
Too Many 3 Sending 100 requests per second to an
429 Requests REESE endpoint limited to 10
==3
=D These codes indicate that the client's request was valid, but the server
=x. failed to fulfllit due to internal issues, downtime, or upstream
e—0 [18 dependency failures.
cs ie [a Re Testing Ange
Internal Server 5 A bug that should be reported with the
50008 [BCT Unhandled exception on the server | ¢,2 18 1% SLC 0% Poorer
Invalid response from an upstream | Often seen in microservice architectures;
E3 Bad Gateway service test when a dependency is down
s03 | Service Server is overloaded or under Verify if the API returns a “Retry-After”
Unavailable mointenance header during heavy load or deployments
Upstream service took too long to | Similar to 502; test with slow-responding

---

## 48. These days, I saw many times the same Cheatsheet on LinkedIn for the most important Playwright CLI c…

### Tekst postu

These days, I saw many times the same Cheatsheet on LinkedIn for the most important Playwright CLI commands. No need to look into printed A4 sheet to check which command you need. Here is the Ultimate Cheatsheet. Just add the ones your team uses to the package. json file.Then, every team member can use the same commands, and as a side benefit is that there are version controlled.Simple as that.Cheers!

### Tekst z obrazka

{
"COMMENT": “TIP: Define NPM scripts for common test execution patterns.”,
"RUN": “Run with: npm run <script-name>",
Ga
: "npx playwright test --project=chromium",
Testra: i Playwright fest —-project=chromium --workers=1",
“test: flaky": "npx playwright test --project=chromium --repeat-each=20",
“test:debug": "npx playwright test --project=chromium --debug",
“test:ui": "npx playwright test --project=chromium --ui",
| | "npx playwright test --grep @Smoke --project=chromivm”,
- “test:sanity": "npx playwright test --grep @Sanity --project=chromium",
“test:api": "npx playwright test --grep @Api --project=chromium",
“fest :regression”: "npx playwright test --grep @Regression ——project=chromium",
“fest :isolated": "npx playwright test --grep @Isolated --project=chromium --workers=1",
) “full Test": "npx playwright test"
}

---

## 49. 1 minute #Playwright trick to test edge cases without touching the databasetesting scenarios like em…

### Tekst postu

1 minute #Playwright trick to test edge cases without touching the databasetesting scenarios like empty states or server errors usually means you need to set up specific data first. modifying the database or write seed scripts just to see how the UI reacts?that is slow. and sometimes risky. and always unnecessarythere is a simpler way. use `page.route` to intercept the API call and return whatever response you wantin the example below, we mock the /api/users endpoint to return an empty list. then we check that the "No users found" message shows upno database setup. no cleanup after. the test controls exactly what the frontend seesit works fantastic, because it provides speed, stability and safety- you skip the database setup entirely- you get the exact same response every single run- you never risk deleting or corrupting real test datathis pattern works for more than empty states. you can mock 500 errors, partial responses, slow connections, or any scenario that would be painful to reproduce with real datado you prefer mocking API responses for edge cases, or do you seed real data in the database?hasztaghasztag

### Tekst z obrazka

import { test, expect } from '@playwright/test';
test ("Verify “No users found” when the user API returns an empty list’, async ({
page,
nt
// 1. Intercept the network request to the users API
await page.route ('**/api/users*', async (route) => {
// 2. Mock the response with an empty list of users
await route. fulfill ({
status: 200, Mock
contentType: ‘application/json’, RESPONSE
json: [1,
Hi—=——
hi
// 3. Navigate to the page that loads the table
await page.goto('https://idavidov.eu/users');
// 4. Assertions
[1] await expect (page.getByText (‘No users found')).toBeVisible ();
H

---

## 50. 1 minute #Playwright trick to catch all bugs in a single test runstandard assertions stop your test …

### Tekst postu

1 minute #Playwright trick to catch all bugs in a single test runstandard assertions stop your test the moment something fails. one broken check and you never find out about the other problems on the same page.soft assertions change this. they log the failure but let the test keep going.use expect.soft instead of expect. that is the only change.below is an example. we validate product names and prices in a list. if "Mouse" shows $30 instead of $25, the test logs it and moves on to check "Keyboard" too.at the end you see every failure at once. no more fixing one thing and rerunning just to find the next.how do you handle multiple checks in one test case?hasztaghasztag

### Tekst z obrazka

import { test, expect } from '@playwright/test';
BST we re
Co SE -
name: op’, price: ,
i d 16551 m $30"
A fine: ‘Mouse free Bb i Let's say the UI actually shows '$30' ~ pATA
dProducts]
El) a tivi ann):
ise. Chek if gc ah Ee Looe
I" (ei Hs eck if ${product.name} is visil a iE
V % price Sard ntinues to check 'Keyboard'
a ec!
I! wait pect Goro locator price) ebave Tex produet. price):
ni

---

## 51. you typed "create a login test"monday it gave you a classtuesday it gave you a functionwednesday it …

### Tekst postu

you typed "create a login test"monday it gave you a classtuesday it gave you a functionwednesday it hallucinated a framework you don't even usethe ai isn't inconsistent. you arevague inputs get vague outputswhere does the file go?what are the naming conventions?do we use fixtures or raw instantiation?so i stopped freestylingbuilt a library of prompt templatesstructured requests. predictable resultsmy "new page object" template:1. context: navigate to [url] and map the accessibility tree2. output: generate pages/app/[name].page.ts3. constraints: locators must be getters. jsdoc is mandatory4. integration: register in page-object-fixture.ts automaticallymy "new test" template is just as strict:1. location: tests/app/[functional|api]/[name].spec.ts2. imports: only from fixtures/pom/test-options3. structure: test.step (given/when/then)4. tags: @[smoke|regression] requiredtemplates are the api for your aisame input format. same output qualitystop guessing. start templating

### Tekst z obrazka

Problem: Unstructured prompts leave too much to interpretation:
« Which folder?
« What naming convention?
« Which fixtures to use?
« What tags to apply?
Solution: Pre-defined templates for every task type:
Code Example:
# Template: Create Page Object
Create a new page object for [PAGE NAME].
First, navigate to [URL] and discover:
~ Element roles, labels, and accessible names
~ Form field structure
~ Button names and actions
Then generate:
~ File: pages/app/ [name] .page. ts
~ Locators as getters with JSDoc
~ Action methods with Promise<void> return
~ Registration in fixtures/pom/page-object-fixture.ts
# Template: Create Test File
Create tests for [FEATURE]:
~ Location: tests/app/[functional|api|e2e]/[name].spec.ts
~ Import from fixtures/pom/test-options. ts
~ Tags: @[smoke|regression] + @[functional|e2e|api]
- Structure: test.describe - test - test.step (Given/When/Then)
Scenarios:
§ 1. [Happy path] 1
2. [Error case]
b 3. [Edge case]
Takeaway: Templates are prompt engineering for consistency. Same input format -> same
output quality.

---

## 52. 1 minute #Playwright trick for polling when stuff is not ready yetsometimes an element does not show…

### Tekst postu

1 minute #Playwright trick for polling when stuff is not ready yetsometimes an element does not show up right away. or a status takes a few seconds to update. you need to wait for it without hardcoding a sleepawait page.waitForTimeout(5000) is the wrong move. always! it wastes time when the condition is met earlier, and it might still be too short when things are slowuse expect.poll instead. it keeps retrying your check until it passes or the timeout hits. it returns as soon as the condition is met. no fixed sleep, no guessworkbelow is an example. we ship an order via the UI, then poll the API until the status is "Shipped"what do you use when you need to wait for a backend or async state?hasztaghasztag

### Tekst z obrazka

import { test, expect } from '@playwright/test’;
interface Order {
[i Serie:
o BEE,
‘test('Ship an order. via UF, and Verify the updated status via API', async ({ page, {
Vera Berta alors ©
TA RT -
Tren: (Playuright Conference Ticket, order
Jy; 3, Customer: “van Davidov’,
Bm cpm penn,
TIT T-ahe order. is shipped via UI", async () => { ship the order
alt BE Ret De hi order chek; Ve UF
SwaTE Test step( THEN: The order status is updated to Shipped (Polled via APT)’, async () => {
aE
lle £X Techonse = avait request.get('/api/orders');
ok ee ony
Set SIRES Sd te ppm Jn0% rn 5:8
PRE
Thien. Fit Psi! not reach ‘Shipped’ status within the timeout.’,
| TET
0. uae
pi

---

## 53. minute trick to speed up your #Playwright tests. stop clicking through the UI just to set up your te…

### Tekst postu

minute trick to speed up your #Playwright tests. stop clicking through the UI just to set up your test dataif your E2E tests take forever, it is usually because you are doing everything through the browserlogging in, navigating menus, filling out forms, clicking save. all just to create the data your test actually needsthere is a faster way. hit the backend API directlythis is Arrange-Act-Assert pattern1️⃣ Arrange: use request to inject data in milliseconds2️⃣ Act & Assert: use page to verify the user actually sees itinstead of 10 clicks and 5 page loads, you make one POST call. the data exists. you move on✅ fewer UI steps mean less flakiness✅ API requests finish in milliseconds, not seconds✅ you test the actual feature, not the data entry formbelow is a quick example. create a product via API, then check that the UI displays ithow often do you put API calls into your UI tests?hasztaghasztag

### Tekst z obrazka

import { test, expect } from '@playwright/test';
test('UI should display newly created item’, async ({ page, request }) => {
// 1. Create data directly via API (Fast!)
cou fechas = await request. post('/api/products’, {
lata:
name: ‘Playwright T-Shirt", R3
price: 25.0,
n }
1) expect (response. status()).toBe (201);
// 2. Go to UL to verify it appears (User perspective)
await page.goto('/products');
const productCard = page.getByRole('link', { name: "Playwright T-Shirt" }); we.
await gxpecht productCard). toBeVisible();
await expect (productCard).foContainText ('$25.00');
b;

---

## 54. you let 10 engineers write tests their own way10 engineers. 10 different stylesthen you added ainow …

### Tekst postu

you let 10 engineers write tests their own way10 engineers. 10 different stylesthen you added ainow you have 100 different stylesdebugging is a nightmare. `grep` is uselessthe reports are a wall of textfreedom sounds great on paperin practice it's chaos. automation needs governanceso i enforced a "constitution" for test structurethree non-negotiable rulesrule one: tags belong on tests. never on describe blockswhen i run `@smoke`, i want specific scenariosnot entire files dragged along because one test mattersrule two: steps are mandatoryno giant blocks of code. we use `test.step()`given. when. thenrule three: taxonomy is defined`@smoke` = runs on every pr`@destructive` = modifies shared state. runs isolated`@api` = no browser needednow the html report reads like a storynot a stack traceps: do you tag your describe blocks or individual tests?episode 9 of 15. standardizing the execution.

### Tekst z obrazka

Problem: Inconsistent test organization:
+ Tags on describe blocks vs. individual tests
« Nostep structure for debugging
+ Random tag names with unclear meaning
Solution: Enforce explicit standards:
Code Example:
import { expect, test } from '../fixtures/pom/test-options';
test.describe('User Authentication’, () => {
/ Tags on TESTS, never on describe
test(
‘should login with valid credentials’,
{ tag: '@smoke' },
async ({ loginPage }) => {
await test.step('GIVEN user is on login page’, async {) => {
await loginPage.navigate();
Hi
await test.step('WHEN user enters valid credentials’, async () => {
await loginPage.login(email, password);
Hi
await test.step('THEN user sees dashboard’, async () => {
await expect (loginPage.welcomeMessage).toBeVisible();
Hi
}
Vi
Hi
Tag taxonomy:
Category Tags Purpose
Importance _@smoke, @sanity, @regression What to run when
= .
Type @e2e, @api How it tests
Behavior  @destructive Modifies shared state (isolated run)
Takeaway: Given/When/Then steps aren't just for readability — they make failures debuggable in HTML reports.

---

## 55. 5 minute #Playwright trick to login once and test foreverevery test suite i have seen starts the sam…

### Tekst postu

5 minute #Playwright trick to login once and test foreverevery test suite i have seen starts the same way. open the app, type the email, type the password, click login, wait for the redirectmultiply that by 50 tests and you are burning real time on a screen that has nothing to do with what you are actually testingthe fix is a setup stage. Playwright calls it a "project dependency". it runs once before your actual tests even starthere is how it works1. hit the login API directly and log in from the UI2. validate the response (i used Zod for schema validation) or the success of the login3. save your cookies and local storage to a session file and your token as env variable4. every other test loads that file and token and skips the login screenyour tests start exactly where the user lands after login. the login screen never touches your test results again. additionally you will have a token for that user for all API requestsin my setup i used a custom API abstraction, Page Object Model as fixtures, Zod for response validation, and dotenv for environment variables. but you don't need any of that. Playwright has built-in support for storage state and API requests out of the box. the pattern works the samei love LinkedIn discussions, so share with me how do you handle authentication in your test suite?hasztaghasztag

### Tekst z obrazka

setup('auth user’, async ({ apiRequest, homePage, navPage, page }) = {
await setup.step('auth for user by API', async () = {
const { status, body } = await apiRequest<User>({
method: 'POST',
url: 'api/users/login’,
Fase) process. env.API_URL,
ly: STEP 1:
tag: Sl HLS
email: process. env.EMAIL,
) password: process.env.PASSWORD,
yi 3
1] expect (status). toBe(200);
1% expect (UserSchema. parse (body) .toBeTruthy ();
» process.env['ACCESS_TOKEN'] = body.user. token;
+ Ee
await setup.step('create logged in user session’, async () => {
await homePage.navigateToHomePageGuest (); STEP 2:
await pil v.EMAIL!, process.env.PASSWORD!); ("UI SESSION
x await page.context()¢storageStatey{ path: '.auth/userSession.json' });
i

---

## 56. 1 minute #Playwright tip to implement data-driven automation with a for...of loopwhen it comes to ve…

### Tekst postu

1 minute #Playwright tip to implement data-driven automation with a for...of loopwhen it comes to verifying software validations, there is one pattern that makes your life significantly easierdata-driven automationthe idea is simple. instead of writing a separate test for each invalid input, you create one array of chaos and let a for...of loop do the heavy lifting1. create an array of invalid data2. unleash a simple for...of loop3. sit back and watch the assertions flyin the code snippet, we have an array of invalid emails. weird formats, missing domains, special characters, double dots, leading dots. all the edge cases that users will definitely trythen a single for...of loop generates a dedicated test for each entry. Playwright runs them all, and every single one gets its own clear pass or fail✅ clean and readable✅ easy to extend with new test cases✅ each input gets its own isolated testyou write it once. one loop, one source of truththe next time someone says "we need 15 more validation tests," you just add 15 strings to the array. donedo you automate validations like this, or do you have a different approach you swear by?hasztaghasztag

### Tekst z obrazka

const invalidEmails = [
test’,
"152445",
'test@test’,
'#@%*&$$@#.com',
'email@111.222.333',
'not"right@example.com',
‘email. .email@example.com',
' .email@example.com',
'__@example.com',
for (const email of invalidEmails) {
test(
erty that em triggers invalid email error’,
tag: '@Regression' },
Som async ({ page }) = {
await page. gethyPlaceholder( 'Email').fill(email); Il
await expect(page.getByText( "Invalid Email"). toBeVisible(); [!
ee
-_—
);
3

---

## 57. you trusted the backend developerthe docs said the field was a string. the production database said …

### Tekst postu

you trusted the backend developerthe docs said the field was a string. the production database said it was nullyour test passed, because you cast the response to any. or maybe you just asserted status === 200then the frontend crashed. because it tried to call `.toUpperCase()` on null`any` is technical debt causing huge issues. api contracts drift. documentation lies. tests must verify reality, not promisesso i started using schemasenter zod. runtime validation + static type inferencewe define the contract once (see the code snippet below)if the backend changes a field name? the test explodes immediately. not a vague undefined error. a specific schema violationcatch the drift before deploymentps: do you validate api responses or just status codes? episode 8 of 15. locking down the contracts.

### Tekst z obrazka

Problem: API responses without type validation:
« Contract drift goes unnoticed until production
« Refactors break silently
« Algenerates = types when uncertain
‘Solution: Zod schemas for every API response:
ous Exrie’ —
// fixtures/api/schemas/app/userSchema.ts
import { z } from 'zod/v4';
import type { output as zOutput } from 'zod/va';
export const UserResponseSchema = z.strictObject({
id: z.uuid(),
email: z.email(),
name: z.string(),
role: z.enum(['admin’, ‘user’, 'guest'l),
createdAt: z.iso.datetime(),
bi
export type UserResponse = zOutput<typeof UserResponseSchemas;
Oe re Ce
test( should return user data’, async ({ apiRequest }) => {
const { status, body } = await apiRequest<UserResponse>({
method: ‘GET’,
url: '/api/users/me',
baseUrl: process.env.APT_URL,
Hi
expect (status). toBe (200);
// Throws if response doesn't match schema
expect (UserResponseSchema. parse (body)) . toBeTruthy ();
expect (body. role). toBe (admin);
Hi
Takeaway: Schema validation at test time catches contract drift before deployment.
‘Type inference from Zod eliminates manual interface maintenance.

---

## 58. pov: you can't reproduce the failurethe ci logs say "email already exists", but you swore you random…

### Tekst postu

pov: you can't reproduce the failurethe ci logs say "email already exists", but you swore you randomized it. or maybe you hardcoded test@test.com. and 5 parallel workers tried to login at the exact same timedata is the silent killer of test stability. most frameworks get it wrong. they mix the chaos of random data with the rigidity of static dataso i bifurcated my strategy. two buckets. two strict rules1. static data (for the edge cases) sql injection strings. xss payloads. invalid boundaries. these must be deterministic. they live in json files. if it fails, i want to know exactly what caused it2. dynamic data for everything else. unique users. fresh emails. valid flows. these must be isolated. we use factoriesmy user.factory.ts generates a unique human for every single test run. my invalid.json breaks the system the exact same way every timestatic for the edges. dynamic for the flow. never mix them upps: do you use faker or hardcoded json? episode 7 of 15. architecting for reliability.

### Tekst z obrazka

Problem: Using the same approach for all test data creates issues:
+ Random data for boundary testing > non-reproducible failures
+ Hardcoded data for happy path -> data collision in parallel runs
Solution: Spit data into two categories:
Category Location Use Case Tool
Static test-data/static/ Boundary, invalid, edge cases JSON files
Dynamic test-data/factories/ Happy path, unique perrun Faker + Zod
Code Example:
Static data (reproducible edge cases):
// test-data/static/app/invalidCredentials. json
{
“invalidCredentials"s [
{ "description": "empty email", "email": "", "password": "valid123" },
{
“description”: "SQL injection”,
"email": "'; DROP TABLE usersi—",
“password”: "x"
i
{
“description”: "XSS attempt”,
"email": “<script>alert(1)</script>",
“password”: "x"
»
1
3
Dynamic data (unique per run):
/1 test-data/factories/app/user. factory. ts
inport { faker } fron ‘@faker-js/faker';
export function generateUser(overrides?: Partial<User): User {
return {
email: faker. internet.enail(),
password: faker. internet.password({ length: 12 }),
name: faker. person. fullNane(),
+..overrides,
bh
bl
// usage
const user = generatelser(); // Unique cach run
const adnin = generateuser({ email: 'adningtest.con' }); // Override specific fields
Takeaway: Static for determinism. Dynamic for isolation. Never mix them up.

---

## 59. 1 minute #Playwright tip to speed up your tests by aborting unnecessary requestsif you are testing f…

### Tekst postu

1 minute #Playwright tip to speed up your tests by aborting unnecessary requestsif you are testing functional logic like a signup or newsletter form, you don't need to wait for high resolution marketing images to loadbut by default, Playwright waits for the page to fully load. that includes every banner, hero image, and background assetthis slows your tests down. and if those images come from a slow CDN or third party, it makes them flaky toothe fix is simple. use `page.route` to intercept and abort requests you don't care about`await page.route('**/*.{png,jpg,jpeg,svg,gif}', route => route.abort())`that one line tells Playwright to immediately kill any image request before it even startsyour page still renders. your buttons, forms, and text are all there. you just skip the heavy stuff that does not matter for your test✅ faster test execution✅ less flakiness from slow or unreliable CDNs✅ cleaner test focus on what actually mattersthis works for more than images. you can abort analytics scripts, tracking pixels, video preloads, or any request pattern that is not relevant to the behavior you are validatinghasztaghasztag

### Tekst z obrazka

import { fest } from '@playwright/tfest' TEE iy
test(' Testing UL Without Waiting for Images wo ge ({ page }) => {
await page.route("**/* {png,jpg,jpeg,svg,gif}', route au) ute.abort())) | |
await page.goto('https://idavidov.eu/')
await page.getByRole( ‘button’, { name: ‘Newsletter’ }).click()
// rest of logic
p)

---

## 60. 1 minute #Playwright tip to use accessibility locators in multi-language apps when i start talking a…

### Tekst postu

1 minute #Playwright tip to use accessibility locators in multi-language apps when i start talking about accessibility locators, i receive a similar question a ton. what to do when the application has different languages?most of the engineers panic because their trusted english text locators would break once they change the testing language. so they abandon accessibility locators and fallback to the dark ages of dom manipulationthey start writing locators like this🚩 page.locator('.form-group .btn.btn-primary .submit-btn') 🚩 page.locator('//div[@class="login-container"]/div[2]/form/button')this is how flaky pipelines are born. and the confidence in the testing is ruineda dev adds one extra wrapper div for a layout tweak and your entire test suite bleedsthere is a much cleaner way to handle this in Playwright. your testing framework just needs a single source of truthkeep it simple. load the correct language json file at runtime using an environment variablethen you just pass that dynamic dictionary right back into your resilient playwright locators✅ page.getbyrole('button', { name: i18n.submitBtn })playwright inserts the correct string automatically. english, french, spanish, or whatsoeverit does not matteryou keep the user-centric accessibility locators and ditch the brittle dom pathsdo not compromise your architecture just because the text changes. smart systems adapt to the contextadding additional language to the framework is done under a minutehasztaghasztag

### Tekst z obrazka

// SOURCE OF TRUTH:

// ' locales/en.json -> { "submitBtn": "Submit" }

// Ww locales/fr.json -> { "submitBtn": "Soumettre" }

// Wk locales/es.json -> { "submitBtn": “Enviar” }

// CONFIG:

// Wr config/i18n.ts

import en from '../locales/en.json';

import fr from ‘../locales/fr.json';

import es from '../locales/es.json";

const locales = { en, fr, es } as const;

type SupportedLang = keyof typeof locales;

const Garrentiang Supported ang =
(process. env.RUN_LANG as Supportediang) || ‘en’;

export const i18n = locales[currentlang];

// PAGE OBJECT:

17 B pages/LoginPage ts

import i! Locator, Page } from '@playwright/test';

import { i18n } from '../../config/i18n";

ie ep clea FEN —

export class LoginPage {
constructor (private readonly page: Page) {}
// X BAD: This hardcoded locator breaks the moment you run tests against the French or Spanish site.
get submitButton(): Locator {
) return this.page.getByRole('button', { name: 'Submit' });

" ///B GOOD: By using the i18n object, Playwright dynamically inserts the correct string
. 1 depending on which JSON fiie was resolved at startup via the RUN_LANG en var.
get submitButton(): Locator {
return this.page.getByRole( "button", { name: i18n.submitBtn });
—————
}

---

## 61. pov: you told the ai to "use page object model"it nodded. it generated code. it was technically corr…

### Tekst postu

pov: you told the ai to "use page object model"it nodded. it generated code. it was technically correct.but it was a messsome locators were properties. some were methods. assertions were scattered everywhere. documentation was missing"page object model" is a concept. not a specification. ai needs a blueprint, not a philosophyso i stopped asking for "pom". i started demanding a specific shape.i enforced three strict rules:- locators are getters. they must be evaluated fresh every time. no stale element references.- jsdoc is mandatory. if you don't document the method, the ai guesses the intent. guesses lead to bugs- actions verify completion. don't just click. click and wait for the state transitionnow my page objects look like this (see the code snippet below)the more prescriptive your patterns, the more consistent your codedon't just ask for a house. give the ai the blueprintsps: do you define locators as properties or getters? episode 6 of 15. standardizing the structure.

### Tekst z obrazka

Problem: “Use Page Object Model* isn't specific enouah for Al:
+ Should locators be methods or properties?
+ Where do assertions belong?
+ How do components compose?
Solution: Define explicit structural rules:
Code Example:
inport { expect, Locator, Page } fron 'eplaywright/test's
export class Loginpage {
constructor(private readonly page: Page) {}
I mmmmmmmeens [gcators (as getters) memmmmmmsmmmmmmmm
get emailInput(): Locator {
return this.page.getByLabel( Enail');
ha
++ The subnit button. +/
get submitButton(): Locator {
return this.page.getByRole( button’, { name: ‘Sign in’ });
ha
+ Logs in with the provided credentials
+ email - User enail address
* password — User password
async login(enail: string, password: string): Promise<void> {
await this.emaillnput.fill(email);
await this.page.getBylabel( Password’). fill(password);
await this.submitButton.click();
await this.page.waitForURL( ss/dashboard’};
i
}
[Er
+ Locators as getters (fresh evaluation)
+ J8Doc on all public methods
« Actions return Pronise<void>
+ Assertions inside actions verify completion
Takeaway: The more prescriptive your patterns, the more consistent your Al-generated code.

---

## 62. stop writing scripts. start building systemson monday we looked at the high-level roadmap. today, le…

### Tekst postu

stop writing scripts. start building systemson monday we looked at the high-level roadmap. today, let's dive into the exact steps to go from "script writer" to "framework architect"if your automation feels messy or hard to maintain, you are probably stuck on one of these levels:🔸 level 1: the foundation - stop using any (it completely dismiss the benefits of typescript. structure your test data using interfaces so it actually matches your app🔸 level 3: the blueprint - stop writing brittle tests. use the page object model to hide your private locators. use getters for efficient lazy evaluation🔸 level 4: the flow - stop fighting the browser. use try / catch to handle random popups without crashing. use promise.all to drastically speed up your execution🔸 level 6: the architect avoid the massive "god object". use composition and mixins to snap your classes together based on exactly what the test needstypescript isn't about adding complexity. it's about adding confidencei put together this full carousel breaking down all 6 levels. there is even a syntax cheat sheet on the very last pagewhich level is your current framework sitting at?

pov: the developer changed a css class nameand 50 tests failedit wasn't a bug. the app worked fine. the button still clicked. the form still submittedbut the tests were brittle. they relied on `.btn-primary`. they relied on `#submit-button`. they relied on implementation details that users don't care aboutso i deleted the css selectors. i banned xpath. i told the ai: "if the user can't see it, you can't test it"we established a strict hierarchy. a priority order for survival- getbyrole (the king. accessibility first)- getbylabel (for forms. if it has a label, use it)- getbyplaceholder (the fallback for inputs)- getbytext (for static content)- getbytestid (the absolute last resort)now, when the design team changes the button from blue to green? the test passes. because it's still a button. it still says "submit"semantic selectors are accessibility selectors. they target meaning, not markupps: is data-testid your first choice or your last resort? episode 5 of 15. writing tests that survive refactorshasztaghasztag

### Tekst z obrazka

Problem: Without a defined hierarchy, Al (and developers) make inconsistent choices:
+ Onetestuses getByTestId("submit-btn')
+ Another uses page. locator (".btn-primary")
+ Athird uses page. locator (*#submitButton')
All select the same element. None are maintainable.
Solution: Define a strict priority order:
1. getByRole() ~ Accessibility-based (buttons, links, headings)
2. getBylabel() ~ Form inputs with labels
3. getByPlaceholder() ~ Inputs without labels
4. getByText() ~ Static content
5. getByTestId() ~ Fallback only
Code Example:
7/ Priority 1: getByRole (preferred)
page. getByRole( button’, { name: ‘Submit’ });
page. getByRole( textbox’, { name: 'Email' });
Priority 2: getBylabel (form fields)
page. getByLabel(' Password’);
// Priority 5: getByTestId (last resort
page. getByTestId( user-avatar'); // Only when no semantic option exists
Forbidden patterns:
// NEVER use XPath
page. locator ('//div @id="test"]');
// AVOID CSS as primary strategy
page. locator (".btn-primary');
page. locator ('#submit-button');
Takeaway: Semantic selectors are accessibility selectors. They survive redesigns because they target meaning, not
implementation.

---

## 63. stop trying to learn "all of typescript" before you start writing automationyou don't need to be a f…

### Tekst postu

stop trying to learn "all of typescript" before you start writing automationyou don't need to be a frontend developer. you just need a safety netif you want to move from manual testing to automation (or upgrade your messy javascript framework), you need a clear plan. here is the exact roadmap:phase 1: building the foundation 🔸 core types - strings, numbers, arrays. make sure your test data actually matches the app🔸 logic & reusability - loops and functions. stop copy-pasting code. keep it dry🔸 interfaces - create strict "contracts" for your object shapes. this is how you validate api payloads without guessingphase 2: framework architecture ✅ scalable page objects - organize locators with classes, private modifiers, and getters. hide the ugly stuff from your tests✅ async mastery - stop fighting the event loop. control the flow with async/await and use promise.all for much faster execution✅ advanced safety - use enums to ban silly typos. use generics to build api clients you can reuse everywheretypescript catches runtime errors before your pipeline failswhich phase are you currently sitting in?

### Tekst z obrazka

The TypeScript for
Automation QA Roadmap
TypeScript serves as a safety net for QA, catching runtime errors during development.
This roadmap transitions from basic variable annotations to professional
architectural patterns like the Page Object Model (POM) and asynchronous mastery.

PHASE 1:
BUILDING THE
FOUNDATION
Jo = []
number
string
0HO-O amay O
@ &)
— ——
—— —_—
Core Types & Logic & i
Collections Reusability Custom Types
Define strings, Use functions and Create ‘contracts’ for Q
numbers, and arrays loops to eliminate object shapes to
to ensure test data repetitive code and validate API payloads
matches application implement DRY and response models.
shapes. principles.
1 PHASE 2:
A PROFESSIONAL FRAMEWORK
\ ARCHITECTURE
Ep)
je——t
ress =) =
Enum @” Client
[EE C3) Promise.all
== LJ Generic
Scalable Page Asynchronous Advanced Safety
Object Model Mastery Patterns
Organize locators using Control test flow with Implement Enums to prevent
Classes with private modifiers async/await and use typos and Generics for
and modern Getters for Promise.all for faster highly reusable API clients.
efficiency. parallel execution.

---

## 64. pov: you see 'new loginpage(page)' in a pull requestit works. but it smellsmanual instantiation is a…

### Tekst postu

pov: you see 'new loginpage(page)' in a pull requestit works. but it smellsmanual instantiation is a trap. your tests know too much about how objects are built. if the constructor changes? you have to fix 50 different filesand when ai writes the tests? it does it differently every time. sometimes a factory. sometimes a class. sometimes inlineso i banned the 'new' keyword in spec files. we moved to dependency injection. in #Playwright, that means fixtureswe don't just use fixtures for data setup. we use them as our DI containerwe define the objects once. loginPage: async ({ page }, use) => await use(new LoginPage(page))and the tests just ask for what they need. test('should login', async ({ loginPage }) => { ... }clean signatures. centralized logic. zero boilerplatefixtures aren't just for setup. they are the backbone of a scalable architectureps: do you use fixtures for page objects or just data? episode 4 of 15. decoupling the test logichasztaghasztag

### Tekst z obrazka

+ Tests know too much about page object construction
+ No centralized setupfteardown
« Harder to swap implementations
+ Al generates inconsistent instantiation patterns
Solution: Use Playwrights fixture system fo dependency nection:
Code Ei
// Fixtures/pon/page-object-fixture.ts
export const test = base. extend<FrameworkFixturess ({
loginPage: async ({ page }, use) => {
await use(new LoginPage(page));
h
dashboardPage: async ({ page }, use) => {
await use(new DashboardPage (page) );
}
bi
// In tests — page objects are injected
test(*should login’, async ({ loginPage, dashboardpage }) => {
await loginPage.login(email, password);
await expect(dashboardPage.welcomeMessage). toBeVisible();
bi
Single import point for all tests: al
// Every test file imports from here
import { expect, test } from '../fixtures/pom/test-options';
Takeaway: Fixtures aren't ust for setup — they're your dependency injection container. Use them for
‘everything tests need.

---

## 65. pov: you let the ai guess the selectorsit was confident. it wrote the code in seconds. it assumed th…

### Tekst postu

pov: you let the ai guess the selectorsit was confident. it wrote the code in seconds. it assumed there was a 'submit' button. it assumed the email field had an idthen i ran the test. fail. element not foundthe ai wasn't coding against my app. it was coding against a hallucination. it was building based on probability, not realityso i introduced a strict rule. explore before you generatenever ask for code immediately. force the discovery phasemy prompts now look like this:- navigate to the target url- read the accessibility tree- capture the real labels and roles- then write the page objectthe result? i get getByRole('button', { name: 'Sign in' }) instead of a broken guess for 'Submit'10 seconds of exploration. saves 10 minutes of debugging. never let the ai code blindps: how often does ai hallucinate elements in your tests? episode 3 of 15. grounding the ai in reality

### Tekst z obrazka

Problem: Al FLITE page objects based on ERE
+ "There's probably a button called Submit"
« "The email field likely has a label"
« "The API response probably has an id field"
These assumptions create locators that fail on first run.
Solution: Mandate Clie dn before IEEE
## AL Workflow
1. Navigate to the target page or API endpoint
2. Capture actual element roles, labels, form structure
3. Generate code based on discovered reality
Code eal
'# Prompt with exploration
Create a page object for the login page.
First, navigate to https://app.example.con/login and discover:
~ Element roles and accessible names
~ Form field labels
~ Button text
~ Error message patterns
Then generate the page object with accurate locators.
Result: getByRole(button*, { name: 'Sign in‘ }) instead of guessing getByRole( button’,
{ name: 'Submit' }). Having the right context, minimizes the time spent in debugging
Takeaway: 10 seconds of exploration saves 10 minutes of debugging. Make discovery a required step, not
fo [i

---

## 66. writing complex xpath is the most useless skill you can masteri see engineers bragging about it"look…

### Tekst postu

writing complex xpath is the most useless skill you can masteri see engineers bragging about it"look at this 50-character unreadable query i wrote"congratulations. go back and refactor it. you just built technical debtthree reasons why i banned xpath in my teams:1. it tests the dom, not the userusers don't click //div[2]/span. they click "checkout"if you test the structure, you are testing implementation details2. it is invisible to accessibilityyour xpath test passes even if the button is hidden from screen readers, because it lacks accessibility tagthat is not a feature. that is a bug3. it breaks on refactordevelopers move divs all the timewhy write a test that needs maintenance every sprint?real seniority isn't about mastering complex syntaxit's about choosing tools that don't require maintenancestop trying to be a "selector wizard"start being a product verifieruse getByRole. sleep better at night

### Tekst z obrazka

MODERN LOCATOR HIERARCHY & BEST USE
Priority Locator Best Situational Why it's “Smart
Strategy Use Engineering”
5 Binds to
Interactive Elements. wry
1 [RR oY id
(Highest) Y checkboxes, tabs, usability, Survived
headings. 99% of refactors.
Mimics user behavior
Form Inputs. ("I need to typeii
ype into
“getByLabel® |e fields, dropdowns = \pmgiy),
<label>. Encourages
: accessible forms.
2 Simple for content
4 Static Content. verification. Use
getByText™ Error messages, &E (exact: true }o
toasts, paragraph text. avoid accidental
matches.
inpne without Decent fallback,
“getBy ShEhta cl better than CSS, but
Placeholder placeholder is the logs pecesibietian
only cue. §
) Me topetoar. SEI
: «Complex grids, iii :
Ee) getByTestId dynamic SVGs, legacy fel rel” osiyieS
oe user-centric.
Legacy Bite Apps. Ie ple eviaton
argeting by letails*. Brittle, hard to
XPath / CSS hierarchy or CSS (x) read, breaks constantly
classes. in modern frameworks.

---

## 67. you delete half your boilerplate codestop instantiating page objects manually in every single testit…

### Tekst postu

you delete half your boilerplate codestop instantiating page objects manually in every single testit creates noise. it kills readabilityyesterday i listed this as a "must" rule. today i am sharing the codethe fix takes five minutes. use #Playwright custom fixturesextend the base test object. inject your pages automatically. never write `new LoginPage(page)` againthe results are immediateless clutter. clearer tests. pure logicps: have you switched to custom fixtures yet?hasztaghasztag

### Tekst z obrazka

[xx
* TIP: Extend Playwright's base test with custom fixtures for Page Objects.
*
# This provides:
* = Automatic instantiation of page objects
* = pests access in test functions
4) - Cleaner test syntax without manual setup
*
import { test as base, Page } from "@playwright/test";
// Example Page Object classes (in real projects, import from separate files)
class LoginPage {
N Gonstructor (private page: Page) {}
class DashboardPage {
N constructor(private page: Page) {}
// Declare the types for your custom fixtures
type MyFixtures = {
.oginPage: Loginpage:
) dashboardPage: DashboardPage;
i
// Extend theibase Est ith yous Eines «
export const test =\base.extend<MyFixtures>
Toginbage: asynelic {DRIER YTS SET AS
await use(new LoginPage(page));
3 PELE EAL CE DO FIXTURE
dashboardPage: async ({ page }, uss = {
’ await usetnew DashboardPage(page));
13
// Re—export expect for conveniance 11
export { expect } from “@playwright/test"; JJ

---

## 68. your tests pass but production is brokenthe dashboard is green. but the api response changed. just o…

### Tekst postu

your tests pass but production is brokenthe dashboard is green. but the api response changed. just one parameter gone. silent drift without a tickettests missed it. because we checked values. not the contractif you use #Playwright, you need #Zodstop guessing. define the schema. infer types with z.inferone schema. zero driftthis is end-to-end type safetyps: are you validating schemas or just status codes?hasztaghasztaghasztaghasztag

### Tekst z obrazka

VEsS
* TIP: Use Zod for runtime validation of API response schemas.
*
* This ensures your API returns the expected structure and types,
* catching contract violations early.
*/
import { test, expect } from "@playwright/test";
import { z “zod";
Define the expected schem: ing Zod
#~ const UserSchema = z.object({
user: z.object({
email: z.string().email(),
username: z.string(),
bio: z.string().nullable(),
image: z.string().nullable(),
token: z.string(),
1,
// Infer TypeScript type from the schema (optional, for type safety)
type User = z.infer<typeof UserSchema>;
test("Validate login API response schema", async ({ request }) => {
const response = await request.post("/api/users/login", {
data: { —————
user: {
email: "test@example.com",
password: "password123",
h
h
b;
expect (response. status()).toBe(200);
const responseBody = await response.json();
// Validate the response matches the expected schema CRITICAL
// This will throw if the structure doesn't match STEP
const validatedData = UserSchema. parse( responseBody) ;
// Now you have type-safe access to the data
expect(validatedData.user.email).toBe("test@example. con");
yeEsctualipdostar user. token). toBeDefined(); oe
oH

---

## 69. using XPath for locators is Technical Debtwith #Playwright you can drill down naturally using filter…

### Tekst postu

using XPath for locators is Technical Debtwith #Playwright you can drill down naturally using filteringhere is how(see the code in the image):1. find the card2. filter for the specific text3. grab the button inside itwrite code for humans first, not only for the machineshasztaghasztag

### Tekst z obrazka

// Brittle, hard to read, and scary to touch later.
) await page. locator (*//div[@class=\"card\"] [. /h3[text()=\"
iPhone 15\"11//button'). click ();
[Ad — :
7l Reads like English: “Find the card with ‘iPhone I5',
then click Add."
await page. ByRole ('listitem')
hr og fot od ‘iPhone 15' })
.getByRole ('button’, { name: ‘Add' }) In
click 0); °o

---

## 70. Stop over-engineering your Page ObjectsI often see overcomplicated frameworks just for the sake of C…

### Tekst postu

Stop over-engineering your Page ObjectsI often see overcomplicated frameworks just for the sake of Clean CodeA common debate in Page Object Model design is how do we handle happy paths vs. negative testingThe textbook answer is often to split the logic:performLogin() (Just actions)loginAndVerify() (Actions + Assertions)I prefer a simpler approachI use a single method with a default parameter`async logIn(username, password, isLoggedIn = true)`Here is why this wins:================- For all happy path tests, I just call logIn(user, pass). The default parameter handles the safety check automatically- For all my negative tests, I explicitly pass false. The code reads just like "Log in, but don't expect success"- New team members don't have to guess which method to use. There is only one.Is passing a boolean flag a Clean Code violation? Maybe. Does it make my test suite cleaner and easier to maintain? Absolutely.

### Tekst z obrazka

1! Oonsimcthad to handle both scenarios Gomera = m3)
async logIn(username: string, pass: string,
// 1. Always perform i actions Be
await this,usernameInput.fill (username);
await Jhi%;passvorcinpushll] (pass);
await this. loginButton.click();
// 2. Conditionally validate (Enabled by default)
if (expectSuccess) {
Se // The "Contract": Ensure we setually logged in |
wm | a expect (this. page). toHaveURL(' /dashboard');
else Le  — TT
// The "Contract": Ensure we stay on login page
) await expect (this.page).toHaveURL('/login');
} . . =
// Usage in Tests:
// Happy Path (Clean & Safe)
await [ginPage . logIn( admin’, '1234');
// Negative Test (Explicit & Flexible) 1]
await loginPage.logIn('admin', 'wrong_pass', false); ..

---

## 71. If you are initializing User Access Tokens inside a beforeEach hook or a standard Fixture, you are s…

### Tekst postu

If you are initializing User Access Tokens inside a beforeEach hook or a standard Fixture, you are slowing down your test suiteI recently saw a framework where every spec file re-authenticated the "Admin" user.100 tests = 100 login requests.That is unnecessary noise and latency.The Fix: Use Playwright's "Setup Projects" (Global Setup).Instead of treating Auth as a "Test Step", treat it as Pre-Test Configuration.Create a Setup Project: Define a dependency in your `playwright.config.ts`Run Auth Once: Execute the login request one time before the test workers spin up.Store Globally: Save the token (or storage state) to process.env or a generic JSON.Now, every test in your suite simply reads process.env.ACCESS_TOKEN_ADMIN and starts validating immediately. No login screen, no API lag.Efficiency isn't just about fast tests. It's about smart architecture.Save this for your next refactoring session.How do you handle Auth in your parallel runs - Storage State files or clean API tokens?

### Tekst z obrazka

import { test as setup, expect } from '../fixtures/pom/test-options';
import { User } from '../fixtures/api/types-quards';
import { UserSchema rom '../fixtures/api/schemas';
setup('auth user', async ({ apiRequest }))=> {Runs ONCE globally
etup.step('auth for ADN by APT', async () => {
const { status, body } = await apiRequest<User>({
method: 'POST', —_—
url: ‘'api/users/login’, Type-Safe Request
baseUrl: process.env.API_URL,
body: {
user: {
email: process.env.EMAIL_ADMIN,
) password: process.env.PASSWORD_ADMIN,
'
wh Injects into
expect(status).toBe(200); ALL workers
expect (UserSchema.parse(body)) . toBeT#uthy) ;
CRITICAL
5 process, env['ACCESS_TOKEN_ADMIN'] = body user. token; | STEP
P
o await setup.step('auth for USER by API', async () => {

---

## 72. lways forget your #Playwright commands?Use 'package.json' to solve this foreverEver been annoyed by …

### Tekst postu

lways forget your #Playwright commands?Use 'package.json' to solve this foreverEver been annoyed by the need to type Playwright commands over and over again in the terminal?Every day I had to type 'npx playwright test --project=chromium --ui' and I even created a txt file with all these commands.Then I found that there is an easy solution for such problem - just define all commands I needed into my `package.json` and use them in my terminal and CI/CD pipelines.You can do it too:1. Add all commands you need into "scripts" section in your 'package.json file'. You can find mine in the image below2. Use the command you need by 'npm run <​script name>'Life can be so much easier!hasztaghasztag

### Tekst z obrazka

"COMMENT": “TIP: Define NPM scripts for common test execution patterns.",

"RUN": "Run with: npm run <script-name>", .

“scripts”: {
"test": "npx playwright test --project=chromium",
“test:ci":"npx playwright test --project=chromium --workers=1",

Cf ao cin ow >
CRITICAL “test:debug": "npx playwright test --project=chromium --debug™,
STEP “test:ui": "npx playwright test --project=chromium --ui",

"test:smoke": "npx playwright test --grep @Smoke --project=chromium",
“test:sanity": “npx playwright test --grep @Sanity --project=chromium",
“test:api": "npx playwright test --grep @Api --project=chromium",
“test:regression": “npx playwright test --grep @Regression --project=chromium",
“test:isolated": "npx playwright test --grep @Isolated --project=chromium --workers=1",
"fullTest": "npx playwright test" Il]

} —_—

}

---

## 73. 1 minute #Playwright trick to perform visual regression tests for UI with dynamic dataThese tests ar…

### Tekst postu

1 minute #Playwright trick to perform visual regression tests for UI with dynamic dataThese tests are powerful but flaky if you have dynamic content like dates, timestamps, or user IDs.You don't need to disable the test! Just use the mask option.Playwright will draw a pink box over these elements before taking the screenshot, ensuring your comparison is always stable.What is your strategy for dynamic data in visual tests?hasztaghasztag

### Tekst z obrazka

import { test, expect } from '@playwright/test’;
test('Visual Check of Dashboard’, async ({ page }) => {
await page.goto('/dashboard');
——————
\\ // Mask dynamic elements like dates or random IDs
‘e
await expect(page).toHaveScreenshot('dashboard.png', {
mask: [ SK THE CRUE
page. locator (' re i a.
page. locator ('[data-testid="transaction-id"]"),
1
);
oH]

---

## 74. 1 minute #Playwright trick to use data, coming as API Response, triggered from UI TestYou should sim…

### Tekst postu

1 minute #Playwright trick to use data, coming as API Response, triggered from UI TestYou should simply use `Promise.all` and run simultaneously both UI function, that sends the API request, and to listen for API Response from the request sent.As an example, we can use `Promise.all` to get the resource ID for test.afterAll, which ensures created resources are deleted via an API call, even after a test fails.hasztaghasztag

### Tekst z obrazka

Promise.all in Ul Tests for Interaction with API Responses
import { test } from 'Gplaywright/test';
test.describe('Test Article Creation’, () => {
let articleld: string;
test('Promise ALL’, { tag: '@smoke' }, async ({ page, articlePage }) => {
const [publishActionResult, response] = await Pronmise.all([
articlePage.publishArticle(title, description, body, tags),
page .waitForResponse( #*/api/articles/'),
Di
const responseBody = await response.json();
articleld = responseBody.id;
await expect(page.getByRole('heading', { name: title })).toBevisible();
i
test. afterALL(
‘Cleanup: Delete the created article’,
async ({ articlePage }) => {
await articlePage.deleteArticle(articleld);
+
iH
bn;

---

## 75. The 1-minute #Playwright trick that saves hours of setupStarting a new automation project often feel…

### Tekst postu

The 1-minute #Playwright trick that saves hours of setupStarting a new automation project often feels like reinventing the wheel. You spend days just setting it up before writing a single test.That is why I want to share Playwright Scaffold.This is a production-ready test automation framework built with TypeScript and Playwright. The goal is to provide a solid foundation for UI, API, and E2E testing so you can start coding immediately.Here is what is included out of the box:---------------------------------------🏗️ Structured Architecture - It uses the Page Object Model design pattern. This keeps your code organized and scalable as your test suite grows.💉 Smart Dependency Injection - The framework utilizes fixture-based dependency injection to manage state and context cleanly.🔍 API Validation - It includes Zod schema validation. This gives you strict type safety and confidence when testing your APIs.🛡️ Quality & Tooling - Code quality is enforced from the first commit. It comes pre-configured with ESLint, Prettier, and Husky.hasztaghasztag

1-minute #Playwright trick to Test File Downloads without headachesTesting file downloads can be flaky if you rely on checking the file system manually. Playwright has a built-in waitForEvent('download') that handles the handshake perfectly.Why this wins:✅ Reliable - It waits for the browser event, not a file system watcher.✅ Clean - You get a stream you can validate instantly.✅ Safe - No need to hardcode download paths.Have you ever had a download test fail because the file wasn't ready yet?hasztaghasztag

### Tekst z obrazka

Verify File Download

import { test, expect } from '@playwright/test';
test('Download and verify newsletter', async ({ page }) => {

await page.goto('/newsletter');

const downloadPromise = page.waitForEvent('download');

await page.getByText('Download Newsletter').click();

const download = await downloadPromise;

expect(download.suggestedFilename()).toBe('newsletter.pdf');

await download.saveAs('./downloads/' + download.suggestedFilename());
Bn;

---

## 76. Here is a 1-minute #Playwright trick to test edge casesTesting specific scenarios like "Empty States…

### Tekst postu

Here is a 1-minute #Playwright trick to test edge casesTesting specific scenarios like "Empty States" or server errors often requires complex data setup. You usually have to modify the database or corrupt data in a staging environment just to see how the UI reacts.That approach is slow and often risky.Instead, you can use Network Interception. This allows you to force the application to handle specific scenarios without actually touching the backend data.On the image is shown how you can verify a "No users found" message by mocking the API response.Why this works well:----------------------✅ Speed: You skip the heavy database setup. ✅ Stability: You get the exact same response every time. ✅ Safety: You do not risk deleting real data in your test environment.This simple pattern gives you total control over the data your frontend consumes.hasztaghasztag

### Tekst z obrazka

Mock the API response
import { test, expect } from '@playwright/test';
test('Verify "No users found" when the user API returns an empty list', async ({
page,
b={
await page.route('sx/api/users*', async (route) => {
await route. fulfill({
status: 200,
contentType: 'application/json’,
json: [1,
Hi;
Bb;
await page.goto('https://idavidov.eu/users');
await expect(page.getByText('No users found')).toBeVisible();
Bb;

---

## 77. The "Happy Path" is a myth.We often pretend that users will follow the flow exactly as we designed i…

### Tekst postu

The "Happy Path" is a myth.We often pretend that users will follow the flow exactly as we designed it. But reality is much messier than our test cases.In the real world:------------------🔸 Users rage-click the submit button. 🔸 API calls time out without warning. 🔸 Mobile networks drop when the user enters an elevator. 🔸 Inputs contain characters the database hates.If you only validate the happy path, you haven't actually tested the software. You have only tested your hopes.True quality assurance isn't about proving it works when conditions are perfect. It is about ensuring the system remains stable when conditions are terrible.Don't just verify the sunshine. Focus on the rainy day scenarios.What is the wildest "edge case" bug you have found that a happy path test would have missed?

---

## 78. 1 minute #Playwright trick to catch all bugs in a single test caseStandard assertions are ruthless. …

### Tekst postu

1 minute #Playwright trick to catch all bugs in a single test caseStandard assertions are ruthless. They stop the test immediately when they fail. This often creates a frustrating cycle where you fix one bug only to find the next one immediately after re-running the suite.Enter Soft Assertions.Soft assertions log the error but allow the test to finish execution. This is perfect for checking multiple UI elements like labels, inputs, or layout consistency in one go.By switching to `expect.soft` in your code the test continues running even if checks fail. The framework collects all the errors and reports them at the very end of the test case.Why this matters:🔸 You get a complete picture of everything that is broken on a page in a single execution. 🔸 You save time by grouping your fixes rather than hoping there are no more.This is a total game-changer for validating complex areas like forms, large data tables, and dashboards.Does your framework fail fast or fail smart?hasztaghasztag

### Tekst z obrazka

Soft Assertions
import { test, expect } from 'Gplaywright/test';
test('Validate Product List Prices’, async ({ page }) => {
await page.goto('/products');
const expectedProducts = [
{ name: 'Laptop', price: '$999' }
{ name: 'Mouse', price: '$25' }
{ name: 'Keyboard', price: '$75' }
1;
for (const product of expectedProducts) {
const row = page.getByTestId( product-row-${product.name})
await expect
.soft(row, ‘Check if ${product.name} is visible
.toBeVisible();
await expect.soft(row.locator('.price')).toHaveText(product.price);
+
Bb;

---

## 79. Dominoes are for games, not for tests. Strictly follow the Test Isolation Principle.If Test A fails,…

### Tekst postu

Dominoes are for games, not for tests. Strictly follow the Test Isolation Principle.If Test A fails, Test B should not care. If Test B depends on Test A’s data, you don't have a test suite. You have a house of cards. One minor UI change shouldn't trigger 50 unrelated red flags.Why isolation matters:-------------------------➡️ Zero Side Effects: One test’s "garbage" shouldn't become another test’s "input"➡️ Order Independence: You should be able to run your tests in reverse, or in parallel, without a single failure.➡️ Debugging Sanity: When a test fails in an isolated environment, you know exactly where the issue is. You don't have to spend two hours "chasing the ghost" through three previous test files.How to enforce it:--------------------➡️ Reset state between tests: Every test starts from a "clean slate."➡️ Use Hooks: Leverage test.beforeEach to set up specific conditions and test.afterEach to tear them down.➡️ Avoid Shared Global State: If you’re using a database, use transactions or unique IDs for every run to prevent data bleeding.Isolation is the key to CI/CD confidence. If your tests are flaky, your team will eventually stop trusting them. And a test suite that no one trusts is just expensive noise.

---

## 80. 1 minute #Playwright trick for Polling for Non-Deterministic StatesSometimes an element doesn't appe…

### Tekst postu

1 minute #Playwright trick for Polling for Non-Deterministic StatesSometimes an element doesn't appear immediately, or a status takes time to update. `expect.poll` allows you to retry a check until it passes or times out.Waiting for a database sync or a long-running process? await page.waitForTimeout(5000) is a bad practice! ❌Use `expect.poll` instead. It keeps retrying the assertion until it passes or the timeout is reached. It returns as soon as the condition is met.hasztaghasztag

### Tekst z obrazka

Polling for Non-Deterministic States
import { test, expect } from '@playwright/test';
test('Ship an order via UI, and Verify the updated status', async ({
page,
request,
B=
let orderId: number;
await test.step('GIVEN: A new order is pending', async () => {
const response = await request.post('/api/orders/create', {
data: {
item: 'Playwright Conference Ticket,
quantity: 1,
customer: ‘Ivan Davidov'
i
Bi
expect(response.ok()). toBeTruthy();
const json = await response.json();
orderId = json.id;
BD;
await test.step('WHEN: The order is shipped via UI', async () => {
await page.goto(’/orders/${orderId}');
await page.getByRole('button', { name: 'Ship Order' }).click()
1D;
await test.step('THEN: The order status is updated to Shipped’, async () => {
await expect
-poli(
async () => {
const status = await page
.getByTestId('order-status')
.textContent();
return status?.trim();
I,
{
message: 'Order status failed to update to Shipped’
intervals: [1000, 2000, 5000]
timeout: 30000,
+
)
.toBe('Shipped');
BH;
BD;

---

## 81. Why process always comes before automation?Automation is a force multiplier. But multipliers only am…

### Tekst postu

Why process always comes before automation?Automation is a force multiplier. But multipliers only amplify what is already there. If you automate a messy, undefined process, you simply get messy results. The only difference is that you get them faster.You can’t expect a testing script to save the day if the development workflow is broken. This is especially true for large teams, where lack of structure doesn't just slow you down. Instead it scales the chaos.Before you build, you must organize.The Golden Rule:🏛️ Stabilize: Define the steps and remove the friction.📄 Standardize: Ensure everyone follows the same path.⚙️ Automate: Use tools to speed up that proven path.In simple words - refine the flow first, then build the robots.

---

## 82. Stop using #Playwright for clicking through the UI to set up your test dataIf your E2E tests are tak…

### Tekst postu

Stop using #Playwright for clicking through the UI to set up your test dataIf your E2E tests are taking forever, it’s usually because you are doing everything via the browser.Here is a simple pattern to speed up your Playwright suite - the API Shortcut.Instead of logging in, clicking "Create New", filling out a form, and clicking "Save", you just hit the backend API directly.The Strategy:1️⃣ Arrange: Use request to inject data in milliseconds2️⃣ Act & Assert: Use page to verify the user actually sees it.Why this wins:✅ Stability: Fewer UI steps mean less flakiness✅ Speed: API requests are instant compared to browser rendering✅ Focus: You test the specific feature, not the data entry formBelow is a quick example of creating a product via API, then checking the UI.hasztaghasztag

### Tekst z obrazka

Using API to Setup the Data Needed for the Test
import { test, expect } from '@playwright/test';
test('UI should display newly created item', async ({ page,
request }) => {
const response = await request.post('/api/products', {
data: {
name: 'Playwright T-Shirt',
price: 25.08,
},
Bb;
expect(response.status()).toBe(201);
await page.goto('/products');
const productCard = page.locator('.product-card', {
hasText: 'Playwright T-Shirt’,
bi;
await expect(productCard).toBeVisible();
await expect(productCard).toContainText('$25.00');
Bb;

---

## 83. #Playwright has time machine for your BugsScreenshots and videos can show you *what* happened.Trace …

### Tekst postu

#Playwright has time machine for your BugsScreenshots and videos can show you *what* happened.Trace Viewer shows you *why* it happened. It captures DOM snapshots, console logs, and network calls for every action.Simply it is a full time-machine for #debugging.Do you actively use it for debugging bugs, or it is not part of your toolbox?hasztaghasztag

---

## 84. he single most important Trick for #Playwright Automation Testing FrameworkIt is not the Fixtures. I…

### Tekst postu

he single most important Trick for #Playwright Automation Testing FrameworkIt is not the Fixtures. It is not the Design Pattern. It is certainly not Zod schema validation.It is joining the community!This is where you learn something new every day. The most successful engineers in IT share one common thing - they never stop learning. They strive for perfection, and that collective effort is why our industry progresses so rapidly.You can learn every trick in a given field, yet if you don't know *Why* to use it or *How* to apply it effectively, it will be worthless.I am deeply grateful for the last few years I’ve been part of this community. It has been essential to my own growth. Now, I want to pay that forward. I am trying my best to help as many fellow QAs as possible build their skills, just as the community helped me build mine.hasztaghasztag

### Tekst z obrazka

The Single Most Important Trick for Playwright
inport { test, expect, type Page } from '@playwright/test';
test('The Single Most Important Trick for Playwright', async ({ page }) => {
const candidates = ['Fixtures', 'Design Patterns', 'Zod Schema'l;
candidates. forEach(c => expect(c).not.toBe('The Secret'));
const theSecret = ‘Joining the Community’;
expect (theSecret). toBeTruthy();
await test.step('Growth Mindset’, async () => {
const engineer = { learning: ‘continuous’, sharing: ‘essential’ };
expect(engineer.learning).toBe('continuous');
bi
await test.step('Pay it Forward’, async () => {
await page.goto(’https://idavidov.eu');
await page.getByRole('button’, { name: ‘Share’ }).click();
await page.getByPlaceholder('What is important to you?').fill('Comment
below!');
Bb;
bi

---

## 85. 5-minute #Playwright trick - Login once, test forever.Implement `setup` stage before your actual tes…

### Tekst postu

5-minute #Playwright trick - Login once, test forever.Implement `setup` stage before your actual test run. Save your cookies/local storage to a `auth.json`. Inject it into other tests. Skip the login screen and jump straight to the feature. Save minutes on every test run.Additionally, you can set your tokens too.P.S. In the example below, I used a custom abstraction for API calls, implemented #POM as fixtures, #Zod for schema validation and #dotenv for managing environment variables. You can implement it by using Playwright out-of-the-box tools.hasztaghasztaghasztaghasztaghasztaghasztaghasztaghasztag

### Tekst z obrazka

auth.setup.ts
setup('auth user’, async ({ apiRequest, homePage, navPage, page }) => {
await setup.step('auth for user by API', async O) => {
const { status, body } = await apiRequest<User>({
method: POST"
url: 'api/users/login',
baseUrl: process.env.API_URL,
body: {
user: {
email: process.env.EMAIL,
password: process. env.PASSWORD,
}
)
bi
expect(status). toBe(200);
expect (UserSchena. parse(body)) . toBeTruthy();
process.env['ACCESS_TOKEN'] = body.user.token;
oH
await setup.step('create logged in user session', async () => {
await homePage.navigateToHonePageGuest();
await navPage.logIn(process.env.EMAIL!, process.env.PASSWORD!);
await page.context().storageState({ path: '.auth/userSession.json' });
Bb;
hi

---

## 86. implest way of implementing Data-Driven Automation is For...Of LoopTalking about the verification of…

### Tekst postu

implest way of implementing Data-Driven Automation is For...Of LoopTalking about the verification of software validations, we can't skip one of the most important tools - for...of loop.Data-Driven Automation can be easily implemented by:1. Creating an array of "chaos" (aka invalid data)2. Unleash a simple For...Of loop3. Sit back and watch the assertions flyIt’s clean and it’s scalableDo you automate them like this, or do you have a different secret you are hiding?

### Tekst z obrazka

( For...Of Loop
1 const invalidEmails = [
“testh;
3 "152445",
"test@test",
5 HEHHFE$SEH. com”,
"email@111.222.333",
7 "not”right@example.com",
8 "email..email@example.com",
9 ".email@example.com",
10 ____@example.com"
1;
12
for (const email of invalidEmails) {
await page.getByPlaceholder("Email").fill(email);
155] await page.getByText("Invalid Email").toBeVisible();
+

---

## 87. 1-minute #Playwright Performance Hack - Abort heavy and slow API request to improve the performance …

### Tekst postu

1-minute #Playwright Performance Hack - Abort heavy and slow API request to improve the performance of your testsIf you are testing functional logic (like a signup for newsletter form), you don't need to wait for high resolution marketing images to load. Aborting them speeds up the tests.Additionally, by using `page.route` to abort such resource you can easily improve stability of your UI tests and reduce the flakiness.Try it and share how it is going in your specific case.hasztaghasztag

### Tekst z obrazka

Abort Slow API Request.json
1 import { test } from '@playwright/test’';
5 test('Testign UI Without Waiting for Images to Load', async
({ page }) => {
await page.route('#*/%.{png, jpg, jpeg,svg,gif}', route =>
route.abort());
await page.goto('https://idavidov.eu/');
8 await page.getByRole('button', { name: 'Newsletter'
}).clickQ;
9 // rest of logic
18 BD;

---

## 88. UI tests are slow and flakyThey must be the tip of the pyramid, not the foundation. This place is re…

### Tekst postu

UI tests are slow and flakyThey must be the tip of the pyramid, not the foundation. This place is reserved for the API tests.Why? Because they provide:1. Speed: 100x faster. 2. Stability: No rendering issues. 3. Coverage: Test edge cases easily. Your UI tests should compliment your testing, not to be the only testing you perform. Support them with a massive API test layer.Tools like #Playwright are fantastic for automation testing, and do tremendous job handling the flakiness, but they cannot overcome the slow nature of these tests.hasztaghasztag

1-minute #Playwright FIX – Stop instantiating Page Objects inside your tests. Start using Custom Fixtures.Instead of instantiating Page Objects in every test, extend the Playwright test object to provide them automatically.By using this, so-called "Page Object Wrapper", you will save so much pain during development.The results are immediate: ⛔ less code✅ better readabilityhasztaghasztag

### Tekst z obrazka

POM as Custom Fixture.jsor
{ test base } 'e wright/test';
{ LoginPage } './pages/LoginPage';
{ DashboardPage } './pages/DashboardPage' ;
MyFixtures = {
loginPage: LoginPage;
dashboardPage: DashboardPage;
}
test = base.extend<MyFixtures>({
loginPage ({ page }, use) {
use( LoginPage(page));
+
dashboardPage ({ page }, use) {
use( DashboardPage (page)) ;
iB
BD;

---

## 89. ired of writing assertions in #Playwright? Here is what you can do:I hate typing the same assertions…

### Tekst postu

ired of writing assertions in #Playwright? Here is what you can do:I hate typing the same assertions or boilerplate code over and over again in my Test Automation Frameworks.That's why I use User Snippets - they can be configured in a few minutes and used forever.Below is an example of probably the most common assertion in UI tests - expecting a locator to be visible. By typing `exv` and hitting enter, you can instantly insert the complete code `await expect().toBeVisible();` with your cursor placed exactly where it should be - in the parentheses of the expect.Do not miss the opportunity to configure and use these snippets in your project.A complete guide with my snippets can be found below.hasztaghasztag

### Tekst z obrazka

userSnippet.json
1{
"Expect toBeVisible": {
“scope: "javascript,typescript”,
"prefix": "exv",
"body": ["await expect(${1}).toBeVisible();"],
"description": "Generate expect locator to be visible code"
+
i)

---

## 90. Using scripts from `package.json` is way easier than typing all #Playwright commands manuallyEver be…

### Tekst postu

Using scripts from `package.json` is way easier than typing all #Playwright commands manuallyEver be annoyed by the need to type Playwright commands over and over again in the terminal?There is an easy solution for such cases - just define this commands into your `package.json` and use them in your terminal and CI/CD pipelines.The life can be so much easier!Do you have additional ticks you are using in your projects?hasztaghasztag

### Tekst z obrazka

package json
{
“scripts”: {
“ui: “npx playwright test --project=chromium --ui®,
"flaky": "npx playwright test --project=chromium --repeat-each=20",
"debug": "npx playwright test --project=chromium --debug",
"smoke": "npx playwright test --grep @Smoke --project=chromium",
sanity": "npx playwright test --grep @Sanity --project=chromium",
“api”: "npx playwright test --grep @Api --project=chromium",
“regression”: "npx playwright test --grep @Regression --project=chromiun”,
"test": "npx playwright test --project=chromium",
"ci": "npx playwright test --project=chromium --workers=1"
+
he

---

## 91. Your API Response Changes Without Ticket, All Your Tests are Passing, and You Find Out in Production…

### Tekst postu

Your API Response Changes Without Ticket, All Your Tests are Passing, and You Find Out in Production!?How many times during development, a functionality that is working has been changed without a ticket or proper notification? Many times, adding or removing a parameter in the API response body goes under the radar, and that can lead to a huge defect.If you are using Playwright, the solution for that is simple - a library, called Zod. You have to define a schema and infer your types from it by method `z.infer`. One schema, two benefits, zero drift. This is called end-to-end type safety.

### Tekst z obrazka

Using Zod for Schema Validation
export const UserSchema = z.object({
user: z.object({
email: z.string().email(),
username: z.string(),
bio: z.string().nullable(),
image: z.string().nullable(),
token: z.string(),
}).strict(),
Bi
export type User = z.infer<typeof UserSchema>;
const { status, body } = await apiRequest<User>({
method: 'POST',
url: 'api/users/login',
baseUrl: process.env.API_URL
body: { email: email, password: password },
Bi;
expect (status) .toBe(200);
expect(UserSchema.parse(body)).toBeTruthy();

---

## 92. If you think a rigid, monolithic BasePage is the only way to share code between your Page Objects in…

### Tekst postu

If you think a rigid, monolithic BasePage is the only way to share code between your Page Objects in Playwright, please scroll down!Traditional inheritance (class A extends B) is powerful, but it has limits. In TypeScript, a class can only extend one other class.The Mixin Design Pattern lets you create reusable "feature packs". They are small classes focused on a single piece of functionality (like navigation or table handling). You can then "mix" these feature packs into any Page Object that needs them.This approach allows you to:🔰 Share functionality across unrelated classes🔰 Avoid deep inheritance chains, keeping your code flat and manageable🔰 Keep Page Objects clean and focused on their primary purposeThe article is a walk through a practical example of adding shared navigation functionality to a object without using inheritance.

---

## 93. BDD is Often Just Glorified, Poorly Written User Stories and Test ScriptsIt is bad that too many tea…

### Tekst postu

BDD is Often Just Glorified, Poorly Written User Stories and Test ScriptsIt is bad that too many teams adopt Gherkin syntax without the collaboration, missing the entire point.It is even worse when the QA is not part of the collaboration in User Story creation.But the worst thing is when the management insist on Cucumber implementation in the automation testing framework.The real value of Behavior-Driven Development is in the collaboration and conversation, not the Gherkin syntax, or in any tool. If you want a successful project - implement proper BDD - collaborative conversation that creates a shared understanding between product, development, and QA.

Since I am a Stubborn, I Want to Continue the Topic About Adding Assertions in the Methods in Automation Frameworks =>Many engineers point out that if we want to test the negative scenario of the method, that has assertion inside, we are stuck. It is partially true, because while we are implementing our method, we can add a default parameter (in our example isLoggedIn) to use the correct assertion with if-else statement.There is a single reason to check if performed login is unsuccessful with invalid credentials and it is while we are testing the login functionality itself. In that case, we have just to add `false` as 3rd parameter in the function and voilà - we use the right assertion.Please, change my mind

### Tekst z obrazka

Method with Assertion for Both Positive and Negative Cases
async logIn(
email: string, password: string, isLoggedIn: boolean = true
): Promise<void> {
await this.emailInput.fill(email);
await this.passwordInput.fill(password);
await this.signInButton.click();
if (isLoggedIn) {
await expect(
this.page.getByRole('navigation').getByText(email)
).toBeVisible();
Helse rf
await expect(
this.page.getByText(' Invalid Credentials')
).toBeVisible();
i;
+

---

## 94. Let's Talk About Single Responsibility Principle (SRP) in Automation Testing Frameworks =>The defini…

### Tekst postu

Let's Talk About Single Responsibility Principle (SRP) in Automation Testing Frameworks =>The definition of SRP for functions/methods, I liked the most, is that "each function should focus on a single task, execute it well, and avoid doing anything unrelated to that task".First, we have to define what is the task of the method we are writing. It is one of those things, that if you ask 10 different engineers, you can receive 11 different answers.I personally think that a method from automation testing framework has the goal to validate outcome, which means that the actions leading to that validation can be treated as prerequisites.Let's look at the example of a `logIn` method from a Playwright framework. There are 4 separate actions - all of them, an abstract method, coming from Playwright itself. There are a few different assumptions that can be made:1. The goal of the method is to fill the login form2. The goal of the method is to submit already filled form3. The goal of the method is to fill and to submit the form4. The goal of the method is to fill the form, to submit it and to validate the successCertainly, there are pros and cons to every single implementation, but let's be clear - the main goal is to solve the problem, and in most of the cases, it is just an engineer solution of it.Do you think that I missed a major point or you have totally different view about creating automation test methods?

### Tekst z obrazka

SRP in Automation Testing Frameworks
async logIn(email: string, password: string): Promise<void> {
await this.emaillnput.fill(email);
await this.passwordInput.fill(password);
await this.signInButton.click();
await expect(
this.page.getByRole(' navigation’) .getByText (email)
).toBeVisible();
by

---

## 95. Are You Adding Assertions to the Method Itself to Verify that It is Successful, or You Add Them in t…

### Tekst postu

Are You Adding Assertions to the Method Itself to Verify that It is Successful, or You Add Them in the Test?I understand that adding an assertion in a method violates the Single Responsibility Principle (SRP), but I personally like to have them inside my methods. It keeps my framework more readable and easily to maintain.

### Tekst z obrazka

With or Without Assertions in Methods

async navigateToHomePage(): Promise<void> {

await this.homePageLink.click();

await expect(this.homePageHeading).toBeVisible();
+
async navigateToHomePage(): Promise<void> {

await this.homePageLink.click();
b

---

## 96. Which Way of Defining Locators in a POM do You Prefer - Using `Getters` or the Classic Definition, a…

### Tekst postu

Which Way of Defining Locators in a POM do You Prefer - Using `Getters` or the Classic Definition, and Why?

### Tekst z obrazka

POM
export class LoginPageq{
constructor(private page: Page) {}
get loginBtn(): Locator {
return this.page.getByRole('button', { name: 'Login' });
+
bh
export class LoginPage{
readonly loginBtn: Locator
constructor (page: Page) {
this.page = page;
this.loginBtn= page.getByRole('button', { name: ‘Login’ })
+
J;

---

## 97. Ever Wonder How to Use Data, coming as API Response, Triggered from UI Test? Here is How =>You shoul…

### Tekst postu

Ever Wonder How to Use Data, coming as API Response, Triggered from UI Test? Here is How =>You should simply use `Promise.all` and run simultaneously both UI function, that sends the API request, and to listen for API Response from the request sent.As an example, we can use `Promise.all` to get the resource ID for test.afterAll, which ensures created resources are deleted via an API call, even after a test fails.

How Important is to Use Optional and Default Parameters in Your Test Automation Functions?For me, it is crucial to use them in your functions, because they give you an opportunity to use your function in different situations.If we use the example below, we can easily test the registration flow with different inputs - we can provide only the mandatory parameters, we can provide mandatory ones and the optional one, or we can provide all 4 of them.This keeps your code clean and easy to maintain. Are you a fan of optional and default function parameters, or you prefer separate functions?

### Tekst z obrazka

unction with Optional and Default Paramet
registerUser(
email: string
password: string
subscribeToNewsletter?: boolean,
acceptTerns: boolean = true
1H
page. getByTestId("enail-input") fill (email);
page. getByTestId("password-input"). Fill (password) ;
page. getByTestId("confirn-password-input") . fill (password);
(subscribeToNewsletter) {
page.getByTestId("subscribe-to-n t checkbox") .check();
¥
(acceptTerns) {
page. getByTestId("accept-terms-checkbox") check ();
¥
page. getByTestId("register-button").click();
¥

---

## 98. Both UI and API Validation Are Crucial - One for the User Experience, the Other for SafetyAutomation…

### Tekst postu

Both UI and API Validation Are Crucial - One for the User Experience, the Other for SafetyAutomation tests, for both UI and API are the best way for verifying validations.And what is a great solution for implementation of these tests? Creating an array of all invalid data, and iterating through it with For...Of loop. Simple, and scalable.Do you care for validation, and if yes, how you verify it?

### Tekst z obrazka

For...Of Loop
const invalidEmails = [
ttestll;
"152445",
"test@test",
"#E%$*&$EH$@# . com”,
"email@111.222.333",
"not”right@example.com",
"email..email@example.com",
".email@example.com",
"____@example.com"
1;
for (const email of invalidEmails) {
await page.getByPlaceholder("Email").fill(email);
await page.getByText("Invalid Email").toBeVisible();
}

---

## 99. Developing Playwright API Testing Framework? Use Initial Setup for User Access TokensDuring my live …

### Tekst postu

Developing Playwright API Testing Framework? Use Initial Setup for User Access TokensDuring my live session about "Developing Playwright Framework for REST API Testing", I received an interesting question."Can I use Playwright Fixtures to initialize access tokens for all my users?"Despite it is possible, there is better solution to this problem. It lies into right setup, before actual testing even start.On the image below, you can find how to execute needed number of steps to create `process.env` variables, holding the auth tokens, for all users you have.Later, you can refer to `process.env.ACCESS_TOKEN_USER` to use it.Once the run is completed, all variables are deleted.

### Tekst z obrazka

Setup Different Users Tokens as process.env Variables
{ test as setup, expect } '../fixtures/pon/test-options';
{ User } '../fixtures/api/types-guards’;
{ UserSchema } "LL [fix Japi/schenas';
setup('auth user’ ({ apiRequest }) {
setup.step('auth for ADMIN by API' [6] {
{ status, body } apiRequest<User>({
method: 'POST'
url i/users/login’
baselrl: process.env.API_URL
body: {
user: {
email: process.env.EMAIL_ADMIN
password: process. env.PASSWORD_ADMIN
+
bd
BH;
expect(status).toBe (200);
expect(UserSchema.parse(body)) .toBeTruthy();
process.env['ACCESS_TOKEN_ADMIN'] = body.user.token;
Bb;
setup.step('auth for USER by API' [6] {
Bb;
Bb;

---

## 100. Writing API Request Logic Inside Your Test Cases? There's a Much More Scalable Way to Handle it with…

### Tekst postu

Writing API Request Logic Inside Your Test Cases? There's a Much More Scalable Way to Handle it with Custom Fixtures!I have the opportunity to work on two large projects with the exact setup, described in the article (check comment section), and I am pretty confident that it works great.The complexity is abstracted is a reusable helper function that handles all your API requests (GET, POST, PUT, DELETE)It prevents runtime errors by using Zod to define schemas and validate API responses, ensuring data integrity from the start.Streamline your tests by implementing a custom Playwright fixture that injects API capabilities directly into your test environment.

### Tekst z obrazka

#### Obraz 1 (`image94.jpeg`)

Old Way

(= CUSTOM FIXTURES
© OF ry id
cl afd bd
A os core | cy fens
7 WEIN :

Stop Writing API Request Logic in Tests!
Use Custom Fixtures!

#### Obraz 2 (`image95.jpeg`)

Agenda

= The Two Sides of an Application

* Where and What Do The Bugs Hide

= API Testing
- What is API
- Types of API Testing

* The Power of a Unified Tool

= Developing Playwright Framework for REST API Testing
— Improving Developer Experience (DX)
— The Abstraction Layer :
— The Magic — Custom Fixtures
— Bulletproof — Zod Schema Validation

---

## 101. Never ever use XPath as your test automation locator strategy! Relying on brittle, implementation-de…

### Tekst postu

Never ever use XPath as your test automation locator strategy! Relying on brittle, implementation-dependent locators is the perfect catalyst for a perfect storm - #flaky, unreliable tests that ruin confidence in the #automation #testing.There're two main approaches for resilient tests: Test IDs and User-Facing LocatorsTo use User Facing Locators, we need a commitment from our Front-End Developers to write semantic and accessible HTML. They should use proper ARIA roles, labels, and descriptive text, which in my understanding is a big win for the quality.To use Test IDs, we need our Front-End Developers to follow a convention for adding these test IDs to key interactive elements during development.In the first case, we ensure that we test the application from a true user's perspective, and in the second, we use the most robust strategy.In either way, we should communicate it ASAP with the Front-End team.hasztaghasztaghasztaghasztaghasztaghasztag

---

## 102. This deep dive into TypeScript's power could redefine your framework.💻 This article tackles common …

### Tekst postu

This deep dive into TypeScript's power could redefine your framework.💻 This article tackles common problems with:1. 🚫 Eliminating hidden bugs from "magic strings" and hardcoded values.2. ♻️ Maximizing code reusability for functions like API requests without sacrificing type safety.3. 🔒 Ensuring data integrity by validating API responses at runtime and keeping types in sync.4. ✨ Creating flexible and precise types for objects and API payloads with minimal effort.

### Tekst z obrazka

Xo ~ —N
5 Patterns for a Es =
World-Class QA Framework =
<4. 2 A oH [02 | Alility Types
we Tamm (QP
=. (BS
‘eats 7m Ea SK
: C 3. Zod & zinfer E
g 3 2.Generics
a % — a x
- 1. Enums = oi
VAN i adie,

---

## 103. Viktor Konovalov

### Tekst postu

Viktor Konovalov

Playwright tip: snapshots aren't just for UIWhen people hear "snapshot testing", they usually think about screenshots.But snapshots are just as useful for API testing.They're especially useful when you need to verify that stable parts of an API response never change unexpectedly.Instead of writing dozens of assertions for large JSON payloads, you can compare the entire response against a stored snapshot.Before creating the snapshot, remove dynamic fields such as IDs, timestamps, or tokens. This keeps the comparison focused on the data that should remain stable.Snapshots work especially well for:• configuration endpoints• feature flags• large API payloads• API contract verificationThe goal isn't to replace assertions.

### Tekst z obrazka

test( title: "Profile API contract", body: async ({ request :APIRequestContext }) => {
// Send API request
const response :APIResponse = await request.get( url: /api/profile");
const body = await response.json();
// Ignore fields that change on every request
delete body.id;
delete body.createdAt;
delete body.lastlogin;
// Verify the stable API contract
expect (body) .toMatchSnapshot( name: "profile-response.json");
Bb;

---

## 104. An interesting AI-powered QA product recently came my way, and I plan to explore it together with yo…

### Tekst postu

An interesting AI-powered QA product recently came my way, and I plan to explore it together with you.The main question is:Can AI manage the complete test automation lifecycle?Generating a test is only the beginning. Real automation also requires:• UI and API coverage• test data• CI/CD• reporting• failure analysis• maintenanceI will test the platform against real automation challenges and openly share the results, limitations and failures.The goal is not to prove that AI can replace QA engineers.It is to understand what work AI can genuinely remove, and where engineering control is still essential."Can AI generate a test?" is no longer the most interesting question.The real question is:"Can we trust the automation it creates?"

### Tekst z obrazka

Can Al manage the complete
test automation lifecycle?
Ll
©) Requirements =
Maintenance Ul Tests
7 \
Ca ddd
A Al QA AGENT /
-_
ile Si)
Reports x Test Data
rd
Cy. CO
Execution Cl/cD
Generating a test is easy. Owning the lifecycle is the real challenge.

---

## 105. Tech Explained #3: Race ConditionsWhat is a race condition?Imagine two users click "Buy" at exactly …

### Tekst postu

Tech Explained #3: Race ConditionsWhat is a race condition?Imagine two users click "Buy" at exactly the same time.There's only one item left in stock.Who gets it?If your system isn't prepared for concurrent requests,both users might succeed.That's a race condition.The final result depends on which request reaches the critical section first.Race conditions often cause random, hard-to-reproduce bugs.Common symptoms include:• Duplicate orders• Negative inventory• Lost updates• Random test failuresThat's why concurrent testing is just as important as functional testing.

### Tekst z obrazka

Tech Explained
#03 Race conditions
Two requests arrive
Request A I Request B
Access same data

---

## 106. Playwright tip: build self-healing locators with a Locator FactoryUI changes happen.• A button gets …

### Tekst postu

Playwright tip: build self-healing locators with a Locator FactoryUI changes happen.• A button gets a new test id.• A CSS class is renamed.• A designer wraps an element in another container.Suddenly dozens of tests start failing.Instead of hardcoding a single locator, create a Locator Factory that knows multiple ways to find the same element.For example, it can try:• getByTestId()• getByRole()• getByLabel()• CSS or XPath as a last resortIf one strategy breaks, the next one is used automatically.Your tests become much more resilient to harmless UI changes without hiding real bugs.Auto-healing isn't about ignoring failures.It's about recovering from locator changes while still failing when the application's behavior is actually broken.

### Tekst z obrazka

// src/project/locators/LocatorFactory. ts
export class LocatorFactory { Show usages
static async find(...locators: Locator[]): Promise<Locators { Show usages
for (const locator : Locator of locators) {
if (await locator.count()) {
return locator;
k
bi
throw new Error( message: "Element not found*);
I
li
// src/pages/LoginPage. ts
export class LoginPage { Show usages
constructor (private readonly page: Page) { Show usages
¥
async clickLoginButton(): Promise<void> { Show usages
const loginButton : Locator = await LocatorFactory.find(
this. page.getByTestId( testid: *Login-btn*),
this.page.getByRole( role: “button”, options: {name: "Login"}),
this. page.getByText( text: "Login®)
Pi
await loginButton.click();
I
li
// src/tests/1ogin. spec. ts
test( title: "Login, ‘body: async ({page : Page }) => {
const loginPage : LoginPage = new LoginPage(page);
await page.goto( uf *https://exanple.con*);
await loginPage.clickLoginButton();
I)

---

## 107. Tech Explained #2: CachingWhat is caching?Imagine you open the same page twice.Should your browser d…

### Tekst postu

Tech Explained #2: CachingWhat is caching?Imagine you open the same page twice.Should your browser download every image, stylesheet and script again?Hopefully not.That's exactly why caching exists.Instead of downloading the same data every time, a cache store previously fetched resources and reuses them when possible.Without caching, applications become:• Slower• More expensive to run• Less responsiveCaching improves performance, but it can also confuse QA engineers.Sometimes you're not testing the latest version of the application.You're testing what your browser saved yesterday.

### Tekst z obrazka

Tech Explained
#02 Caching
Browser requests page
Cache miss Cache hit

---

## 108. Playwright tip: use locator.evaluateAll() to process elements in one browser callNeed to read data f…

### Tekst postu

Playwright tip: use locator.evaluateAll() to process elements in one browser callNeed to read data from many elements?Many tests do something like this:• get all locators• loop through them• call textContent() for each oneIt works.But every textContent() is a separate round tripbetween Node.js and the browser.locator.evaluateAll() executes your code inside thebrowser and returns the final result in a single call.This is especially useful when extracting:• table values• lists of links• product names• IDs• attributes• any transformed dataLess browser communication.Cleaner and often faster tests.

### Tekst z obrazka

// Every textContent() is a separate browser call
const users : Locator = page.locator( selector: 'table tbody tr td:first-child');
const namesl: string[] = [];
for (let i :number = 8; i < await users.count(); i++) {
names1.push(
await users.nth(i).textContent() 22 ''
pH
i
// Read all values in one browser call
const names2 :string[] = await page
.locator( selector: 'table tbody tr td:first-child')
.evaluateAll( pageFunction: cells :(.)[] =>
cells.map (cell : SVGElement | HTMLElement =>
cell.textContent?.trim() ?? '')
PH

---

## 109. Playwright tip: control time with the Clock APITime is one of the biggest sources of flaky tests.Thi…

### Tekst postu

Playwright tip: control time with the Clock APITime is one of the biggest sources of flaky tests.Think about features like:• countdown timers• session expiration• scheduled notifications• delayed UI updates• date and time calculationsWaiting for real time makes tests slower and less reliable.Playwright's Clock API lets you freeze time, fast-forward it, or set any date you need.Your tests become deterministic because time only moves when you tell it to.

### Tekst z obrazka

// start controlling browser time
await page.clock.installQ);
JI Freeze tine at a specific moment
await page.clock.setFixedTime(

new Date( Value: '2026-1-01T09:00:00Z*)
2
// Jump forward instantly
J AUL timers within this period are executed
await page.clock.fastForward( ticks: '5:60');
// milliseconds work too
await page.clock.fastForward( ticks: 30.600);
// change the current systen time
// Useful for expiration and scheduled events
await page.clock.setSystenTime(

new Date( value: '2026-1-02T09:08:002")
D;
// Advance time normally
// Timers fire as time progresses
await page.clock.runFor( ticks: 16_006) ;
// Execute pending timers immediately
await page.clock.runFor ( ticks: 0);
// Restore the real browser clock
await page.clock.resune();

---

## 110. Playwright tip: understand the Route objectImagine the browser is about to send a request:GET /api/u…

### Tekst postu

Playwright tip: understand the Route objectImagine the browser is about to send a request:GET /api/usersPlaywright intercepts it and gives you a route object.Your job is to decide what happens next.The most common options are:• fulfill() -> return your own response instead of calling the real backend.• abort() -> block the request completely.• continue() -> send the request to the real , optionally modifying it.• fallback() -> pass the request to the next matching route handler.• fetch() → get the real response and modify it.• request() → inspect request details without changing its behavior.Once you understand these methods, network mocking in Playwright becomes much easier to reason about.You're not just intercepting requests.You're deciding their entire lifecycle.

### Tekst z obrazka

// Return a mocked response
await route. fulfill( options: {
status: 208,
json: [{name: 'viktor'}]
b;
// cancel the request
await route.abort( erorCode: failed’);
// Send the modified request to the real backend
await route.continue( options: {
headers: {
...route.request().headers(),
‘x-test-run': ‘true’, }
12)
// Pass the request to the next route handler
await route. fallback( options: {
headers: {
...route.request().headers(),
| *x-test-run': ‘true’, }
5
// Get the real response and modify it
const response : APIResponse = await route.fetch();
const body = await response.json();
// Inspect request details
const url ‘string = route.request().url();
const method : string = route.request().method();

---

## 111. Playwright tip: use route.fallback() when one request needs multiple handlersMost teams use page.rou…

### Tekst postu

Playwright tip: use route.fallback() when one request needs multiple handlersMost teams use page.route() to mock requests.But sometimes you want to do more than one thing with the same request.For example:• add a custom header to every API call• log all requests for debugging• mock a specific endpointWithout route.fallback(), the first matching route handles the request and the rest never run.With route.fallback(), the request can continue to the next matching route.This allows you to split network logic into small, focused handlers instead of building one large route full of conditions.A simple way to think about it:> continue() → send the request to the backend> fallback() → send the request to the next route handler

### Tekst z obrazka

// Result: the request is mocked, but the header is never added.
test( tile: ‘shows users’, body: async ({page :Page }) => {
// Mock a specific endpoint
await page.route( uf 'x+/api/users’, handler async route :Route => {
await route.fulfill( options: {
json: [{name: 'viktor'}],
By
Bb;
// Add a custon header to all APT requests
await page.route( ur '#%/api/#%', hander async route :Route => {
await route.continue( options: {
headers: {
...route.request() .headers(),
*x-test-run': true’
3
28
bi
325
// Result: the header is added first, then the request is mocked
test( ile: ‘submit form’, body: async ({page :Page }) => {
// Add a custon header to all API requests
await page.route( ud: '#+/api/++’, hander async route : Route => {
await route.fallback( options: {
headers: {
...route. request ().headers(),
*x-test-nun': ‘true’
I
21
b;
// Mock a specific endpoint
await page.route( ur ‘x+/api/users’, handler async route :Route => {
await route. fUlLFill( options: {
json: [{name: 'Viktor'}]
28
1);
b;

---

## 112. laywright tip: validate the accessibility tree, not the DOMMany UI tests are tightly coupled to impl…

### Tekst postu

laywright tip: validate the accessibility tree, not the DOMMany UI tests are tightly coupled to implementation details: CSS classes, element IDs, and page structure. The problem is that users never interact with those things.Instead of asking "Does this div exist?", ask "Can a user find and use this button?"Using accessibility-based locators like getByRole() and getByLabel() gives a few advantages:• tests focus on user behavior instead of implementation details• selectors become more stable during UI refactoring• accessibility issues are often discovered earlier• test code becomes easier to readGood tests verify what users can do, not how the page is built.

### Tekst z obrazka

// DOM-based - bad
test( title: 'submit form', body: async ({page :Page }) => {
await page.locator( selector: '#email-input').fill( value: 'vser@test.com');
await page.locator( selector: '.submit-button').click()
await expect(
page.locator( selector: '.success-message')
).toBeVisible();
bs
// Accessibility-based - better
test ( title: ‘submit form', body: async ({page :Page }) => {
await page.getBylabel( text: 'Email').fill( value: 'user@test.com');
await page.getByRole( role: ‘button’, options: {name: 'Submit'}).click();
await expect(
page.getByRole( role: 'status')
).toContainText( expected: 'Form submitted’);
1H;

---

## 113. Playwright tip: use page.addInitScript() to modify the env. before the app startsSometimes you need …

### Tekst postu

Playwright tip: use page.addInitScript() to modify the env. before the app startsSometimes you need to control browser state before your application loads.For example:• feature flags• localStorage values• session data• Math.random()• browser APIsMany teams set these values after page.goto().The problem is that the application may have already read them during startup.page.addInitScript() runs before any page scripts execute.This makes tests more predictable and avoids race conditions during initialization.

### Tekst z obrazka

test( title: "enable new checkout', body: async ({ page :Page }) => {
await page.goto( url: '/');
// Too late: the app may have already read the flag
await page.evaluate( pageFunction: () => {
localStorage.setItem('newCheckout', 'true');
1:
await page.reload();
await expect(
page.getByText( text 'New Checkout')
).toBeVisible();
127
test( title: ‘enable new checkout', body: async ({ page :Page }) => {
// Applied before the app starts
await page.addInitScript( script () => {
localStorage.setItem('newCheckout', 'true');
le
await page.goto( url: '/');
await expect(
page.getByText( text: ‘New Checkout’)
).toBeVisible();
1:

---

## 114. Playwright tip: use expect.toPass() when a single assertion retry is not enoughMost Playwright asser…

### Tekst postu

Playwright tip: use expect.toPass() when a single assertion retry is not enoughMost Playwright assertions already retry automatically.But sometimes the flaky part is not the assertion.It's the entire flow.For example:• trigger an action• wait for backend processing• verify the resultIn these cases, retrying only the final check may not help.expect.toPass() retries the whole block until it succeeds or times out.Useful for eventually consistent systems, background jobs, and delayed updates.Good automation is not about adding retries everywhere.

### Tekst z obrazka

test( file: ‘report generation’, body: async ({page :Page }) => {
// Trigger report generation once
await page.getByRole( role: ‘button’, options: {name: 'Generate report'}).click();
// only the assertion retries
await expect(
page.getByText( text 'Report ready')
).toBeVisible();
Bb;
test( title: ‘report generation', body: async ({page :Page }) => {
// Retry the entire flow until it succeeds
await expect( acwal async () => {
await page.getByRole( role: ‘button’, options: {name: ‘Generate report'}).click();
await expect(
page.getByText( text 'Report ready’)
).toBeVisible();
}).toPass();
Bb;

---

## 115. Playwright tip: use .toHaveScreenshot() where visuals actually matterThis assertion captures a scree…

### Tekst postu

Playwright tip: use .toHaveScreenshot() where visuals actually matterThis assertion captures a screenshot of the page or elementand compares it with a baseline image.It helps catch visual regressions that regular assertions often miss:• broken layout• spacing issues• missing elements• wrong styles• UI shiftsBut not every UI check needs visual comparison.Usually, regular assertions are enough:• await expect(button).toBeVisible();• await expect(price).toHaveText('$99');Use .toHaveScreenshot() for areas where the visual appearance itself is important:• design systems• critical components• checkout flows• dashboards• marketing pagesAvoid using it everywhere.Full-page visual tests on highly dynamic pages often become flaky and noisy.Keep screenshots small and stable.Mask only truly dynamic content.Visual tests should protect the user experience,not become another maintenance problem.

### Tekst z obrazka

// Full page visual check
test( title: 'home page visual’, body: async ({page :Page }) => {
await page.goto( url '/');
await expect(page).toHaveScreenshot( name: 'home-page.png', options: {
// hide dynamic content
mask: [
page.getByTestId( testid: 'current-time')
page.getByTestId( testid: 'Live-banner'),
page.getByTestId( testid: 'notification-counter'),
| 1
ol
3p)
// Component-level visual check
test( title: 'product card visval', body: async ({page :Page }) => {
await page.goto( url: '/products/1');
// verify only the important UI block
const productCard : Locator = page.getByTestId( testid: 'product-card');
await expect(productCard).toHaveScreenshot( name: 'product-card.png');
ipl

---

## 116. laywright tip: use test.step() for readable reportsA test can be technically correct and still be ha…

### Tekst postu

laywright tip: use test.step() for readable reportsA test can be technically correct and still be hard to analyze after failure.Especially in CI.When a long E2E test fails, the question is not only:"Which test failed?"The better question is:"Which user action failed?"Without clear steps, you often need to open the trace, inspect the logs, and mentally rebuild the whole scenario.test.step() helps structure the test around meaningful business actions.It does not make the test more stable by itself.But it makes failure analysis faster, reports cleaner, and debugging much easier.This is especially useful for smoke and regression scenarios where one test may cover several user actions.Good automation is not only about execution.It is also about fast failure investigation.

### Tekst z obrazka

// Before
test( title: '@smoke @regression Scroll up and down with arrow button',
async ({home} : PlaywrightTestArgs & PlaywrightTestOption... ) => {
// Scroll to footer
await home.scrollToFooter();
await home.verifySubscriptionVisible();
// Press the scroller arrow and validate the title
await home.pressArrowUpScroll();
await home.checkMainPageCarouselTitle();
BH
// After
test( title: '@smoke @regression Scroll up and down with arrow button’,
async ({ home } :PlaywrightTestArgs & PlaywrightTestOption... ) => {
await test.step( title: 'Scroll to footer', async () => {
await home.scrollToFooter();
await home.verifySubscriptionVisible();
oH
await test.step( title: 'Scroll back to top using arrow button', async () => {
await home.pressArrowUpScroll();
await home.checkMainPageCarouselTitle();
Bi
oH

---

## 117. Playwright tip: be careful with force: trueOne of the most common “fixes” in Playwright tests is add…

### Tekst postu

Playwright tip: be careful with force: trueOne of the most common “fixes” in Playwright tests is adding force: true to a click.The test passes.The pipeline becomes green.But the real issue often stays hidden.In many cases, force: true bypasses actual UI problems:• loading overlays• unstable state• wrong locators• timing issuesBy default, Playwright waits until the element is truly actionable.force: true skips those checks.So while the automation passes,the real user experience may still be broken.Sometimes force: true is valid.But as a default solution, it is usually a red flag.If force: true “fixes” the test,the test was probably trying to tell you something.

### Tekst z obrazka

// Bad
await page.locator( selector: ‘.submit-btn').click( options: {force: true})
// Better: wait for overlay/spinner
await expect(page.locator( selector: '.loading-spinner')).toBeHidden();
await page.getByRole( role: ‘button’, options: {name: 'Submit'}).click();
// Better: wait for correct UI state
const submitButton :Locator = page.getByRole( role: ‘button’, options: {name: 'Submit'})
await expect(submitButton).toBeEnabled();
await submitButton.click();
// Better: wait for request + action together
await Promise.all( values: [
page. waitForResponse ( uriOrPredicate: res : Response =>
res.url().includes('/orders') &&
res.status() === 200
J,
page. getByRole( role: ‘button’, options: {name: 'Submit'F).click(
nD;

---

## 118. Playwright tip: use test.use() for authentication separationOne of the most popular uses of test.use…

### Tekst postu

Playwright tip: use test.use() for authentication separationOne of the most popular uses of test.use() is authentication handling.But far from the only one.A very common problem in Playwright suites:tests accidentally inherit the wrong user state.For example:• guest tests run as authenticated users• admin tests run as regular users• /login redirects to /dashboard• state leaks between scenariosUsing test.use() makes the expected session explicit.When declared inside a describe block, the configuredstorageState is automatically applied to every test inside it.At the same time, each test still runs in its own isolatedbrowser context, helping prevent state leakage between scenarios.This improves:• readability• isolation• maintainability• scalability of the framework

### Tekst z obrazka

test. describe( tite: ‘Guest flows’, () => {
// Creates a completely clean browser session
test.use({storageState: {cookies: [], origins: [1}});
test( title: Open cart as guest user’,
async ({home} : PlaywrightTestArgs & PlaywrightTestOption... ) => {
// Guest user test
anait home.openCart();
bi
bi
test. describe( fit: ‘Authenticated user flows’, () => {
// Loads saved authenticated user session
test.use({storageState: STORAGE_STATE_PATH});
test( title: Open cart as authenticated user’,
async ({home} : PlaywrightTestArgs & PlaywrightTestOption... ) => {
// Authenticated user test
anait home.openCart();
hi
bi
test. describe( tite: *Adnin user flows’, () => {
// Loads saved admin user session
test.use({storageState: ADMIN_STORAGE_STATE_PATH});
test( title: Open cart as admin user’,
async ({home} : PlaywrightTestArgs & PlaywrightTestOption... ) => {
// Adnin user test
anait home.openCart();
bi
bi

---

## 119. laywright tip: use locator.drop() for drag-and-drop testingPlaywright 1.60 introduced locator.drop()…

### Tekst postu

laywright tip: use locator.drop() for drag-and-drop testingPlaywright 1.60 introduced locator.drop()Before this, drag-and-drop tests usually required:• manual DataTransfer• custom browser events• helper utilities• extra boilerplateNow Playwright handles it with a single API.> Cleaner tests.> Less setup.> Less browser event plumbing.One of those small Playwright features that quietly improves everyday test automation work.

### Tekst z obrazka

// Before
// Create native drag-and-drop payload
const dataTransfer : JSHandle<DataTransfer>
= await page.evaluateHandle( pageFunction: () => new DataTransfer());
// Upload file into browser memory
await page.locator( selector: 'input[type=file]').setInputFiles( files: 'file.pdf');
// Manually trigger drop event
await page.dispatchEvent(
selector: *.dropzone’,
type: ‘drop’,
eventinit: {dataTransfer}
pH
// After
// Drop file directly into target area
await page.locator( selector: '.dropzone').drop( payload: {
files: ['file.pdf']
Bb;

---

## 120. Playwright tip: use storageState instead of logging in every testOne of the biggest slowdowns in UI …

### Tekst postu

Playwright tip: use storageState instead of logging in every testOne of the biggest slowdowns in UI suites:logging in through the UI before every test.It works.But over time it creates:• slower execution• duplicated setup• flaky authentication flows• unnecessary dependency on login pages• longer CI pipelinesMost tests are not testing authentication itself.They are testing what happens after login.Playwright already provides a built-in solution:storageStateThe idea is simple:authenticate once, save the browser session, and reuse it across the suite.Now tests start already authenticated:• faster• more stable• less setup duplication• cleaner test intentSave UI login flows for dedicated auth tests.Not for the entire regression suite.

### Tekst z obrazka

test.beforeEach(async ({ page } ) => {
await page.goto( [login');
await page.getByLabel( Email’). fill( test@test.com');
await page.getBylLabel( Password"). fill( value: 'password');
await page.getByRole( ‘button’, { name: 'Login' }).click();
BD;
import { test as setup } from '@playwright/test';
setup( ‘authenticate’, async ({page} =o
await page.goto( login');
await page.getByLabel( 'Email'). fill( 'test@test.com');
await page.getByLabel( Password"). Fill( password');
await page.getByRole( button’, {name: 'Login'}).click();
await page.context().storageState({
path: 'playwright/.auth/user.json',
Bb;
Bi;
export default defineConfig({
use:
storageState: 'playwright/.auth/user.json',
Bi

---

## 121. Playwright tip: stop using .nth() to find elements.nth() feels like a quick win.Need the second card…

### Tekst postu

Playwright tip: stop using .nth() to find elements.nth() feels like a quick win.Need the second card? Just use .nth(1) and move on.But this is one of the most fragile patterns in UI tests.Why it fails:• UI order changes → test breaks• data changes → test breaks• new element appears → test breaksNot because behavior changed.But because your test depends on position.That’s not what users do.Users don’t click "the 3rd card".They click "the card with Premium plan".Better approach: select by context, not indexPlaywright gives you a built-in way to do that: has / hasText

### Tekst z obrazka

// Bad

await page
.locator( selector: '.card')
.nth( index: 2)
.locator( selectorOrLocator: 'button.buy')
welick@®;

// Better

await page
.locator( selector: '.card', {
| has: page.getByText( text: 'Premium plan')
7)
.getByRole( role: 'button', { name: 'Buy' })
.click();

---

## 122. Playwright tip: stop branching for UI statesLooks reasonable.Your UI can end up in different states …

### Tekst postu

Playwright tip: stop branching for UI statesLooks reasonable.Your UI can end up in different states -so you handle it with if/else.Tests pass. Logic is covered.But why does it still feel flaky?>>> The problemif/else checks the UI at a single moment in time.Modern apps are async:• network is still in flight• state is not updated yet• UI hasn’t renderedYour check runs too early.Nothing is visible -> wrong branch -> test fails.>>> Why this becomes flakyYou lose synchronization.You are:• reading the state too early• making decisions on incomplete UI• fighting timing instead of modeling behaviorResult:• race conditions• intermittent failures• non-reproducible bugs

### Tekst z obrazka

// Bad (race condition)
await page.getByRole( role: 'button', {name: 'Pay'}).click();
if (await page.getByText( text: 'Payment successful').isVisible()) {
// handle successful payment
} else {
// assume payment is pending
+
// Better (deterministic and stable)
await page.getByRole( role: 'button', {name: 'Pay'}).click();
await expect(
page.getByText( text: 'Payment successful')
.or(page.getByText( text: 'Payment pending'))
) .toBeVisible();

---

## 123. Playwright tip: stop using .first() as a shortcutLooks harmless.Your locator matches multiple elemen…

### Tekst postu

Playwright tip: stop using .first() as a shortcutLooks harmless.Your locator matches multiple elements - so you just grab the first one.Tests pass. CI is green. But what are you actually testing?The problem.first() doesn’t fix ambiguity. It hides it.Your test no longer targets a specific element.It targets whatever happens to be first in the DOM.When UI changes:• a new item is added• sorting changes• layout shiftsYour test clicks something else.Same code. Different behavior.Why this becomes flakyFrom a testing perspective, you lose determinism.The outcome now depends on DOM order, not on business intent.This leads to:• wrong element interactions• false positives• hard-to-debug failures

### Tekst z obrazka

// Before (ambiguous and risky)

await page
-getByRole( role: ‘button’, {name: 'Add to cart'})
-first()
.click();

// same problem, different syntax

await page
.getByRole( role: 'button', {name: 'Add to cart'})
.nth( index: 0)
.click);

// After (intent-driven and stable)

const productCard :Locator = page
.locator( selector: '.product-card')
.filter({hasText: 'Product 1'});

await productCard
.getByRole( role: 'button‘, {name: 'Add to cart'})
.click();

await expect(page.locator( selector: '.cart-items')).toContainText( expected: 'Product 1');

---

## 124. Playwright tip: stop asserting implementation detailsMany flaky tests are not flaky because of timin…

### Tekst postu

Playwright tip: stop asserting implementation detailsMany flaky tests are not flaky because of timing.They are flaky because they assert the wrong thing.A common mistake:We verify how something is built instead of what the user actually experiences.It works… until:• a refactor changes the DOM• styles are updated• API structure evolvesResult: tests fail - but the feature still works.That’s not a bug. That’s a bad assertion.Shift the focus:Test user-observable outcomes, not implementation.This approach is most relevant for:• UI / E2E tests• user journeys• business flowsFor lower levels:• API tests → validate contracts, schemas, statuses• component/unit tests → validate internal logicDifferent levels - different goals.

### Tekst z obrazka

// Bad pattern (implementation-driven)
test( title: ‘checkout completes order’, async ({ page } :PlaywrightTestArgs & PlaywrightTestOption... ) => {
await page.goto( url: '/cart');
await page.getByRole( role: ‘button’, { name: ‘Checkout’ }).click();
await page.waitForResponse(r :Response =>
| r.url0.includes( searchString: '/api/create-order')
Ji
await expect(page.locator( selector: '.order-success'))
-toHaveCSS( name: ‘display’, value: 'block');
Bi
// Better pattern (user-oriented)
test( title: ‘checkout completes order’, async ({ page } :PlaywrightTestArgs & PlaywrightTestOption... ) => {
await page.goto( ur '/cart');
await page.getByRole( role: ‘button’, { name: ‘Checkout’ }).click();
await expect(
page.getByRole( role: ‘heading’, { name: 'Thank you for your order’ })
).toBeVisible();
await expect(page.getByText( text: /order id/i)).toBeVisible();
Bb;

---

## 125. Playwright tip: stop fighting popups in UIFlaky tests often come from ads, consent banners, and thir…

### Tekst postu

Playwright tip: stop fighting popups in UIFlaky tests often come from ads, consent banners, and third-party overlays.> They appear randomly.> They block clicks.> They break tests.A common approach is to handle them in UI:Detect -> Check -> Close.Seems logical. But it doesn’t scale.Why it fails:• popups can be inside iframes• they appear unpredictably• selectors are unstable• logic gets duplicated across testsResult: complex and still flaky tests.The real problem is not the popup. It’s the uncontrolled environment.Better approach: Remove the source of noise.> No ads.> No overlays.> No randomness.Key idea:Don’t fix symptoms in UI. Control the environment.

### Tekst z obrazka

// Bad approach - inside test
test( title: ‘add to cart', async ({page} :PlaywrightTestArgs & PlaywrightTestOption... ) => {
await page.goto( urk '/');
const closeBtn :Locator = page.locator( selector: 'button:has-text("Close")');
if (await closeBtn.isVisible()) {
await closeBtn.click();
I
await page.getByRole( role: 'button', {name: 'Add to cart'}).click();
Bb;
// Better approach - fixture / setup
test.beforeEach(async ({page} : PlaywrightTestArgs & PlaywrightTestOption... ) => {
await page.route( url: 'xx/googlesyndication.com/#*', r :Route => r.abort());
await page.route( url '#x/doubleclick.net/*x', r :Route => r.abort());
await page.route( uri 'sx/google-analytics.com/**', pr :Route => p.abort());
5

---

## 126. Playwright tip: stop waiting blindly - use expect.poll()Some waits don’t fit into standard UI assert…

### Tekst postu

Playwright tip: stop waiting blindly - use expect.poll()Some waits don’t fit into standard UI assertions.Not everything is:• visible• clickable• present in DOMSometimes you need to wait for:• backend job completion• async state change• external system updateAnd this is where many tests become flaky.Why it works:• retries automatically• stops as soon as condition is met• faster + more stableWhen to use expect.poll()• async backend processing• status endpoints (pending -> done)• queues, jobs, payments• anything outside UI lifecycleRisks:• unnecessary load on the backend• rate limiting• unrealistic user behavior

### Tekst z obrazka

// Bad practice
await page.waitForTimeout( timeout: 5000);
const response : APIResponse = await page.request.get( url '/api/status');
const body :any = await response.json();
expect (body.state).toBe( expected: 'completed');
// Good practice
await expect.poll(async () => {
const response :APIResponse = await page.request.get( ur: '/api/status');
const body :any = await response.json();
return body.state;
}).toBe( expected: 'completed');

---

## 127. laywright tip: stop testing what you don’t ownFlaky tests often come from one mistake:We rely on rea…

### Tekst postu

laywright tip: stop testing what you don’t ownFlaky tests often come from one mistake:We rely on real backend behavior even when we test UI.• APIs are slow.• Data changes.• Environments are unstable.Result:Tests fail - but not because of your feature.The fix:Control dependencies.Mock what is outside your responsibility.This does NOT replace end-to-end tests.It makes UI tests:• deterministic• fast• focusedUse real backend for smoke.Use controlled data for stability.Most flaky tests are not UI problems.They are uncontrolled dependencies.

### Tekst z obrazka

// Bad - Uncontrolled dependency
await page.goto( url: "/profile");
await expect(page.getByText( text: "Premium")).toBeVisible();
// Better - Controlled dependency
await page.route( url "xx/api/profile", async (route :Route ) => {
await route.fulfill({
status: 200,
contentType: "application/json",
body: JSON.stringify({
name: "Victor",
plan: "Premium",
BD,
Di
Bi
await page.goto( url: "/profile");
await expect(page.getByText( text: "Victor")).toBeVisible();
await expect(page.getByText( text: "Premium")).toBeVisible();

---

## 128. Playwright tip: why your "successful click" does nothingFlaky tests often come from one mistake:We w…

### Tekst postu

Playwright tip: why your "successful click" does nothingFlaky tests often come from one mistake:We wait for UI. But not for the system.A button can be visible and clickable - but the application is still not ready.Result:Playwright reports a successful click, but nothing actually happens.The fixWait for real signals - not just UI.Think in terms of:• network response• state change• backend confirmation(not just "element is visible")Most flaky tests are not timing issues.They are wrong waiting strategy.

### Tekst z obrazka

// Bad
await page.getByRole( role: “button”, { name: "Submit" }).click();
/1 Better
await Promise.all([

page.waitForResponse(res : Response =>

res.url(). includes ( searchSting: "/api/submit”) 8&

| res.request().method() === "POST" &&

| res.status() === 200

de

page.getByRole( role: “button”, { name: "Submit" }).click()
n;

---

## 129. How to Write UAT Test Cases (for Business Analysts)I remember when I was a junior BA - the idea of U…

### Tekst postu

How to Write UAT Test Cases (for Business Analysts)I remember when I was a junior BA - the idea of UAT scared me.I didn’t want to admit I didn’t know where to start... So I stayed quiet and tried to figure it out on my own.Turns out, I’d built it up to be more complicated than it really is.Here’s what I learned: As a BA, your role in User Acceptance Testing (UAT) is to ensure the solution actually meets the business need… not just that it functions.To do that effectively, you need a structured approach to writing UAT test cases. Here's how I do it:1️⃣ Start with the Requirement→ Begin with a single requirement or user story.→ Each requirement must be tested, and depending on how many acceptance criteria it has, you may need multiple test cases.(Think: What is the business expecting from this requirement?)2️⃣ Review the Acceptance Criteria→ Acceptance criteria define the boundary of success for a requirement.→ They help you understand what “good” looks like from the business’s perspective.(Use these criteria as your guideposts for what to test)3️⃣ Develop Test Cases Based on the Acceptance Criteria→ Each acceptance criterion should translate into at least one test case.→ Some may need both a positive (happy path) and negative (error or edge case) scenario.(If a criterion says “User must receive a confirmation email,” test both a valid scenario and one where the email fails)4️⃣ Complete the UAT Template for Each Test Case→ For each test case, fill in these fields:☑ Test Description – A clear statement of what’s being testede.g. “Test password reset email is triggered for valid email addresses”☑ Preconditions – Any setup required before testinge.g. “User is logged out and on the login page”☑ Test Steps – Step-by-step actions for the tester to performe.g. Click “Forgot Password”, enter email, submit form☑ Expected Result – What should happen if the system works correctlye.g. “User receives reset email within 2 minutes”(TIP: Keep the language business-friendly so anyone can run the test)5️⃣ Repeat for Each Requirement→ Once you've completed the test cases for one requirement, move to the next and repeat the process.→ This ensures full coverage and traceability back to each business objective.6️⃣ Review with Business Stakeholders→ Once your test cases are drafted, share them with your business SMEs or stakeholders.(This step is critical - their feedback confirms that you’re testing what really matters to them)7️⃣ Prepare for Execution→ After validation, the test cases are ready to be run.→ Depending on your project, UAT may be carried out by business users, or you may help execute or facilitate it as a BA.

---

## 130. 8 Handling Authentication in Playwright 🔑Authentication in test automation should be fast, efficien…

### Tekst postu

8 Handling Authentication in Playwright 🔑Authentication in test automation should be fast, efficient, and secure. Here’s how to do it right.𝗛𝗮𝗻𝗱𝗹𝗶𝗻𝗴 𝗗𝗶𝗳𝗳𝗲𝗿𝗲𝗻𝘁 𝗔𝘂𝘁𝗵𝗲𝗻𝘁𝗶𝗰𝗮𝘁𝗶𝗼𝗻 𝗠𝗲𝘁𝗵𝗼𝗱𝘀✔ 𝗕𝗮𝘀𝗶𝗰 𝗔𝘂𝘁𝗵 – Simple but limitedUse httpCredentials to send credentials automatically.test.use({ baseURL: 'https://example.com', httpCredentials: { username: 'admin', password: 'admin' } });✔ 𝗢𝗔𝘂𝘁𝗵 / 𝗦𝗦𝗢 – Handles redirects automatically. Playwright seamlessly manages multi-domain logins like Google or Oktaawait page.goto('https://example.com/login');await page.getByLabel('Email or phone').fill(process.env.GOOGLE_USER);await page.getByRole('button', { name: 'Next' }).click();await page.getByLabel('Enter your password').fill(process.env.GOOGLE_PWD);✔ 2𝗙𝗔 & 𝗢𝗧𝗣 – Automate One-Time PasswordsUse otpauth to generate verification codes dynamically.import * as OTPAuth from 'otpauth';const totp = new OTPAuth.TOTP({ secret: process.env.GITHUB_OTP, digits: 6, period: 30 });await page.getByPlaceholder('XXXXXX').fill(totp.generate());✔ 𝗔𝗣𝗜 𝗧𝗼𝗸𝗲𝗻𝘀 – Secure & scalableStore tokens as environment variables and pass them in request headers.const response = await request.get('https://lnkd.in/dz48DrYF', {headers: { Authorization: `Bearer ${process.env.API_TOKEN}` }});🔄 𝗥𝗲𝘂𝘀𝗶𝗻𝗴 𝗔𝘂𝘁𝗵𝗲𝗻𝘁𝗶𝗰𝗮𝘁𝗶𝗼𝗻 𝗦𝘁𝗮𝘁𝗲 𝗘𝗳𝗳𝗶𝗰𝗶𝗲𝗻𝘁𝗹𝘆1️⃣ 𝗖𝗮𝗽𝘁𝘂𝗿𝗲 𝗦𝘁𝗮𝘁𝗲 𝗢𝗻𝗰𝗲Save session data:👉 await page.context().storageState({ path: 'playwright/.auth.json' });2️⃣ 𝗨𝘀𝗲 𝗜𝘁 𝗶𝗻 𝗧𝗲𝘀𝘁𝘀Load saved authentication to skip login:👉 test.use({ storageState: 'playwright/.auth.json' });3️⃣ 𝗚𝗹𝗼𝗯𝗮𝗹 𝗦𝗲𝘁𝘂𝗽 (𝗕𝗲𝘀𝘁 𝗣𝗿𝗮𝗰𝘁𝗶𝗰𝗲 💡)Run authentication once before tests in auth.setup.ts, saving time and API requests.import { test as setup } from '@playwright/test';setup('authenticate', async ({ page }) => { await page.goto('https://example.com'); await page.getByRole('button', { name: 'Login' }).click(); await page.fill('#email', process.env.USER_EMAIL); await page.fill('#password', process.env.USER_PASSWORD); await page.getByRole('button', { name: 'Sign In' }).click(); await page.context().storageState({ path: 'playwright/.auth.json' }); });hasztaghasztaghasztaghasztag

---

## 131. Can ARIA Snapshots Slash Your UI Test Suite?

### Tekst postu

Can ARIA Snapshots Slash Your UI Test Suite?

Callum Porter

Staff Quality Engineer at Ceros | AI-Enabled Quality Engineering Leader | QA Automation Specialist

What if the key to faster and more reliable test automation is eliminating UI tests instead of writing more?

In the high-stakes world of quality assurance, where every second counts and reliability is essential, Playwright's latest update prompts us to rethink our approach to UI test automation. Traditional UI tests have long been the backbone of QA strategies, but are they preventing us from achieving efficiency? Enter ARIA snapshots, the tool that could transform how we validate web applications by emphasising accessibility instead of exhaustive checks of UI content.

Why More UI Tests Aren't the Answer

For years, the prevailing belief has been that writing more UI tests results in higher application quality. However, this perspective is beginning to show its limitations. Comprehensive UI tests are not only time-consuming but also highly fragile. Even minor changes to the user interface can cause these tests to fail, leading to a maintenance headache and slowing down your release cycles.

The myth of comprehensive UI testing fails to acknowledge the diminishing returns associated with adding more tests. Each additional UI test can increase the maintenance burden without significantly improving defect detection rates. Furthermore, redundant tests frequently check the same functionalities, wasting valuable testing resources and resulting in flaky tests undermining trust in your test suite.

Accessibility is becoming the new frontier in QA automation. By moving our focus from superficial UI validations to meaningful accessibility checks, we can better address applications' semantic correctness and usability. ARIA snapshots provide a detailed view of a web page's accessibility tree, including the roles, attributes, and relationships between elements. This approach ensures compliance with standards like WCAG and inherently addresses essential aspects of UI correctness, reducing the need for redundant UI content tests.

Harnessing ARIA Snapshots for Efficient QA

ARIA snapshots can significantly enhance your test automation strategy. They provide a human-readable and machine-processable YAML representation of the accessibility tree. This structured format enables thorough validation of your application's accessibility features.

Implementing ARIA Snapshots is straightforward:

Capture an ARIA Snapshot: Utilise Playwright's toMatchAriaSnapshot assertion to capture the current accessibility tree.

await expect(page.locator('body')).toMatchAriaSnapshot();

Store Snapshots: Save the generated YAML snapshots in your test repository to enable future comparisons.

Automate Comparisons: Integrate snapshot comparisons into your CI/CD pipeline to automatically identify discrepancies.

Update Snapshots: Use the --update-snapshots flag to refresh snapshots when legitimate UI changes occur.

npx playwright test --update-snapshots

Balancing ARIA Snapshots with Essential UI Tests:

While ARIA snapshots can effectively replace many UI content tests, specific scenarios still necessitate traditional UI testing. Traditional UI tests help verify visual layouts, validate data, and assess user interactions. By adopting a hybrid testing strategy—where ARIA snapshots manage accessibility and semantic structure alongside selective UI tests for visual and interactive elements—teams can achieve a comprehensive approach that combines the strengths of both methods.

Best Practices:

Selective Snapshotting: Concentrate on creating snapshots of critical components to minimise noise and reduce maintenance efforts.

Regular Updates: Periodically review and update snapshots to ensure they accurately reflect intentional changes made to the user interface.

Version Control Integration: Store snapshots in version control systems to monitor changes over time.

Embracing a Smarter Approach to Test Automation

Traditional UI tests are valuable, but they often involve high maintenance costs, brittleness, and slow execution times, which can hinder the efficiency and scalability of test automation efforts. ARIA snapshots provide a focused, resilient, and efficient alternative by validating the semantic structure and accessibility of web applications, thereby reducing the reliance on extensive UI content tests.

The Future of QA Automation is increasingly centred on accessibility-driven testing. As the industry emphasises inclusivity, tools like ARIA snapshots are becoming essential for developing robust and maintainable test suites. By adopting ARIA snapshots, QA professionals can overcome the limitations of traditional UI testing and achieve greater efficiency, reliability, and inclusivity in their test automation strategies.

In the quest for faster and more reliable test automation, sometimes the path forward is not to add more tests but to reimagine what and how we test.

Have you experimented with ARIA snapshots in your test automation workflows yet? I'd love to hear about your experiences—whether you've seen significant improvements in test efficiency or faced challenges in implementation.

### Tekst z obrazka

ab
o > i |

---
