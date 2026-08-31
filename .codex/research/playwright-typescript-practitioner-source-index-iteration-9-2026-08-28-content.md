---
title: "Playwright + TypeScript practitioner source index — article content"
source: ".codex/research/playwright-typescript-practitioner-source-index-iteration-9-2026-08-28.md"
retrieved: "2026-08-29"
---

# Playwright + TypeScript practitioner source index

## Kolejność i pokrycie

The source index contains 20 HTTP/HTTPS Markdown links in the order of appearance; all are unique canonical URLs, so no duplicates were merged. Local Markdown links (the iteration 1–8 index files and the Michal Drajna catalogue) were not fetched.

| # | Autor/serwis | Tytuł | URL | Status |
|---:|---|---|---|---|
| 1 | Anton Gulin | Your AI Model Is a Dependency: Pin It, Keep a Fallback, Re-Verify | https://www.anton.qa/blog/posts/your-ai-model-is-a-dependency | complete |
| 2 | Anton Gulin | Porting Anthropic's Skill Creator from Python to TypeScript | https://www.anton.qa/blog/posts/porting-anthropic-s-skill-creator-from-python-to-typescript | complete |
| 3 | ScrollTest / Promode | AI Test Failure Triage for Playwright Teams | https://scrolltest.com/ai-test-failure-triage-playwright/ | complete |
| 4 | ScrollTest / Promode | Playwright PromptFoo Starter Suite for QA Teams | https://scrolltest.com/playwright-promptfoo-starter-suite/ | complete |
| 5 | ScrollTest / Promode | MCP 2.0 Breaking Changes Every QA Engineer Must Know | https://scrolltest.com/mcp-2-0-breaking-changes-qa-guide/ | complete |
| 6 | ScrollTest / Promode | DeepEval vs Ragas: What QA Engineers Should Learn | https://scrolltest.com/deepeval-vs-ragas-ai-qa-day-63/ | complete |
| 7 | Currents (Andrew Goldis) | Playwright 1.60.0 Release Updates | https://currents.dev/posts/pw-1.60.0 | complete |
| 8 | TestDino (Dhruv Rai) | Playwright AI Ecosystem 2026: MCP, Agents & Self-Healing Tests | https://testdino.com/blog/playwright-ai-ecosystem | complete |
| 9 | TestDino (Dhruv Rai) | Playwright Test Agents: Planner, Generator and Healer Guide | https://testdino.com/blog/playwright-test-agents | complete |
| 10 | TestDino (Savan Vaghani) | Playwright Screencast: Record Tests | https://testdino.com/blog/playwright-screencast | complete |
| 11 | TestDino (Dhruv Rai) | Playwright Locators Guide | https://testdino.com/blog/playwright-locators | complete |
| 12 | TestDino (Savan Vaghani) | GitHub Copilot with Playwright: Setup, MCP & Test Guide | https://testdino.com/blog/playwright-tests-with-copilot | complete |
| 13 | TestDino | What is the Accessibility Tree? | https://testdino.com/blog/accessibility-tree | metadata-only |
| 14 | Artem Bondar / Bondar Academy | Is Playwright MCP Worth It for Test Automation? | https://bondaracademy.com/blog/is-playwright-mcp-worth-it | complete |
| 15 | Playwright | Best Practices (official docs) | https://playwright.dev/docs/best-practices | complete |
| 16 | Playwright | Fixtures (official docs) | https://playwright.dev/docs/test-fixtures | complete |
| 17 | Playwright | Assertions (official docs) | https://playwright.dev/docs/test-assertions | complete |
| 18 | Playwright | Configuration (official docs) | https://playwright.dev/docs/test-configuration | complete |
| 19 | Playwright | Test API: `test` (incl. `test.step`) | https://playwright.dev/docs/api/class-test#test-step | complete |
| 20 | Playwright | Authentication (official docs) | https://playwright.dev/docs/auth | complete |

## Artykuły

### 1. Anton Gulin — Your AI Model Is a Dependency: Pin It, Keep a Fallback, Re-Verify

- Source: https://www.anton.qa/blog/posts/your-ai-model-is-a-dependency
- Retrieved: 2026-08-29
- Exa status: complete

July 8, 2026.

Nineteen days. That is how long one of the world's best AI models was simply gone this summer.

Claude Fable 5 went offline on June 12 by government order. It came back on July 1. Between those dates, every test suite that leaned on it had a problem. Some teams fixed it in one line. Others lost days, twice.

The difference was not luck. It was whether they treated the model like what it is: a dependency.

## What "the model is a dependency" actually means

Your test suite already has dependencies. A database version. A browser version. A Node version. You pin them all. An AI model is the same kind of moving part, with three extra ways to hurt you:

1. **It changes silently.** Providers update models behind the same name. The model you tested in March is not always the model answering in July.
2. **It disappears.** Deprecations happen every quarter. And as of this summer, so do government-ordered shutdowns. Fable 5 was not deprecated — it was switched off overnight, worldwide.
3. **It gets swapped without telling you.** The fine print of the Fable 5 return says blocked requests are rerouted to a different model (Opus 4.8). Your pipeline asks one brain and sometimes gets another.

A test that stands on a part like this, unpinned and unwatched, is not a test. It is a hope.

## Rule 1 — Pin the version

Never let a test suite float on "latest" or on a bare model family name. Point it at the exact model ID, in one place.

```typescript
// config/models.ts — ONE place the whole suite imports from
export const MODELS = {
  primary: "claude-fable-5",      // pinned: the exact ID we validated
  fallback: "claude-opus-4-8",    // pinned: the exact ID we validated
} as const;
```

Every agent, every AI-assisted test imports from this file. Nothing names a model directly. When the world changes, you edit one line, not forty files.

If you use an agent framework, the same rule applies to its config. In Stagehand, for example, the model is an explicit setting:

```typescript
const stagehand = new Stagehand({
  modelName: MODELS.primary,   // never omit this
});
```

## Rule 2 — Keep a fallback you have already validated

A fallback you pick during the outage is not a fallback. It is a gamble made under pressure.

Pick the second model now, on a calm day. The bar is simple: it must run your real suite acceptably. Not "it is a good model" — *your suite, green, at a cost you accept.*

Write it into the same config file as `MODELS.fallback`. The article continues with a third rule on running a parity suite on both models cyclically.

