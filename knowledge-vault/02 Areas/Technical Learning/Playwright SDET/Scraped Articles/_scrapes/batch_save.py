#!/usr/bin/env python3
"""Batch persist MCP scrape results from inline definitions."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent.parent
SCRAPE = Path(__file__).parent


def save_item(slug: str, markdown: str, metadata: dict) -> bool:
    data = {"markdown": markdown, "metadata": metadata}
    jpath = SCRAPE / f"{slug}.json"
    jpath.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    r = subprocess.run(
        ["python3", str(OUT / "_orchestrate.py"), "save", str(jpath)],
        capture_output=True,
        text=True,
        cwd=str(OUT),
    )
    if r.returncode:
        print(f"FAIL {slug}: {r.stderr.strip()}", file=sys.stderr)
        return False
    print(f"OK {slug}: {r.stdout.strip()}")
    return True


def load_batch(name: str):
    path = SCRAPE / f"{name}.json"
    if not path.exists():
        print(f"Missing batch file: {path}", file=sys.stderr)
        return 1
    items = json.loads(path.read_text(encoding="utf-8"))
    ok = sum(save_item(i["slug"], i["markdown"], i["metadata"]) for i in items)
    print(f"Batch {name}: {ok}/{len(items)} saved")
    return 0 if ok == len(items) else 1


if __name__ == "__main__":
    sys.exit(load_batch(sys.argv[1] if len(sys.argv) > 1 else "batch2"))
