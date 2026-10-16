#!/usr/bin/env bash
# Blue-Green / Canary cho moi truong chua script nay.
#   bluegreen.sh stage <tag>    trien khai tag len mau dang ranh (chua nhan traffic)
#   bluegreen.sh canary <1-99>  chuyen <phan tram> traffic sang mau ranh
#   bluegreen.sh switch         chuyen 100% traffic sang mau ranh (cung dung de rollback)
#   bluegreen.sh abort          dung mau ranh, traffic ve 100% mau dang chay
#   bluegreen.sh status
set -euo pipefail

cd "$(dirname "$(readlink -f "$0")")"
[[ -f .env ]] || { echo "Thieu .env trong $PWD" >&2; exit 2; }
set -a; . ./.env; set +a
: "${APP_PORT:?Thieu APP_PORT trong .env}"

ENV_NAME="$(basename "$PWD")"
PROJECT="wallet-$ENV_NAME"

log() { echo "[$(date '+%F %T')] [$ENV_NAME] $*" | tee -a deploy.log; }
tag_of() { cat "$1_tag" 2>/dev/null || echo none; }
active() { cat active_color 2>/dev/null || echo none; }
idle() { if [[ "$(active)" == "blue" ]]; then echo green; else echo blue; fi; }
preview_port() { if [[ "$1" == "blue" ]]; then echo 8091; else echo 8092; fi; }

compose() {
  BLUE_TAG="$(tag_of blue)" GREEN_TAG="$(tag_of green)" \
    docker compose --project-name "$PROJECT" -f compose.bluegreen.yaml "$@"
}

wait_ready() {
  local url attempt
  url="http://127.0.0.1:$(preview_port "$1")/actuator/health/readiness"
  for attempt in $(seq 1 40); do
    if curl -fsS "$url" > /dev/null 2>&1; then
      return 0
    fi
    sleep 3
  done
  return 1
}

backup_db() {
  local file
  if [[ -z "$(compose ps -q db)" ]]; then
    log "DB chua chay, bo qua backup"
    return 0
  fi
  mkdir -p backups && chmod 700 backups
  file="backups/${ENV_NAME}-$(date +%Y%m%d-%H%M%S).dump"
  compose exec -T db pg_dump -U wallet -d wallet -Fc > "$file"
  log "Backup DB: $file"
  ls -1t backups/*.dump | tail -n +11 | xargs -r rm --
}

write_nginx() {
  # write_nginx <mau-chinh> [<mau-canary> <phan-tram>]
  local main="$1" canary="${2:-}" percent="${3:-0}" servers
  if [[ -n "$canary" ]]; then
    servers="    server app-$main:8080 weight=$((100 - percent));
    server app-$canary:8080 weight=$percent;"
  else
    servers="    server app-$main:8080;"
  fi
  mkdir -p nginx
  cat > nginx/default.conf.tmp <<EOF
upstream wallet_backend {
$servers
}
server {
    listen 80;
    location / {
        proxy_pass http://wallet_backend;
        proxy_set_header Host \$host;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_next_upstream error timeout http_502 http_503;
    }
}
EOF
  if [[ -f nginx/default.conf ]]; then
    cp nginx/default.conf nginx/default.conf.bak
  fi
  mv nginx/default.conf.tmp nginx/default.conf
  if [[ -n "$(compose ps -q nginx)" ]]; then
    if ! compose exec -T nginx nginx -t; then
      [[ -f nginx/default.conf.bak ]] && mv nginx/default.conf.bak nginx/default.conf
      log "LOI: cau hinh nginx khong hop le, da khoi phuc cau hinh cu"
      return 1
    fi
    compose exec -T nginx nginx -s reload
  else
    compose up -d nginx
  fi
}

cmd_stage() {
  local tag="${1:-}" color old_tag
  [[ "$tag" =~ ^[A-Za-z0-9._-]{1,128}$ ]] || { echo "Tag khong hop le: '$tag'" >&2; exit 2; }
  color="$(idle)"
  old_tag="$(tag_of "$color")"
  backup_db
  echo "$tag" > "${color}_tag"
  log "Stage $tag len $color (preview: http://127.0.0.1:$(preview_port "$color"))"
  if ! compose pull "app-$color" || ! compose up -d "app-$color" || ! wait_ready "$color"; then
    log "LOI: $color voi $tag khong san sang"
    compose logs --tail 50 "app-$color" 2>&1 | tee -a deploy.log || true
    compose stop "app-$color" || true
    echo "$old_tag" > "${color}_tag"
    exit 1
  fi
  log "$color san sang voi $tag. Traffic CHUA chuyen."
}

cmd_canary() {
  local percent="${1:-}" target
  [[ "$percent" =~ ^[0-9]+$ && "$percent" -ge 1 && "$percent" -le 99 ]] || { echo "Phan tram 1-99" >&2; exit 2; }
  [[ "$(active)" != none ]] || { echo "Chua co mau nao dang nhan traffic" >&2; exit 2; }
  target="$(idle)"
  wait_ready "$target" || { log "LOI: $target chua san sang"; exit 1; }
  write_nginx "$(active)" "$target" "$percent"
  log "Canary: $percent% traffic -> $target ($(tag_of "$target"))"
}

cmd_switch() {
  local target
  target="$(idle)"
  [[ "$(tag_of "$target")" != none ]] || { echo "Mau $target chua co phien ban" >&2; exit 2; }
  wait_ready "$target" || { log "LOI: $target khong san sang, KHONG chuyen traffic"; exit 1; }
  write_nginx "$target"
  echo "$target" > active_color
  echo "$(date '+%F %T') $(tag_of "$target") ACTIVE ($target)" >> releases.log
  log "Traffic 100% -> $target ($(tag_of "$target"))"
}

cmd_abort() {
  local target
  target="$(idle)"
  if [[ "$(active)" != none ]]; then
    write_nginx "$(active)"
  fi
  compose stop "app-$target" || true
  log "Abort: dung $target, traffic 100% -> $(active)"
}

cmd_status() {
  echo "Active: $(active) | blue: $(tag_of blue) | green: $(tag_of green)"
  compose ps
}

if [[ "${1:-}" != "status" ]]; then
  exec 9> .deploy.lock
  flock -n 9 || { echo "Dang co thao tac deploy khac" >&2; exit 3; }
fi

case "${1:-}" in
  stage)  cmd_stage "${2:-}" ;;
  canary) cmd_canary "${2:-}" ;;
  switch) cmd_switch ;;
  abort)  cmd_abort ;;
  status) cmd_status ;;
  *) sed -n '2,7p' "$0"; exit 2 ;;
esac
