#!/usr/bin/env python3
"""Save batch 4 MCP scrape results from embedded markdown."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent
MD_DIR = OUT / "_scrapes" / "md"
META_DIR = OUT / "_scrapes" / "meta"

ARTICLES = {
    "websockets-mocking": "websockets-mocking.md",
    "playwright-api-wait": "playwright-api-wait.md",
    "playwright-tips-4": "playwright-tips-4.md",
    "network-cache": "network-cache.md",
}


def save(slug: str) -> None:
    md = (MD_DIR / ARTICLES[slug]).read_text(encoding="utf-8")
    meta = json.loads((META_DIR / f"{slug}.json").read_text(encoding="utf-8"))
    data = {"markdown": md, "metadata": meta}
    r = subprocess.run(
        [sys.executable, str(OUT / "_mcp_store.py"), slug],
        input=json.dumps(data, ensure_ascii=False),
        text=True,
        capture_output=True,
        cwd=OUT,
    )
    print(r.stdout, end="")
    if r.returncode:
        print(r.stderr, file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    for slug in ARTICLES:
        save(slug)
