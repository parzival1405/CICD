package com.example.wallet.repository;

import com.example.wallet.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

// Thêm code Bước 2 - mục 6 "Thực hành từng bước", Bài 03 Giai đoạn 3
public interface AccountRepository extends JpaRepository<Account, Long> {
}
