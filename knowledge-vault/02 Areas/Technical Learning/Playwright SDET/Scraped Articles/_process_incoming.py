#!/usr/bin/env python3
"""Process all JSON files in _scrapes/incoming/ via persist pipeline."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent
INCOMING = OUT / "_scrapes" / "incoming"
PERSIST = OUT / "_persist_mcp.py"


def main():
    files = sorted(INCOMING.glob("*.json"))
    if not files:
        print("No incoming files")
        return
    results = []
    for f in files:
        slug = f.stem
        data = json.loads(f.read_text(encoding="utf-8"))
        r = subprocess.run(
            [sys.executable, str(PERSIST), slug],
            input=json.dumps(data),
            capture_output=True,
            text=True,
            cwd=OUT,
        )
        line = r.stdout.strip() or r.stderr.strip()
        print(line)
        results.append((slug, line, r.returncode))
        if r.returncode == 0:
            f.unlink()
    return results


if __name__ == "__main__":
    main()