### 2. Anton Gulin — Porting Anthropic's Skill Creator from Python to TypeScript

- Source: https://www.anton.qa/blog/posts/porting-anthropic-s-skill-creator-from-python-to-typescript
- Retrieved: 2026-08-29
- Exa status: complete

April 16, 2026 · 6 min read.

Technical decisions and lessons learned from rewriting a Python CLI tool as an OpenCode plugin. Adjacent to Playwright, but useful for idiomatic TypeScript: separate workflow knowledge (`SKILL.md`) from executable tools, validate input structure, use eval→improve→benchmark, preserve baselines, and keep working artifacts outside the repo. This is not direct advice about the Playwright runner.

## Why Port It?

Anthropic's skill-creator for Claude Code is excellent. It introduced eval-driven development for AI agent skills — write a skill, test it with evals, optimize the description, benchmark the results. The methodology is proven.

But it has a limitation: it only works with Claude Code, and skill access requires a paid subscription ($20/month minimum). Free tier users can't use it at all.

OpenCode is free and supports 300+ models. The author wanted to bring the same methodology to OpenCode users — for free, with no paywall.

## High-Level Architecture

The original has this structure:

```text
Anthropic skill-creator/
├── SKILL.md                 # The skill instructions
├── scripts/
│   ├── run_loop.py          # Eval→improve optimization loop
│   ├── improve_description.py
│   ├── aggregate_benchmark.py
│   └── generate_review.py
└── evals/
    └── evals.json           # Test query definitions
```

The ported version:

```text
opencode-skill-creator/
├── skill-creator/           # The SKILL
│   ├── SKILL.md             # Main skill instructions
│   ├── agents/
│   ├── references/
│   └── templates/
└── plugin/                  # The PLUGIN (npm package)
    ├── package.json
    ├── skill-creator.ts     # Entry point
    └── lib/
        ├── validate.ts      # Skill structure validation
        ├── run-eval.ts      # Trigger evaluation
        ├── run-loop.ts      # Eval→improve loop
        └── ...
```

Key difference: the skill provides workflow knowledge, the plugin provides executable tools. The agent orchestrates everything by calling tools during its session.

## Decision 1: Scripts → Plugin Tool Calls

Original: Python scripts invoked via CLI. New: Plugin tool calls in OpenCode sessions — the agent calls the tool inline and gets results back in the session. No subprocess management, no Python environment.

This is cleaner integration but also more composable. The agent can interleave tool calls with other work between optimization iterations.

## Decision 2: Python → TypeScript

The original requires Python 3.11+ and pyyaml. The ported version requires nothing beyond Node.js. All pipeline components are TypeScript modules in the plugin. ~256kB unpacked on npm. Dependency tree is minimal: the plugin only depends on `@opencode-ai/plugin` (peer dependency).

## Decision 3: Static HTML → HTTP Review Server

Original: Python script generates a static HTML file and opens it in the browser. New: Plugin starts a local HTTP server that serves an interactive eval viewer with real-time updates and interactive save buttons.

### 3. ScrollTest / Promode — AI Test Failure Triage for Playwright Teams

- Source: https://scrolltest.com/ai-test-failure-triage-playwright/
- Retrieved: 2026-08-29
- Exa status: complete

August 9, 2026. Day 62 of 100 Days of AI in QA and SDET.

AI test failure triage is where many QA teams should apply AI first, before they ask an agent to write hundreds of new tests. If your Playwright suite already produces traces, screenshots, videos, console logs, network events, and retry data, you are sitting on a better dataset than most AI tools get by default.

I see teams waste expensive engineer hours on the same boring question: did this failure come from the product, the test, the environment, or missing data? A small AI triage layer can answer that question faster, but only when the pipeline feeds it structured evidence and forces it to explain its confidence.

## Why AI Test Failure Triage Matters Now

AI test failure triage is not about replacing a senior SDET. It is about removing the first 10 to 20 minutes of repetitive evidence reading after a red CI run. That time looks small until a 40-person engineering group burns it every day across pull requests, nightly suites, release branches, and hotfix builds.

Playwright adoption makes this moment practical: the Microsoft Playwright GitHub repository shows more than 94,000 stars, and the npm downloads API reported more than 208 million downloads for `@playwright/test` in the last month.

### Why failure review is the bottleneck

Most automation reports still stop at pass or fail. A test named `checkout should apply coupon` fails, the HTML report shows a screenshot, and somebody has to open the trace manually. That is investigation, not testing.

### What AI is good at here

An LLM is useful when the job is to read mixed evidence and produce a structured first draft. Screenshots, trace summaries, request failures, stack traces, and console logs are messy for dashboards, but they are understandable to a model when you compress them correctly.

The author does not want the model to decide whether we ship. The desired output: "This looks like a test bug because the locator changed from `data-testid=pay-now` to `data-testid=submit-payment`, confidence 0.78, suggested owner QA framework." That is a useful first pass.

## The Four-Bucket Failure Taxonomy

A triage agent is only as good as the labels you give it. If you ask "why did this fail?" you will get a paragraph. If you ask it to classify a failure into a controlled taxonomy, you get data you can trend over time.

Four top-level buckets for AI test failure triage:

1. **Product bug:** the application behavior is wrong or regressed.
2. **Test bug:** the test logic, locator, assertion, wait, or fixture is wrong.
3. **Environment issue:** infrastructure, dependency, browser, network, service, or CI capacity caused the failure.
4. **Data issue:** setup data, user state, seed records, feature flags, or tenant configuration caused the failure.

### Product bug

A product bug usually has evidence outside the test: a 500 response from a checkout API, a JavaScript exception after clicking a valid button, or a UI state that violates the expected business rule. The AI should cite specific application evidence, not just "assertion failed."

### Test bug

A test bug is usually boring and common: the locator is brittle, the test assumes ordering that no longer holds, or the fixture doesn't clean up.

### 4. ScrollTest / Promode — Playwright PromptFoo Starter Suite for QA Teams

- Source: https://scrolltest.com/playwright-promptfoo-starter-suite/
- Retrieved: 2026-08-29
- Exa status: complete

August 4, 2026. Day 57 of the 100 Days of AI in QA and SDET series.

A Playwright PromptFoo starter suite gives QA teams one small repo where browser checks, API checks, and LLM evaluation checks run together before a release. This pattern turns AI testing from a vague experiment into a clear pull request gate.

