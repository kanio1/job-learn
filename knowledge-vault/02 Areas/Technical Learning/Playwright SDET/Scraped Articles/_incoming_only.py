#!/usr/bin/env python3
"""Write Firecrawl scrape JSON to _scrapes/incoming/{slug}.json (no flush)."""
import json
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    slug = sys.argv[1]
    src = Path(sys.argv[2]) if len(sys.argv) > 2 else None
    data = json.loads(src.read_text(encoding="utf-8")) if src else json.load(sys.stdin)
    dest = OUT / "_scrapes" / "incoming" / f"{slug}.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    print(dest.name)


if __name__ == "__main__":
    main()
