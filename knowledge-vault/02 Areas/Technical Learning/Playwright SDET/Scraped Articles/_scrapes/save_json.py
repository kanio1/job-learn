#!/usr/bin/env python3
"""Save scrape dict passed as JSON file path."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent.parent
SCRAPE = Path(__file__).parent

jpath = Path(sys.argv[1])
data = json.loads(jpath.read_text(encoding="utf-8"))
slug = sys.argv[2] if len(sys.argv) > 2 else jpath.stem
out = SCRAPE / f"{slug}.json"
out.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
r = subprocess.run(
    ["python3", str(OUT / "_orchestrate.py"), "save", str(out)],
    capture_output=True,
    text=True,
    cwd=str(OUT),
)
if r.returncode:
    print(r.stderr, file=sys.stderr)
    sys.exit(1)
print(r.stdout.strip())
