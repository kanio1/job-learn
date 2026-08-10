#!/usr/bin/env python3
"""Combine _scrapes/md/*.md + _scrapes/meta/*.json and save via orchestrate."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent
MD_DIR = OUT / "_scrapes" / "md"
META_DIR = OUT / "_scrapes" / "meta"
SCRAPES = OUT / "_scrapes"


def save(slug: str) -> None:
    md = (MD_DIR / f"{slug}.md").read_text(encoding="utf-8")
    meta = json.loads((META_DIR / f"{slug}.json").read_text(encoding="utf-8"))
    data = {"markdown": md, "metadata": meta}
    path = SCRAPES / f"{slug}.json"
    path.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    r = subprocess.run(
        [sys.executable, str(OUT / "_orchestrate.py"), "save", str(path)],
        capture_output=True,
        text=True,
        cwd=OUT,
    )
    if r.returncode:
        print(r.stderr, file=sys.stderr)
        sys.exit(1)
    name = r.stdout.strip()
    body = (OUT / name).read_text(encoding="utf-8").split("## Treść artykułu", 1)[-1].strip()
    lines = len((OUT / name).read_text(encoding="utf-8").splitlines())
    ok = len(body) > 3000 or lines > 150
    print(f"{'OK' if ok else 'STUB'} {name} ({len(body)} chars, {lines} lines)")


def main():
    slugs = sys.argv[1:] if len(sys.argv) > 1 else [p.stem for p in MD_DIR.glob("*.md")]
    for slug in slugs:
        if not (MD_DIR / f"{slug}.md").exists():
            print(f"SKIP missing {slug}")
            continue
        save(slug)


if __name__ == "__main__":
    main()