We will set up the repo shape, decide what belongs in Playwright, decide what belongs in PromptFoo, and wire the results into CI without pretending one tool can test everything.

## Why a Playwright PromptFoo starter suite is useful now

Most QA teams now test two products at the same time. The first product is still the normal web app: pages, forms, APIs, roles, permissions, and data flows. The second product is the AI behavior wrapped inside that app: summarizers, support copilots, test generation tools, search assistants, and decision helpers.

Traditional automation handles the first product well. AI behavior needs a different style of checking because the output can vary while still being acceptable. That is where PromptFoo fits beside Playwright instead of replacing it.

The mistake I see in teams: they put every assertion inside Playwright. A browser test then becomes a 300-line monster that logs in, opens a feature, sends an AI prompt, parses text, judges tone, checks safety, checks retrieval quality, and takes a screenshot. The failure report becomes unreadable.

A better split: Playwright proves the user journey works. PromptFoo proves the AI answer meets the rubric. CI reads both results and makes one release decision.

### What goes where

- Use Playwright for login, navigation, selectors, network mocking, API setup, file downloads, traces, screenshots, and user-visible flows.
- Use PromptFoo for prompt regression, model comparison, factuality checks, rubric-based grading, red-team style cases, and dataset-driven answer quality.
- Use CI for policy: pass rate threshold, critical failures, flaky retry rules, and artifact upload.
- Use a shared fixtures folder for test data so browser and LLM checks speak about the same examples.

### Pin versions, do not float them

For learning, latest is fine. For a production QA repo, pin exact versions in package.json and update on purpose. AI tooling moves quickly, and a minor change in evaluator behavior can create noisy failures on Monday morning.

```typescript
{
  "devDependencies": {
    "@playwright/test": "1.62.1",
    "promptfoo": "0.121.20",
    "typescript": "^5.5.0"
  }
}
```

The article continues with the repository architecture, wiring Playwright and PromptFoo into a single CI policy gate.

### 5. ScrollTest / Promode — MCP 2.0 Breaking Changes Every QA Engineer Must Know

- Source: https://scrolltest.com/mcp-2-0-breaking-changes-qa-guide/
- Retrieved: 2026-08-29
- Exa status: complete

August 19, 2026.

On July 28, 2026, the MCP Python SDK shipped version 2.0.0 as a stable release, and `pip install mcp` now installs 2.x by default. If your team built test tooling, AI agents, or browser automation on top of the Model Context Protocol, some of it broke on the next dependency update.

Two things happened at the same time. First, the Python SDK was rebuilt: a new dispatcher engine under both the client and the server, a first-class `Client` class, and a set of renames. Second, the protocol itself moved from the 2025-11-25 revision to the 2026-07-28 revision. The new revision removes the connection handshake, removes the session concept, and removes every server-initiated request. That is a bigger deal than the rename, because it changes what your tests can even assert.

The stable release notes are short and worth reading. The v2.0.0 release states plainly that v1.x is now in maintenance mode and will only receive security fixes. If your project is not ready to migrate, the official guidance is to pin an upper bound: `mcp>=1.28,<2`.

One detail the author likes: v2 serves both protocol eras from the same `MCPServer`. It speaks the 2026-07-28 revision and still serves every 2025-era client over Streamable HTTP and stdio with nothing to configure. A migration does not force you to upgrade every client in your fleet on the same day.

## The Version Split Nobody Explains Clearly

If you check the npm registry, the main package `@modelcontextprotocol/sdk` is still on 1.30.0, not 2.0.0. The Python SDK is on 2.0.0. The TypeScript server package `@modelcontextprotocol/server` hit 2.0.0 on July 27, 2026. So you have npm SDK at 1.30.0, TypeScript server at 2.0.0, and Python at 2.0.0, all describing the same protocol revision. Three version lines, one spec.

Why does this matter for a QA lead? Your test harness probably pins one SDK version while the tool it tests pins another. The author has seen teams where the agent under test runs the Python server at 2.0.0 while the test harness drives it through a TypeScript client at 1.30.0, then spend a day chasing a handshake mismatch that was actually a version mismatch.

## Breaking Change One: FastMCP Is Now MCPServer

The old import path is gone rather than deprecated. `FastMCP` is now `MCPServer`, and it moved modules. The article continues with additional breaking changes including schema source modules moved into `@modelcontextprotocol/core`.

### 6. ScrollTest / Promode — DeepEval vs Ragas: What QA Engineers Should Learn

- Source: https://scrolltest.com/deepeval-vs-ragas-ai-qa-day-63/
- Retrieved: 2026-08-29
- Exa status: complete

August 11, 2026. Day 63 of 100 Days of AI in QA and SDET.

DeepEval vs Ragas is the comparison to use when QA engineers ask "Which LLM evaluation tool should I learn first?" The short answer: learn both, but do not use them for the same job. DeepEval is stronger when you want to test an AI application's behavior, while Ragas is strongest when your risk sits inside retrieval augmented generation.

Teams make one expensive mistake: they treat every LLM test as a chatbot test and forget that a RAG product can fail because retrieval pulled the wrong chunks before the model even generated an answer.

## Why DeepEval vs Ragas Matters for QA

Most QA engineers are comfortable with deterministic software. Click this button, call this API, assert that field. AI products are different because a "pass" can be shallow. The response can be grammatical, confident, and still wrong.

That creates a new job for SDETs. We must test the behavior of the model-facing feature and also test the evidence that feeds the model. If we only evaluate the final answer, we miss retrieval bugs. If we only evaluate retrieval, we miss instruction-following bugs, hallucinations, tone issues, refusal bugs, and unsafe completions.

Adoption numbers: DeepEval version 4.1.7, about 6.06 million recent monthly downloads on PyPI. Ragas 0.4.3, about 1.57 million recent monthly downloads. These are not toys.

### Why a normal automation mindset is not enough

Traditional automation asks, "Did the system produce the expected output?" LLM evaluation asks a harder question: "Is this output acceptable against a rubric, dataset, policy, and real user intent?" The oracle is no longer one string. It becomes a scoring method.

### Where the risk hides

Failures usually hide in one of five buckets:

