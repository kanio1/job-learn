#!/usr/bin/env python3
"""Persist full MCP scrape JSON and run _orchestrate.py save."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent.parent
SCRAPE = Path(__file__).parent


def main():
    if len(sys.argv) < 2:
        print("Usage: save_mcp.py <json-file> [slug]", file=sys.stderr)
        sys.exit(1)

    jpath = Path(sys.argv[1])
    data = json.loads(jpath.read_text(encoding="utf-8"))
    result = subprocess.run(
        ["python3", str(OUT / "_orchestrate.py"), "save", str(jpath)],
        capture_output=True,
        text=True,
        cwd=str(OUT),
    )
    if result.returncode:
        print(result.stderr, file=sys.stderr)
        sys.exit(1)
    print(result.stdout.strip())


if __name__ == "__main__":
    main()
