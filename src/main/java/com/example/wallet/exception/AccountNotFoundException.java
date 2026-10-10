package com.example.wallet.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long id) {
        super("Khong tim thay tai khoan " + id);
    }
}
