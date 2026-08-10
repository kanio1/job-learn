#!/usr/bin/env python3
"""Write MCP scrape JSON from stdin and run save pipeline."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent
SCRAPES = OUT / "_scrapes"


def main():
    slug = sys.argv[1] if len(sys.argv) > 1 else "article"
    data = json.load(sys.stdin)
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
        sys.exit(result.returncode)
    name = result.stdout.strip()
    content = (OUT / name).read_text(encoding="utf-8")
    body = content.split("## Treść artykułu", 1)[-1].strip()
    lines = len(content.splitlines())
    ok = len(body) > 3000 or lines > 150
    print(f"{'OK' if ok else 'STUB'} {name} ({len(body)} chars, {lines} lines)")


if __name__ == "__main__":
    main()
