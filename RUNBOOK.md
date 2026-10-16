<!-- Sửa bài tập 2 (Trung bình) - mục 10 "Bài tập tự làm", Bài 03 Giai đoạn 0: runbook deploy thủ công -->
# Runbook — Deploy thủ công wallet-service (môi trường lab Windows)

Tài liệu này dành cho người **chưa từng deploy** wallet-service. Làm đúng thứ tự, đánh dấu `[x]` từng bước. Không bỏ bước kiểm tra.

## 0. Thông tin chung

| Mục | Giá trị |
|---|---|
| Mã nguồn | `D:\Study\CI_CD\lab\wallet-service` |
| "Server" (giả lập) | `D:\fake-server\wallet` |
| Nơi lưu các bản | `D:\fake-server\wallet\releases\<version>\wallet-service-<version>.jar` |
| Cổng 1 server | `8080` |
| Cổng 3 server (rolling) | `8081`, `8082`, `8083` |
| Terminal dùng | **Git Bash** (không dùng CMD/PowerShell cho các lệnh dưới) |
| Kiểm tra sức khỏe | `GET /actuator/health` phải trả `{"status":"UP"...}` |
| Kiểm tra phiên bản | `GET /actuator/info` trả `"version":"<version>"` |

⚠️ Dữ liệu tài khoản lưu **trong bộ nhớ**: mỗi lần dừng/khởi động lại là **mất hết dữ liệu**; mỗi instance có dữ liệu riêng. Báo trước cho người dùng thử nghiệm.

## 1. Chuẩn bị (trước khi deploy)

- [ ] Mở Git Bash, kiểm tra Java 21: `java -version` → dòng đầu có `21`.
- [ ] Vào thư mục mã nguồn: `cd /d/Study/CI_CD/lab/wallet-service`
- [ ] Xác định **version sẽ deploy**: `VERSION=$(./mvnw -q help:evaluate -Dexpression=project.version -DforceStdout) && echo "$VERSION"`
- [ ] Xác định **version đang chạy** (để biết đường rollback):
  - 1 server: `curl -s localhost:8080/actuator/info`
  - 3 server: `scripts/rolling-deploy.sh status`
  - Ghi lại: `VERSION_CU=________`
- [ ] Nếu VERSION trùng VERSION_CU nhưng code đã thay đổi → **DỪNG**, yêu cầu tăng version trong `pom.xml`. Không bao giờ deploy hai bản khác nhau cùng một số version.

## 2. Build và kiểm thử

- [ ] `./mvnw clean package`
- [ ] Cuối log phải có `BUILD SUCCESS` và `Tests run: ..., Failures: 0, Errors: 0`. Nếu không → **DỪNG**, không deploy, báo người viết code.
- [ ] File tồn tại: `ls -l target/wallet-service-$VERSION.jar`

## 3. Đưa bản mới lên "server"

- [ ] `mkdir -p /d/fake-server/wallet/releases/$VERSION`
- [ ] Nếu thư mục đã có file jar cùng version → **DỪNG** (không ghi đè bản đã phát hành).
- [ ] `cp target/wallet-service-$VERSION.jar /d/fake-server/wallet/releases/$VERSION/`

## 4A. Deploy — trường hợp 1 server (cổng 8080, có downtime vài giây)

- [ ] Ở cửa sổ đang chạy bản cũ: bấm **Ctrl+C**, đợi dấu nhắc lệnh trở lại.
- [ ] `cd /d/fake-server/wallet/releases/$VERSION && java -jar wallet-service-$VERSION.jar`
- [ ] Đợi dòng `Started WalletServiceApplication`.
- [ ] Sang **cửa sổ Git Bash khác** làm bước 5.

## 4B. Deploy — trường hợp 3 server (rolling, không downtime)

