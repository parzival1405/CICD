[![CI](https://github.com/parzival1405/CICD/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/parzival1405/CICD/actions/workflows/ci.yml)

<!-- Thêm feature Bước 3 - mục 6 "Thực hành từng bước", Bài 02 Giai đoạn 1: mô tả API trong README -->
# wallet-service — vi dien tu mau

Ứng dụng ví điện tử mẫu (Spring Boot 3.5.6, Java 21) dùng xuyên suốt lộ trình CI/CD. Dữ liệu lưu trong **PostgreSQL** (Spring Data JPA, optimistic locking bằng `@Version`).

## API

| Method | Đường dẫn | Mô tả | Body | Thành công | Lỗi |
|---|---|---|---|---|---|
| `POST` | `/api/accounts` | Tạo tài khoản | `{"owner":"An"}` | `201` + tài khoản | `400` owner rỗng |
| `GET` | `/api/accounts` | Liệt kê tài khoản (sắp theo id) | — | `200` + danh sách | — |
| `GET` | `/api/accounts/{id}` | Xem tài khoản và số dư | — | `200` | `404` không tồn tại |
| `POST` | `/api/accounts/{id}/deposit` | Nạp tiền | `{"amount":100000}` | `200` + số dư mới | `400` số tiền ≤ 0 hoặc vượt hạn mức (`WALLET_MAXDEPOSIT`, mặc định 50 triệu), `404`, `409` bị giao dịch khác cập nhật trước |
| `POST` | `/api/accounts/{id}/withdraw` | Rút tiền | `{"amount":50000}` | `200` + số dư mới | `400`, `404`, `409` không đủ số dư hoặc bị giao dịch khác cập nhật trước |
| `GET` | `/actuator/health` | Kiểm tra sức khỏe | — | `200` `{"status":"UP"}` | — |
| `GET` | `/actuator/info` | Tên và phiên bản đang chạy | — | `200` | — |

Lỗi trả về dạng `ProblemDetail` (RFC 9457):

```json
{"type":"about:blank","title":"Conflict","status":409,"detail":"Tai khoan 1 khong du so du: hien co 100000, can rut 500000","instance":"/api/accounts/1/withdraw"}
```

## Chạy ở local

Cách nhanh nhất — app + PostgreSQL bằng Docker Compose:

```bash
cp .env.example .env          # rồi đặt DB_PASSWORD trong .env (không commit file .env)
docker compose up -d --build
./scripts/wait-for-health.sh
docker compose down           # dữ liệu vẫn còn trong volume db-data; `down -v` mới xóa
```

Chạy jar trực tiếp (cần PostgreSQL ở `localhost:5432`, hoặc đặt `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`):

```bash
./mvnw clean package          # verify cần Docker: integration test dùng Testcontainers
java -jar target/wallet-service-*.jar                  # cổng 8080
java -jar target/wallet-service-*.jar --spring.profiles.active=dev   # log DEBUG, health chi tiết
```

Thử nhanh:

```bash
curl -s -X POST localhost:8080/api/accounts -H "Content-Type: application/json" -d '{"owner":"An"}'
curl -s -X POST localhost:8080/api/accounts/1/deposit -H "Content-Type: application/json" -d '{"amount":100000}'
curl -s localhost:8080/api/accounts
```

## Cấu trúc

```
src/main/java/com/example/wallet/
├── controller/   REST API
├── service/      interface + impl/ (nghiệp vụ)
├── model/        Account (JPA entity)
├── repository/   AccountRepository (Spring Data JPA)
├── dto/          request/response
├── exception/    exception + GlobalExceptionHandler
├── config/  repository/  util/
```

## Vận hành

Deploy thủ công, rollback, rolling deploy 3 instance: xem [RUNBOOK.md](RUNBOOK.md) và `scripts/rolling-deploy.sh`.
