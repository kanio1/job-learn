#!/usr/bin/env python3
"""Assemble markdown + metadata JSON and run _orchestrate.py save."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent.parent


def main():
    if len(sys.argv) < 4:
        print("Usage: save_from_md.py <slug> <md-file> <url> [title] [author] [date]", file=sys.stderr)
        sys.exit(1)

    slug, md_path, url = sys.argv[1:4]
    title = sys.argv[4] if len(sys.argv) > 4 else ""
    author = sys.argv[5] if len(sys.argv) > 5 else "Unknown"
    date = sys.argv[6] if len(sys.argv) > 6 else "Unknown"

    markdown = Path(md_path).read_text(encoding="utf-8")
    metadata = {
        "url": url,
        "sourceURL": url,
        "og:url": url,
        "title": title,
        "ogTitle": title,
        "author": author,
        "article:published_time": date,
        "publishedTime": date,
        "statusCode": 200,
    }
    jpath = OUT / "_scrapes" / f"{slug}.json"
    jpath.write_text(json.dumps({"markdown": markdown, "metadata": metadata}, ensure_ascii=False, indent=2), encoding="utf-8")

    result = subprocess.run(
        ["python3", str(OUT / "_orchestrate.py"), "save", str(jpath)],
        capture_output=True,
        text=True,
        cwd=str(OUT),
    )
    if result.returncode:
        print(result.stderr, file=sys.stderr)
        sys.exit(1)
    print(result.stdout.strip())


if __name__ == "__main__":
    main()
