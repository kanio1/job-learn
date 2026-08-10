#!/usr/bin/env python3
"""Save MCP scrape from stdin: python3 _mcp_store.py <slug> < _scrapes/raw/in.json"""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent
slug = sys.argv[1] if len(sys.argv) > 1 else "article"
data = json.load(sys.stdin)
raw = OUT / "_scrapes" / "raw" / f"{slug}.json"
raw.parent.mkdir(parents=True, exist_ok=True)
raw.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
dest = OUT / "_scrapes" / f"{slug}.json"
dest.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
r = subprocess.run(
    [sys.executable, str(OUT / "_orchestrate.py"), "save", str(dest)],
    capture_output=True,
    text=True,
    cwd=OUT,
)
if r.returncode:
    print(r.stderr, file=sys.stderr)
    sys.exit(1)
name = r.stdout.strip()
body = (OUT / name).read_text(encoding="utf-8").split("## Treść artykułu", 1)[-1].strip()
lines = len((OUT / name).read_text(encoding="utf-8").splitlines())
ok = len(body) > 3000 or lines > 150
print(f"{'OK' if ok else 'STUB'} {name} ({len(body)} chars, {lines} lines)")