- [ ] Kiểm tra cả 3 cổng đang chạy: `scripts/rolling-deploy.sh status` → không có dòng `khong chay`. Nếu có: `scripts/rolling-deploy.sh start $VERSION_CU` trước.
- [ ] `scripts/rolling-deploy.sh rolling $VERSION`
- [ ] Bảng kết quả phải có đủ 3 dòng, cột "So ban van chay" luôn = 2. Script tự dừng nếu không đảm bảo được ≥ 2 bản.

## 5. Kiểm tra sau deploy (bắt buộc)

Với 1 server dùng cổng `8080`; với 3 server lặp lại cho `8081`, `8082`, `8083`:

- [ ] `curl -s localhost:<cong>/actuator/health` → `"status":"UP"`
- [ ] `curl -s localhost:<cong>/actuator/info` → `"version":"<VERSION>"`
- [ ] Thử nghiệp vụ:
  - `curl -s -X POST localhost:<cong>/api/accounts -H "Content-Type: application/json" -d '{"owner":"kiem-tra"}'` → mã 201, có `"id"`
  - `curl -s -X POST localhost:<cong>/api/accounts/<id>/deposit -H "Content-Type: application/json" -d '{"amount":1000}'` → `"balance":1000`
  - `curl -s localhost:<cong>/api/accounts` → danh sách có tài khoản vừa tạo (từ 0.0.3)
- [ ] Bất kỳ bước nào sai → làm **mục 6 Rollback** ngay, sau đó báo cáo.

## 6. Rollback (quay về VERSION_CU)

- 1 server: Ctrl+C → `cd /d/fake-server/wallet/releases/$VERSION_CU && java -jar wallet-service-$VERSION_CU.jar` → kiểm tra lại mục 5 với VERSION_CU.
- 3 server: `scripts/rolling-deploy.sh rolling $VERSION_CU` → kiểm tra lại mục 5.

## 7. Kết thúc

- [ ] Ghi vào nhật ký: ngày giờ, người deploy, VERSION_CU → VERSION, kết quả, thời gian downtime (nếu có).
- [ ] Không xóa thư mục `releases/<version>` cũ — cần cho rollback.

## Xử lý lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách làm |
|---|---|---|
| `Port 8080 was already in use` | Bản cũ vẫn chạy | PowerShell: `netstat -ano \| findstr :8080` → `taskkill /PID <pid> /F` |
| `java: command not found` | Chưa cài/chưa có PATH | Cài JDK 21, mở lại terminal |
| `UnsupportedClassVersionError` | Máy chạy Java cũ hơn 21 | Dùng Java 21 |
| `curl: (7) Failed to connect` | Ứng dụng chưa khởi động xong hoặc đã chết | Đợi thêm; xem log (`/d/fake-server/wallet/logs/<cong>.log` với 3 server) |
| `/actuator/info` ra version cũ | Chạy nhầm file jar | Kiểm tra đường dẫn jar trong lệnh `java -jar` |

## Khôi phục database từ backup (phương án cuối cùng)

⚠️ Mất toàn bộ dữ liệu ghi sau thời điểm backup. Cần người phụ trách đồng ý.
1. Thông báo, dừng nhận traffic: `docker compose --project-name wallet-<env> stop app`
2. Chọn file: `ls -lt /opt/wallet/<env>/backups/`
3. Khôi phục:
   `docker compose --project-name wallet-<env> exec -T db pg_restore -U wallet -d wallet --clean --if-exists < backups/<file>.dump`
4. Chạy lại phiên bản ứng dụng tương ứng thời điểm backup: `./deploy.sh <tag trong tên file>`
5. Kiểm tra dữ liệu, ghi lại sự cố.
   
## Khi deploy staging/production fail
1. Mở log job Deploy: tìm "LOI:" và 50 dòng log app được in kèm.
2. Kiểm tra trạng thái: `cat /opt/wallet/<env>/current_tag`, `curl .../actuator/health/readiness`.
3. Nếu thấy "ROLLBACK THAT BAI": chạy `./deploy.sh <tag ổn định gần nhất trong releases.log>`.
4. Lỗi do cấu hình môi trường (.env) thì rollback image KHÔNG giải quyết được — sửa .env rồi deploy lại.
test