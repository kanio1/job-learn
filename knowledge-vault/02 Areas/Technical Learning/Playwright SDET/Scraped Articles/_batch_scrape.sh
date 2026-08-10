#!/usr/bin/env bash
# Batch scrape via Firecrawl REST API (requires FIRECRAWL_API_KEY)
set -euo pipefail
OUT_DIR="$(cd "$(dirname "$0")" && pwd)"
URLS_FILE="$OUT_DIR/_urls.txt"
DELAY="${DELAY:-7}"

if [[ -z "${FIRECRAWL_API_KEY:-}" ]]; then
  echo "Set FIRECRAWL_API_KEY to use this script" >&2
  exit 1
fi

while IFS= read -r url || [[ -n "$url" ]]; do
  [[ -z "$url" || "$url" =~ ^# ]] && continue
  slug=$(echo "$url" | sed 's|https\?://||' | tr '/:?&=%' '-' | cut -c1-60)
  json_file="$OUT_DIR/_raw/${slug}.json"
  mkdir -p "$OUT_DIR/_raw"
  if [[ -f "$json_file" ]]; then
    echo "SKIP (cached): $url"
    python3 "$OUT_DIR/_save_scrape.py" "$OUT_DIR" < "$json_file"
    continue
  fi
  echo "SCRAPE: $url"
  http_code=$(curl -sS -o "$json_file" -w "%{http_code}" \
    -X POST "https://api.firecrawl.dev/v1/scrape" \
    -H "Authorization: Bearer $FIRECRAWL_API_KEY" \
    -H "Content-Type: application/json" \
    -d "{\"url\":\"$url\",\"formats\":[\"markdown\"],\"onlyMainContent\":true}")
  if [[ "$http_code" == "429" ]]; then
    echo "Rate limited, sleeping 35s..."
    rm -f "$json_file"
    sleep 35
    continue
  fi
  python3 "$OUT_DIR/_save_scrape.py" "$OUT_DIR" < "$json_file" || true
  sleep "$DELAY"
done < "$URLS_FILE"
