# How to author a Spring Modulith + JDK 25 skill for this lab

## Answer

Author one **model-invoked** process skill at `.agents/skills/spring-modulith/` using the Agent Skills spec (`name` + `description`, progressive disclosure, SKILL.md under 500 lines, references one level deep). Pin **this repo’s versions**, not “latest Spring”: Java 25, Spring Boot 4.0.6, Spring Framework 7, Spring Modulith 2.0.6.

The skill is for **placing and testing production Java** (modules, Flyway/JPA, `@ApplicationModuleTest`). It is not a Spring tutorial, not Effective Java mentoring, and not a review pass (`java-spring-review` stays review-only).

## Why it matters here

Agents already know Spring. They do not know this lab’s packaging, OPEN `shared` module, payment → merchant public API only, Flyway-owned schema, or which JDK 25 features are allowed. Those facts belong in a skill so `implement` / `tdd` do not re-discover them from `AGENTS.md` every time.

## Project impact

- Canonical copy: `.agents/skills/spring-modulith/`
- Consumed via existing symlinks from `.cursor/skills/` and `.opencode/skills/`
- Composed by `implement` and `codebase-design`; reviewed later by `java-spring-review`

## Test impact (REST Assured / Playwright REST / Playwright E2E)

HTTP seams stay with `tdd`. This skill only owns Modulith architecture tests and module-slice Testcontainers tests (`ModulithArchitectureTest`, `*ModuleTest`). Do not move REST Assured coverage here.

## Sources

- [Agent Skills specification](https://agentskills.io/specification) — `name`/`description`, progressive disclosure, one-level references, SKILL.md < 500 lines
- [Claude Code skills](https://code.claude.com/docs/en/skills) — model vs user invocation; keep frontmatter on the open spec so Codex/Cursor also load it
- [Codex / ChatGPT build skills](https://learn.chatgpt.com/docs/build-skills) — repo skills live in `.agents/skills`; concise descriptions; prefer instructions over scripts
- [Claude skill authoring best practices](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices) — concise, WHAT+WHEN description, no time-sensitive “before date X” rules
- [Spring Modulith fundamentals](https://docs.spring.io/spring-modulith/reference/fundamentals.html) — API vs `internal`, OPEN modules, named interfaces, `@ApplicationModule`
- [Spring Modulith verification](https://docs.spring.io/spring-modulith/reference/verification.html) — `ApplicationModules.of(…).verify()`, no cycles, no internal access
- [Spring Modulith testing](https://docs.spring.io/spring-modulith/reference/testing.html) — `@ApplicationModuleTest` modes STANDALONE / DIRECT_DEPENDENCIES; `@MockitoBean` for efferent deps
- [Spring Boot 4.0 system requirements](https://docs.spring.io/spring-boot/4.0/system-requirements.html) — Java 17–26; Framework 7.x (page rendered 4.0.7; lab stays on Boot **4.0.6**)
- [JDK 25 JEPs](https://openjdk.org/projects/jdk/25/) — Scoped Values final; compact source files; flexible constructors; several **preview** JEPs
- This repo: `apps/backend/pom.xml`, `package-info.java` modules, `PaymentModuleTest`, `MerchantModuleTest`, `application.yml`

## Uncertainty / follow-up

- **Context7 MCP** authenticated in-session but every `query-docs` / `resolve-library-id` call returned `Invalid API key` (`ctx7sk` prefix). Skill facts therefore come from Firecrawl scrapes of official docs plus `pom.xml`. Re-run Context7 once `CONTEXT7_API_KEY` is a valid `ctx7sk…` key if a Spring API changes.
- Live Modulith HTML currently labels itself **2.1.0**. Lab is **2.0.6**. Do not copy 2.1-only APIs; copy patterns already in this codebase.
- Firecrawl `developer_search` (skills index) was rejected by the host; no third-party “Spring Modulith skill” was imported.
