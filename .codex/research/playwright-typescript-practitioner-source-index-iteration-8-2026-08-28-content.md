---
title: "Playwright + TypeScript practitioner source index — article content"
source: ".codex/research/playwright-typescript-practitioner-source-index-iteration-8-2026-08-28.md"
retrieved: "2026-08-29"
---

# Playwright + TypeScript practitioner source index

## Kolejność i pokrycie

The source index contains 17 HTTP/HTTPS Markdown links in the order of appearance; all are unique canonical URLs, so no duplicates were merged. Local Markdown links (the iteration 1–7 index files and the Michal Drajna catalogue) were not fetched. One plain-text mention of the `pw-1.60` URL inside the "Uncertainty" section is not a Markdown link and was not counted.

| # | Autor/serwis | Tytuł | URL | Status |
|---:|---|---|---|---|
| 1 | Anton Gulin | AI Test Automation Architecture: The 3-Layer System | https://www.anton.qa/blog/posts/ai-test-automation-architecture-3-layer-system | complete |
| 2 | ScrollTest / Pramod Dutta | Day 13: Debugging — Trace Viewer, UI Mode, and Inspector | https://scrolltest.com/21-day-playwright-day-13-debugging-trace-viewer-ui-mode/ | complete |
| 3 | Currents.dev | Playwright Custom Reporters: Build Your Own | https://currents.dev/posts/playwright-custom-reporters-build-your-own | complete |
| 4 | Currents.dev (Andrew Goldis) | What does "skipped" mean — test statuses across JavaScript runners | https://currents.dev/posts/test-status-translation-guide | complete |
| 5 | Currents | Playwright 1.60 (release-note link) | https://currents.dev/posts/pw-1.60 | unavailable |
| 6 | TestDino (Pratik Patel) | Playwright Skill: Train Your AI Agent to Write Better Tests | https://testdino.com/blog/playwright-skill | complete |
| 7 | TestDino (Pratik Patel) | Playwright CLI and MCP: Key Differences and Integration with AI Agents | https://testdino.com/blog/playwright-cli-vs-mcp | complete |
| 8 | TestDino (Pratik Patel) | Playwright MCP Explained: Setup, Config & Real-World Examples | https://testdino.com/blog/playwright-mcp | complete |
| 9 | TestDino (Pratik Patel) | How to install Playwright MCP on Claude Code | https://testdino.com/blog/playwright-mcp-installation/ | complete |
| 10 | TestDino (Jashn Jain) | Learn Playwright in 2026: The Complete Roadmap | https://testdino.com/blog/learn-playwright | complete |
| 11 | Butch Mayhew | 600 subscribers — AI in QA Newsletter | https://www.linkedin.com/posts/butchmayhew_softwaretesting-qa-aiinqa-activity-7490440181498560512-uEgt | complete |
| 12 | Playwright | Best Practices (official docs) | https://playwright.dev/docs/best-practices | complete |
| 13 | Playwright | Fixtures (official docs) | https://playwright.dev/docs/test-fixtures | complete |
| 14 | Playwright | Assertions (official docs) | https://playwright.dev/docs/test-assertions | complete |
| 15 | Playwright | Configuration (official docs) | https://playwright.dev/docs/test-configuration | complete |
| 16 | Playwright | Test API: `test` (incl. `test.step`) | https://playwright.dev/docs/api/class-test#test-step | complete |
| 17 | Playwright | Authentication (official docs) | https://playwright.dev/docs/auth | complete |

## Artykuły

### 1. Anton Gulin — AI Test Automation Architecture: The 3-Layer System

- Source: https://www.anton.qa/blog/posts/ai-test-automation-architecture-3-layer-system
- Retrieved: 2026-08-29
- Exa status: complete

Published: May 13, 2026 · 4 min read.

AI test automation architecture is the system that tells AI what to test. It also defines how to run tests and prove the result. I split it into three layers: orchestration, execution, and evidence. Without all three, AI testing becomes prompt output with no production gate.

## Why tool lists fail

Most AI testing content starts with tools.

That is backwards.

AI means software that predicts. Predictions can help QA teams move faster. But predictions do not prove quality.

A tool can generate a test. It cannot decide release risk alone. It cannot prove the browser state was clean. It cannot explain why a failure matters.

That work belongs to architecture.

## The 3-layer model

| Layer | Plain meaning | Main question |
| --- | --- | --- |
| Orchestration | test control plan | What risk should this cover? |
| Execution | actual test run | Did it run in the real pipeline? |
| Evidence | proof from runs | Can a human review it? |

If one layer is missing, the system gets weak. If evidence is missing, the team gets false confidence.

## Layer 1: Orchestration

Orchestration means test control plan.

This layer defines the work before AI writes anything. It answers five questions:

1. What user flow matters?
2. What risk does this test cover?
3. What data must exist first?
4. What browser state is allowed?
5. What failure should block release?

AI can help draft the first version. But a human still owns the risk call. That is the difference between generation and architecture.

## Layer 2: Execution

Execution means actual test run.

This layer proves the test can survive the real path. That path is usually CI (automated build server). A local demo is useful. It is not enough.

Run the test where code ships. Check browser state, cleanup, retries, test data, and worker isolation.

This is where Playwright and MCP matter. Playwright is a browser test tool. MCP is a tool connection standard. Together, they let AI agents use a live browser. But the run still needs stable launch control. That is why `playwright-mcp v0.0.75` matters: it serialized shared browser launch in isolated mode, so parallel runs get ordered startup. Small release note. Real architecture impact.

## Layer 3: Evidence

Evidence means proof from runs. This is the layer most teams skip.

Every AI-created test should leave receipts. Useful receipts include:

- trace
- screenshot
- log
- video when timing matters
- saved browser state when auth matters

