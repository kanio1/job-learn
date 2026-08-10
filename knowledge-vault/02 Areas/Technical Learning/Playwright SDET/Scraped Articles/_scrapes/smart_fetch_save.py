#!/usr/bin/env python3
"""Fetch article content from public APIs where possible and save via orchestrate."""
import json
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from urllib.parse import urlparse

OUT = Path(__file__).parent.parent
SCRAPE = Path(__file__).parent


def http_json(url: str) -> dict:
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode())


def fetch_devto(url: str) -> dict:
    m = re.search(r"dev\.to/([^/]+)/([^/?#]+)", url)
    if not m:
        raise ValueError("not dev.to article")
    api = f"https://dev.to/api/articles/{m.group(1)}/{m.group(2)}"
    d = http_json(api)
    title = d.get("title", "Untitled")
    author = (d.get("user") or {}).get("name", "Unknown")
    published = d.get("published_at", "Unknown")
    return {
        "markdown": d.get("body_markdown", ""),
        "metadata": {
            "url": url,
            "sourceURL": url,
            "og:url": url,
            "title": title,
            "ogTitle": title,
            "author": author,
            "publishedTime": published,
            "article:published_time": published,
            "statusCode": 200,
            "contentType": "application/json",
        },
    }


def fetch_ghost(url: str) -> dict:
    path = urlparse(url).path.strip("/")
    slug = path.split("/")[-1] if path else ""
    base = url.split("/")[0] + "//" + url.split("/")[2]
    api = f"{base}/ghost/api/content/posts/slug/{slug}/?include=authors"
    d = http_json(api)
    posts = d.get("posts") or []
    if not posts:
        raise ValueError("no ghost post")
    post = posts[0]
    title = post.get("title", "Untitled")
    authors = post.get("authors") or []
    author = authors[0].get("name", "Unknown") if authors else "Unknown"
    published = post.get("published_at", "Unknown")
    html = post.get("html", "")
    # Ghost returns HTML; use as markdown fallback (better than empty)
    return {
        "markdown": post.get("plaintext") or html,
        "metadata": {
            "url": url,
            "sourceURL": url,
            "og:url": url,
            "title": title,
            "ogTitle": title,
            "author": author,
            "publishedTime": published,
            "article:published_time": published,
            "statusCode": 200,
        },
    }


def save_data(data: dict, slug: str) -> str:
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


def slugify(title: str) -> str:
    t = re.sub(r"[^\w\s-]", "", title.lower())
    t = re.sub(r"[-\s]+", "-", t).strip("-")
    return t[:80] or "article"


def fetch_url(url: str) -> dict:
    if "dev.to/" in url:
        return fetch_devto(url)
    if "blog.martioli.com/" in url or "martioli.com/" in url:
        try:
            return fetch_ghost(url)
        except Exception:
            pass
    raise ValueError(f"No API fetcher for {url}")


def main():
    urls = sys.argv[1:] if len(sys.argv) > 1 else []
    if not urls:
        r = subprocess.run(
            ["python3", str(OUT / "_orchestrate.py"), "pending"],
            capture_output=True,
            text=True,
            cwd=str(OUT),
        )
        urls = [u for u in r.stdout.strip().split("\n") if u]

    ok, fail = 0, 0
    for url in urls:
        try:
            data = fetch_url(url)
            if len(data.get("markdown", "")) < 500:
                print(f"SKIP thin: {url}")
                fail += 1
                continue
            slug = slugify(data["metadata"].get("title", url))
            name = save_data(data, slug)
            print(f"OK {url} -> {name}")
            ok += 1
        except Exception as e:
            print(f"FAIL {url}: {e}", file=sys.stderr)
            fail += 1
        time.sleep(1)
    print(f"Done: {ok} ok, {fail} fail")


if __name__ == "__main__":
    main()
