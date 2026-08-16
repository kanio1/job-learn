# Domain docs

How engineering skills consume this repo's domain language.

## Before exploring, read these

- `CONTEXT.md` at the repo root — orientation map, not a glossary.
- `.codex/CONTEXT.md` — glossary of resolved terms (created lazily by `grill-with-docs`).
- `AGENTS.md` and `.kiro/steering/product.md` — standing domain and non-goals.
- `.codex/adr/` — hard-to-reverse decisions (created lazily).

If `.codex/CONTEXT.md` or `.codex/adr/` do not exist yet, proceed silently.

## Use the glossary's vocabulary

When output names a domain concept (ticket title, test name, hypothesis), use the term as defined in `.codex/CONTEXT.md` or product steering. Do not invent synonyms the glossary avoids.

## Flag ADR conflicts

If output contradicts an existing ADR, surface it rather than silently overriding.