The point is simple: a reviewer should inspect the run without rerunning it. If that is impossible, the test is not ready.

AI can write code quickly. Review still needs proof.

## A practical gate

Here is the gate to use before AI-generated tests ship.

| Gate | Pass condition |
| --- | --- |
| Scope | The test maps to one named risk |
| Data | Test data setup is explicit |
| State | Browser state is controlled |
| Run | The test passes in CI |
| Evidence | Trace or equivalent proof exists |
| Review | A human can explain the failure mode |

This is not heavy process. It is a small guardrail. It stops weak tests from becoming permanent debt.

## What this changes for QA teams

The goal is not to slow AI down. The goal is to make AI work reviewable.

When the architecture is clear, AI becomes useful in three places: it drafts coverage ideas, it writes first-pass test code, and it explains failures from evidence.

### 2. ScrollTest / Pramod Dutta — Day 13: Debugging — Trace Viewer, UI Mode, and Inspector

- Source: https://scrolltest.com/21-day-playwright-day-13-debugging-trace-viewer-ui-mode/
- Retrieved: 2026-08-29
- Exa status: complete

By Pramod Dutta, August 22, 2026. Day 13 of the 21-Day Playwright with TypeScript Challenge.

Stop adding console.log. Playwright has 3 built-in debugging tools that show exactly what happened, step by step.

## Trace Viewer — Post-Mortem Debugging

```typescript
// Enable in config
use: { trace: 'retain-on-failure' }

// Open trace after failure
// npx playwright show-trace test-results/my-test/trace.zip
```

Shows: action timeline, DOM snapshots before/after each step, network requests with bodies, console logs, source code mapping.

## UI Mode — Interactive Testing

```bash
npx playwright test --ui
```

Watch tests execute in real-time. Click any step to see DOM state. Re-run individual tests. Filter by file. Time-travel through test execution.

## Inspector — Step-by-Step

```bash
npx playwright test --debug
```

Pauses at each action. Step through one action at a time. Inspect element selectors live. Evaluate locators in console.

## 60-Second Debugging Workflow

1. Open trace file (5s)
2. Jump to failing action in timeline (10s)
3. Compare DOM before/after (15s)
4. Check network tab — did API return expected data? (15s)
5. Check console for JS errors (10s)
6. Root cause identified (5s)

### 3. Currents.dev — Playwright Custom Reporters: Build Your Own

- Source: https://currents.dev/posts/playwright-custom-reporters-build-your-own
- Retrieved: 2026-08-29
- Exa status: complete

Currents Team, June 30, 2026.

A Playwright custom reporter takes 20 minutes to prototype and months to maintain. Here's what it actually takes to build one that holds up in production CI.

Building a Playwright custom reporter may look simple at first. The Reporter interface has a clean surface area, and the first prototype that logs test names to stdout takes about 20 minutes to write. A class, a few methods, an `export default`, and you're done. The reporter runs fine on your machine and passes the first CI run. That simplicity breaks down as requirements grow.

Suppose a team builds a working Slack-on-failure reporter one afternoon. A week later, someone asks for failure history, as in, "Has this test failed before?" Then someone wants flakiness trends. The team lead wants a dashboard the whole org can see, and suddenly there's a GitHub Actions workflow step that fires a Lambda that writes to DynamoDB. Six months after that afternoon, a senior engineer is debugging why DynamoDB writes are silently failing on retries, why Slack alerts stopped firing the week of a Playwright minor version bump, and why the dashboard shows 47 tests running when the suite has 312. None of that was on the original whiteboard. What started as a 200-line file is now a maintenance liability with no owner of record.

This article is about avoiding that outcome, or at least making it a deliberate choice. It walks through the full Reporter interface and what each hook actually gives you, where reporters routinely break under parallelism and sharding, and async handling so the reporter doesn't quietly drop data or hang CI — closing with a framework for deciding whether to build, extend, or reach for a platform.

## The Reporter Interface: Complete Map

Before writing any reporter code, you need to understand the full contract. The Playwright Reporter interface has 10 lifecycle methods and one utility method. Each one has specific data availability and timing characteristics that determine whether your reporter produces correct output.

If you've worked with Jest's `TestEvents` or Mocha's runner emitter, Playwright's design will feel different. The reporter is a typed class with explicit lifecycle hooks rather than an event emitter you subscribe to with string event names.

It produces stricter typing and clearer ordering guarantees, but it also means the constraints (synchronous-by-default for most hooks, strict main-process isolation, silent error swallowing) are baked into the design rather than being accidents.

### Lifecycle Overview

The reporter lifecycle tracks test execution:

`onBegin(config, suite)` runs exactly once at the start of the run, after all test files have been discovered and resolved. `config` is the fully resolved `FullConfig` object, meaning merges from `playwright.config.ts`, CLI overrides, and environment variables have all been applied. `suite` is the root `Suite` containing the full tree of child suites and `TestCase` objects. The suite tree in `onBegin` reflects the CLI filters already applied, including `--grep`, `--project`, and file path arguments. If you're building an "expected test set" for gap detection, you're working with the filtered set, not the full suite.

`onTestBegin(test, result)` is called when a test starts. The `result` object exists at this point but is almost empty.

### 4. Currents.dev (Andrew Goldis) — What does "skipped" mean: test statuses across JavaScript runners

- Source: https://currents.dev/posts/test-status-translation-guide
- Retrieved: 2026-08-29
- Exa status: complete

Andrew Goldis, July 31, 2026.

JavaScript test runners share status words and disagree about what the words mean. Playwright and Cypress use "skipped" for opposite cases. Jest carries 7 statuses but not "flaky". This guide translates each runner's vocabulary and shows how runners derive flakiness from attempts.

