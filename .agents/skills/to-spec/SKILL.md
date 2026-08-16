---
name: to-spec
description: Turn the current conversation into a spec without interviewing. Use when the user wants a spec from what was already discussed, or to publish a ready-to-implement write-up.
disable-model-invocation: true
---

# To Spec

Synthesize what you already know. Do **not** interview.

If alignment is still missing, tell the user to run `grill-me` first.

## Process

1. Read the current codebase, `.codex/CONTEXT.md` if present, and nearby specs.
2. Sketch test **seams** (prefer existing REST Assured / Playwright REST / Playwright E2E / domain unit). Confirm the seam list with the user before writing the spec.
3. Write the spec using the template below.
4. Save it under `.codex/specs/<feature-slug>.md` (see `docs/agents/issue-tracker.md`). Historical files under `specs/` and `.kiro/specs/**` are prior art only.

## Template

## Problem Statement

The problem from the user's perspective.

## Solution

The solution from the user's perspective.

## User Stories

Numbered `As a <actor>, I want <feature>, so that <benefit>`. Cover the feature thoroughly.

## Implementation Decisions

Modules, interfaces, lifecycle rules, schema, HTTP contracts, authorities. No file paths or code dumps unless a prototype snippet encodes a decision (state machine, schema) more precisely than prose.

## Testing Decisions

- What a good test is (external behavior)
- Seams: REST Assured / Playwright REST / Playwright E2E / unit
- Prior art in this repo (similar test classes)

## Out of Scope

Include standing lab non-goals unless the user explicitly expanded scope.

## Further Notes
