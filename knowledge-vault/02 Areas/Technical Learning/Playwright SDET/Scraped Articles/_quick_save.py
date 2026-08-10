#!/usr/bin/env python3
"""Persist Firecrawl scrape: python3 _quick_save.py <slug> <json_file>"""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    slug, src = sys.argv[1], Path(sys.argv[2])
    data = json.loads(src.read_text(encoding="utf-8"))
    r = subprocess.run(
        [sys.executable, str(OUT / "_persist_mcp.py"), slug],
        input=json.dumps(data, ensure_ascii=False),
        capture_output=True,
        text=True,
        cwd=OUT,
    )
    print(r.stdout.strip() or r.stderr.strip())
    sys.exit(r.returncode)


if __name__ == "__main__":
    main()
