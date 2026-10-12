package com.example.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.wallet.config.WalletProperties;
import com.example.wallet.exception.AccountNotFoundException;
import com.example.wallet.model.Account;
import com.example.wallet.service.impl.AccountServiceImpl;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Thêm code Bước 2 - mục 6 "Thực hành từng bước", Bài 09 Giai đoạn 1: unit test cho AccountService
// (trong project, lớp thực thi là AccountServiceImpl)
class AccountServiceTest {

    private AccountService service;

    // Sửa theo Bước 2 - mục 6 "Thực hành từng bước", Bài 10 Giai đoạn 1: service cần WalletProperties (hạn mức 1000)
    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(new WalletProperties(new BigDecimal("1000")));
    }

    // Thêm code Bước 2 - mục 6 "Thực hành từng bước", Bài 10 Giai đoạn 1: test nạp vượt hạn mức
    @Test
    void deposit_overLimit_throws() {
        Long id = service.create("An").getId();

        assertThatThrownBy(() -> service.deposit(id, new BigDecimal("1000.01")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("han muc");
    }

    @Test
    void create_assignsIncreasingIds() {
        Account first = service.create("An");
        Account second = service.create("Binh");

        assertThat(second.getId()).isGreaterThan(first.getId());
    }

    @Test
    void get_unknownId_throwsNotFound() {
        assertThatThrownBy(() -> service.get(999L))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void depositThenWithdraw_updatesBalance() {
        Long id = service.create("An").getId();

        service.deposit(id, new BigDecimal("200"));
        Account result = service.withdraw(id, new BigDecimal("50"));

        assertThat(result.getBalance()).isEqualByComparingTo("150");
    }
}
