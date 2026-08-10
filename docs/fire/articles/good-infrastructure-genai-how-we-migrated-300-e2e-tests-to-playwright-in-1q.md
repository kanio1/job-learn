# Good Infrastructure + GenAI: How we migrated 300+ E2E tests to Playwright in 1Q

| Field | Value |
|-------|-------|
| **Author** | Liron Arad |
| **Published** | 2026-01-28T21:28:01.643Z (Jan 28, 2026) |
| **URL** | https://medium.com/kenshoos-engineering-blog/good-infrastructure-genai-how-we-migrated-300-e2e-tests-to-playwright-in-1q-b6afe90d750f |
| **Scraped with** | Firecrawl `firecrawl_scrape` (`formats: ["markdown"]`, `onlyMainContent: true`) |

## Firecrawl metadata

```json
{
  "title": "Good Infrastructure + GenAI: How we migrated 300+ E2E tests to Playwright in 1Q | by Liron Arad | skai engineering blog | Medium",
  "og:title": "Good Infrastructure + GenAI: How we migrated 300+ E2E tests to Playwright in 1Q",
  "author": "Liron Arad",
  "publishedTime": "2026-01-28T21:28:01.643Z",
  "article:published_time": "2026-01-28T21:28:01.643Z",
  "og:url": "https://medium.com/kenshoos-engineering-blog/good-infrastructure-genai-how-we-migrated-300-e2e-tests-to-playwright-in-1q-b6afe90d750f",
  "description": "This is the story of how we migrated our entire E2E testing infrastructure to Playwright — and how we did it in less than three months.",
  "statusCode": 200,
  "sourceURL": "https://medium.com/kenshoos-engineering-blog/good-infrastructure-genai-how-we-migrated-300-e2e-tests-to-playwright-in-1q-b6afe90d750f"
}
```

---

In the world of fast-paced SaaS development, your testing suite can either be an anchor or a sail. For the past few years, we relied on **Testim.io** for our UI test automation. It served us well during our growth phase, but as our engineering organization matured, we found ourselves hitting the limits of "low-code" solutions and facing the looming shadow of vendor lock-in.

With our contract set to expire in December, we faced a choice: renew the status quo or rebuild our entire E2E stack under an aggressive deadline. We chose the latter. This is the story of how we migrated our entire E2E testing infrastructure to **Playwright** — and how we did it in less than three months.

## Why Playwright? The Strategic Shift

Playwright is a modern, open-source testing framework designed for web applications. It enables developers to write reliable, fast, and scalable tests across major browsers — including Chromium, Firefox, and WebKit — with a unified API. By providing a code-first, developer-centric experience with intuitive APIs and built-in tools for debugging, Playwright makes it easier to integrate E2E testing directly into the development workflow.

Our decision to adopt Playwright wasn't just about saving on licensing costs or technical features. It was driven by a fundamental organizational shift: the closing of our dedicated QA department.

Today, our developers are the owners of E2E quality. In this new reality, we needed a tool that matched their workflow. Playwright emerged as the winner because it provides a code-first, developer-centric experience. Developers feel more comfortable writing tests in the same IDEs and languages they use for feature code, turning testing from a specialized "extra" task into a core part of the development lifecycle.

## Technical Foundation: From Dockers to Labs

Internally, we operate a set of managed, persistent test environments we call Automation Labs.

A major part of this migration was moving away from the overhead of managing local Dockerized environments for every test run. While we still support Docker for isolated component testing (using a simple custom annotation that informs the test runner to construct such a dedicated local environment), the heavy lifting for our E2E suite moved to **Automation Labs**.

These Labs are persistent, managed testing environments that provide a critical advantage: Production-Grade Fidelity. Unlike ephemeral Dockers that often lack the complexity of a live system, Labs provide:

- **Production Mirroring:** They allow teams to run tests against environments that accurately replicate production configurations, enabling the reliable reproduction of production-like scenarios and edge cases.
- **Consistent Data State:** Unlike ephemeral Dockers that start from scratch, Labs maintain a baseline that is refreshed on a predictable schedule, ensuring a stable foundation for every run.
- **Scalability:** They enable the execution of massive test suites across many environments in parallel without the local machine resource drain or performance bottlenecks.
- **Build Time Optimization**: The shift to Playwright significantly boosted performance, reducing our overall **build time by 60%**.

## Our Execution Strategy: From Code to Release

A key part of our migration was defining a clear execution lifecycle. We categorized our runs into three tiers to balance developer speed with the stability required for a daily release cycle:

## 1. PR-Level Checks: Fast & Isolated

For every Pull Request, we utilize **Dockerized environments**.

- **The Goal**: Immediate feedback and total isolation. Every developer gets a fresh instance that is destroyed after the check, preventing any cross-contamination.
- **The Data**: We use automated data preparation tools to spin up specific data sets within the container, allowing tests to run in a controlled environment within approximately 30 minutes.

## Nightly Runs: Preparing the Daily Build

