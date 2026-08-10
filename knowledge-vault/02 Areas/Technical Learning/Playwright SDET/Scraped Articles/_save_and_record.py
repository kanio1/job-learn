#!/usr/bin/env python3
"""Save Firecrawl JSON result and record progress."""
import json
import re
import sys
from pathlib import Path

from _save_scrape import save_scrape, extract_title, extract_author, extract_date

OUT_DIR = Path(__file__).parent
PROGRESS_FILE = OUT_DIR / "_progress.json"


def record(url: str, status: str, filename: str = "", error: str = "", title: str = "", author: str = "", date: str = ""):
    progress = {}
    if PROGRESS_FILE.exists():
        progress = json.loads(PROGRESS_FILE.read_text())
    progress.setdefault("results", {})[url] = {
        "status": status,
        "filename": filename,
        "error": error,
        "title": title,
        "author": author,
        "date": date,
    }
    PROGRESS_FILE.write_text(json.dumps(progress, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    out_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else OUT_DIR
    data = json.load(sys.stdin)
    metadata = data.get("metadata", {})
    url = metadata.get("url") or metadata.get("sourceURL") or ""
    try:
        path = save_scrape(data, out_dir)
        title = extract_title(metadata)
        author = extract_author(metadata, data.get("markdown", ""))
        date = extract_date(metadata)
        record(url, "ok", path.name, title=title, author=author, date=date)
        print(path)
    except Exception as e:
        record(url, "failed", error=str(e))
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)
