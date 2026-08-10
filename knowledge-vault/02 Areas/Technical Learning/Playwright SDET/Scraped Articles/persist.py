#!/usr/bin/env python3
"""Persist Firecrawl scrape JSON from file or stdin."""
import json
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    if src and src.exists():
        data = json.loads(src.read_text(encoding="utf-8"))
    else:
        data = json.load(sys.stdin)
    tmp = OUT / "_scrapes" / "last.json"
    tmp.parent.mkdir(exist_ok=True)
    tmp.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    sys.path.insert(0, str(OUT))
    from _orchestrate import save_json
    name = save_json(tmp)
    body = (OUT / name).read_text(encoding="utf-8").split("## Treść artykułu", 1)[-1].strip()
    print(f"OK {name} ({len(body)} chars)")


if __name__ == "__main__":
    main()
