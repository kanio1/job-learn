#!/usr/bin/env python3
"""Process a JSON array of MCP scrape results: write _scrapes/*.json and save each."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent
SCRAPES = OUT / "_scrapes"


def save_one(data: dict) -> tuple[str, bool]:
    metadata = data.get("metadata", {})
    url = metadata.get("url") or metadata.get("sourceURL") or "unknown"
    slug = url.rstrip("/").split("/")[-1][:60]
    SCRAPES.mkdir(exist_ok=True)
    path = SCRAPES / f"{slug}.json"
    path.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    result = subprocess.run(
        [sys.executable, str(OUT / "_orchestrate.py"), "save", str(path)],
        capture_output=True,
        text=True,
        cwd=OUT,
    )
    if result.returncode:
        print(result.stderr, file=sys.stderr)
        raise SystemExit(result.returncode)
    name = result.stdout.strip()
    content = (OUT / name).read_text(encoding="utf-8")
    body = content.split("## Treść artykułu", 1)[-1].strip()
    lines = len(content.splitlines())
    ok = len(body) > 3000 or lines > 150
    return name, ok


def main():
    batch = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    for data in batch:
        name, ok = save_one(data)
        body_len = len((OUT / name).read_text(encoding="utf-8").split("## Treść artykułu", 1)[-1].strip())
        print(f"{'OK' if ok else 'STUB'} {name} ({body_len} chars)")


if __name__ == "__main__":
    main()
