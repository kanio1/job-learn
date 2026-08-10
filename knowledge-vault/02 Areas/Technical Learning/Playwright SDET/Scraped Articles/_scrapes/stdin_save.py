#!/usr/bin/env python3
"""Save MCP scrape JSON from stdin: python3 stdin_save.py <slug>"""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent.parent
SCRAPE = Path(__file__).parent

slug = sys.argv[1]
data = json.load(sys.stdin)
jpath = SCRAPE / f"{slug}.json"
jpath.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
r = subprocess.run(
    ["python3", str(OUT / "_orchestrate.py"), "save", str(jpath)],
    capture_output=True,
    text=True,
    cwd=str(OUT),
)
if r.returncode:
    print(r.stderr, file=sys.stderr)
    sys.exit(1)
print(r.stdout.strip())
