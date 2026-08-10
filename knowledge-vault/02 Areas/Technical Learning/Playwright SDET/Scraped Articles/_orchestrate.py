#!/usr/bin/env python3
"""Orchestrate Playwright article scraping via Firecrawl MCP results.

Usage:
  python3 _orchestrate.py pending          # list URLs still to scrape
  python3 _orchestrate.py save <jsonfile>  # save one MCP scrape result
  python3 _orchestrate.py index            # build INDEX.md
  python3 _orchestrate.py status           # show counts
"""
import json
import re
import sys
from pathlib import Path

OUT_DIR = Path(__file__).parent
URLS_FILE = OUT_DIR / "_urls.txt"
PROGRESS_FILE = OUT_DIR / "_progress.json"


def load_urls():
    return [ln.strip() for ln in URLS_FILE.read_text().splitlines() if ln.strip()]


def load_progress():
    if PROGRESS_FILE.exists():
        return json.loads(PROGRESS_FILE.read_text(encoding="utf-8"))
    return {"results": {}}


def get_saved_urls():
    saved = set()
    for md in OUT_DIR.glob("*.md"):
        if md.name.startswith("_") or md.name == "INDEX.md":
            continue
        m = re.search(r'^url:\s*"(.+)"', md.read_text(encoding="utf-8"), re.M)
        if m:
            saved.add(m.group(1))
    return saved


def pending():
    saved = get_saved_urls()
    prog = load_progress()
    done = saved | {u for u, v in prog.get("results", {}).items() if v.get("status") in ("ok", "failed")}
    return [u for u in load_urls() if u not in done]


def save_json(json_path: Path):
    sys.path.insert(0, str(OUT_DIR))
    from _save_and_record import record
    from _save_scrape import save_scrape, extract_title, extract_author, extract_date

    data = json.loads(json_path.read_text(encoding="utf-8"))
    metadata = data.get("metadata", {})
    url = metadata.get("url") or metadata.get("sourceURL") or ""
    try:
        path = save_scrape(data, OUT_DIR)
        record(url, "ok", path.name,
               title=extract_title(metadata),
               author=extract_author(metadata, data.get("markdown", "")),
               date=extract_date(metadata))
        print(path.name)
        return path.name
    except Exception as e:
        record(url, "failed", error=str(e))
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)


def build_index():
    urls = load_urls()
    prog = load_progress()
    lines = [
        "# Playwright Articles Index",
        "",
        f"Total URLs: {len(urls)}",
        "",
        "| # | Title | Author | Date | Filename | Status |",
        "|---|-------|--------|------|----------|--------|",
    ]
    ok = fail = 0
    for i, url in enumerate(urls, 1):
        info = prog.get("results", {}).get(url, {})
        if info.get("status") == "failed":
            pass
        elif not info or info.get("status") != "ok":
            for md in OUT_DIR.glob("*.md"):
                if md.name.startswith("_") or md.name == "INDEX.md":
                    continue
                text = md.read_text(encoding="utf-8")
                if f'url: "{url}"' in text:
                    tm = re.search(r'^title:\s*"(.+)"', text, re.M)
                    am = re.search(r'^author:\s*"(.+)"', text, re.M)
                    dm = re.search(r'^date:\s*"(.+)"', text, re.M)
                    info = {"status": "ok", "filename": md.name,
                            "title": tm.group(1) if tm else "—",
                            "author": am.group(1) if am else "—",
                            "date": dm.group(1) if dm else "—"}
                    break
        status = info.get("status", "pending")
        if status == "ok":
            ok += 1
        elif status == "failed":
            fail += 1
        lines.append(
            f"| {i} | {info.get('title', '—')} | {info.get('author', '—')} | "
            f"{info.get('date', '—')} | {info.get('filename', '—')} | {status} |"
        )
    lines.extend(["", f"**Summary:** {ok} ok, {fail} failed, {len(urls) - ok - fail} pending"])
    idx = OUT_DIR / "INDEX.md"
    idx.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(idx)
    return ok, fail, len(urls)


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "status"
    if cmd == "pending":
        for u in pending():
            print(u)
    elif cmd == "save" and len(sys.argv) > 2:
        save_json(Path(sys.argv[2]))
    elif cmd == "index":
        build_index()
    elif cmd == "status":
        urls = load_urls()
        saved = get_saved_urls()
        pend = pending()
        print(f"total={len(urls)} saved={len(saved)} pending={len(pend)}")
