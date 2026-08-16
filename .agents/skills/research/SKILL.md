---
name: research
description: Investigate a question against primary sources and save a cited Markdown note under .codex/research. Use when the user wants docs, API facts, or reading delegated; uses Firecrawl MCP plus official-docs-and-versioned-research for stack versions.
---

# Research

Investigate against **primary sources** — official docs, first-party APIs, this repo's code — not a blog summarizing them. Write one cited Markdown file.

Save to `.codex/research/<slug>.md`.

## How to gather

Prefer tools in this order:

1. **This repo** — `pom.xml`, `package.json`, Flyway, controllers, existing tests.
2. **Firecrawl MCP** — `firecrawl_scrape` for a known URL; `firecrawl_developer_search` (optionally `skills: "only"`) for GitHub/docs/issues; `firecrawl_search` for the open web; `firecrawl_map` when you need a site's URL inventory.
3. **official-docs-and-versioned-research** when the claim is version-sensitive (Java 25, Spring Boot 4, Playwright 1.61, REST Assured 6, Nuxt 4, Keycloak). Do not invent versions.

Follow every claim back to the source that owns it.

## Output

```markdown
# <question>

## Answer
## Why it matters here
## Project impact
## Test impact (REST Assured / Playwright REST / Playwright E2E)
## Sources
- [title](url) — what it supports
## Uncertainty / follow-up
```

If running as a background/sub-agent, still write the file and return its path. Research **feeds** `grill-with-docs` / `wayfinder`; it does not replace them.
