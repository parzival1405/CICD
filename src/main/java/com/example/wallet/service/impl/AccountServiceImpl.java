package com.example.wallet.service.impl;

import com.example.wallet.config.WalletProperties;
import com.example.wallet.exception.AccountNotFoundException;
import com.example.wallet.model.Account;
import com.example.wallet.repository.AccountRepository;
import com.example.wallet.service.AccountService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Sửa code Bước 2 - mục 6 "Thực hành từng bước", Bài 03 Giai đoạn 3: lưu bằng PostgreSQL qua JPA.
// Bỏ synchronized: chống ghi đè đồng thời giờ do @Version (optimistic locking) trong Account đảm nhận.
@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository repository;
    private final WalletProperties properties;

    public AccountServiceImpl(AccountRepository repository, WalletProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public Account create(String owner) {
        return repository.save(new Account(owner));
    }

    @Override
    @Transactional(readOnly = true)
    public Account get(Long id) {
        return repository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    // Giữ thứ tự theo id như README mô tả
    @Override
    @Transactional(readOnly = true)
    public List<Account> findAll() {
        return repository.findAll(Sort.by("id"));
    }

    // Không gọi save: dirty checking tự UPDATE khi transaction commit
    @Override
    public Account deposit(Long id, BigDecimal amount) {
        if (amount != null && amount.compareTo(properties.maxDeposit()) > 0) {
            throw new IllegalArgumentException("Vuot han muc nap moi lan: " + properties.maxDeposit());
        }
        Account account = get(id);
        account.deposit(amount);
        return account;
    }

    @Override
    public Account withdraw(Long id, BigDecimal amount) {
        Account account = get(id);
        account.withdraw(amount);
        return account;
    }
}
