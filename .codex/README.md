# Codex CLI Execution Overlay

This directory is the Codex CLI working layer for continuing implementation from Kiro-generated specifications without editing `.kiro` planning files.

## Purpose

- Preserve `.kiro/specs/**` as the read-only source of product requirements, design rationale, and task sequencing.
- Give Codex CLI a smaller execution system made of Markdown files that describe the current branch state, active wave, guardrails, and review checklist.
- Keep implementation evidence and execution notes outside `.kiro` so Kiro plans and tasks remain stable.

## Read Order for Codex CLI

1. Root `AGENTS.md` — repository-wide operating model and safety rules.
2. `.codex/current-state.md` — current branch status, completed work, and next active wave.
3. `.codex/tenant-model-and-isolation.md` — actionable continuation plan for the tenant isolation spec.
4. `.codex/review-checklist.md` — review gates before and after implementation.
5. `.codex/prompts/continue-tenant-wave-2.md` — ready-to-paste prompt for a Codex CLI implementation session.

## Source-of-Truth Rules

- `.kiro/specs/tenant-model-and-isolation/requirements.md` remains the requirements source of truth.
- `.kiro/specs/tenant-model-and-isolation/design.md` remains the architecture source of truth.
- `.kiro/specs/tenant-model-and-isolation/tasks.md` remains the original Kiro task map.
- `.codex/current-state.md` is the mutable Codex execution status overlay.
- Do not check or uncheck `.kiro` task boxes unless the user explicitly requests Kiro spec maintenance.

## Active Spec

Active continuation target: `tenant-model-and-isolation` on branch `018-rest-security-p1-error-auth-method-hardening`.

Current known checkpoint: Wave 1 is complete and backend tests were reported green by the previous implementation run. Codex should continue with Wave 2 unless the user asks for a review-only pass first.
