#!/usr/bin/env python3
"""Fetch HTML pages and extract article text for sites without public API."""
import json
import re
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

OUT = Path(__file__).parent.parent
SCRAPE = Path(__file__).parent


def fetch_html(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=90) as resp:
        return resp.read().decode("utf-8", errors="replace")


def html_to_markdown(html: str) -> str:
    m = re.search(r"<article[^>]*class=\"[^\"]*post-content[^\"]*\"[^>]*>(.*?)</article>", html, re.S)
    if not m:
        m = re.search(r"<article[^>]*>(.*?)</article>", html, re.S)
    body = m.group(1) if m else html
    text = re.sub(r"<script[^>]*>.*?</script>", "", body, flags=re.S)
    text = re.sub(r"<style[^>]*>.*?</style>", "", text, flags=re.S)
    text = re.sub(r"<h([1-6])[^>]*>(.*?)</h\1>", lambda m: f"\n{'#' * int(m.group(1))} {m.group(2)}\n", text, flags=re.S)
    text = re.sub(r"<li[^>]*>(.*?)</li>", r"\n- \1", text, flags=re.S)
    text = re.sub(r"<br\s*/?>", "\n", text, flags=re.I)
    text = re.sub(r"<p[^>]*>(.*?)</p>", r"\n\1\n", text, flags=re.S)
    text = re.sub(r"<pre[^>]*>(.*?)</pre>", r"\n```\n\1\n```\n", text, flags=re.S)
    text = re.sub(r"<code[^>]*>(.*?)</code>", r"`\1`", text, flags=re.S)
    text = re.sub(r"<[^>]+>", "", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def extract_meta(html: str, url: str) -> dict:
    title = "Untitled"
    m = re.search(r"<title[^>]*>(.*?)</title>", html, re.S | re.I)
    if m:
        title = re.sub(r"<[^>]+>", "", m.group(1)).strip()
    author = "Unknown"
    m = re.search(r'"author":\s*\{"@type":"Person","name":"([^"]+)"', html)
    if m:
        author = m.group(1)
    date = "Unknown"
    m = re.search(r'"datePublished":"([^"]+)"', html)
    if m:
        date = m.group(1)
    return {
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


def main():
    urls = sys.argv[1:] if len(sys.argv) > 1 else []
    ok, fail = 0, 0
    for url in urls:
        try:
            html = fetch_html(url)
            md = html_to_markdown(html)
            if len(md) < 800:
                print(f"SKIP thin {url} ({len(md)} chars)")
                fail += 1
                continue
            meta = extract_meta(html, url)
            slug = slugify(meta["title"])
            name = save_data({"markdown": md, "metadata": meta}, slug)
            print(f"OK {url} -> {name} ({len(md)} chars)")
            ok += 1
        except Exception as e:
            print(f"FAIL {url}: {e}", file=sys.stderr)
            fail += 1
        time.sleep(1)
    print(f"Done: {ok} ok, {fail} fail")


if __name__ == "__main__":
    main()
