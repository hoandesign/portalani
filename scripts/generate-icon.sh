#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HTML="$ROOT/scripts/icon-generator.html"
OUT="$ROOT/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

if [[ ! -f "$HTML" ]]; then
  echo "Missing icon template: $HTML" >&2
  exit 1
fi

if [[ ! -x "$CHROME" ]]; then
  echo "Google Chrome not found at: $CHROME" >&2
  exit 1
fi

mkdir -p "$(dirname "$OUT")"

"$CHROME" \
  --headless=new \
  --disable-gpu \
  --hide-scrollbars \
  --window-size=512,512 \
  --screenshot="$OUT" \
  "file://$HTML"

echo "Wrote $OUT"
