#!/usr/bin/env python3
"""Save multiple articles from predefined scrape data."""
import json
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).parent

ARTICLES = [
    {
        "metadata": {
            "url": "https://adequatica.medium.com/is-it-worth-mocking-websockets-by-playwright-e611cb016ec5",
            "sourceURL": "https://adequatica.medium.com/is-it-worth-mocking-websockets-by-playwright-e611cb016ec5",
            "title": "Is It Worth Mocking WebSockets by Playwright?",
            "author": "Andrey Enin",
            "article:published_time": "2024-11-05T22:10:59.071Z",
        },
        "markdown": """# Is It Worth Mocking WebSockets by Playwright?

Rather yes. WebSocket testing in automation is challenging due to connection handling, async communication, message formats, and tooling limitations.

## Evolution of testing WebSockets

Teams often progress from manual testing → console logging → HTTP API fallback → MSW mocking → Playwright WebSocketRoute (since v1.48).

## How to mock WebSockets with Playwright?

Use WebSocketRoute via page.routeWebSocket(). Key tips:
1. Call routeWebSocket() before page navigation
2. Use RegExp URL patterns (ws:// or wss://)
3. Playwright takes full control — WS won't appear in DevTools Network tab

Example intercepting page-to-server messages with ws.onMessage() and ws.send().

Playwright WebSocket mocking requires advanced skills but works well for simple applications.
""",
    },
]


def main():
    for art in ARTICLES:
        tmp = OUT / "_scrapes" / "last.json"
        tmp.write_text(json.dumps(art, ensure_ascii=False), encoding="utf-8")
        r = subprocess.run([sys.executable, str(OUT / "persist.py"), str(tmp)], capture_output=True, text=True)
        print(r.stdout.strip() or r.stderr.strip())


if __name__ == "__main__":
    main()
