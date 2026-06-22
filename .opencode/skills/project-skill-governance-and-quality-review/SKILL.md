---
name: project-skill-governance-and-quality-review
description: Review, curate and evolve the project-local skills collection: detect overlap, missing triggers, stale versions, unsafe imports, overlong skills, missing references and unclear skill boundaries.
license: MIT
metadata:
  category: skill-governance
  author: project-custom
  version: "1.0.0"
---

# Project Skill Governance and Quality Review

## Use when
- reviewing this skills pack,
- importing/adapting a public skill,
- deciding whether a new skill is needed,
- splitting a skill into SKILL.md vs references,
- checking overlap/conflict.

## Review criteria
1. Clear description/trigger?
2. Specialized enough?
3. Overlap with another skill?
4. `When Not to Use` present if ambiguity risk exists?
5. Version-sensitive claims routed to research?
6. Long reference material moved to `references/`?
7. External public skill reviewed for license, scripts, maintenance, security and fit?

## When Not to Use
Do not use this for product implementation or generic web research without governance purpose.

See:
- `references/public-skill-adoption-checklist.md`
- `scripts/validate_skills.py`
