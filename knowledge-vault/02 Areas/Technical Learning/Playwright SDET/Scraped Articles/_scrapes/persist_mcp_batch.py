#!/usr/bin/env python3
"""Persist MCP scrape dicts passed as JSON files in batch dir."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent.parent
SCRAPE = Path(__file__).parent)


def save(data: dict) -> str:
    jpath = SCRAPE / "last-batch.json"
    jpath.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    r = subprocess.run(
        ["python3", str(OUT / "_orchestrate.py"), "save", str(jpath)],
        capture_output=True,
        text=True,
        cwd=str(OUT),
    )
    if r.returncode:
        raise RuntimeError(r.stderr.strip())
    return r.stdout.strip()


if __name__ == "__main__":
    for path in sys.argv[1:]:
        data = json.loads(Path(path).read_text(encoding="utf-8"))
        print(f"OK {path}: {save(data)}")
