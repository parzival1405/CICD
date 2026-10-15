#!/usr/bin/env bash
# Trien khai wallet-service vao moi truong chua script nay (/opt/wallet/<env>).
# Cach dung: deploy.sh <image-tag>
set -euo pipefail

NEW_TAG="${1:?Cach dung: deploy.sh <image-tag>}"
if [[ ! "$NEW_TAG" =~ ^[A-Za-z0-9._-]{1,128}$ ]]; then
  echo "Tag khong hop le: $NEW_TAG" >&2
  exit 2
fi

cd "$(dirname "$(readlink -f "$0")")"
[[ -f .env ]] || { echo "Thieu file .env trong $PWD" >&2; exit 2; }
set -a; . ./.env; set +a
: "${APP_PORT:?Thieu APP_PORT trong .env}"

ENV_NAME="$(basename "$PWD")"
PROJECT="wallet-$ENV_NAME"
HEALTH_URL="http://127.0.0.1:${APP_PORT}/actuator/health/readiness"
PREV_TAG="$(cat current_tag 2>/dev/null || true)"

log() { echo "[$(date '+%F %T')] [$ENV_NAME] $*" | tee -a deploy.log; }
compose() { APP_TAG="$1" docker compose --project-name "$PROJECT" "${@:2}"; }

wait_healthy() {
  local attempt
  for attempt in $(seq 1 40); do
    if curl -fsS "$HEALTH_URL" > /dev/null 2>&1; then
      log "Healthy sau $attempt lan kiem tra"
      return 0
    fi
    sleep 3
  done
  return 1
}

exec 9> .deploy.lock
flock -n 9 || { echo "Dang co deploy khac chay o $ENV_NAME" >&2; exit 3; }

log "Bat dau deploy $NEW_TAG (hien tai: ${PREV_TAG:-chua co})"
compose "$NEW_TAG" pull app
compose "$NEW_TAG" up -d

if wait_healthy; then
  if [[ -n "$PREV_TAG" && "$PREV_TAG" != "$NEW_TAG" ]]; then
    echo "$PREV_TAG" > previous_tag
  fi
  echo "$NEW_TAG" > current_tag
  echo "$(date '+%F %T') $NEW_TAG OK" >> releases.log
  log "THANH CONG: $NEW_TAG dang chay"
  exit 0
fi

log "LOI: $NEW_TAG khong healthy. 50 dong log cuoi:"
compose "$NEW_TAG" logs --tail 50 app 2>&1 | tee -a deploy.log || true
echo "$(date '+%F %T') $NEW_TAG FAILED" >> releases.log

if [[ -n "$PREV_TAG" && "$PREV_TAG" != "$NEW_TAG" ]]; then
  log "Rollback ve $PREV_TAG"
  compose "$PREV_TAG" up -d
  if wait_healthy; then
    log "Rollback thanh cong, dang chay $PREV_TAG"
  else
    log "ROLLBACK THAT BAI - can xu ly thu cong (xem RUNBOOK)"
  fi
fi
exit 1
