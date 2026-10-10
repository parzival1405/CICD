package com.example.wallet.service;

import com.example.wallet.model.Account;
import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    Account create(String owner);

    Account get(Long id);

    // Sửa bài tập 1 (Dễ) - mục 10 "Bài tập tự làm", Bài 03 Giai đoạn 0: liệt kê tất cả tài khoản
    List<Account> findAll();

    Account deposit(Long id, BigDecimal amount);

    Account withdraw(Long id, BigDecimal amount);
}
