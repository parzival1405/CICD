#!/usr/bin/env bash
# Sửa bài tập 3 (Khó) - mục 10 "Bài tập tự làm", Bài 03 Giai đoạn 0
# Rolling deploy thủ công lên 3 "server" giả (cổng 8081, 8082, 8083) trên Windows bằng Git Bash.
# Luôn giữ ít nhất 2 bản đang chạy: chỉ thay một cổng mỗi lần, và chỉ thay khi cả 3 cổng đang khỏe.
#
# Cách dùng (chạy trong thư mục wallet-service):
#   scripts/rolling-deploy.sh start <version>     chạy <version> trên cả 3 cổng
#   scripts/rolling-deploy.sh rolling <version>   thay lần lượt từng cổng bằng <version>, in bảng thời gian
#   scripts/rolling-deploy.sh status              xem cổng nào đang chạy bản nào
#   scripts/rolling-deploy.sh stop                dừng cả 3 cổng
#
# Yêu cầu: đã build jar của <version> (target/wallet-service-<version>.jar) hoặc đã có trong releases/.
set -euo pipefail

PORTS=(8081 8082 8083)
SERVER_DIR="/d/fake-server/wallet"
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# PID Windows của tiến trình đang LISTEN trên cổng (cột cuối của netstat)
# "|| true": cổng trống thì grep/curl trả mã lỗi; không có nó, set -e + pipefail làm script thoát im lặng
pid_on_port() {
  netstat -ano | grep -E "TCP +0\.0\.0\.0:$1 .*LISTENING" | awk '{print $5}' | head -1 || true
}

version_on_port() {
  curl -fsS "http://localhost:$1/actuator/info" 2>/dev/null \
    | sed -n 's/.*"version":"\([^"]*\)".*/\1/p' || true
}

count_up() {
  local n=0 p
  for p in "${PORTS[@]}"; do
    if curl -fsS -o /dev/null "http://localhost:$p/actuator/health" 2>/dev/null; then
      n=$((n + 1))
    fi
  done
  echo "$n"
}

# Copy jar vào releases/<version>/ (mỗi bản một thư mục, không ghi đè bản cũ)
release_jar() {
  local v="$1" src dir
  src="$PROJECT_DIR/target/wallet-service-$v.jar"
  dir="$SERVER_DIR/releases/$v"
  if [[ -f "$dir/wallet-service-$v.jar" ]]; then
    return 0
  fi
  [[ -f "$src" ]] || { echo "Khong tim thay $src - hay build ban $v truoc (./mvnw clean package)" >&2; exit 1; }
  mkdir -p "$dir"
  cp "$src" "$dir/"
}

start_on_port() {
  local v="$1" p="$2"
  mkdir -p "$SERVER_DIR/logs"
  nohup java -jar "$SERVER_DIR/releases/$v/wallet-service-$v.jar" --server.port="$p" \
    > "$SERVER_DIR/logs/$p.log" 2>&1 &
}

# Lưu ý: taskkill /F dừng ép (giống SIGKILL), không có graceful shutdown.
# Trên Linux sẽ dùng kill (SIGTERM) để Spring Boot xử lý nốt request - Giai đoạn 1, Bài 04.
stop_on_port() {
  local p="$1" pid
  pid="$(pid_on_port "$p")"
  if [[ -n "$pid" ]]; then
    taskkill //PID "$pid" //F > /dev/null
  fi
  while [[ -n "$(pid_on_port "$p")" ]]; do
    sleep 0.5
  done
}

wait_version() {
  local p="$1" v="$2" i
  for i in $(seq 1 60); do
    if [[ "$(version_on_port "$p")" == "$v" ]]; then
      return 0
    fi
    sleep 1
  done
  return 1
}

cmd_start() {
  local v="${1:?Thieu version}" p
  release_jar "$v"
  for p in "${PORTS[@]}"; do
    if [[ -n "$(pid_on_port "$p")" ]]; then
      echo "Cong $p dang duoc dung - bo qua"
      continue
    fi
    start_on_port "$v" "$p"
  done
  for p in "${PORTS[@]}"; do
    wait_version "$p" "$v" || { echo "LOI: cong $p khong chay duoc ban $v (xem $SERVER_DIR/logs/$p.log)" >&2; exit 1; }
  done
  cmd_status
}

cmd_rolling() {
  local v="${1:?Thieu version}" p old up t0 t1 t2 begin
  release_jar "$v"
  begin=$(date +%s)
  echo "| Cong | Ban cu -> moi | Dung ban cu (s) | Khoi dong ban moi (s) | So ban van chay trong luc thay |"
  echo "|---|---|---|---|---|"
  for p in "${PORTS[@]}"; do
    up="$(count_up)"
    if (( up < ${#PORTS[@]} )); then
      echo "DUNG LAI: chi co $up/${#PORTS[@]} ban dang chay - thay tiep se con duoi 2 ban" >&2
      exit 1
    fi
    old="$(version_on_port "$p")"
    t0=$(date +%s)
    stop_on_port "$p"
    t1=$(date +%s)
    start_on_port "$v" "$p"
    if ! wait_version "$p" "$v"; then
      echo "LOI: cong $p khong len ban $v - rollback cong nay ve $old" >&2
      stop_on_port "$p"
      start_on_port "$old" "$p"
      exit 1
    fi
    t2=$(date +%s)
    echo "| $p | $old -> $v | $((t1 - t0)) | $((t2 - t1)) | $((up - 1)) |"
  done
  echo
  echo "Tong thoi gian rolling: $(( $(date +%s) - begin ))s"
}

cmd_status() {
  local p v
  for p in "${PORTS[@]}"; do
    v="$(version_on_port "$p")"
    echo "Cong $p: ${v:-khong chay}"
  done
}

cmd_stop() {
  local p
  for p in "${PORTS[@]}"; do
    stop_on_port "$p"
  done
  echo "Da dung ca ${#PORTS[@]} cong"
}

case "${1:-}" in
  start)   cmd_start "${2:-}" ;;
  rolling) cmd_rolling "${2:-}" ;;
  status)  cmd_status ;;
  stop)    cmd_stop ;;
  *) sed -n '2,11p' "$0"; exit 2 ;;
esac
