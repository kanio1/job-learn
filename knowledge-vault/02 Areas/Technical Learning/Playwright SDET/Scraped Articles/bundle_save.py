#!/usr/bin/env python3
"""Save scrape from markdown file + metadata JSON."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    if len(sys.argv) < 2:
        print("Usage: bundle_save.py <slug>", file=sys.stderr)
        sys.exit(1)
    slug = sys.argv[1]
    md_path = OUT / "_scrapes" / "md" / f"{slug}.md"
    meta_path = OUT / "_scrapes" / "meta" / f"{slug}.json"
    if not md_path.exists() or not meta_path.exists():
        print(f"Missing {md_path} or {meta_path}", file=sys.stderr)
        sys.exit(1)
    data = {
        "markdown": md_path.read_text(encoding="utf-8"),
        "metadata": json.loads(meta_path.read_text(encoding="utf-8")),
    }
    tmp = OUT / "_scrapes" / "last.json"
    tmp.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    r = subprocess.run([sys.executable, str(OUT / "persist.py"), str(tmp)], capture_output=True, text=True)
    print(r.stdout.strip() or r.stderr.strip())
    sys.exit(r.returncode)


if __name__ == "__main__":
    main()
