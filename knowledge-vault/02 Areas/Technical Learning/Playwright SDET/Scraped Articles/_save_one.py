#!/usr/bin/env python3
"""Save one MCP scrape result from a JSON file path."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent
path = Path(sys.argv[1])
data = json.loads(path.read_text(encoding="utf-8"))
SCRAPES = OUT / "_scrapes"
SCRAPES.mkdir(exist_ok=True)
out = SCRAPES / path.name
out.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
r = subprocess.run([sys.executable, str(OUT / "_orchestrate.py"), "save", str(out)], capture_output=True, text=True, cwd=OUT)
if r.returncode:
    print(r.stderr, file=sys.stderr)
    sys.exit(1)
name = r.stdout.strip()
body = (OUT / name).read_text(encoding="utf-8").split("## Treść artykułu", 1)[-1].strip()
lines = len((OUT / name).read_text(encoding="utf-8").splitlines())
ok = len(body) > 3000 or lines > 150
print(f"{'OK' if ok else 'STUB'} {name} ({len(body)} chars, {lines} lines)")
