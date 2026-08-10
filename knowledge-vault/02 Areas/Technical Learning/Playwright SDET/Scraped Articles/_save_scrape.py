#!/usr/bin/env python3
"""Save Firecrawl scrape JSON to formatted markdown file."""
import json
import re
import sys
from pathlib import Path
from urllib.parse import urlparse


def slugify(text: str, max_len: int = 80) -> str:
    text = re.sub(r"[^\w\s-]", "", text.lower())
    text = re.sub(r"[-\s]+", "-", text).strip("-")
    return text[:max_len] or "article"


def extract_author(metadata: dict, markdown: str) -> str:
    if metadata.get("author"):
        return metadata["author"]
    if metadata.get("article:author"):
        return metadata["article:author"]
    if metadata.get("og:author"):
        return metadata["og:author"]
    return "Unknown"


def extract_date(metadata: dict) -> str:
    for key in (
        "article:published_time",
        "publishedTime",
        "date",
        "last-updated",
        "article:modified_time",
    ):
        if metadata.get(key):
            return metadata[key]
    return "Unknown"


def extract_title(metadata: dict) -> str:
    title = metadata.get("title") or metadata.get("og:title") or metadata.get("ogTitle") or "Untitled"
    title = re.sub(r"\s*[-|]\s*(DEV Community|Medium|The Green Report.*)$", "", title, flags=re.I)
    return title.strip()


def save_scrape(data: dict, out_dir: Path) -> Path:
    metadata = data.get("metadata", {})
    markdown = data.get("markdown", "")
    title = extract_title(metadata)
    author = extract_author(metadata, markdown)
    date = extract_date(metadata)
    url = metadata.get("url") or metadata.get("sourceURL") or metadata.get("og:url") or ""

    slug = slugify(title)
    path = out_dir / f"{slug}.md"

    meta_json = json.dumps(metadata, indent=2, ensure_ascii=False)
    content = f"""---
title: "{title.replace('"', '\\"')}"
author: "{author.replace('"', '\\"')}"
date: "{date}"
url: "{url}"
---

# {title}

| Pole | Wartość |
|------|---------|
| **Tytuł** | {title} |
| **Autor** | {author} |
| **Data publikacji** | {date} |
| **URL** | {url} |

## Metadane

```json
{meta_json}
```

## Treść artykułu

{markdown}
"""
    path.write_text(content, encoding="utf-8")
    return path


if __name__ == "__main__":
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(".")
    data = json.load(sys.stdin)
    p = save_scrape(data, out)
    print(p)
