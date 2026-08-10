#!/usr/bin/env python3
"""Read full MCP JSON from stdin or file and save via orchestrate."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent.parent
SCRAPE = Path(__file__).parent


def main():
    slug = sys.argv[1] if len(sys.argv) > 1 else "scrape"
    if len(sys.argv) > 2:
        data = json.loads(Path(sys.argv[2]).read_text(encoding="utf-8"))
    else:
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


if __name__ == "__main__":
    main()