Your CI report says 340 passed and 27 skipped. Skipped through a deliberate `it.skip()`, or by the runner because a broken hook took a block of tests down with it? The report will not tell you. The answer depends on which runner wrote it.

Currents builds test reporting for Cypress and Playwright and normalizes results from Jest, Vitest and mocha along the way. Teams misread statuses constantly. The statuses themselves are not complicated. The problem is that different runners assigned the same five or six English words to different concepts, and nobody gets to rename them now.

TL;DR

- A test attempt is one execution of a test; the test's outcome is derived from all attempts together. Most confusion comes from mixing these two levels.
- Cypress reports `it.skip()` as `pending`. Cypress reports a test that a failed `beforeEach` hook prevented from running as `skipped`. The alarming word is `skipped`.
- Playwright reverses the convention: `test.skip()` produces `skipped`, and a halted run produces `interrupted`.
- No runner observes "flaky" on a single attempt. Runners compute flakiness across attempts. Playwright and Vitest give it a name; Cypress, mocha and Cucumber keep enough data for you to derive it; Jest and Bun hide it in their default output.
- `node:test` has no status strings at all: just a `passed` boolean plus independent `skip`/`todo` flags, mirroring TAP.
- Cucumber ties Jest for the largest vocabulary (seven values), including `UNDEFINED` and `AMBIGUOUS`, statuses that describe the glue code rather than the test.

## Playwright

Playwright has the richest status model of the nine runners, and it is the only runner that separates "what happened" from "was that expected".

Attempt statuses (`TestResult.status`):

- `passed`: the test body and its `beforeEach`/`afterEach` hooks completed without error.
- `failed`: an assertion failed, or the test threw an exception.
- `timedOut`: the attempt exceeded the configured timeout, and Playwright terminated it. `timedOut` is separate from `failed` for a reason: a timeout usually signals a hang or a missing element rather than a wrong assertion, and you can filter for it.
- `skipped`: Playwright did not execute the test. Usually you asked for that: `test.skip()`, `test.fixme()`, or a conditional skip. The remaining tests in a worker also become `skipped` when the worker dies before reaching them.
- `interrupted`: the attempt started (or was queued), and the run halted before the attempt finished. `Ctrl+C` produces `interrupted`, and so does hitting `maxFailures`.

A worker crash is the surprising case. Playwright marks the test that was running as `failed`, with a "worker process exited unexpectedly" error, not as `interrupted`. Only the tests that never started come back as `skipped`.

Outcome (`TestCase.outcome()`) is the derived level:

- `expected`: the result matches `expectedStatus`. A passing test is expected, and so is a test marked `test.fail()` that fails.
- `unexpected`: the result does not match `expectedStatus`.
- `skipped`: you excluded the test on purpose.
- `flaky`: some attempts matched `expectedStatus` and some did not.

`expectedStatus` is the piece most people miss. Playwright does not ask "did the test pass". Playwright asks "did the test do what you declared it would do". That question makes `test.fail()` work cleanly: a known-broken test that fails stays green, and it turns red the day someone actually fixes the bug.

### 5. Currents — Playwright 1.60 (release-note link)

- Source: https://currents.dev/posts/pw-1.60
- Retrieved: 2026-08-29
- Exa status: unavailable

- Exa error: `CRAWL_NOT_FOUND` (initial fetch and retry after a 30 s backoff marker both failed).

The source index itself flags this link as found by Exa Search but never verified by Fetch, and explicitly does not treat it as a validated source. Runner versions should be checked in the official Playwright release notes instead. No content was substituted.

### 6. TestDino (Pratik Patel) — Playwright Skill: Train Your AI Agent to Write Better Tests

- Source: https://testdino.com/blog/playwright-skill
- Retrieved: 2026-08-29
- Exa status: complete

Published February 13, 2026, updated February 27, 2026.

AI agents write decent Playwright tests out of the box, but they fall apart on real-world sites. Wrong selectors, broken auth, flaky CI runs.

The problem? Agents don't have context about your app. That's where skills come in — structured guides that tell the agent what patterns to follow instead of guessing.

Install 70+ production-tested skills with a single command: `npx skills add testdino-hq/playwright-skill`.

## What is a Playwright Skill?

A Playwright Skill is a curated collection of markdown guides that teach AI coding agents (and humans) how to write production-grade Playwright tests.

Here is the problem it solves. Playwright's official documentation is excellent. But it is spread across dozens of pages. When you ask an AI agent like Claude Code or GitHub Copilot to write tests, it pulls from its general training data. The output works for tutorials. It falls apart on real sites.

A Skill changes this. It gives the AI agent a structured, battle-tested reference to draw from. Instead of guessing which locator strategy to use or how to handle auth flows, the agent reads the relevant guide and produces code that follows patterns proven in production.

The Playwright Skill is not just for AI agents. Every guide is written in plain markdown. Human developers can read them, bookmark them, and use them as a cheat sheet.

### Why structured skill guides beat scattered documentation

Documentation tells you what an API does. A skill guide tells you when to use it, when to avoid it, and what pattern to follow in a real project.

Every guide follows the same structure:

- When to use: exact scenarios where the pattern applies
- Avoid when: anti-patterns and wrong use cases
- Quick reference: copy-paste code ready to go
- Full patterns: real-world implementations with context
- TypeScript and JavaScript examples for both camps

This consistency matters. Whether you are a junior tester opening Playwright for the first time or a senior engineer migrating a Cypress suite, you get the same structure and depth across all 70+ guides.

### The Playwright Skill repository

The Playwright Skill is an open-source repository maintained by TestDino, MIT licensed. The repo contains 70+ guides organized into five skill packs:

