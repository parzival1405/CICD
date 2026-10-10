package com.example.wallet.dto;

import com.example.wallet.model.Account;
import java.math.BigDecimal;

public record AccountResponse(Long id, String owner, BigDecimal balance) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getOwner(), account.getBalance());
    }
}
