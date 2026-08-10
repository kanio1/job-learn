#!/usr/bin/env python3
"""Write MCP scrape JSON files and run orchestrate save."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent.parent
SCRAPE_DIR = Path(__file__).parent


def save_and_record(slug: str, data: dict):
    path = SCRAPE_DIR / f"{slug}.json"
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    result = subprocess.run(
        ["python3", str(OUT / "_orchestrate.py"), "save", str(path)],
        capture_output=True,
        text=True,
        cwd=str(OUT),
    )
    if result.returncode != 0:
        print(f"ERROR {slug}: {result.stderr}", file=sys.stderr)
        return False
    print(f"OK {slug}: {result.stdout.strip()}")
    return True


if __name__ == "__main__":
    # Load batch file passed as argument: JSON array of {slug, data}
    batch = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    ok = 0
    for item in batch:
        if save_and_record(item["slug"], item["data"]):
            ok += 1
    print(f"Saved {ok}/{len(batch)}")
