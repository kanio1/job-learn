#!/usr/bin/env python3
"""Combine md + meta and flush to overwrite stub articles."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    slug = sys.argv[1]
    md = OUT / "_scrapes" / "md" / f"{slug}.md"
    meta = OUT / "_scrapes" / "meta" / f"{slug}.json"
    if not md.exists() or not meta.exists():
        print(f"Missing {md} or {meta}", file=sys.stderr)
        sys.exit(1)
    data = {
        "markdown": md.read_text(encoding="utf-8"),
        "metadata": json.loads(meta.read_text(encoding="utf-8")),
    }
    inc = OUT / "_scrapes" / "incoming" / f"{slug}.json"
    inc.parent.mkdir(parents=True, exist_ok=True)
    inc.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    r = subprocess.run(
        [sys.executable, str(OUT / "_flush_incoming.py")],
        cwd=OUT,
        capture_output=True,
        text=True,
    )
    print(r.stdout.strip())
    if r.returncode:
        print(r.stderr, file=sys.stderr)
        sys.exit(r.returncode)
    name = r.stdout.strip().split()[-1] if r.stdout.strip() else ""
    if name:
        body = (OUT / name).read_text(encoding="utf-8").split("## Treść artykułu", 1)[-1].strip()
        ok = len(body) > 3000
        print(f"{'OK' if ok else 'STUB'} {name} ({len(body)} chars)")


if __name__ == "__main__":
    main()
