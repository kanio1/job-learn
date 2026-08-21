---
name: beautiful-mermaid
description: >-
  Render Mermaid diagrams to SVG/ASCII with @vercel/beautiful-mermaid
  (vercel-labs/beautiful-mermaid). Use when adding or updating .mmd files,
  session/OIDC logout diagrams, or when the user asks for beautiful-mermaid.
  Do not add this package to apps/frontend.
---

# Beautiful Mermaid (this lab)

Package lives in `tools/beautiful-mermaid` so it does **not** share `node_modules` with `apps/frontend`. Upstream: [vercel-labs/beautiful-mermaid](https://github.com/vercel-labs/beautiful-mermaid).

Pinned via that folder's `package.json`. Do not bump it as a side effect of frontend work.

## Invoke (repo root)

```bash
corepack pnpm --dir tools/beautiful-mermaid install
corepack pnpm --dir tools/beautiful-mermaid run render
corepack pnpm --dir tools/beautiful-mermaid run view
```

Default input: `docs/testing/diagrams/*.mmd`. Output: sibling `.svg` files (Vercel light) and `docs/testing/diagrams/index.html` (gallery). `run view` opens that HTML in the browser.

Do **not** paste tall SVGs into contract markdown. Link the gallery instead.

## Rules

- Source of truth is the `.mmd` file, not the SVG.
- Split multiple diagrams in one file with a line `%% diagram: slug`.
- Supported types here: flowchart, state, sequence (library also has class/ER).
- Do not invent logout depths that are not in `docs/testing/session-bff-oidc-contract.md`.