```text
playwright-skill/
├── core/            # 46 guides. The foundation.
├── playwright-cli/  # 11 guides. CLI browser automation.
├── pom/             # 2 guides. Page Object Model patterns.
├── ci/              # 9 guides. CI/CD pipelines.
├── migration/       # 2 guides. Moving from Cypress or Selenium.
├── LICENSE          # MIT
├── README.md
└── SKILL.md         # Metadata for AI agent loading
```

Prerequisite: an AI coding agent like Claude Code, Cursor, Windsurf, or GitHub Copilot installed before getting started.

### 7. TestDino (Pratik Patel) — Playwright CLI and MCP: Key Differences and Integration with AI Agents

- Source: https://testdino.com/blog/playwright-cli-vs-mcp
- Retrieved: 2026-08-29
- Exa status: complete

Published February 25, 2026, updated March 27, 2026.

Playwright CLI keeps AI-driven automation fast, cheap, and reliable by saving browser state to disk instead of flooding the model with context.

The author spent months using MCP with Playwright. At first it felt like the right choice — it exposed browser state, DOM structure, accessibility tree, everything. But once used in real automation workflows, the cracks showed up: the agent got too much context, token usage went up, responses slowed down, debugging became harder, and sometimes the agent over-analyzed instead of just running the test.

Switching to CLI wasn't about features. It was about control, speed, and reliability.

## What is Playwright CLI

The Playwright CLI (@playwright/cli) is a command-line tool published by Microsoft, built specifically for AI coding agents. It launched in early 2026 as a companion to the existing Playwright MCP server, but the approach is fundamentally different.

Instead of streaming browser state back into the AI model's context window, the CLI saves everything to disk. Snapshots go to YAML files. Element references stay local. The agent issues short shell commands like open, click, type, fill, screenshot, close, and snapshot, and gets back minimal, structured responses.

A typical CLI interaction:

```bash
# Open the demo store in a visible (headed) browser
playwright-cli open https://storedemo.example.com/ --headed

# Capture the current page state and generate element reference IDs
playwright-cli snapshot

# Click on Product 1 using its reference ID from the snapshot
playwright-cli click e255

# Take another snapshot because the page state has changed (cart updated)
playwright-cli snapshot

# Click the Checkout tab using the latest reference ID
playwright-cli click e2609

# Final snapshot to confirm navigation to checkout and capture new elements
playwright-cli snapshot

# Close the browser
playwright-cli close
```

The key idea: the agent decides what it needs to read from disk, rather than having the full browser state pushed into its context on every single action. This keeps token usage low and the agent focused.

Note: playwright-cli is different from `npx playwright test`. The CLI lets an AI agent drive a browser interactively, explore pages, automate user flows, and convert those flows into proper Playwright tests. Think of it as the exploration and generation layer that sits before your test suite, while `npx playwright test` remains the deterministic CI runner.

## What is Playwright MCP

Playwright MCP (Model Context Protocol) is an MCP server that streams rich accessibility/DOM context to the model — the contrasting approach described in the comparison.

### 8. TestDino (Pratik Patel) — Playwright MCP Explained: Setup, Config & Real-World Examples

- Source: https://testdino.com/blog/playwright-mcp
- Retrieved: 2026-08-29
- Exa status: complete

Updated August 19, 2026. The page is dynamic; the first Exa fetch returned only the loading shell, and a retry with a rendering scraper recovered the full article.

Playwright MCP lets AI control a real browser. Not a simulated one.

It reads the page through the accessibility tree, the same semantic structure screen readers use, and returns structured data instead of screenshots. That single design decision is what separates Playwright MCP from every vision-based automation tool on the market right now.

If you've been writing Playwright tests manually — translating test cases into selectors, waits, and assertions by hand — Playwright MCP changes the workflow. You describe what to test in plain English. The AI opens a real browser, interacts with your app, and generates working Playwright code from actual page state.

**New in 2026:** Microsoft now recommends Playwright CLI over MCP for coding agents. CLI uses 4x fewer tokens per session. Playwright 1.59 added Screencast, browser.bind() interoperability, and CLI debugging for agents.

## What is Playwright MCP?

The Playwright MCP server is a Model Context Protocol (MCP) server built by Microsoft that gives AI models direct browser automation capabilities using the Playwright framework. Instead of relying on screenshots or pixel-based interactions, it provides LLMs with a structured accessibility snapshot of web pages, allowing AI to interact with elements deterministically using unique references.

In simpler terms: Playwright MCP is a bridge between your AI assistant (Claude, Copilot, Cursor, Grok, or any MCP-compatible client) and a real browser. The AI sends commands like "click the Sign In button" and Playwright MCP executes them in an actual browser session.

The server uses Playwright's accessibility tree instead of screenshots. This means:

- **No vision models needed.** The AI works with structured text data, not images.
- **Deterministic element targeting.** Each element gets a unique `ref` (e.g., `ref="e5"`), eliminating the ambiguity of coordinate-based clicking.
- **Faster and cheaper.** Text-based accessibility snapshots consume fewer tokens than base64-encoded screenshots.

Playwright MCP was originally released by Microsoft in late 2025 and has grown to over 36,000 GitHub stars. It supports Chromium, Firefox, and WebKit.

### How does Playwright MCP differ from regular Playwright?

Regular Playwright is a testing framework where you write scripts in TypeScript, Python, Java, or C# to automate browsers. Playwright MCP wraps Playwright's capabilities behind the Model Context Protocol, so an AI model can control the browser conversationally.

The key difference: **regular Playwright is code-first. Playwright MCP is AI-first.**

## Why use Playwright MCP for automated testing?

