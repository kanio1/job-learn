#!/usr/bin/env python3
"""Save scrape dict from raw JSON file to incoming."""
import json
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    slug, src = sys.argv[1], Path(sys.argv[2])
    data = json.loads(src.read_text(encoding="utf-8"))
    dest = OUT / "_scrapes" / "incoming" / f"{slug}.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    print(slug)


if __name__ == "__main__":
    main()
