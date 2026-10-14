package com.example.wallet.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.wallet.exception.InsufficientBalanceException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// Thêm code Bước 1 - mục 6 "Thực hành từng bước", Bài 09 Giai đoạn 1: unit test cho Account
class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account("An");
    }

    @Test
    void newAccount_hasZeroBalance() {
        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void deposit_increasesBalance() {
        account.deposit(new BigDecimal("100000"));

        assertThat(account.getBalance()).isEqualByComparingTo("100000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.01"})
    void deposit_nonPositiveAmount_throws(String amount) {
        assertThatThrownBy(() -> account.deposit(new BigDecimal(amount)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withdraw_withinBalance_decreasesBalance() {
        account.deposit(new BigDecimal("100000"));

        account.withdraw(new BigDecimal("30000"));

        assertThat(account.getBalance()).isEqualByComparingTo("70000");
    }

    @Test
    void withdraw_exactBalance_leavesZero() {
        account.deposit(new BigDecimal("50000"));

        account.withdraw(new BigDecimal("50000"));

        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void withdraw_moreThanBalance_throwsAndKeepsBalance() {
        account.deposit(new BigDecimal("100"));

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("150")))
            .isInstanceOf(InsufficientBalanceException.class);
        assertThat(account.getBalance()).isEqualByComparingTo("100");
    }
}
