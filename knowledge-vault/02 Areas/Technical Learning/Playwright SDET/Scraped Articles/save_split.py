#!/usr/bin/env python3
"""Combine _scrapes/md/{slug}.md + _scrapes/meta/{slug}.json and persist."""
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
    tmp = OUT / "_scrapes" / "last.json"
    tmp.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    r = subprocess.run([sys.executable, str(OUT / "persist.py"), str(tmp)], capture_output=True, text=True)
    print(r.stdout.strip() or r.stderr.strip())
    sys.exit(r.returncode)


if __name__ == "__main__":
    main()
