---
name: prototype
description: Build throwaway code that answers one design question — a shareable HTML state-machine demo, or several UI variants on a Nuxt route. Use when a logic/state model or UI cannot be settled on paper.
---

# Prototype

A prototype is **throwaway code that answers a question**. The question decides the shape.

## Pick a branch

- **"Does this logic / state model feel right?"** → [LOGIC.md](LOGIC.md). One HTML file. Lab examples: payment lifecycle (authorize/capture/cancel/refund/expire), refund dual-control, merchant DRAFT→ACTIVE→SUSPENDED, tenant-masked 404 vs 403.
- **"What should this look like?"** → [UI.md](UI.md). Several structurally different variants on an existing Nuxt admin page, `?variant=`, floating switcher. Use Nuxt UI already in the app — do not invent a new component library.

If ambiguous and the user is away: backend/lifecycle → logic; page/component → UI.

## Rules for both

1. Marked throwaway from day one. Logic HTML lives in `.codex/prototypes/<slug>.html`. UI variants stay on an existing admin route or a path that contains `prototype`.
2. Trivial to run: double-click the HTML, or `corepack pnpm dev` for UI.
3. No persistence by default. No real PSP, no production Keycloak writes.
4. No tests, no extra error handling, no abstractions.
5. Surface full relevant state after every action / variant switch.
6. Fold the **decision** into real code; keep the prototype as a primary source under `.codex/prototypes/` (do not merge throwaway UI onto the main design). Link the verdict from the originating `.codex` ticket or spec.

Do not ship prototype routes or switchers in production builds.
