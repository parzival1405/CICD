package com.example.wallet.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(Long id, BigDecimal balance, BigDecimal amount) {
        super("Tai khoan " + id + " khong du so du: hien co " + balance + ", can rut " + amount);
    }
}