- **Prompt drift:** a prompt change breaks previous behavior.
- **Retrieval issue:** the system fetches irrelevant or incomplete context.
- **Dataset gap:** the evaluation set misses an important user path.
- **Model variance:** the same input behaves differently after a model or parameter change.
- **Product bug:** the UI, API, auth layer, or workflow around the model is broken.

## The Quick Answer: Use Both, But Separate the Risk

DeepEval is for evaluating the AI application's answer and behavior; Ragas is for evaluating RAG quality, especially retrieval, context, and answer grounding.

The practical split:

1. Use DeepEval when the test asks, "Did the assistant answer correctly, follow the instruction, avoid hallucination, and satisfy the user's goal?"
2. Use Ragas when the test asks, "Did the retriever fetch the right evidence, and was the generated answer faithful to that evidence?"
3. Use Playwright or API tests around both when the test asks, "Can the user actually complete the workflow?"

### A simple decision table

| Testing need | Better fit | Why |
| --- | --- | --- |
| Chatbot answer quality | DeepEval | Application-level metrics and test cases |
| RAG context relevance | Ragas | RAG-specific evaluation focus |
| Hallucination checks | DeepEval | Verifies factual claims in answers |
| Retrieval faithfulness | Ragas | Measures grounding in retrieved context |

The article continues with CI integration, sample test configurations, and what Playwright/API layers should and should not contain.

### 7. Currents — Playwright 1.60.0 Release Updates

- Source: https://currents.dev/posts/pw-1.60.0
- Retrieved: 2026-08-29
- Exa status: complete

Andrew Goldis, May 11, 2026 (updated May 19, 2026).

Version 1.60.0 of Playwright has been released on May 11, 2026 and introduced a few compatibility issues with Currents. To use newer versions of Playwright (>= 1.60.x) update to the new v2 of the Currents Playwright reporter.

TL;DR

- New v2 of the Currents Reporter for Playwright to restore compatibility: `npm install @currents/playwright@latest`.
- Breaking changes for Orchestration feature users (pwc-p): follow the migration guide.
- If you don't use pwc-p to run your tests, you can simply update the Currents Playwright reporter version.

## Timeline

**May 19, 2026, 5pm PDT:** Released version 2.0.0. `npm install @currents/playwright@2.0.0`. For users of orchestration there are breaking changes, and you need to update your CI to use the new subcommand format.

**May 12, 2026, 11pm PDT:** Released 2.0.0-beta.3 with additional improvements. `npm install @currents/playwright@2.0.0-beta.3`.

**May 11, 2026, 11pm PDT:** Released beta version that restores compatibility with Playwright 1.60.0. `npm install @currents/playwright@beta`.

**May 11, 2026, 1pm PDT:** Version 1.60.0 of Playwright introduced compatibility issues that temporarily broke integration with Currents. Specifically, a couple of internal configuration items that were used by Currents were removed.

The lesson per the index synthesis: upgrading Playwright means testing compatibility of the entire toolchain (reporter/orchestrator/config), not just `package.json`; check release notes and run a small gate before the full suite.

### 8. TestDino (Dhruv Rai) — Playwright AI Ecosystem 2026: MCP, Agents & Self-Healing Tests

- Source: https://testdino.com/blog/playwright-ai-ecosystem
- Retrieved: 2026-08-29
- Exa status: complete

Published March 13, 2026, updated June 30, 2026.

Every week your test suite breaks, and it's almost never because of a real bug. A button ID changes. A class name gets refactored. A modal loads 200ms slower than before. Your CI pipeline turns red. An engineer spends the next hour fixing tests instead of building features.

This is the maintenance trap that most test automation teams live in. Traditional automation simply was not built to keep up with how fast modern UIs change.

That is exactly the problem the Playwright AI ecosystem was designed to solve. With the release of MCP, built-in AI agents, and accessibility-tree-first execution in 2026, Playwright now gives AI models a structured way to explore your app, generate tests, and self-heal failures automatically.

## What is the Playwright AI ecosystem and how does it work in 2026?

The Playwright AI ecosystem is the integrated stack of protocols, built-in agents, CLI tooling, third-party platforms, and AI-assisted authoring capabilities that let AI models plan, write, execute, and repair Playwright tests using structured browser access instead of guesswork.

Four layers working together:

- **Protocol layer:** Playwright MCP (Model Context Protocol) gives AI models a controlled, standardized way to interact with a live browser session through structured tools and accessibility snapshots.
- **Agent layer:** Three specialized agents (Planner, Generator, Healer) handle the full test lifecycle from exploration to maintenance.
- **Authoring layer:** Playwright Codegen, CLI with AI Skills, and IDE integrations (GitHub Copilot, Claude Code) provide entry points for both recording-based and prompt-based test creation.
- **Tooling layer:** External platforms like TestDino, ZeroStep, Bug0, Octomind, TestSprite, and AgentQL plug into this foundation for reporting, natural-language querying, analytics, and scale.

None of these layers rely on screenshots or pixel-matching. The entire ecosystem is built on the browser's accessibility tree, a semantic, structured representation of every element on the page.

| Dimension | Traditional automation | Playwright AI ecosystem (2026) |
| --- | --- | --- |
| Test creation | Manual script writing | AI-generated from natural language, Codegen recordings, or app exploration |
| Selector strategy | CSS / XPath (brittle) | Accessibility-tree-first with `getByRole()` (semantic, stable) |
| Failure recovery | Manual debugging + fix | Healer agent auto-patches selectors and re-runs |
| Maintenance cost | High (selector rot, flakiness) | Significantly reduced via self-healing and MCP-guided fixes |
| AI interaction model | None or screenshot-based | Structured MCP tools + accessibility YAML snapshots |
| CI/CD integration | Manual pipeline config | Azure App Testing, Docker images, native CI hooks |
| Skill requirement | Strong coding knowledge | Natural language input supported, coding still valuable for review |

The ecosystem works with LLMs like Claude, GPT, and Gemini through the standardized MCP protocol and is not locked into any single provider.

The author organizes the ecosystem into protocol (MCP), agents (Planner/Generator/Healer), authoring (Codegen/CLI/IDE) and tooling layers. Claims about "self-healing" and stability should be treated experimentally and require human review of oracle and locators.

### 9. TestDino (Dhruv Rai) — Playwright Test Agents: Planner, Generator and Healer Guide

- Source: https://testdino.com/blog/playwright-test-agents
- Retrieved: 2026-08-29
- Exa status: complete

