---
name: handoff
description: Compact the current conversation into a handoff document so another agent can continue. Use when ending a session, switching agents, or the user asks for a handoff.
disable-model-invocation: true
---

# Handoff

Write a handoff so a fresh agent can continue. Save to `.codex/handoffs/YYYY-MM-DD-<slug>.md`.

Do not duplicate specs, ADRs, tickets, commits, or diffs — link them by path.

Redact secrets, tokens, passwords, PAN-like data.

If the user described the next session's focus, tailor the doc to that.

## Template

```markdown
# Handoff — <slug>

## Goal
## Done
## Not done
## Repo pointers
- spec / tickets / ADR / branch
## Suggested skills
- name the next Skill to follow (`implement`, `tdd`, `code-review`, `diagnosing-bugs`, …)
## Risks / open questions
```
