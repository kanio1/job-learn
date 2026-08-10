#!/usr/bin/env python3
"""Fix stub articles by persisting markdown files from _scrapes/md/."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent

FIXES = [
    ("allure-reports", "sdet-quick-introduction-to-allure-reports-with-playwright.md",
     "sdet-quick-introduction-to-allure-reports-with-playwright",
     {"title": "SDET: Quick introduction to Allure reports with Playwright",
      "og:title": "SDET: Quick introduction to Allure reports with Playwright",
      "author": "Kostiantyn Teltov",
      "article:published_time": "2024-02-18T09:27:39.763Z",
      "url": "https://medium.com/@dneprokos/sdet-quick-introduction-to-allure-reports-with-playwright-397d5c9986b5",
      "sourceURL": "https://medium.com/@dneprokos/sdet-quick-introduction-to-allure-reports-with-playwright-397d5c9986b5"}),
    ("advanced-snapshot", "advanced-snapshot-testing-in-playwright.md",
     "advanced-snapshot-testing",
     {"title": "Advanced Snapshot Testing in Playwright",
      "author": "Mike Stop Continues",
      "article:published_time": "2024-03-05T00:00:37.545Z",
      "url": "https://mikestopcontinues.hashnode.dev/advanced-snapshot-testing-in-playwright",
      "sourceURL": "https://mikestopcontinues.hashnode.dev/advanced-snapshot-testing-in-playwright"}),
    ("playwright-stories", "playwright-stories-navigating-tricky-ui-automation-scenarios-for-beginners.md",
     "playwright-stories",
     {"title": "Playwright stories: Navigating Tricky UI Automation Scenarios for Beginners",
      "author": "Kostiantyn Teltov",
      "article:published_time": "2024-01-24T06:18:44.466Z",
      "url": "https://medium.com/@dneprokos/playwright-stories-navigating-tricky-ui-automation-scenarios-for-beginners-7195e50486eb",
      "sourceURL": "https://medium.com/@dneprokos/playwright-stories-navigating-tricky-ui-automation-scenarios-for-beginners-7195e50486eb"}),
    ("playwright-standards", "our-playwright-testing-standards-at-houseful.md",
     "playwright-standards",
     {"title": "Our Playwright testing standards at Houseful",
      "author": "Boyana Staneva",
      "url": "https://www.houseful.blog/posts/2023/playwright-standards/",
      "sourceURL": "https://www.houseful.blog/posts/2023/playwright-standards/"}),
]

def persist(slug, data):
    r = subprocess.run(
        [sys.executable, str(OUT / "_persist_mcp.py"), slug],
        input=json.dumps(data, ensure_ascii=False),
        capture_output=True, text=True, cwd=OUT,
    )
    print(r.stdout.strip() or r.stderr.strip())

def main():
    md_dir = OUT / "_scrapes" / "md"
    for md_name, out_file, slug, meta in FIXES:
        md_path = md_dir / f"{md_name}.md"
        if not md_path.exists():
            print(f"SKIP missing {md_path.name}")
            continue
        data = {"markdown": md_path.read_text(encoding="utf-8"), "metadata": meta}
        persist(slug, data)

if __name__ == "__main__":
    main()
