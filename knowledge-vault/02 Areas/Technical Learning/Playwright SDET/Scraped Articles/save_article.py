#!/usr/bin/env python3
"""Combine _content/<slug>.md + _content/<slug>.meta.json and save via orchestrate."""
import json
import sys
from pathlib import Path

OUT = Path(__file__).parent


def main():
    slug = sys.argv[1]
    md = OUT / "_content" / f"{slug}.md"
    meta = OUT / "_content" / f"{slug}.meta.json"
    if not md.exists() or not meta.exists():
        print(f"Missing {md} or {meta}", file=sys.stderr)
        sys.exit(1)
    data = {
        "markdown": md.read_text(encoding="utf-8"),
        "metadata": json.loads(meta.read_text(encoding="utf-8")),
    }
    incoming = OUT / "_incoming.json"
    incoming.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    sys.path.insert(0, str(OUT))
    from _orchestrate import save_json
    print(save_json(incoming))


if __name__ == "__main__":
    main()
