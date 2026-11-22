#!/usr/bin/env bash
# Records scripts/demo.sh into demo.cast (asciicast v2, so every player can read it).
# Requires asciinema >= 3 and the sample running: ./gradlew :samples:spring-boot-sample:bootRun
set -euo pipefail
cd "$(dirname "$0")/.."
BASE="${DEMO_BASE_URL:-http://localhost:8080}"
if ! curl -sf "$BASE/items" >/dev/null; then
  echo "spring-boot-sample is not answering on $BASE; start it first (see demo.md)" >&2
  exit 1
fi
asciinema rec --overwrite --output-format asciicast-v2 --window-size 100x22 --idle-time-limit 3 \
  --title "dmx-fun: a controller that returns the Result" \
  --command "bash scripts/demo.sh" demo.cast
echo "recorded demo.cast ($(wc -c < demo.cast | tr -d ' ') bytes); play it with: pnpm demo:play"
