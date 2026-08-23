---
name: grill-with-docs
description: Grilling session that also sharpens domain language and records hard-to-reverse decisions. Use when aligning on a change that will rename concepts, touch lifecycle language, or need an ADR.
disable-model-invocation: true
---

# Grill With Docs

Follow `grilling`. While grilling, maintain the lab glossary and ADRs.

This is the default grill entrypoint in this repo; there is no separate bare-grill wrapper skill.

## Where docs live in this lab

- Repo map (already exists): `CONTEXT.md` at the repo root — orientation, not a glossary.
- Glossary: `.codex/CONTEXT.md` (terms only — no implementation, no file paths). Create lazily.
- ADRs: `.codex/adr/NNNN-title.md` (create the directory lazily)

Read `CONTEXT.md`, `AGENTS.md`, and `.kiro/steering/product.md` first. If a term already lives there, reuse it; only add to `.codex/CONTEXT.md` when the conversation **resolves** a term that was fuzzy.

Do not replace root `CONTEXT.md` with a glossary dump.

## During the session

- Challenge terms against the glossary and product steering. "Your glossary defines cancellation as X, but you seem to mean Y — which is it?"
- Stress-test with concrete payment scenarios (authorize then capture vs refund dual-control; tenant-masked 404 vs 403).
- If the user states how something works, check the code. Surface contradictions.

## ADR gate — create only when all three are true

1. Hard to reverse
2. Surprising without context
3. Result of a real trade-off

## ADR shape

```markdown
# NNNN — Title

Status: accepted
Date: YYYY-MM-DD

## Context
## Decision
## Consequences
```

Do not implement until the user confirms shared understanding.