Published February 28, 2026, updated April 7, 2026.

Playwright agents are AI-powered helpers built into Playwright starting from v1.56. They assist with planning test scenarios, generating Playwright test code, and repairing broken tests. Unlike generic AI code generators that predict what your page might look like, Playwright agents interact with a real browser session and make decisions based on live DOM state.

There are 3 agents, each responsible for a different stage of the testing lifecycle:

| Agent | Primary role | Input | Output | Best used for |
| --- | --- | --- | --- | --- |
| Planner | Scenario discovery | Seed test + running app | Markdown test plan | New features, coverage mapping |
| Generator | Test code creation | Markdown test plan | `.spec.ts` files | Building automation fast |
| Healer | Test maintenance | Failing test suite | Updated, stabilized tests | UI changes, locator drift |

Together, these agents automate the plan-write-fix cycle while keeping your standard Playwright setup unchanged.

## How Playwright agents work under the hood

Playwright agents use the Model Context Protocol (MCP) to connect a large language model with a real browser. The AI doesn't guess what the page looks like. It interacts with the actual application, observes live DOM state, and makes decisions based on real behavior.

### The 3 layers

**Playwright engine** handles browser automation through the Chrome DevTools Protocol.

**LLM layer** uses a large language model (GPT, Claude, or similar) to interpret DOM structure, page routes, and application behavior. The model receives structured accessibility snapshots rather than raw screenshots. The Accessibility Object Model (AOM) gives the LLM clean, semantic information about every element: its role, name, state, and position in the hierarchy. An agent targeting `Role: button, Name: Checkout` is far more stable than one using `div.checkout-btn-v3`.

**Orchestration loop** coordinates the exchange between the engine and the LLM. The article continues with a live demo repository where 67 test scenarios and a fully passing E2E test were generated for a real e-commerce app, plus CI/CD integration guidance.

Per the index synthesis: the plan→code→repair flow should leverage existing fixtures, conventions, data, and trace; the agent does not replace mutation tests, assertions, or isolation. Source says support from v1.56+; exact availability should be checked against the pinned repo version.

### 10. TestDino (Savan Vaghani) — Playwright Screencast: Record Tests

- Source: https://testdino.com/blog/playwright-screencast
- Retrieved: 2026-08-29
- Exa status: complete

Published April 14, 2026, updated May 7, 2026.

A test fails in CI. The log says "element not visible." You check the screenshot, but it only shows the final state. The button is clearly there. So what happened in the three seconds before the failure?

That is the exact gap Playwright screencast fills. It records what the browser actually rendered frame-by-frame during your test, producing a .webm video you can replay, share with your team, or attach to a bug report.

As of Playwright v1.59, screencast recording is no longer just a passive dump. The new `page.screencast` API lets you start and stop recordings mid-test, annotate interactions visually, insert chapter markers for narration, stream frames to AI vision models in real time, and produce video receipts that coding agents can hand off for human review.

Requires: Playwright ≥ 1.59 for the `page.screencast` API. Config-based video recording (`video: 'on'`) works on all recent versions. Supported across Chromium, Firefox, and WebKit.

## What is Playwright screencast?

Playwright screencast is the built-in capability that records the browser viewport during test execution and saves it as a WebM video file. It captures frames directly from the browser rendering engine at the protocol level, which means it works identically in headless and headed modes, requires no external tools, and matches your test's actual viewport dimensions.

The output lands in the `test-results/` folder as a .webm file. Two ways to enable it:

1. **Globally via config** — set `video: 'on'` in `playwright.config.ts` for automatic lifecycle-based recording.
2. **Programmatically via the screencast API** — use `page.screencast.start()` and `page.screencast.stop()` for precise, mid-test control (v1.59+).

```typescript
import { defineConfig } from '@playwright/test';

export default defineConfig({
  use: {
    video: 'on',
  },
});
```

## How to enable Playwright video recording

Four recording modes via the `video` option:

For most CI setups, `'retain-on-failure'` strikes the best balance: video evidence for every failure without burning storage on recordings of passing tests.

```typescript
import { defineConfig } from '@playwright/test';

export default defineConfig({
  use: {
    video: 'retain-on-failure',
  },
});
```

### Customizing video size and enabling action annotations (v1.59+)

```typescript
import { defineConfig } from '@playwright/test';

export default defineConfig({
  use: {
    video: {
      mode: 'on',
      size: { width: 640, height: 480 },
      show: {
        actions: { position: 'top-left' },
        test: { position: 'top-right' },
      },
    },
  },
});
```

Per the index synthesis: trace is usually the first artifact; video should be enabled where visual timing matters. `video: 'retain-on-failure'` limits retention cost.

### 11. TestDino (Dhruv Rai) — Playwright Locators Guide

- Source: https://testdino.com/blog/playwright-locators
- Retrieved: 2026-08-29
- Exa status: complete

Updated May 27, 2026.

Playwright locators are the foundation of stable automation. Learn every locator type, when to use them, and how to debug failures without adding manual waits.

You've probably been there. You write a test, it passes locally, and then it breaks in CI because the button you were clicking hadn't finished rendering yet. Or worse, you're targeting a CSS selector like `div.main > ul > li:nth-child(3) > a` and someone on the front-end team shuffles the layout.

Locators are objects that find elements at interaction time, wait for them to be ready, and retry if something goes wrong. No more `await page.waitForSelector()` followed by a prayer.

## How Locators Differ from Selectors

What Are Locators? A Playwright locator is an object that:

- **Resolves lazily** — it doesn't search the DOM when you create it. It waits until you actually need to interact with an element.
- **Auto-waits** — before clicking, typing, or doing anything, Playwright checks that the element is attached, visible, stable, enabled, and able to receive events.
- **Retries automatically** — if an element isn't ready, Playwright keeps trying until the timeout (30 seconds by default).

This three-part behavior is what makes locators different from raw selectors.

You don't need `await page.waitForSelector('.submit-btn')` before clicking. Just use a locator and Playwright handles the timing:

```typescript
// Old way (selector + manual wait)
await page.waitForSelector('.submit-btn');
await page.click('.submit-btn');

// Locator way (auto-waits built in)
await page.getByRole('button', { name: 'Submit' }).click();
```

## What Changed in Playwright Locators (v1.58 to v1.60)

### Removals in v1.58

