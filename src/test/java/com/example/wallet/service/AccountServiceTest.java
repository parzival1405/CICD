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

    // Thêm code Bước 3 - mục 6 "Thực hành từng bước", Bài 01 Giai đoạn 6: phủ đủ 3 nhánh của withdrawFee
    // để quality gate (new code coverage >= 80%) PASSED
    @Test
    void withdrawFee_overOneMillion_chargesZeroPointOnePercent() {
        assertThat(withdrawFee("2000000")).isEqualByComparingTo("2000");
    }

    @Test
    void withdrawFee_overOneHundredThousand_chargesFlatFee() {
        // biên: đúng 1.000.000 chưa vượt mốc trên -> vẫn phí cố định
        assertThat(withdrawFee("1000000")).isEqualByComparingTo("1000");
    }

    @Test
    void withdrawFee_upToOneHundredThousand_isFree() {
        // biên: đúng 100.000 chưa vượt mốc dưới -> miễn phí
        assertThat(withdrawFee("100000")).isEqualByComparingTo("0");
    }

    // withdrawFee chỉ có ở AccountServiceImpl, chưa đưa vào interface AccountService
    private BigDecimal withdrawFee(String amount) {
        return ((AccountServiceImpl) service).withdrawFee(new BigDecimal(amount));
    }
}
