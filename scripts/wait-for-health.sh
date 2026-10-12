#!/usr/bin/env bash
# Thêm code Bước 3 - mục 6 "Thực hành từng bước", Bài 06 Giai đoạn 1
# Cho den khi URL health tra ve HTTP 2xx, toi da MAX_ATTEMPTS lan.
set -euo pipefail

URL="${1:-http://localhost:8080/actuator/health}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-30}"
SLEEP_SECONDS="${SLEEP_SECONDS:-2}"

for attempt in $(seq 1 "$MAX_ATTEMPTS"); do
  if curl -fsS "$URL" > /dev/null 2>&1; then
    echo "OK: $URL healthy (lan thu $attempt)"
    exit 0
  fi
  echo "Chua healthy (lan thu $attempt/$MAX_ATTEMPTS), doi ${SLEEP_SECONDS}s..."
  sleep "$SLEEP_SECONDS"
done

echo "LOI: $URL khong healthy sau $((MAX_ATTEMPTS * SLEEP_SECONDS))s" >&2
exit 1