- **Faster test creation.** Describe the user flow in plain English; the AI generates working Playwright test code by interacting with the real application.
- **Better selectors from the start.** Because MCP reads the accessibility tree, the AI naturally uses semantic selectors like `getByRole` and `getByTestId` instead of brittle CSS or XPath.
- **Live debugging with context.** When a test fails, ask the AI to navigate to the failing page, inspect the current state, and suggest fixes within the same browser session.
- **Exploratory testing at scale.** Direct the AI to explore user flows, reproduce bugs, and validate edge cases without writing code.
- **Lower barrier to entry.** Team members can create and validate tests using natural language.

Here's what MCP doesn't replace: stable, human-reviewed regression suites that run in CI. MCP is best for creation and exploration. CI pipelines should run standard Playwright test scripts.

## How Playwright MCP works

**Step 1: Snapshot.** The AI requests a `browser_snapshot`; the server captures the page's accessibility tree and returns it as structured text with element references.

**Step 2: Decision.** The AI reads the snapshot, identifies the target element by its `ref` attribute, and decides what action to take.

**Step 3: Action.** The AI calls an MCP tool like `browser_click(ref="e12")`. MCP executes the action in the real browser.

**Step 4: Updated snapshot.** MCP returns a new accessibility snapshot reflecting the updated page state.

### Snapshot mode vs. Vision mode

**Snapshot mode (default).** Uses the accessibility tree. Fast, deterministic, and token-efficient. Recommended for most use cases.

**Vision mode.** Uses screenshots instead of accessibility snapshots. Enables coordinate-based interactions. Useful for canvas elements, games, or pages with poor accessibility markup. Enable with `--caps=vision`.

## Playwright MCP tools reference

Core tools (always enabled) include `browser_snapshot`, `browser_click`, `browser_navigate`, `browser_navigate_back`, `browser_type`, `browser_press_key`, `browser_fill_form`, `browser_hover`, `browser_drag`, `browser_select_option`, `browser_take_screenshot`, `browser_evaluate`, `browser_handle_dialog`, `browser_file_upload`, `browser_wait_for`, `browser_console_messages`, `browser_network_requests`, `browser_tabs`, `browser_close`, `browser_resize`, `browser_run_code`, `browser_find`, and `browser_drop`.

Extended capability groups (via `--caps` or capabilities config):

| Capability | Tools added | Use case |
| --- | --- | --- |
| network | `browser_route`, `browser_route_list`, `browser_unroute`, `browser_network_state_set` | Mock API responses, simulate offline mode |
| storage | Cookie, localStorage, sessionStorage management | Test auth flows, session persistence |
| vision | Coordinate-based click, drag, screenshot tools | Canvas elements, visually complex UIs |
| pdf | PDF generation and manipulation | Testing PDF exports |
| devtools | Developer tools inspection | Advanced debugging |

## Playwright MCP vs Playwright CLI

| Feature | Playwright MCP | Playwright CLI |
| --- | --- | --- |
| **Interface** | Model Context Protocol (MCP) | Shell commands + SKILLS |
| **Best for** | Chat-based agents (Claude Desktop, Cursor, Windsurf) | Coding agents (Claude Code, Copilot, Codex) |
| **Token efficiency** | Higher token cost (full accessibility snapshots in context) | **4x fewer tokens** per session |
| **Browser state** | Lives in AI's context window | Lives on disk/filesystem |
| **Interaction style** | Conversational, iterative, exploratory | Task-oriented, scripted, high-throughput |
| **Setup** | JSON config in MCP client | CLI commands + SKILLS files |
| **Ideal use case** | Exploratory testing, bug reproduction, demos | Test generation, CI automation, batch operations |

When to use MCP: IDE with MCP support, interactive exploration, exploratory testing, bug reproduction, live debugging, self-healing or long-running autonomous workflows.

When to use CLI: terminal-based coding agents, token budget matters, batch test generation, balancing browser automation with large codebase reasoning.

## What's new in 2026

- **Playwright 1.59: Screencast API** — agents record video of their work with chapter markers and action annotations; the "receipt" that makes AI-generated tests trustworthy.
- **Playwright 1.59: browser.bind()** — a single browser instance shared across the MCP server, the CLI, and custom clients simultaneously; enables mixed human/agent workflows (a QA engineer logs in manually, handling MFA/CAPTCHA, then hands off to the agent in the same authenticated session).
- **Playwright CLI + SKILLS** — markdown instruction files that teach agents how to use CLI tools effectively.
- **Test Agents: Planner, Generator, Healer** — three specialized agents invocable from Claude Code:

```bash
npx playwright init-agents --loop=claude
```

- **Planner** analyzes the application and creates a structured test plan. **Generator** takes a plan and generates working test code by interacting with the real application. **Healer** monitors failing tests and attempts to fix broken selectors.

## Benefits and limitations

Benefits: no vision models required; cross-browser support; device emulation (`--device="iPhone 15"`); network interception; persistent sessions; works with any MCP client; open source (Apache-2.0).

Limitations: token-intensive for complex pages; not a replacement for CI suites (generated tests need human review); accessibility tree gaps (canvas, SVGs, custom widgets); single browser per profile; AI hallucinations.

## Playwright MCP configuration options

Standard configuration:

```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": ["@playwright/mcp@latest"]
    }
  }
}
```

Commonly used flags:

| Flag | Description | Example |
| --- | --- | --- |
| --headless | Run browser without visible UI | `--headless` |
| --browser | Choose browser engine | `--browser=firefox` |
| --device | Emulate a mobile device | `--device="iPhone 15"` |
| --caps | Enable/disable tool capabilities | `--caps=core,network,vision` |
| --isolated | Use isolated browser context (no persistent state) | `--isolated` |
| --user-data-dir | Set custom profile directory | `--user-data-dir=/path/to/profile` |
| --storage-state | Load cookies/localStorage from file | `--storage-state=auth.json` |
| --port | Enable HTTP transport on specified port | `--port=8931` |
| --config | Load configuration from JSON file | `--config=mcp-config.json` |
| --codegen | Generate code in specified language | `--codegen=typescript` |
| --viewport-size | Set browser viewport | `--viewport-size=1280x720` |
| --timeout-navigation | Set navigation timeout (ms) | `--timeout-navigation=60000` |
| --timeout-action | Set action timeout (ms) | `--timeout-action=5000` |
| --no-sandbox | Disable browser sandboxing (for Docker/CI) | `--no-sandbox` |
| --init-page | Run a TypeScript file on page initialization | `--init-page=setup.ts` |
| --init-script | Inject JavaScript on every page load | `--init-script=overrides.js` |
| --save-session | Save session data to output directory | `--save-session` |

Configuration via JSON file:

```bash
npx @playwright/mcp@latest --config path/to/config.json
```

```json
{
  "browser": {
    "browserName": "chromium",
    "headless": false,
    "launchOptions": {
      "channel": "chrome"
    },
    "contextOptions": {
      "viewport": { "width": 1280, "height": 720 }
    }
  },
  "capabilities": ["core", "network"],
  "network": {
    "allowedOrigins": ["https://myapp.com:*"],
    "blockedOrigins": ["https://analytics.example.com:*"]
  },
  "outputDir": "./mcp-output",
  "codegen": "typescript"
}
```

### User profile management

Playwright MCP runs in three profile modes:

**Persistent profile (default).** Login sessions, cookies, and browser data are saved between sessions. A persistent profile can only be used by one browser instance at a time — for concurrent MCP sessions, start additional clients with `--isolated` or a distinct `--user-data-dir`.

**Isolated mode.** Each session starts fresh; all session data is lost when the browser closes:

```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": ["@playwright/mcp@latest", "--isolated", "--storage-state=auth.json"]
    }
  }
}
```

**Browser Extension mode.** The Chrome Extension connects to existing browser tabs, leveraging already logged-in sessions — useful behind corporate SSO or complex auth flows.

Docker setup for isolated, reproducible environments:

```bash
docker run -d -i --rm --init --pull=always \
  --entrypoint node \
  --name playwright \
  -p 8931:8931 \
  mcr.microsoft.com/playwright/mcp \
  /app/cli.js --headless --browser chromium --no-sandbox --port 8931 --host 0.0.0.0
```

Docker supports headless Chromium only; for Firefox/WebKit use the local npx installation.

The guide's key practice (per the index synthesis): limit origins/permissions, version the MCP configuration, and use MCP for exploration while coding critical paths as `.spec.ts` files with deterministic assertions.

### 9. TestDino (Pratik Patel) — How to install Playwright MCP on Claude Code

- Source: https://testdino.com/blog/playwright-mcp-installation/
- Retrieved: 2026-08-29
- Exa status: complete

Published December 11, 2025. Historical installation guide; per the index, the package name and options must be verified against current Microsoft documentation.

Playwright MCP connects Claude directly to your local Playwright setup, enabling fast AI-generated tests and hands-free browser automation inside your editor.

Manual testing breaks down as releases accelerate. Repeating flows by hand is slow, error-prone, and unsustainable. But even automation has limits: someone still needs to write, debug, and maintain every test. This is where Playwright MCP changes the game by letting AI understand your application UI and generate tests in real-time.

TL;DR

1. Install Node.js (LTS).
2. Sign in to Claude Desktop.
3. Open Settings → Developer → Edit MCP configuration file.
4. Add Playwright MCP as a local server using npx.
5. Restart Claude Desktop to apply changes.
6. Verify the setup by running a simple browser action.

## What is Playwright MCP?

MCP (Model Context Protocol) is a protocol that allows AI models to interact with external tools and environments, such as browsers or code editors, by exchanging structured context and actions in a standardized way.

Playwright MCP connects AI models directly to a Playwright-controlled browser, allowing them to understand the UI through the accessibility tree instead of relying on screenshots.

Simply provide a prompt detailing the automation task, and Playwright MCP will automatically launch the browser, execute the instructions, and generate the corresponding Playwright code. This enables faster and more accurate test generation while keeping all execution local and application data secure.

## How to install Playwright MCP on Claude Desktop

#### 1. Setup Claude Desktop

Prerequisites: Node.js LTS and npm available on the system; basic familiarity with the CLI.

Open Claude Desktop Settings (Ctrl + , on Windows, Cmd + , on macOS), navigate to the Developer tab, and click Edit Config to open the config file. The file lives at:

```text
Windows: C:\Users\{username}\AppData\Roaming\Claude\claude-desktop-config.json
macOS:   ~/Library/Application Support/Claude/claude-desktop-config.json
```

#### 2. Configure Claude Desktop JSON

This configuration tells Claude Desktop where to find and launch the Playwright MCP server:

```json
{
  "mcpServers": {
    "playwright": {
      // ... npx @playwright/mcp@latest server entry
    }
  }
}
```

The article continues with the full JSON body, the client restart step, and connection verification.

### 10. TestDino (Jashn Jain) — Learn Playwright in 2026: The Complete Roadmap

- Source: https://testdino.com/blog/learn-playwright
- Retrieved: 2026-08-29
- Exa status: complete

Published March 20, 2026, updated March 24, 2026.

Playwright has 84K+ GitHub stars and 33 million weekly NPM downloads. Tutorials exist everywhere. And yet most learners stall after writing 2 tests.

The problem isn't the framework. It's the order. People jump to Page Objects before they understand locators. Or skip CI/CD and wonder why their suite falls apart at 50 tests.

This guide is the 10-step sequence, from JavaScript basics to the AI ecosystem (MCP, test agents, skill packs) that shipped in 2025-2026. Every step has a time estimate, working code against a live demo app, and a clear checkpoint.

