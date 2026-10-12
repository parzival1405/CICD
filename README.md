<!-- Thêm feature Bước 3 - mục 6 "Thực hành từng bước", Bài 02 Giai đoạn 1: mô tả API trong README -->
# wallet-service — vi dien tu mau

Ứng dụng ví điện tử mẫu (Spring Boot 3.5.6, Java 21) dùng xuyên suốt lộ trình CI/CD. Dữ liệu hiện lưu **trong bộ nhớ** — khởi động lại là mất.

## API

| Method | Đường dẫn | Mô tả | Body | Thành công | Lỗi |
|---|---|---|---|---|---|
| `POST` | `/api/accounts` | Tạo tài khoản | `{"owner":"An"}` | `201` + tài khoản | `400` owner rỗng |
| `GET` | `/api/accounts` | Liệt kê tài khoản (sắp theo id) | — | `200` + danh sách | — |
| `GET` | `/api/accounts/{id}` | Xem tài khoản và số dư | — | `200` | `404` không tồn tại |
| `POST` | `/api/accounts/{id}/deposit` | Nạp tiền (tối đa `wallet.max-deposit` mỗi lần) | `{"amount":100000}` | `200` + số dư mới | `400` số tiền ≤ 0 hoặc vượt hạn mức, `404` |
| `POST` | `/api/accounts/{id}/withdraw` | Rút tiền | `{"amount":50000}` | `200` + số dư mới | `400`, `404`, `409` không đủ số dư |
| `GET` | `/actuator/health` | Kiểm tra sức khỏe | — | `200` `{"status":"UP"}` | — |
| `GET` | `/actuator/info` | Tên và phiên bản đang chạy | — | `200` | — |

Lỗi trả về dạng `ProblemDetail` (RFC 9457):

```json
{"type":"about:blank","title":"Conflict","status":409,"detail":"Tai khoan 1 khong du so du: hien co 100000, can rut 500000","instance":"/api/accounts/1/withdraw"}
```

## Chạy ở local

```bash
./mvnw clean package
java -jar target/wallet-service-*.jar                  # cổng 8080
java -jar target/wallet-service-*.jar --spring.profiles.active=dev   # log DEBUG, health chi tiết
```

Thử nhanh:

```bash
curl -s -X POST localhost:8080/api/accounts -H "Content-Type: application/json" -d '{"owner":"An"}'
curl -s -X POST localhost:8080/api/accounts/1/deposit -H "Content-Type: application/json" -d '{"amount":100000}'
curl -s localhost:8080/api/accounts
```

<!-- Thêm theo Bài 10 Giai đoạn 1: cấu hình qua biến môi trường -->
## Cấu hình

| Thuộc tính | Biến môi trường | Mặc định | Ý nghĩa |
|---|---|---|---|
| `wallet.max-deposit` | `WALLET_MAXDEPOSIT` | `50000000` | Hạn mức nạp mỗi lần; phải > 0, sai thì ứng dụng không khởi động |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | (không) | `dev`: log DEBUG, health chi tiết · `prod`: log INFO, ẩn chi tiết |

Ở local: `cp .env.example .env`, sửa giá trị, rồi `set -a; source .env; set +a` trước khi `java -jar`. Không commit `.env`.

## Cấu trúc

```
src/main/java/com/example/wallet/
├── controller/   REST API
├── service/      interface + impl/ (nghiệp vụ)
├── model/        Account
├── dto/          request/response
├── exception/    exception + GlobalExceptionHandler
├── config/  repository/  util/
```

## Vận hành

Deploy thủ công, rollback, rolling deploy 3 instance: xem [RUNBOOK.md](RUNBOOK.md) và `scripts/rolling-deploy.sh`.
