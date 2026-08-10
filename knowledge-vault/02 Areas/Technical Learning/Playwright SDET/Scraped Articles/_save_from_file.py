#!/usr/bin/env python3
"""Save a Firecrawl scrape dict to incoming/ and flush."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent


def save(slug: str, data: dict) -> None:
    dest = OUT / "_scrapes" / "incoming" / f"{slug}.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    r = subprocess.run([sys.executable, str(OUT / "_flush_incoming.py")], cwd=OUT, capture_output=True, text=True)
    print(r.stdout.strip())
    if r.returncode:
        print(r.stderr, file=sys.stderr)
        sys.exit(r.returncode)


if __name__ == "__main__":
    slug, src = sys.argv[1], Path(sys.argv[2])
    save(slug, json.loads(src.read_text(encoding="utf-8")))
