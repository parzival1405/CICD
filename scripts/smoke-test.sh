#!/usr/bin/env bash
# Smoke test nghiep vu co ban sau khi deploy.
# Cach dung: smoke-test.sh <base-url>
set -euo pipefail

BASE_URL="${1:?Cach dung: smoke-test.sh <base-url>}"
OWNER="smoke-test-$(date +%s)"

fail() { echo "SMOKE FAIL: $*" >&2; exit 1; }

call() {
  # call <method> <path> [json-body] -> in body, luu HTTP status vao bien STATUS
  local method="$1" path="$2" body="${3:-}" response
  if [[ -n "$body" ]]; then
    response=$(curl -sS -X "$method" "$BASE_URL$path" -H 'Content-Type: application/json' -d "$body" -w '\n%{http_code}')
  else
    response=$(curl -sS -X "$method" "$BASE_URL$path" -w '\n%{http_code}')
  fi
  STATUS="${response##*$'\n'}"
  BODY="${response%$'\n'*}"
}

call GET /actuator/health/readiness
[[ "$STATUS" == 200 ]] || fail "readiness tra $STATUS"

call POST /api/accounts "{\"owner\":\"$OWNER\"}"
[[ "$STATUS" == 201 ]] || fail "tao tai khoan tra $STATUS: $BODY"
ID=$(jq -r '.id' <<< "$BODY")

call POST "/api/accounts/$ID/deposit" '{"amount":1000}'
[[ "$STATUS" == 200 ]] || fail "nap tien tra $STATUS: $BODY"
[[ $(jq -r '.balance' <<< "$BODY") == 1000* ]] || fail "so du sau nap sai: $BODY"

call POST "/api/accounts/$ID/withdraw" '{"amount":400}'
[[ "$STATUS" == 200 ]] || fail "rut tien tra $STATUS: $BODY"
[[ $(jq -r '.balance' <<< "$BODY") == 600* ]] || fail "so du sau rut sai: $BODY"

call POST "/api/accounts/$ID/withdraw" '{"amount":10000}'
[[ "$STATUS" == 409 ]] || fail "rut qua so du phai tra 409, nhan $STATUS"

call GET /actuator/info
echo "Phien ban: $(jq -r '.app.version // "khong ro"' <<< "$BODY")"
echo "SMOKE OK ($BASE_URL, tai khoan test $ID)"
