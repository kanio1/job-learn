#!/usr/bin/env python3
"""Scrape URLs via Firecrawl API and save via orchestrate."""
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

OUT = Path(__file__).parent.parent
SCRAPE = Path(__file__).parent
API = "https://api.firecrawl.dev/v1/scrape"


def scrape(url: str, api_key: str) -> dict:
    body = json.dumps({
        "url": url,
        "formats": ["markdown"],
        "onlyMainContent": True,
    }).encode()
    req = urllib.request.Request(
        API,
        data=body,
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        payload = json.loads(resp.read().decode())
    if not payload.get("success"):
        raise RuntimeError(payload.get("error") or "scrape failed")
    return payload["data"]


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


def slug_from_url(url: str) -> str:
    from urllib.parse import urlparse
    p = urlparse(url)
    slug = p.path.strip("/").replace("/", "-") or "article"
    return slug[:60]


def main():
    api_key = os.environ.get("FIRECRAWL_API_KEY")
    if not api_key:
        # Try common MCP env locations
        for env_file in [
            Path.home() / ".cursor" / "mcp.json",
            Path.home() / ".config" / "cursor" / "mcp.json",
        ]:
            if env_file.exists():
                cfg = json.loads(env_file.read_text())
                for srv in cfg.get("mcpServers", {}).values():
                    env = srv.get("env", {})
                    if env.get("FIRECRAWL_API_KEY"):
                        api_key = env["FIRECRAWL_API_KEY"]
                        break
    if not api_key:
        print("FIRECRAWL_API_KEY not found", file=sys.stderr)
        sys.exit(1)

    urls = sys.argv[1:] if len(sys.argv) > 1 else []
    if not urls:
        urls = [ln.strip() for ln in (OUT / "_urls.txt").read_text().splitlines() if ln.strip()]
        # filter pending
        r = subprocess.run(
            ["python3", str(OUT / "_orchestrate.py"), "pending"],
            capture_output=True,
            text=True,
            cwd=str(OUT),
        )
        urls = [u for u in r.stdout.strip().split("\n") if u]

    for i, url in enumerate(urls):
        slug = slug_from_url(url)
        print(f"[{i+1}/{len(urls)}] {url}")
        for attempt in range(3):
            try:
                data = scrape(url, api_key)
                name = save_data(data, slug)
                print(f"  OK -> {name}")
                break
            except urllib.error.HTTPError as e:
                if e.code == 429:
                    print("  429 rate limit, waiting 30s...")
                    time.sleep(30)
                else:
                    body = e.read().decode()[:200]
                    print(f"  FAIL HTTP {e.code}: {body}", file=sys.stderr)
                    break
            except Exception as e:
                print(f"  FAIL: {e}", file=sys.stderr)
                break
        if i < len(urls) - 1:
            time.sleep(6)  # pace requests


if __name__ == "__main__":
    main()