It's educational material, not a new idiom — but it confirms that fixtures/config/CI should be taught before the AI layer.

## Prerequisites before you start

| Prerequisite | Why it matters | Time to learn |
| --- | --- | --- |
| JavaScript/TypeScript basics | Playwright tests are JS/TS code | 1-2 weeks if new |
| HTML/CSS fundamentals | You're finding elements on web pages | 3-5 days |
| VS Code | Playwright has a dedicated extension | 1 day |
| Node.js 18+ | Playwright runs on Node | 30-minute install |
| Command line basics | You'll run tests from the terminal | 1 day |

Can you skip JavaScript? Playwright supports Python, Java, and C#. But the TypeScript/JavaScript ecosystem has the most tutorials, community support, and the fastest feature adoption.

## The 10-step Playwright roadmap

The sequence: JavaScript basics, project setup, first test, locators and assertions, Page Object Model, API testing, CI/CD pipelines, debugging with Trace Viewer, advanced patterns like sharding, and the AI ecosystem including MCP and test agents.

## Step 1: Learn JavaScript/TypeScript basics

Time: 1-2 weeks (skip if you already know JS). The single most important concept? async/await. Every Playwright command is asynchronous:

```typescript
// async/await is the #1 concept for Playwright
async function fetchData() {
  const response = await fetch('https://api.example.com/data');
  const data = await response.json();
  return data;
}

// Without async/await, Playwright tests won't work
// Every page action returns a Promise
```

## Step 2: Install Playwright and understand project structure

Time: 1 day. What you'll learn: `npm init playwright@latest`, folder structure, `playwright.config.ts`, and the `tests/` directory:

```bash
# Create a new Playwright project
npm init playwright@latest

# This creates:
# ├── tests/                ← your test files go here
# ├── playwright.config.ts  ← configuration
# ├── package.json
# └── .github/workflows/    ← optional CI config
```

Run the example tests immediately:

```bash
npx playwright test
```

Open the built-in HTML report:

```bash
npx playwright show-report
```

Tip: install the Playwright VS Code extension to run and debug tests directly in the editor.

### 11. Butch Mayhew — 600 subscribers — AI in QA Newsletter

- Source: https://www.linkedin.com/posts/butchmayhew_softwaretesting-qa-aiinqa-activity-7490440181498560512-uEgt
- Retrieved: 2026-08-29
- Exa status: complete

LinkedIn post, 2026-08-04. Curation/newsletter post without new Playwright API content; per the index, comments from Anton Gulin and Vitaliy Potapov are reactions, not authorship.

600 subscribers.

Thank you to everyone who has signed up for the AI in QA Newsletter.

Every week I spend real time reading and curating the best articles I come across for software quality professionals. AI is moving fast and most QA folks do not have hours to sort signal from noise (and trust me there is a lot of noise I sift through).

If you are a QA engineer trying to learn or grow, subscribing to a newsletter is one of the lowest effort habits you can build. You do not have to read all of it. Read one article a week.

That is 52 new ideas a year! It's free and weekly: https://aiinqa.com

Engagement: 24 reactions; commenters highlight the curation value ("One solid article a week beats a feed full of noise").

### 12. Playwright — Best Practices (official docs)

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
  // Runs before each test and signs in each page.
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

### 13. Playwright — Fixtures (official docs)

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

### 14. Playwright — Assertions (official docs)

- Source: https://playwright.dev/docs/test-assertions
- Retrieved: 2026-08-29
- Exa status: complete

## Introduction

Playwright includes test assertions in the form of the `expect` function. To make an assertion, call `expect(value)` and choose a matcher that reflects the expectation. There are many generic matchers like `toEqual`, `toContain`, `toBeTruthy` that can be used to assert any conditions.

```javascript
expect(success).toBeTruthy();
```

Playwright also includes web-specific async matchers that will wait until the expected condition is met:

```javascript
await expect(page.getByTestId('status')).toHaveText('Submitted');
```

Playwright will re-test the element with the test id of `status` until the fetched element has the `"Submitted"` text, until the condition is met or until the timeout is reached. The timeout can be configured once via the `testConfig.expect` value in the test config. By default, the timeout for assertions is set to 5 seconds.

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

These assertions allow testing any conditions, but do not auto-retry. Most of the time, web pages show information asynchronously, and using non-retrying assertions can lead to a flaky test. Prefer auto-retrying assertions whenever possible. For more complex assertions that need to be retried, use `expect.poll` or `expect.toPass`.

### 15. Playwright — Configuration (official docs)

- Source: https://playwright.dev/docs/test-configuration
- Retrieved: 2026-08-29
- Exa status: complete

## Introduction

Playwright has many options to configure how your tests are run. You can specify these options in the configuration file. Note that test runner options are top-level; do not put them into the `use` section.

## Basic Configuration

```javascript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  // Look for test files in the "tests" directory, relative to this configuration file.
  testDir: 'tests',
  // Run all tests in parallel.
  fullyParallel: true,
  // Fail the build on CI if you accidentally left test.only in the source code.
  forbidOnly: !!process.env.CI,
  // Retry on CI only.
  retries: process.env.CI ? 2 : 0,
  // Opt out of parallel tests on CI.
  workers: process.env.CI ? 1 : undefined,
  // Reporter to use
  reporter: 'html',
  use: {
    // Base URL to use in actions like `await page.goto('/')`.
    baseURL: 'http://localhost:3000',
    // Collect trace when retrying the failed test.
    trace: 'on-first-retry',
  },
  // Configure projects for major browsers.
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  // Run your local dev server before starting the tests.
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
  // Folder for test artifacts such as screenshots, videos, traces, etc.
  outputDir: 'test-results',
  // Path to the global setup file, run before all the tests.
  globalSetup: require.resolve('./global-setup'),
  // Path to the global teardown file, run after all the tests.
  globalTeardown: require.resolve('./global-teardown'),
  // Each test is given 30 seconds.
  timeout: 30000,
});
```

