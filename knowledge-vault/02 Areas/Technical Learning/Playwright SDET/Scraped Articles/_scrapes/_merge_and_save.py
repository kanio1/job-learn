#!/usr/bin/env python3
"""Merge md + meta json and run orchestrate save."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent.parent
SCRAPE = Path(__file__).parent


def save(slug: str) -> str:
    md = (SCRAPE / "md" / f"{slug}.md").read_text(encoding="utf-8")
    meta = json.loads((SCRAPE / "meta" / f"{slug}.json").read_text(encoding="utf-8"))
    data = {"markdown": md, "metadata": meta}
    jpath = SCRAPE / f"{slug}.json"
    jpath.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    r = subprocess.run(
        ["python3", str(OUT / "_orchestrate.py"), "save", str(jpath)],
        capture_output=True,
        text=True,
        cwd=str(OUT),
    )
    if r.returncode:
        print(f"FAIL {slug}: {r.stderr.strip()}", file=sys.stderr)
        sys.exit(1)
    print(r.stdout.strip())
    return r.stdout.strip()


if __name__ == "__main__":
    for slug in sys.argv[1:]:
        save(slug)
