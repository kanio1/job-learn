#!/usr/bin/env bash
# Generate gitignored mkcert material for the TLS lab overlay.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TLS_DIR="$REPO_ROOT/infra/tls"
export PATH="$HOME/.local/bin:$PATH"

if ! command -v mkcert >/dev/null 2>&1; then
  echo "mkcert is required (https://github.com/FiloSottile/mkcert). Also check ~/.local/bin." >&2
  exit 1
fi

if ! mkcert -install; then
  echo "Could not install the local CA into the system trust store (sudo). Certs will still be written; Playwright TLS POM uses ignoreHTTPSErrors." >&2
fi
mkdir -p "$TLS_DIR"
cd "$TLS_DIR"
mkcert -cert-file cert.pem -key-file key.pem \
  app.payment-quality.local \
  api.payment-quality.local \
  auth.payment-quality.local \
  localhost \
  127.0.0.1
cp "$(mkcert -CAROOT)/rootCA.pem" "$TLS_DIR/rootCA.pem"

cat <<'EOF'
Wrote infra/tls/cert.pem and infra/tls/key.pem (gitignored).

Add to /etc/hosts if missing:
  127.0.0.1 app.payment-quality.local api.payment-quality.local auth.payment-quality.local

Then: scripts/dev-stack.sh --tls
EOF
