#!/usr/bin/env python3
"""Persist MCP scrape results from JSON files in _scrapes/firecrawl/."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent


def persist(slug: str, data: dict) -> None:
    r = subprocess.run(
        [sys.executable, str(OUT / "_persist_mcp.py"), slug],
        input=json.dumps(data, ensure_ascii=False),
        capture_output=True,
        text=True,
        cwd=OUT,
    )
    print(r.stdout.strip() or r.stderr.strip())


def main():
    fc = OUT / "_scrapes" / "firecrawl"
    for jf in sorted(fc.glob("*.json")):
        data = json.loads(jf.read_text(encoding="utf-8"))
        slug = jf.stem
        persist(slug, data)


if __name__ == "__main__":
    main()
