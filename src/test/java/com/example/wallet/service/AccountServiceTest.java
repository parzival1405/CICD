package com.example.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.wallet.config.WalletProperties;
import com.example.wallet.exception.AccountNotFoundException;
import com.example.wallet.model.Account;
import com.example.wallet.repository.AccountRepository;
import com.example.wallet.service.impl.AccountServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Sửa code Bước 4 - mục 6 "Thực hành từng bước", Bài 03 Giai đoạn 3:
// service giờ phụ thuộc repository -> unit test dùng mock, không Spring, không DB
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository repository;

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(repository, new WalletProperties(new BigDecimal("1000")));
    }

    @Test
    void get_unknownId_throwsNotFound() {
        given(repository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(999L))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void depositThenWithdraw_updatesBalance() {
        Account account = new Account("An");
        given(repository.findById(1L)).willReturn(Optional.of(account));

        service.deposit(1L, new BigDecimal("200"));
        service.withdraw(1L, new BigDecimal("50"));

        assertThat(account.getBalance()).isEqualByComparingTo("150");
    }

    @Test
    void deposit_overLimit_throwsWithoutTouchingRepository() {
        assertThatThrownBy(() -> service.deposit(1L, new BigDecimal("1000.01")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("han muc");
        verifyNoInteractions(repository);
    }
}
