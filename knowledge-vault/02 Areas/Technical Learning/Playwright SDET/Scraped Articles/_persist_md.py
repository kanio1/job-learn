#!/usr/bin/env python3
"""Persist scrape from markdown file + metadata args.

Usage:
  python3 _persist_md.py <mdfile> <url> <title> <author> <date>
"""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    if len(sys.argv) < 6:
        print("Usage: python3 _persist_md.py <mdfile> <url> <title> <author> <date>", file=sys.stderr)
        sys.exit(1)
    mdfile, url, title, author, date = sys.argv[1:6]
    md = Path(mdfile).read_text(encoding="utf-8")
    meta = {
        "url": url,
        "sourceURL": url,
        "title": title,
        "author": author,
        "article:published_time": date,
        "last-updated": date,
    }
    data = {"markdown": md, "metadata": meta}
    p = OUT / "_scrapes" / "last.json"
    p.parent.mkdir(exist_ok=True)
    p.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    subprocess.run([sys.executable, str(OUT / "persist.py"), str(p)], cwd=OUT, check=True)


if __name__ == "__main__":
    main()
