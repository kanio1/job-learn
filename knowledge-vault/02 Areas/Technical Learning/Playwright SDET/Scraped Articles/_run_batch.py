#!/usr/bin/env python3
"""Write scrape JSON and run save pipeline."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent
SCRAPES = OUT / "_scrapes"


def save_scrape(slug: str, data: dict) -> str:
    SCRAPES.mkdir(exist_ok=True)
    path = SCRAPES / f"{slug}.json"
    path.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    result = subprocess.run(
        [sys.executable, str(OUT / "_orchestrate.py"), "save", str(path)],
        capture_output=True,
        text=True,
        cwd=OUT,
    )
    if result.returncode != 0:
        print(result.stderr, file=sys.stderr)
        raise SystemExit(result.returncode)
    name = result.stdout.strip()
    body = (OUT / name).read_text(encoding="utf-8").split("## Treść artykułu", 1)[-1].strip()
    lines = len((OUT / name).read_text(encoding="utf-8").splitlines())
    ok = len(body) > 3000 or lines > 150
    print(f"{'OK' if ok else 'STUB'} {name} ({len(body)} chars, {lines} lines)")
    return name


if __name__ == "__main__":
    data = json.loads(sys.stdin.read())
    for item in data:
        save_scrape(item["slug"], item["data"])
