#!/usr/bin/env python3
"""Persist scrape from md + meta files. Usage: persist_md_meta.py <slug> <md> <meta.json>"""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    slug, md_path, meta_path = sys.argv[1:4]
    data = {
        "markdown": Path(md_path).read_text(encoding="utf-8"),
        "metadata": json.loads(Path(meta_path).read_text(encoding="utf-8")),
    }
    r = subprocess.run(
        [sys.executable, str(OUT / "_persist_mcp.py"), slug],
        input=json.dumps(data, ensure_ascii=False),
        capture_output=True,
        text=True,
        cwd=OUT,
    )
    print(r.stdout.strip() or r.stderr.strip())
    sys.exit(r.returncode)


if __name__ == "__main__":
    main()
