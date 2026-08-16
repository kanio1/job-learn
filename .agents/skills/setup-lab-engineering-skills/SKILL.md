---
name: setup-lab-engineering-skills
description: Configure or confirm this repo's local-markdown tracker, triage Status strings, and domain-doc layout. Run once, or again only to switch tracker.
disable-model-invocation: true
---

# Setup Lab Engineering Skills

This repo is already configured. Confirm the files exist; do not re-ask tracker questions unless the user wants to change them.

## Expected config

- Tracker: local markdown under `.codex/` — `docs/agents/issue-tracker.md`
- Triage `Status:` strings — `docs/agents/triage-labels.md`
- Domain docs — `docs/agents/domain.md`

This lab does **not** use GitHub Issues or Linear as the work tracker.

## Process

1. Read the three files above. If they exist, report that setup is complete and which skills consume them (`to-spec`, `to-tickets`, `triage`, `wayfinder`, `implement`).
2. If a file is missing, recreate it from the copies already in git history / this skill's sibling docs — do not invent a GitHub tracker.
3. If the user explicitly wants a different tracker, grill that choice, then rewrite `docs/agents/issue-tracker.md` only.

Do not create root `CONTEXT.md` (it already exists as a repo map). Glossary stays `.codex/CONTEXT.md`.
