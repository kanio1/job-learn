#!/usr/bin/env python3
"""Process _scrapes/raw/*.json (full MCP responses) into saved markdown."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent
RAW = OUT / "_scrapes" / "raw"
SCRAPES = OUT / "_scrapes"


def save(path: Path) -> None:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not data.get("markdown", "").strip():
        print(f"SKIP empty markdown: {path.name}")
        return
    dest = SCRAPES / path.name
    dest.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    r = subprocess.run(
        [sys.executable, str(OUT / "_orchestrate.py"), "save", str(dest)],
        capture_output=True,
        text=True,
        cwd=OUT,
    )
    if r.returncode:
        print(f"FAIL {path.name}: {r.stderr}", file=sys.stderr)
        return
    name = r.stdout.strip()
    body = (OUT / name).read_text(encoding="utf-8").split("## Treść artykułu", 1)[-1].strip()
    lines = len((OUT / name).read_text(encoding="utf-8").splitlines())
    ok = len(body) > 3000 or lines > 150
    print(f"{'OK' if ok else 'STUB'} {name} ({len(body)} chars, {lines} lines)")


def main():
    RAW.mkdir(parents=True, exist_ok=True)
    files = sorted(RAW.glob("*.json"))
    if not files:
        print("No raw JSON files in _scrapes/raw/")
        return
    for f in files:
        save(f)


if __name__ == "__main__":
    main()
