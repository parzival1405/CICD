package com.example.wallet.model;

import com.example.wallet.exception.InsufficientBalanceException;
import java.math.BigDecimal;

public class Account {

    private final Long id;
    private final String owner;
    private BigDecimal balance;

    public Account(Long id, String owner) {
        this.id = id;
        this.owner = owner;
        this.balance = BigDecimal.ZERO;
    }

    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(id, balance, amount);
        }
        balance = balance.subtract(amount);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("So tien phai lon hon 0");
        }
    }

    public Long getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }
}
