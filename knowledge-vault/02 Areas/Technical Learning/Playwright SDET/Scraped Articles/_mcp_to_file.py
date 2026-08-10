#!/usr/bin/env python3
"""Save MCP scrape JSON from file argument and run persist pipeline."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    if len(sys.argv) < 3:
        print("Usage: _mcp_to_file.py <slug> <jsonfile>", file=sys.stderr)
        sys.exit(1)
    slug, jsonfile = sys.argv[1], Path(sys.argv[2])
    data = json.loads(jsonfile.read_text(encoding="utf-8"))
    r = subprocess.run(
        [sys.executable, str(OUT / "_persist_mcp.py"), slug],
        input=json.dumps(data),
        capture_output=True,
        text=True,
        cwd=OUT,
    )
    print(r.stdout.strip() or r.stderr.strip())
    sys.exit(r.returncode)


if __name__ == "__main__":
    main()