### 16. Playwright — Test API: `test` (incl. `test.step`)

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

Added in: v1.10. Declares a test. Signatures: `test(title, body)` and `test(title, details, body)`.

Tags: you can tag tests by providing additional test details, or include tags in the test title. Each tag must start with the `@` symbol.

```javascript
import { test, expect } from '@playwright/test';

test('basic test', {
  tag: '@smoke',
}, async ({ page }) => {
  await page.goto('https://playwright.dev/');
  // ...
});
```

Test tags are displayed in the test report and are available to a custom reporter via `TestCase.tags`. You can also filter tests by their tags during test execution: in the command line, or in the config with `testConfig.grep` and `testProject.grep`.

Annotations: you can annotate tests by providing additional test details:

```javascript
import { test, expect } from '@playwright/test';

test('basic test', {
  annotation: {
    type: 'issue',
    description: 'https://github.com/microsoft/playwright/issues/23180',
  },
}, async ({ page }) => {
  await page.goto('https://playwright.dev/');
  // ...
});
```

Test annotations are displayed in the test report and are available via `TestCase.annotations`; annotations can also be added during runtime via `testInfo.annotations`.

### test.abort

Added in: v1.60. Aborts the currently running test by throwing an error. The test is immediately marked as failed and execution stops. Useful from inside a fixture or a route handler when an unrecoverable misuse has been detected:

```javascript
import { test, expect } from '@playwright/test';

test('does not publish to shared page', async ({ page }) => {
  await page.route('**/publish', route => {
    test.abort('Tests must not publish to the shared page. Use the `clone` option.');
    return route.abort();
  });
  // ...
});
```

### test.afterAll

Added in: v1.10. Declares an `afterAll` hook executed once per worker after all tests. When called in the scope of a test file, runs after all tests in the file; inside a `test.describe()` group, runs after all tests in the group.

```javascript
test.afterAll(async () => {
  console.log('Done with tests');
  // ...
});
```

When multiple `afterAll` hooks are added, they run in the order of their registration. The page also documents `test.beforeAll`, `test.beforeEach`, `test.afterEach`, `test.describe`, `test.step` (reportable steps for readable reports), `test.setTimeout`, `test.skip`, and the fixtures API.

### 17. Playwright — Authentication (official docs)

- Source: https://playwright.dev/docs/auth
- Retrieved: 2026-08-29
- Exa status: complete

## Introduction

Playwright executes tests in isolated environments called browser contexts. This isolation model improves reproducibility and prevents cascading test failures. Tests can load existing authenticated state. This eliminates the need to authenticate in every test and speeds up test execution.

## Core concepts

Regardless of the authentication strategy you choose, you are likely to store authenticated browser state on the file system.

We recommend creating a `playwright/.auth` directory and adding it to your `.gitignore`. The authentication routine will produce authenticated browser state and save it to a file in this directory. Later on, tests will reuse this state and start already authenticated.

> **Danger:** the browser state file may contain sensitive cookies and headers that could be used to impersonate you or your test account. Checking them into private or public repositories is strongly discouraged.

```bash
mkdir -p playwright/.auth
echo $'\nplaywright/.auth' >> .gitignore
```

## Basic: shared account in all tests

This is the recommended approach for tests without server-side state. Authenticate once in the setup project, save the authentication state, and then reuse it to bootstrap each test already authenticated.

When to use: when you can imagine all your tests running at the same time with the same account, without affecting each other.

When not to use: your tests modify server-side state (e.g. one test checks the rendering of the settings page while the other is changing the setting, in parallel — in that case tests must use different accounts); or your authentication is browser-specific.

Create `tests/auth.setup.ts` that will prepare authenticated browser state for all other tests:

```javascript
// tests/auth.setup.ts
import { test as setup, expect } from '@playwright/test';
import path from 'path';

const authFile = path.join(__dirname, '../playwright/.auth/user.json');

setup('authenticate', async ({ page }) => {
  // Perform authentication steps. Replace these actions with your own.
  await page.goto('https://github.com/login');
  await page.getByLabel('Username or email address').fill('username');
  await page.getByLabel('Password').fill('password');
  await page.getByRole('button', { name: 'Sign in' }).click();
  // Wait until the page receives the cookies.
  //
  // Sometimes login flow sets cookies in the process of several redirects.
  // Wait for the final URL to ensure that the cookies are actually set.
  await page.waitForURL('https://github.com/');
  // Alternatively, wait until the page reaches a state where all cookies are set.
  await expect(page.getByRole('button', { name: 'View profile and more' })).toBeVisible();
  // End of authentication steps.
  await page.context().storageState({ path: authFile });
});
```

Create a new `setup` project in the config and declare it as a dependency for all testing projects. This project will always run and authenticate before all the tests. All testing projects should use the authenticated state as `storageState`:

```javascript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  projects: [
    // Setup project
    { name: 'setup', testMatch: /.*\.setup\.ts/ },
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // Use prepared auth state.
        storageState: 'playwright/.auth/user.json',
      },
      dependencies: ['setup'],
    },
    {
      name: 'firefox',
      use: {
        ...devices['Desktop Firefox'],
        // Use prepared auth state.
        storageState: 'playwright/.auth/user.json',
      },
      dependencies: ['setup'],
    },
  ],
});
```

Tests start already authenticated because `storageState` is specified in the config:

```javascript
// tests/example.spec.ts
import { test } from '@playwright/test';

test('test', async ({ page }) => {
  // page is authenticated
});
```

