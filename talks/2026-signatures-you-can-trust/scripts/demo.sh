#!/usr/bin/env bash
# The demo, scripted: the same four requests from demo.md against the running spring-boot-sample.
# Used live (bash scripts/demo.sh) and to record the fallback (scripts/record-demo.sh).
set -u
BASE="${DEMO_BASE_URL:-http://localhost:8080}"
TYPE_DELAY="${DEMO_TYPE_DELAY:-0.03}"   # seconds per character when "typing" a command
PAUSE="${DEMO_PAUSE:-1.6}"              # seconds to read each response

bold=$'\e[1m'; dim=$'\e[2m'; blue=$'\e[38;2;88;166;255m'; amber=$'\e[38;2;210;153;34m'; reset=$'\e[0m'

say()   { printf '%s# %s%s\n' "$dim" "$1" "$reset"; }
type_() { printf '%s$%s ' "$blue" "$reset"; for ((i = 0; i < ${#1}; i++)); do printf '%s' "${1:i:1}"; sleep "$TYPE_DELAY"; done; printf '\n'; }
run()   { type_ "$1"; eval "$1"; printf '\n'; sleep "$PAUSE"; }

clear
printf '%s%sdmx-fun · spring-boot-sample%s  %sa controller that returns Result and Option%s\n\n' "$bold" "$blue" "$reset" "$dim" "$reset"
sleep 1

say "Result.ok(item): 200 with the value as the body"
run "curl -s -X POST $BASE/items -H 'Content-Type: application/json' -d '{\"name\":\"Widget\",\"description\":\"A fine widget\"}' -w '\n%{http_code}\n' | sed '\$s/^/HTTP /'"

say "Result.err(message): 500 with the message as the body, no exception handler involved"
run "curl -s -X POST $BASE/items -H 'Content-Type: application/json' -d '{\"name\":\"\",\"description\":\"x\"}' -w '\n%{http_code}\n' | sed '\$s/^/HTTP /'"

say "Option.none(): 404 with an empty body"
run "curl -s -o /dev/null -w 'HTTP %{http_code}\n' $BASE/items/-1"

say "Result<NonEmptyList<Item>, String>: the list is a plain JSON array, serialized by DmxFunModule"
run "curl -s '$BASE/items/search?name=Widget' | jq -c ."

printf '%s%s✓%s %sfour status codes, zero hand-written mappings%s\n' "$bold" "$blue" "$reset" "$dim" "$reset"
sleep 2
