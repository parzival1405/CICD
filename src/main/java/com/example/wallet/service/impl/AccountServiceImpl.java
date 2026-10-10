package com.example.wallet.service.impl;

import com.example.wallet.exception.AccountNotFoundException;
import com.example.wallet.model.Account;
import com.example.wallet.service.AccountService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

    private final Map<Long, Account> accounts = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public Account create(String owner) {
        Account account = new Account(sequence.incrementAndGet(), owner);
        accounts.put(account.getId(), account);
        return account;
    }

    @Override
    public Account get(Long id) {
        Account account = accounts.get(id);
        if (account == null) {
            throw new AccountNotFoundException(id);
        }
        return account;
    }

    // Sửa bài tập 1 (Dễ) - mục 10 "Bài tập tự làm", Bài 03 Giai đoạn 0:
    // ConcurrentHashMap không giữ thứ tự nên sắp xếp theo id để kết quả ổn định
    @Override
    public List<Account> findAll() {
        return accounts.values().stream()
            .sorted(Comparator.comparing(Account::getId))
            .toList();
    }

    @Override
    public synchronized Account deposit(Long id, BigDecimal amount) {
        Account account = get(id);
        account.deposit(amount);
        return account;
    }

    @Override
    public synchronized Account withdraw(Long id, BigDecimal amount) {
        Account account = get(id);
        account.withdraw(amount);
        return account;
    }
}