- **Removed Selectors:** The framework-specific `_react` and `_vue` selector engines, along with the Shadow DOM-piercing `:light` suffix, are gone. The article continues with migration guidance and the v1.59/v1.60 additions.

Per the index synthesis: prefer `getByRole`, `getByLabel`, `getByText` and a consciously chosen test ID; CSS/XPath only as a deliberate fallback. Do not pre-wait actions that already have actionability checks.

### 12. TestDino (Savan Vaghani) — GitHub Copilot with Playwright: Setup, MCP & Test Guide

- Source: https://testdino.com/blog/playwright-tests-with-copilot
- Retrieved: 2026-08-29
- Exa status: complete

Published March 26, 2026, updated June 30, 2026.

Have you asked AI to write Playwright tests and received code that works in a demo but breaks on a real app?

If you write Playwright tests regularly, you know the pain: finding stable locators, covering real user flows, fixing tests after UI changes, chasing flaky CI failures.

This guide shows how to write Playwright tests with GitHub Copilot and Playwright MCP that work in real projects. You will set up VS Code, load Playwright skills, generate your first test, view reports with TestDino, and fix unstable tests using the Healer agent.

TL;DR

- Connect Playwright MCP to VS Code so GitHub Copilot can interact with a real browser instead of guessing selectors.
- Use Playwright Skills (70+ markdown guides) to give the agent structured context.
- Write `.github/copilot-instructions.md` to enforce your team's conventions.
- Generate tests, run them through CI, and push results to TestDino for centralized reporting, flaky test tracking, and AI failure classification.
- When tests break, use TestDino MCP alongside Playwright's Healer agent to diagnose and fix failures with full historical context.

## Why use GitHub Copilot with Playwright for test generation?

GitHub Copilot runs natively inside VS Code. It reads your codebase, supports MCP servers as of VS Code 1.99, and lets you switch between models mid-session.

For Playwright testing, this matters because test generation is not just about writing code. It is about writing code that matches your project structure, uses the right locator strategies, and runs reliably in CI.

Most AI coding tools generate tutorial-quality Playwright tests. They use brittle CSS selectors, skip auth handling, and ignore your project's fixture patterns. GitHub Copilot solves this with three features:

- **MCP support in VS Code** lets Copilot connect to the Playwright MCP server. Instead of guessing the DOM, the AI drives a real browser, reads the accessibility tree, and generates locators from actual page state.
- **Copilot Chat supports loading Skills**, which are structured markdown guides that teach the AI your testing patterns.
- **`.github/copilot-instructions.md`** lets you define project-level instructions that every Copilot interaction follows automatically.

The combination of live browser context, skill-based knowledge, and enforced instructions is what separates GitHub Copilot from tools that just autocomplete code.

## Prerequisites

- Node.js 18 or newer installed
- Playwright installed in your project (`npm init playwright@latest` if starting fresh)
- VS Code 1.99 or newer with the GitHub Copilot extension installed and active
- Playwright browsers installed (`npx playwright install --with-deps`)
- A working Playwright project with at least one passing test, so the AI has a reference spec to learn from

The article continues with connecting Playwright MCP to GitHub Copilot in VS Code, loading Skills, generating tests, and the Healer agent workflow.

Per the index synthesis: live MCP helps the agent observe the real accessibility tree, Skills provide rules, and `.github/copilot-instructions.md` codifies repo conventions. MCP/agent is for exploration and debugging; large critical regressions should remain as normal, deterministic `.spec.ts` tests in CI.

### 13. TestDino — What is the Accessibility Tree?

- Source: https://testdino.com/blog/accessibility-tree
- Retrieved: 2026-08-29
- Exa status: metadata-only

The page is dynamic; the Exa fetch returned only the shell/loading stub ("Loading blog post"). The meta title is: "What is the Accessibility Tree? How Testing Frameworks Use It Differently" — explaining the difference between the DOM and accessibility tree and how Playwright uses accessibility-based locators compared to Cypress and Selenium. No body content was retrieved; no content was substituted.

### 14. Artem Bondar / Bondar Academy — Is Playwright MCP Worth It for Test Automation?

- Source: https://bondaracademy.com/blog/is-playwright-mcp-worth-it
- Retrieved: 2026-08-29
- Exa status: complete

April 6, 2026 · 9 min read.

Playwright MCP is everywhere right now. Every other blog post and YouTube video is telling you that MCP will revolutionize your test automation. But is Playwright MCP worth it when you actually sit down and try to use it? The author decided to find out himself. No beautifying, no cherry-picking the best results, no fancy prompt engineering. Just a raw, out-of-the-box experience.

The conclusion: not impressed.

## What is Playwright MCP server?

Playwright MCP Server is a bridge between your LLM of choice (Claude, ChatGPT, Gemini) and the Playwright framework. The LLM sends instructions to Playwright, and Playwright interacts with the browser on its behalf.

You describe what you want in plain English, and the AI uses Playwright to do it in a real browser. Click buttons, fill forms, navigate pages. The LLM reads the page structure through accessibility snapshots and decides what actions to take next.

## Setting up Playwright MCP in VS Code with Copilot

The setup is quick. Open VS Code, go to Extensions, and scroll down to the MCP server section. Click the MCP servers link, find Playwright MCP in the list, and hit Install.

Then create a `.vscode` folder in your project root and add a `settings.json` file:

```json
{
  "mcp": {
    "servers": {
      "playwright": {
        "command": "npx",
        "args": ["@playwright/mcp@latest"]
      }
    }
  }
}
```

To verify it's working, open Copilot Chat, click the Tools icon, and scroll down. You should see the Playwright MCP server listed with all its tools.

## The real test: automating a test case with MCP

Here's the use case. You have manual test cases written in plain language, maybe in Excel or a test management tool. You feed those test cases to the LLM, and through Playwright MCP, it navigates the app, executes the steps, and generates an automated Playwright test script for you.

The author used a simple Conduit application with a basic happy-path scenario:

1. Log in to the application
2. Create a new article with a title, description, and body
3. Verify the article was created
4. Delete the article

A very basic scenario. Started with a completely blank Playwright project, just a fresh framework with zero tests. Then gave Copilot a prompt with the test case steps.

### What happened next

