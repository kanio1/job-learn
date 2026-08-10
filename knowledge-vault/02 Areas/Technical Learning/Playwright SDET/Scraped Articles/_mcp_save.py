#!/usr/bin/env python3
"""Save Firecrawl MCP JSON (stdin or file) via orchestrate pipeline."""
import json
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    if len(sys.argv) > 1:
        data = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    else:
        data = json.load(sys.stdin)
    slug = sys.argv[2] if len(sys.argv) > 2 else "ingest"
    p = OUT / "_scrapes" / f"{slug}.json"
    p.parent.mkdir(exist_ok=True)
    p.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    sys.path.insert(0, str(OUT))
    from _orchestrate import save_json
    name = save_json(p)
    body = (OUT / name).read_text(encoding="utf-8").split("## Treść artykułu", 1)[-1].strip()
    print(f"OK {name} ({len(body)} chars)")


if __name__ == "__main__":
    main()
