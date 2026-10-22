# Quy trình ngoại lệ bảo mật
1. Mở PR sửa `.trivyignore.yaml` hoặc `.gitleaksignore`, mỗi mục có: ID, lý do, biện pháp giảm thiểu, người duyệt, ticket.
2. Bắt buộc `expired_at` tối đa 90 ngày (CRITICAL: 14 ngày).
3. Người duyệt: Tech Lead + người phụ trách bảo mật. Không tự duyệt.
4. Hết hạn -> pipeline đỏ -> đánh giá lại: nâng cấp, gia hạn có lý do mới, hoặc chấp nhận rủi ro bằng văn bản.
5. Secret thật bị phát hiện: KHÔNG được ghi ngoại lệ — thực hiện RUNBOOK "Lộ secret".