Copilot kicked off the Playwright MCP server, and the browser opened. The AI started going through the steps. The experiment reveals the real-world gaps: MCP can accelerate a sketch, but the test needs manual fixes — wrong element type selection, ambiguous locators, brittle regex assertions, and incorrect navigation after data deletion. The conclusion (anti-hype): MCP may speed up drafting, but the test must be run, locators narrowed, oracle checked, and the result maintained like any other code.

### 15. Playwright — Best Practices (official docs)

- Source: https://playwright.dev/docs/best-practices
- Retrieved: 2026-08-29
- Exa status: complete

## Testing philosophy

### Test user-visible behavior

Automated tests should verify that the application code works for the end users, and avoid relying on implementation details such as things which users will not typically use, see, or even know about such as the name of a function, whether something is an array, or the CSS class of some element. The end user will see or interact with what is rendered on the page, so your test should typically only see/interact with the same rendered output.

### Make tests as isolated as possible

Each test should be completely isolated from another test and should run independently with its own local storage, session storage, data, cookies etc. Test isolation improves reproducibility, makes debugging easier and prevents cascading test failures.

In order to avoid repetition for a particular part of your test you can use before and after hooks. Within your test file add a before hook to run a part of your test before each test such as going to a particular URL or logging in to a part of your app. This keeps your tests isolated as no test relies on another. However it is also ok to have a little duplication when tests are simple enough, especially if it keeps your tests clearer and easier to read and maintain.

```javascript
import { test } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.goto('https://github.com/login');
  await page.getByLabel('Username or email address').fill('username');
  await page.getByLabel('Password').fill('password');
  await page.getByRole('button', { name: 'Sign in' }).click();
});

test('first', async ({ page }) => {
  // page is signed in.
});

test('second', async ({ page }) => {
  // page is signed in.
});
```

You can also reuse the signed-in state in the tests with a setup project. That way you can log in only once and then skip the log in step for all of the tests.

### Avoid testing third-party dependencies

Only test what you control. Don't try to test links to external sites or third-party servers that you do not control. Not only is it time consuming and can slow down your tests, but also you cannot control the content of the page you are linking to, or if there are cookie banners or overlay pages or anything else that might cause your test to fail.

### 16. Playwright — Fixtures (official docs)

- Source: https://playwright.dev/docs/test-fixtures
- Retrieved: 2026-08-29
- Exa status: complete

## Introduction

Playwright Test is based on the concept of test fixtures. Test fixtures are used to establish the environment for each test, giving the test everything it needs and nothing else. Test fixtures are isolated between tests. With fixtures, you can group tests based on their meaning, instead of their common setup.

### Built-in fixtures

You have already used test fixtures in your first test:

```javascript
import { test, expect } from '@playwright/test';

test('basic test', async ({ page }) => {
  await page.goto('https://playwright.dev/');
  await expect(page).toHaveTitle(/Playwright/);
});
```

The `{ page }` argument tells Playwright Test to set up the `page` fixture and provide it to your test function.

Pre-defined fixtures:

| Fixture | Type | Description |
| --- | --- | --- |
| page | Page | Isolated page for this test run. |
| context | BrowserContext | Isolated context for this test run. The `page` fixture belongs to this context as well. |
| browser | Browser | Browsers are shared across tests to optimize resources. |
| browserName | string | The name of the browser currently running the test. Either `chromium`, `firefox` or `webkit`. |
| request | APIRequestContext | Isolated APIRequestContext instance for this test run. |

### With fixtures

Fixtures have a number of advantages over before/after hooks:

- Fixtures encapsulate setup and teardown in the same place so it is easier to write. If you have an after hook that tears down what was created in a before hook, consider turning them into a fixture.
- Fixtures are reusable between test files — define them once and use them in all your tests.
- Fixtures are on-demand — Playwright Test sets up only the ones needed by a test and nothing else.
- Fixtures are composable — they can depend on each other to provide more complex behavior.

### 17. Playwright — Assertions (official docs)

- Source: https://playwright.dev/docs/test-assertions
- Retrieved: 2026-08-29
- Exa status: complete

## Introduction

Playwright includes test assertions in the form of the `expect` function. To make an assertion, call `expect(value)` and choose a matcher that reflects the expectation.

```javascript
expect(success).toBeTruthy();
```

Playwright also includes web-specific async matchers that will wait until the expected condition is met:

```javascript
await expect(page.getByTestId('status')).toHaveText('Submitted');
```

Playwright will re-test the element until the fetched element has the `"Submitted"` text, until the condition is met or until the timeout is reached. The timeout can be configured once via the `testConfig.expect` value in the test config. By default, the timeout for assertions is set to 5 seconds.

## Auto-retrying assertions

The following assertions will retry until the assertion passes, or the assertion timeout is reached. Retrying assertions are async, so you must `await` them.

| Assertion | Description |
| --- | --- |
| `await expect(locator).toBeAttached()` | Element is attached |
| `await expect(locator).toBeChecked()` | Checkbox is checked |
| `await expect(locator).toBeDisabled()` | Element is disabled |
| `await expect(locator).toBeEditable()` | Element is editable |
| `await expect(locator).toBeEmpty()` | Container is empty |
| `await expect(locator).toBeEnabled()` | Element is enabled |
| `await expect(locator).toBeFocused()` | Element is focused |
| `await expect(locator).toBeHidden()` | Element is not visible |
| `await expect(locator).toBeInViewport()` | Element intersects viewport |
| `await expect(locator).toBeVisible()` | Element is visible |
| `await expect(locator).toContainText()` | Element contains text |
| `await expect(locator).toContainClass()` | Element has specified CSS classes |
| `await expect(locator).toHaveAccessibleDescription()` | Element has a matching accessible description |
| `await expect(locator).toHaveAccessibleName()` | Element has a matching accessible name |
| `await expect(locator).toHaveAttribute()` | Element has a DOM attribute |
| `await expect(locator).toHaveClass()` | Element has specified CSS class property |
| `await expect(locator).toHaveCount()` | List has exact number of children |
| `await expect(locator).toHaveCSS()` | Element has CSS property |
| `await expect(locator).toHaveId()` | Element has an ID |
| `await expect(locator).toHaveJSProperty()` | Element has a JavaScript property |
| `await expect(locator).toHaveRole()` | Element has a specific ARIA role |
| `await expect(locator).toHaveScreenshot()` | Element has a screenshot |
| `await expect(locator).toHaveText()` | Element matches text |
| `await expect(locator).toHaveValue()` | Input has a value |
| `await expect(locator).toHaveValues()` | Select has options selected |
| `await expect(locator).toMatchAriaSnapshot()` | Element matches the Aria snapshot |
| `await expect(page).toMatchAriaSnapshot()` | Page matches the Aria snapshot |
| `await expect(page).toHaveScreenshot()` | Page has a screenshot |
| `await expect(page).toHaveTitle()` | Page has a title |
| `await expect(page).toHaveURL()` | Page has a URL |
| `await expect(response).toBeOK()` | Response has an OK status |

