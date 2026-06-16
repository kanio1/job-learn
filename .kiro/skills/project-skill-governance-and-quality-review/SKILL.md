---
name: project-skill-governance-and-quality-review
description: Use when reviewing, curating, or evolving the project skill collection across both .kiro/skills/ and .kilocode/skills/, detecting overlap, missing triggers, stale versions, or unclear boundaries.
---

# Project Skill Governance and Quality Review

## Use when
- reviewing this skills pack,
- importing/adapting a public skill,
- deciding whether a new skill is needed,
- splitting a skill into SKILL.md vs references,
- checking overlap/conflict between .kiro/skills/ and .kilocode/skills/.

## Review criteria
1. Clear description/trigger?
2. Specialized enough?
3. Overlap with another skill?
4. `When Not to Use` present if ambiguity risk exists?
5. Version-sensitive claims routed to research?
6. Long reference material moved to `references/`?
7. External public skill reviewed for license, scripts, maintenance, security and fit?
8. Kiro SKILL.md frontmatter valid (name + description, no Kilo-specific wording)?
9. Is the skill updated in both `.kiro/skills/` and `.kilocode/skills/` when both tools are active?

## When Not to Use
Do not use this for product implementation or generic web research without governance purpose.

See:
- `.kilocode/skills/project-skill-governance-and-quality-review/references/public-skill-adoption-checklist.md`
- `.kilocode/skills/project-skill-governance-and-quality-review/scripts/validate_skills.py`
