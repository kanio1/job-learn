#!/usr/bin/env python3
"""Write Firecrawl JSON to incoming/ and flush."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    slug = sys.argv[1]
    src = Path(sys.argv[2]) if len(sys.argv) > 2 else None
    if src:
        data = json.loads(src.read_text(encoding="utf-8"))
    else:
        data = json.load(sys.stdin)
    dest = OUT / "_scrapes" / "incoming" / f"{slug}.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    r = subprocess.run([sys.executable, str(OUT / "_flush_incoming.py")], cwd=OUT, capture_output=True, text=True)
    print(r.stdout.strip())
    if r.returncode:
        print(r.stderr, file=sys.stderr)
        sys.exit(r.returncode)


if __name__ == "__main__":
    main()