## Non-retrying assertions

These assertions allow testing any conditions, but do not auto-retry. Prefer auto-retrying assertions whenever possible. For more complex assertions that need to be retried, use `expect.poll` or `expect.toPass`.

### 18. Playwright — Configuration (official docs)

- Source: https://playwright.dev/docs/test-configuration
- Retrieved: 2026-08-29
- Exa status: complete

## Introduction

Playwright has many options to configure how your tests are run. You can specify these options in the configuration file. Note that test runner options are top-level; do not put them into the `use` section.

## Basic Configuration

```javascript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: 'tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'npm run start',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
  },
});
```

| Option | Description |
| --- | --- |
| `testConfig.forbidOnly` | Whether to exit with an error if any tests are marked as `test.only`. Useful on CI. |
| `testConfig.fullyParallel` | Have all tests in all files run in parallel. |
| `testConfig.projects` | Run tests in multiple configurations or on multiple browsers |
| `testConfig.reporter` | Reporter to use. |
| `testConfig.retries` | The maximum number of retry attempts per test. |
| `testConfig.testDir` | Directory with the test files. |
| `testConfig.use` | Options with `use{}` |
| `testConfig.webServer` | To launch a server during the tests, use the `webServer` option |
| `testConfig.workers` | The maximum number of concurrent worker processes. Can also be set as percentage of logical CPU cores, e.g. `'50%'`. |

## Filtering Tests

Filter tests by glob patterns or regular expressions with `testIgnore` and `testMatch`.

## Advanced Configuration

```javascript
import { defineConfig } from '@playwright/test';

export default defineConfig({
  outputDir: 'test-results',
  globalSetup: require.resolve('./global-setup'),
  globalTeardown: require.resolve('./global-teardown'),
  timeout: 30000,
});
```

### 19. Playwright — Test API: `test` (incl. `test.step`)

- Source: https://playwright.dev/docs/api/class-test#test-step
- Retrieved: 2026-08-29
- Exa status: complete

Playwright Test provides a `test` function to declare tests and `expect` function to write assertions.

```javascript
import { test, expect } from '@playwright/test';

test('basic test', async ({ page }) => {
  await page.goto('https://playwright.dev/');
  const name = await page.innerText('.navbar__title');
  expect(name).toBe('Playwright');
});
```

## Methods

### test

Added in: v1.10. Declares a test.

Tags: you can tag tests by providing additional test details, or include tags in the test title. Each tag must start with the `@` symbol.

```javascript
test('basic test', {
  tag: '@smoke',
}, async ({ page }) => {
  await page.goto('https://playwright.dev/');
});
```

Test tags are displayed in the test report and are available to a custom reporter via `TestCase.tags`.

Annotations: you can annotate tests by providing additional test details:

```javascript
test('basic test', {
  annotation: {
    type: 'issue',
    description: 'https://github.com/microsoft/playwright/issues/23180',
  },
}, async ({ page }) => {
  await page.goto('https://playwright.dev/');
});
```

### test.abort

Added in: v1.60. Aborts the currently running test by throwing an error. The test is immediately marked as failed and execution stops.

```javascript
test('does not publish to shared page', async ({ page }) => {
  await page.route('**/publish', route => {
    test.abort('Tests must not publish to the shared page. Use the `clone` option.');
    return route.abort();
  });
});
```

### test.afterAll

Added in: v1.10. Declares an `afterAll` hook executed once per worker after all tests.

The page also documents `test.beforeAll`, `test.beforeEach`, `test.afterEach`, `test.describe`, `test.step` (reportable steps for readable reports), `test.setTimeout`, `test.skip`, and the fixtures API.

### 20. Playwright — Authentication (official docs)

- Source: https://playwright.dev/docs/auth
- Retrieved: 2026-08-29
- Exa status: complete

## Introduction

Playwright executes tests in isolated environments called browser contexts. This isolation model improves reproducibility and prevents cascading test failures. Tests can load existing authenticated state. This eliminates the need to authenticate in every test and speeds up test execution.

## Core concepts

Regardless of the authentication strategy you choose, you are likely to store authenticated browser state on the file system.

We recommend creating a `playwright/.auth` directory and adding it to your `.gitignore`. The authentication routine will produce authenticated browser state and save it to a file in this directory.

> **Danger:** the browser state file may contain sensitive cookies and headers that could be used to impersonate you or your test account. Checking them into private or public repositories is strongly discouraged.

```bash
mkdir -p playwright/.auth
echo $'\nplaywright/.auth' >> .gitignore
```

## Basic: shared account in all tests

This is the recommended approach for tests without server-side state. Authenticate once in the setup project, save the authentication state, and then reuse it to bootstrap each test already authenticated.

Create `tests/auth.setup.ts`:

```javascript
import { test as setup, expect } from '@playwright/test';
import path from 'path';

const authFile = path.join(__dirname, '../playwright/.auth/user.json');

setup('authenticate', async ({ page }) => {
  await page.goto('https://github.com/login');
  await page.getByLabel('Username or email address').fill('username');
  await page.getByLabel('Password').fill('password');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.waitForURL('https://github.com/');
  await expect(page.getByRole('button', { name: 'View profile and more' })).toBeVisible();
  await page.context().storageState({ path: authFile });
});
```

Create a `setup` project in the config and declare it as a dependency:

```javascript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  projects: [
    { name: 'setup', testMatch: /.*\.setup\.ts/ },
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        storageState: 'playwright/.auth/user.json',
      },
      dependencies: ['setup'],
    },
    {
      name: 'firefox',
      use: {
        ...devices['Desktop Firefox'],
        storageState: 'playwright/.auth/user.json',
      },
      dependencies: ['setup'],
    },
  ],
});
```

Tests start already authenticated because `storageState` is specified in the config.

