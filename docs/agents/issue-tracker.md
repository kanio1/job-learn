# Issue tracker: local markdown

Issues, specs, and wayfinder maps for this repo live as markdown under `.codex/`. This lab does not use GitHub Issues or Linear as the work tracker.

Read historical material under `specs/` and `.kiro/specs/**` as prior art only.

## Layout

| Kind | Path |
|---|---|
| Spec | `.codex/specs/<feature-slug>.md` |
| Implementation tickets | `.codex/tickets/<feature-slug>/<NN>-<slug>.md` |
| Incoming / untriaged | `.codex/inbox/<NN>-<slug>.md` |
| Wayfinder map | `.codex/wayfinder/<effort>/MAP.md` |
| Wayfinder decision tickets | `.codex/wayfinder/<effort>/tickets/<NN>-<slug>.md` |
| Research notes | `.codex/research/<slug>.md` |
| Handoffs | `.codex/handoffs/YYYY-MM-DD-<slug>.md` |
| Rejected enhancements | `.codex/out-of-scope/<concept>.md` |

## Conventions

- One feature per directory for tickets.
- Triage state is a `Status:` line near the top (see `triage-labels.md`).
- Conversation appends under `## Comments`.
- Category is `Category: bug` or `Category: enhancement`.

## When a skill says "publish to the issue tracker"

Create the matching file under `.codex/` (create directories as needed).

## When a skill says "fetch the relevant ticket"

Read the path the user passed, or scan `.codex/tickets/` and `.codex/inbox/`.

## Wayfinding operations

Used by `wayfinder`.

- **Map**: `.codex/wayfinder/<effort>/MAP.md`
- **Child ticket**: `.codex/wayfinder/<effort>/tickets/NN-<slug>.md` with `Type:` (`research` / `prototype` / `grilling` / `task`) and `Status:` (`open` / `claimed` / `resolved`)
- **Blocking**: `Blocked by: NN, NN`. Unblocked when every listed file is `resolved`.
- **Frontier**: open, unblocked, unclaimed; lowest number first.
- **Claim**: set `Status: claimed` before any work.
- **Resolve**: append `## Answer`, set `Status: resolved`, append a one-line gist + path to the map's Decisions so far.
