#!/usr/bin/env python3
"""Ingest MCP scrape JSON file(s) and save via pipeline."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent)


def ingest(path: Path, slug: str | None = None) -> None:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not data.get("markdown", "").strip():
        print(f"SKIP empty: {path.name}")
        return
    s = slug or path.stem
    r = subprocess.run(
        [sys.executable, str(OUT / "_mcp_store.py"), s],
        input=json.dumps(data, ensure_ascii=False),
        text=True,
        capture_output=True,
        cwd=OUT,
    )
    print(r.stdout or r.stderr, end="")


def main():
    for p in sys.argv[1:]:
        ingest(Path(p))


if __name__ == "__main__":
    main()
