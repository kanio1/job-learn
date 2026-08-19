---
name: typescript6-playwright-engineering
description: Use when designing, reviewing, or implementing a Playwright 1.61 + TypeScript 6 test architecture with fixtures, role-aware flows, business-level page objects, API-assisted setup, and worker-aware parallel execution. Lab placement lives in playwright-pom.
---

# TypeScript 6 and Playwright Engineering

Superseded as a placement skill. Follow `.agents/skills/playwright-pom` (write) and `.agents/skills/playwright-sdet-review` (review).

Do not use this file for frontend product architecture. Do not install a public Playwright skill pack (TestDino, LambdaTest, on-the-fly MCP) into this repo.

Parallel isolation notes stay one line: unique references per worker, `storageState` per role, no file-order dependence. See `tests-pom/data/factories.ts`.
