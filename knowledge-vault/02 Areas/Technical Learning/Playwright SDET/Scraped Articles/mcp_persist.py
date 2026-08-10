#!/usr/bin/env python3
"""Persist MCP scrape from JSON file or stdin."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    if len(sys.argv) > 1:
        data = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    else:
        data = json.load(sys.stdin)
    p = OUT / "_scrapes" / "last.json"
    p.parent.mkdir(exist_ok=True)
    p.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    subprocess.run([sys.executable, str(OUT / "persist.py"), str(p)], cwd=OUT, check=True)


if __name__ == "__main__":
    main()