The core of our release engine is the Nightly run, executed against the **Nightly Automation Lab**.

- **The Goal**: This serves as our primary validation for the **Daily Build**. By running the full suite overnight, we identify cross-team regressions that PR checks might miss, ensuring the codebase is stable enough for the next day's release.
- **The Environment**: A persistent lab that is **refreshed daily** to ensure tests are always running against the most current data baseline.

## 3. Release Sanity: The Final Sign-off

Before a version is officially shipped, we conduct final sanity checks in the **Pre-Prod Automation Lab**.

- **The Goal**: Production-grade validation.
- **The Data**: This is our most robust environment, refreshed weekly, combining automated test data with synced live production data to simulate real-world scale and complexity for a final "Go/No-Go" decision.

## Solving the Data Problem: The DB Builder

One of the biggest friction points in E2E testing is data setup. To solve this, we utilized our **DB Builder** tool. Instead of manually clicking through the UI to create an account and some business entities within it before every test, we use **Cucumber feature files** to generate test data prior to test execution.

This process runs periodically on the environments before the E2E tests begin, ensuring the required state is "baked in" and ready for the browser.

By using the `@PlaywrightDbBuilder` annotation, developers can define their required state in Gherkin:

```
Given I create the following agencies:
 | name | user feature |
 | TestAgency | FEATURE_X |
And I create the following profiles:
 | profile id | in agency | name |
 | 300001 | TestAgency | TestProfile |
```

This tool automatically injects the data into the Lab's database. This ensures our tests are **atomic and idempotent**, as the DB Builder handles the heavy lifting of backend entity creation before the Playwright script ever opens a browser.

**Navigating Engineering Challenges**

The migration wasn't without friction. We solved three core technical hurdles:

- **Concurrency & Isolation**: To prevent teams from impacting each other, we implemented strict **test isolation**. Every suite runs in a sandboxed state.
- **The "False Negative" Trap**: We eliminated phantom failures from previous data remnants by enforcing **strict environment cleanup** for every run.
- **DB Schema Coupling**: Since the **DB Builder** generates data directly into the schema, refactors can break tests. We now ensure tests always run against a **matching DB Builder version** to keep data in sync.

## The AI Secret Sauce: Agentic Workflows with MCP

Now that we ensured stable environments and deterministic data, we're left with the actual migration of hundreds of scenarios. Rewriting 310 tests manually would have been impossible in one quarter. We leveraged **Agentic AI** — specifically using **multiple MCP (Model Context Protocol) servers** giving it access to existing resources— to turn our AI agents from mere chat-bots into active participants in our migration.

The real "magic" happened through a structured instruction set that allowed the AI agent to navigate our internal repositories autonomously:

1. **Architecture-Aware Generation:** We created a shared library of internal utilities that interact with our specific UI components. We instructed the agents to use these helpers exclusively. This not only ensured the generated code followed our modular architecture but also enforced "Clean Code" principles across all 310 tests.
2. **Context Discovery:** Using MCP servers, the agents could "pull" documentation for specific UI modules on demand, ensuring they stayed updated with our latest API changes without needing a full model retrain.
3. **Cross-Repository Data Prep:** The agents weren't limited to the UI repo. If a test needed a specific DB state, they were prompted to use the GitHub MCP server to read the **DB Builder** docs in our backend repository, generate the necessary Cucumber feature file, and submit it as a PR automatically.
4. **Local Execution & Self-Healing:** Agents ran the generated tests against the lab using Playwright MCP. If a test failed, the agent analyzed the failure, fixed the script, and produced a summary mapping each fix for future training.

To maintain high architectural standards, we implemented a **"human-in-the-loop"** checkpoint here — Instead of letting the agent commit code blindly, developers reviewed the proposed "self-healing" fixes.

This step ensured that the agent didn't just make the test _pass_ (e.g. by adding brittle selectors), but fixed it in a way that aligned with our long-term maintenance goals.

## The Results: By the Numbers

The migration was a total success. By our mid-December deadline:

- **Total Tests Migrated:** ~310
- **Completion Rate:** 99.2%
- **Squads Involved:** 22
- **Infrastructure:** 100% of new UI tests now run on Automation Labs using Playwright.

## Lessons Learned

The success of this migration wasn't just about the tool — it was about **infrastructure readiness**. While it's tempting to call the process "frictionless," the reality is that no migration at this scale is. We faced real engineering hurdles, from ensuring test isolation in shared labs to keeping data generation in sync with evolving database schemas.

However, by leaning into these challenges rather than ignoring them, we reached our main takeaway: **Focus on the infrastructure, and let AI do the repetitive work.**

By providing a stable foundation through our dedicated labs and data generation tools, we created an environment where a well-crafted AI agent could thrive. This combination turned a task that would have required endless hours of grunt work into a feasible engineering undertaking. Today, our UI testing (just like all other testing layers, e.g. unit and component testing) is no longer a separate task performed in a third-party UI; it is version-controlled code and a core part of how we ship software.
