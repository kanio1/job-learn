#!/usr/bin/env python3
"""Read MCP scrape JSON from stdin, save markdown article immediately."""
import json
import sys
from pathlib import Path

OUT = Path(__file__).parent
sys.path.insert(0, str(OUT))


def main():
    data = json.load(sys.stdin)
    from _save_json_files import save_data
    print(save_data(data))


if __name__ == "__main__":
    main()
