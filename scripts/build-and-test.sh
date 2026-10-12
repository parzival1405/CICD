#!/usr/bin/env bash
# Thêm code Bước 5 - mục 6 "Thực hành từng bước", Bài 06 Giai đoạn 1 (bài tập giai đoạn 1)
# Build va chay toan bo test cho wallet-service; ghi log vao build.log.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_FILE="$PROJECT_DIR/build.log"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

fail() {
  log "LOI: $*"
  exit 1
}

on_exit() {
  local code=$?
  if [[ $code -eq 0 ]]; then
    log "KET QUA: THANH CONG"
  else
    log "KET QUA: THAT BAI (exit code $code) - xem $LOG_FILE"
  fi
}
trap on_exit EXIT

: > "$LOG_FILE"
cd "$PROJECT_DIR"

log "Kiem tra moi truong"
command -v java > /dev/null 2>&1 || fail "Khong tim thay java trong PATH"
[[ -x ./mvnw ]] || fail "Khong tim thay ./mvnw hoac thieu quyen thuc thi"
log "Java: $(java -version 2>&1 | head -1)"

start=$(date +%s)
log "Chay: ./mvnw -B -ntp clean verify"
./mvnw -B -ntp clean verify 2>&1 | tee -a "$LOG_FILE"
end=$(date +%s)

jar=$(find target -maxdepth 1 -name '*.jar' | head -1)
[[ -n "$jar" ]] || fail "Khong tim thay file jar trong target/"

log "Jar: $jar ($(du -h "$jar" | cut -f1))"
log "Tom tat test:"
grep -E 'Tests run: [0-9]+' "$LOG_FILE" | grep -v 'Time elapsed' | tee -a "$LOG_FILE" || true
log "Thoi gian build: $((end - start))s"
