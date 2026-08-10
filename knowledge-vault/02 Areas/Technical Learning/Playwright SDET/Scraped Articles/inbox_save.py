#!/usr/bin/env python3
"""Save Firecrawl MCP JSON from a .json file in _scrapes/inbox/."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent
INBOX = OUT / "_scrapes" / "inbox"


def main():
    INBOX.mkdir(parents=True, exist_ok=True)
    files = sorted(INBOX.glob("*.json"))
    if not files:
        print("No inbox JSON files", file=sys.stderr)
        return
    for f in files:
        data = json.loads(f.read_text(encoding="utf-8"))
        tmp = OUT / "_scrapes" / "last.json"
        tmp.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
        r = subprocess.run([sys.executable, str(OUT / "persist.py"), str(tmp)], capture_output=True, text=True)
        print(r.stdout.strip() or r.stderr.strip())
        if r.returncode == 0:
            f.unlink()
        else:
            sys.exit(r.returncode)


if __name__ == "__main__":
    main()
