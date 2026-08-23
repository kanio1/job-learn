# Ponytail in this lab

Adapted from [DietrichGebert/ponytail](https://github.com/DietrichGebert/ponytail) (MIT,
see `LICENSE-mattpocock-skills.md` sibling convention — upstream license text is in the
upstream repo; imported 2026-08-23). See [SOURCE.md](../SOURCE.md).

The six `ponytail*` skills here are **unmodified upstream** except for this note. The
lab-specific reading order when they trigger:

1. `ponytail` runs **after** lab placement rules, never instead of them: module
   placement (`spring-modulith`, `nuxt-frontend`, `playwright-pom`) and non-goals
   (`AGENTS.md`) decide *where/whether* code lives; ponytail decides *how little*.
2. "Reuse what exists" (ladder rung 2) means this repo's seams first: existing
   composables (`useMerchantsApi`, …), `useApiClient`, shared widgets
   (`app/components/shared/`), REST Assured support classes — before any new helper.
3. Flyway migrations and REST contracts are not candidates for "skip it" (YAGNI):
   schema changes stay Flyway-owned; contract stability rules in `java-spring-review`
   override laziness.
4. `ponytail-review` / `ponytail-audit` findings are a **subset lens** of
   `code-review`; run them alongside, not as a replacement. Never delete tests to look
   lazy — coverage decisions belong to `rest-api-test-design`.
5. `ponytail-debt` ledgers go under `.codex/` next to other tracker markdown.
