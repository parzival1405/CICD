package com.example.wallet.model;

import com.example.wallet.exception.InsufficientBalanceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

// Sửa code Bước 2 - mục 6 "Thực hành từng bước", Bài 03 Giai đoạn 3: Account thành JPA entity
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String owner;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    // Optimistic locking: hai giao dịch cùng sửa một tài khoản -> giao dịch sau bị từ chối thay vì ghi đè
    @Version
    private long version;

    protected Account() {
        // JPA cần constructor không tham số
    }

    public Account(String owner) {
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
