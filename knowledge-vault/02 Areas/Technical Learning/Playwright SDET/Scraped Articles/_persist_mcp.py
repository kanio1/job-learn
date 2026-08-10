#!/usr/bin/env python3
"""Persist MCP firecrawl_scrape result: write JSON and run save pipeline."""
import json
import re
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent
SCRAPES = OUT / "_scrapes"


def delete_stub_for_url(url: str) -> None:
    for md in OUT.glob("*.md"):
        if md.name.startswith("_") or md.name == "INDEX.md":
            continue
        if f'url: "{url}"' in md.read_text(encoding="utf-8"):
            body = md.read_text(encoding="utf-8").split("## Treść artykułu", 1)[-1].strip()
            if len(body) <= 3000 and len(md.read_text(encoding="utf-8").splitlines()) <= 150:
                print(f"DELETE stub: {md.name}")
                md.unlink()


def persist(slug: str, data: dict) -> str:
    SCRAPES.mkdir(exist_ok=True)
    url = data.get("metadata", {}).get("url") or data.get("metadata", {}).get("sourceURL") or ""
    if url:
        delete_stub_for_url(url)
    path = SCRAPES / f"{slug}.json"
    path.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    r = subprocess.run(
        [sys.executable, str(OUT / "_orchestrate.py"), "save", str(path)],
        capture_output=True,
        text=True,
        cwd=OUT,
    )
    if r.returncode:
        print(f"ERROR {slug}: {r.stderr}", file=sys.stderr)
        return "ERROR"
    name = r.stdout.strip()
    content = (OUT / name).read_text(encoding="utf-8")
    body = content.split("## Treść artykułu", 1)[-1].strip()
    lines = len(content.splitlines())
    status = "OK" if len(body) > 3000 or lines > 150 else "STUB"
    print(f"{status} {name} ({len(body)} chars, {lines} lines)")
    return status


if __name__ == "__main__":
    slug = sys.argv[1]
    data = json.load(sys.stdin)
    persist(slug, data)
