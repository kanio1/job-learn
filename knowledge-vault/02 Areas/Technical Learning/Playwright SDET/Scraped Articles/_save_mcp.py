#!/usr/bin/env python3
"""Save MCP scrape dict to markdown via persist.py. Usage: python3 _save_mcp.py <jsonfile>"""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    src = Path(sys.argv[1])
    data = json.loads(src.read_text(encoding="utf-8"))
    p = OUT / "_scrapes" / "last.json"
    p.parent.mkdir(exist_ok=True)
    p.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    subprocess.run([sys.executable, str(OUT / "persist.py"), str(p)], cwd=OUT, check=True)


if __name__ == "__main__":
    main()
