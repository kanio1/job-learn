#!/usr/bin/env python3
"""Save MCP scrape dict from a Python module exporting DATA."""
import importlib.util
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent.parent
SCRAPE = Path(__file__).parent


def main():
    mod_path = Path(sys.argv[1])
    slug = sys.argv[2] if len(sys.argv) > 2 else mod_path.stem
    spec = importlib.util.spec_from_file_location("scrapedata", mod_path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    data = mod.DATA
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
