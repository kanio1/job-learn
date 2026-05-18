# Agent Operating Model

The Payment Quality Engineering Lab uses a clear responsibility split.

## Coding Agent Responsibilities

- Implement product and infrastructure foundation tasks from Spec Kit artifacts.
- Keep changes inside the approved phase scope.
- Preserve modular-monolith boundaries and Spring Modulith verification.
- Maintain runnable backend, frontend, infrastructure, and baseline test commands.
- Produce documentation that helps the tester understand what exists and what is deferred.

## Tester/Learner Responsibilities

- Analyze risks, ambiguity, and testability gaps.
- Design test conditions, data-isolation strategies, and exploratory charters.
- Automate tests where the phase provides behavior worth automating.
- Review documentation from a clean-contributor perspective.
- Preserve the distinction between foundation behavior and future payment business behavior.

## Phase 0 Guardrails

- Do not add payment business functionality.
- Do not add `POST /payments`.
- Do not add Kafka.
- Do not add PSP integration or PSP mock flows.
- Do not add complete OAuth/OIDC application integration.
- Do not create complete business dashboards.
- Do not introduce `.kilocode/` as a new project-organization target; current Kilo configuration references use `.kilo/`.
