# Rà soát least privilege — pipeline wallet-service

> Bước 3 - mục 6 "Thực hành từng bước", Bài 05 Giai đoạn 6.
> Ngày rà soát: 2026-10-21. Nguồn: `.github/workflows/*.yml`, `Jenkinsfile`, `jenkins-shared-library`,
> `deploy/compose*.yaml`, `id <user>` trên WSL. Ô có dấu **?** không đọc được từ code — cần kiểm tra trên GitHub/Jenkins.

## GitHub Actions

| Thành phần | Quyền hiện tại | Có cần không? | Hành động |
|---|---|---|---|
| `GITHUB_TOKEN` mặc định (ci, rollback, codeql, dependency-review) | `contents: read` ở cấp workflow | ✔ | giữ |
| Job `docker` | `contents: read`, `packages: write` | ✔ (push GHCR) | giữ — job duy nhất được ghi package |
| Job `analyze` (CodeQL) | `contents: read`, `security-events: write` | ✔ (tải SARIF) | giữ — job duy nhất được ghi Security |
| Job `secrets` (Gitleaks) | `contents: read`, `pull-requests: read` | ✔ (liệt kê commit của PR) | giữ |
| Secret `SONAR_TOKEN` (job `build`) | Token cá nhân SonarQube Cloud — quyền theo tài khoản của bạn; hạn: **?** | ⚠ | đặt ngày hết hạn khi tạo lại, rotate ≤ 90 ngày |
| Secret `SSH_PRIVATE_KEY`, `SSH_KNOWN_HOSTS` (deploy-staging, deploy-production, rollback) | SSH vào user `deploy`; repo secret hay environment secret: **?** | ✔ | kiểm tra Settings → Environments: là *environment secret*, key riêng cho staging và production |
| Self-hosted runner `wsl-lab` (user `gha-runner`) | Không sudo, không nhóm docker; chỉ nhận job deploy (push `main` / `workflow_dispatch`) | ✔ | giữ — PR từ fork không chạy được trên runner này |

## Jenkins

| Thành phần | Quyền hiện tại | Có cần không? | Hành động |
|---|---|---|---|
| Credential `ghcr-cred` (PAT push image) | Scope và hạn: **?** | ? | chỉ `write:packages`, hạn ngắn (bài tập 2); thu hồi PAT cũ sau khi thay |
| Credential `deploy-ssh` (SSH → `deploy@localhost`) | SSH vào user `deploy`; dùng chung key với GitHub Actions: **?** | ✔ | giữ; dùng key riêng cho Jenkins để thu hồi độc lập |
| User `jenkins-agent` | Nhóm `docker` (≈ root), không sudo | ⚠ | ghi nhận rủi ro lab; thực tế: agent tạm thời (container/VM dùng xong bỏ) |

## Server (WSL Ubuntu)

| Thành phần | Quyền hiện tại | Có cần không? | Hành động |
|---|---|---|---|
| User `deploy` | Nhóm `docker` (≈ root), không sudo; thư mục `/opt/wallet` | ⚠ | ghi nhận rủi ro lab; thực tế: docker rootless / script sudo giới hạn |
| User cá nhân (quản trị WSL) | sudo + nhóm `docker` | ✔ | chỉ dùng để quản trị, không dùng cho CI/deploy |

## Database

| Thành phần | Quyền hiện tại | Có cần không? | Hành động |
|---|---|---|---|
| User `wallet` (app runtime + Flyway migration) | Là `POSTGRES_USER` của image postgres → **superuser** | ✘ | lab: ghi nhận; thực tế: tách user migration (owner schema) và user runtime (chỉ SELECT/INSERT/UPDATE/DELETE) |

## Token khác

| Thành phần | Quyền hiện tại | Có cần không? | Hành động |
|---|---|---|---|
| SonarQube local — token `wallet-service-local` | Project Analysis Token, chỉ project `wallet-service`, không hạn | ✔ | giữ cho lab; khi tạo lại thì đặt hạn |
| PAT `ghcr-local` trên máy (Giai đoạn 3) | **?** | ✘ (đã xong Giai đoạn 3) | thu hồi nếu còn: GitHub → Settings → Developer settings → Personal access tokens |
| Nexus `ci-deployer` | Chưa có (Bài 04 chưa làm) | — | rà soát khi làm Bài 04 |

## Vi phạm least privilege đang chấp nhận trong lab

1. **User DB `wallet` là superuser** — app bị chiếm quyền thì kẻ tấn công có toàn quyền PostgreSQL. Khắc phục: tách user migration/runtime.
2. **`deploy` và `jenkins-agent` thuộc nhóm `docker`** — tương đương root trên host. Khắc phục: docker rootless, agent tạm thời.
3. **Token không có hạn hoặc chưa rõ hạn** (`SONAR_TOKEN`, `ghcr-cred`, token SonarQube local) — lộ thì dùng được mãi. Khắc phục: đặt hạn, ghi vào danh mục secret (bài tập 1), rotate định kỳ.
