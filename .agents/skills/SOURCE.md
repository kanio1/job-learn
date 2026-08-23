# Source and license

Engineering process skills in this directory are **adapted** from [mattpocock/skills](https://github.com/mattpocock/skills) (MIT License, Copyright (c) 2026 Matt Pocock).

They are not a verbatim install. The originals are generic; these copies are rewritten for Payment Quality Engineering Lab:

- Spring Modulith modules and public/internal seams
- REST Assured HTTP contracts
- Playwright E2E UI journeys
- Playwright REST / BFF HTTP tests
- Existing review skills (`java-spring-review`, `rest-api-test-design`, `playwright-sdet-review`)
- Existing domain skills (JUnit/REST Assured testcraft, Playwright engineering, test analysis)

The MIT license text is in [LICENSE-mattpocock-skills.md](LICENSE-mattpocock-skills.md).

`wizard/template.sh` is copied from mattpocock/skills (MIT). Prototype LOGIC/UI guidance is adapted from the same repo.

Still skipped: `writing-for-agents`, `resolving-merge-conflicts`, `improve-codebase-architecture`, `to-questionnaire`, `wait-what`. `ask-matt` is folded into `ask-engineering-flow`.

## Other upstream imports (2026-08-23)

| Import | Upstream | License | Form |
|---|---|---|---|
| `ponytail*` (six dirs) + `ponytail/LICENSE-upstream` | [DietrichGebert/ponytail](https://github.com/DietrichGebert/ponytail) | MIT (file copied) | Unmodified skills; lab reading order in `ponytail/lab-notes.md`. Node toolchain/tests/MCP from the upstream repo were **not** imported. |
| `playwright-skill-upstream/` | [testdino-hq/playwright-skill](https://github.com/testdino-hq/playwright-skill) v2.4.0 | MIT (LICENSE kept in pack) | Guide pack; nested `SKILL.md` renamed to `GUIDE.md` so only the index triggers; scoped description; overrides in its `lab-notes.md`. |
| `java-spring-framework-upstream/` | [AyrtonAldayr/agent-skill-java-spring-framework](https://github.com/AyrtonAldayr/agent-skill-java-spring-framework) | `"license": "MIT"` declared, **no LICENSE file upstream** — re-check before relying on it | Reference-only (`REFERENCE.md`), deliberately not triggerable; generic scaffolding/microservices content stays out of lab scope. |
