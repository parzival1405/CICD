package com.example.wallet.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Thêm code Bước 1 - mục 6 "Thực hành từng bước", Bài 10 Giai đoạn 1 (bổ sung khi làm Bài 03 Giai đoạn 3):
// hạn mức nạp mỗi lần, đổi bằng biến môi trường WALLET_MAXDEPOSIT
@Validated
@ConfigurationProperties(prefix = "wallet")
public record WalletProperties(@NotNull @Positive BigDecimal maxDeposit) {
}
