#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[3]
SKILLS = ROOT / "skills"

errors = []
warnings = []

for skill_dir in sorted(SKILLS.iterdir()):
    if not skill_dir.is_dir():
        continue
    skill_file = skill_dir / "SKILL.md"
    if skill_file.exists():
        text = skill_file.read_text(encoding="utf-8")
        if not text.startswith("---"):
            errors.append(f"{skill_dir.name}: missing YAML frontmatter opener")
        if "name:" not in text:
            errors.append(f"{skill_dir.name}: missing name")
        if "description:" not in text:
            errors.append(f"{skill_dir.name}: missing description")
        if "when not to use" not in text.lower():
            warnings.append(f"{skill_dir.name}: no 'When Not to Use' section")
    elif (skill_dir / "REFERENCE.md").exists():
        # Reference-only pack: intentionally not triggerable, no SKILL.md required.
        continue
    else:
        errors.append(f"{skill_dir.name}: missing SKILL.md")
        continue

print("== Skill Validation ==")
print(f"Skills path: {SKILLS}")
print(f"Skills found: {len([p for p in SKILLS.iterdir() if p.is_dir()])}")

if errors:
    print("\nERRORS")
    for e in errors:
        print("-", e)
else:
    print("\nNo structural errors found.")

if warnings:
    print("\nWARNINGS")
    for w in warnings:
        print("-", w)
else:
    print("\nNo warnings.")

sys.exit(1 if errors else 0)
