# Codex CLI Execution Overlay

This directory is the mutable execution layer: current state, specs, tickets, wayfinder maps, research notes, and handoffs.

Tracker: `docs/agents/issue-tracker.md`.

## Purpose

- Keep new specs/tickets/maps in `.codex/` so they are the live work tracker.
- Treat `.kiro/specs/**` and `specs/` as historical prior art only.
- Keep implementation evidence and execution notes out of `.kiro`.

## Read Order for Codex CLI

1. Root `AGENTS.md`
2. `.agents/skills/README.md`
3. `docs/agents/issue-tracker.md`
4. `.codex/current-state.md`
5. `.codex/review-checklist.md`

## Layout used by process skills

- Glossary: `.codex/CONTEXT.md` (lazy)
- Specs: `.codex/specs/`
- Tickets: `.codex/tickets/`
- Inbox / triage: `.codex/inbox/`
- Wayfinder: `.codex/wayfinder/`
- Research: `.codex/research/`
- Prototypes: `.codex/prototypes/`
- Handoffs: `.codex/handoffs/`
- Wizards: `.codex/wizards/`
- Teach workspaces: `.codex/teach/`
- ADRs: `.codex/adr/`
- Guides: `.codex/guides/`
- Rejected enhancements: `.codex/out-of-scope/`

## Source-of-Truth Rules

- New work: `.codex/specs` and `.codex/tickets`.
- Historical tenant spec under `.kiro/specs/tenant-model-and-isolation/` is prior art only.
- `.codex/current-state.md` is the mutable status overlay.

## Historical continuation notes

Older tenant-isolation continuation files may still exist under `.codex/` (for example `tenant-model-and-isolation.md`). Treat them as prior art.
