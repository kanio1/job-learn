#!/usr/bin/env python3
"""Save scrape by writing markdown+metadata files then merging."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent.parent
SCRAPE = Path(__file__).parent


def save(slug: str, url: str, title: str, author: str, date: str, markdown: str, extra_meta: dict | None = None):
    meta = {
        "url": url,
        "sourceURL": url,
        "og:url": url,
        "title": title,
        "ogTitle": title,
        "author": author,
        "publishedTime": date,
        "article:published_time": date,
        "statusCode": 200,
    }
    if extra_meta:
        meta.update(extra_meta)
    data = {"markdown": markdown, "metadata": meta}
    jpath = SCRAPE / f"{slug}.json"
    jpath.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    r = subprocess.run(
        ["python3", str(OUT / "_orchestrate.py"), "save", str(jpath)],
        capture_output=True,
        text=True,
        cwd=str(OUT),
    )
    if r.returncode:
        raise RuntimeError(r.stderr.strip())
    return r.stdout.strip()


if __name__ == "__main__":
    # slug url title author date markdown_file
    slug, url, title, author, date, md_file = sys.argv[1:7]
    md = Path(md_file).read_text(encoding="utf-8")
    print(save(slug, url, title, author, date, md))
