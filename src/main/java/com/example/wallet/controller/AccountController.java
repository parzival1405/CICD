package com.example.wallet.controller;

import com.example.wallet.dto.AccountResponse;
import com.example.wallet.dto.AmountRequest;
import com.example.wallet.dto.CreateAccountRequest;
import com.example.wallet.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return AccountResponse.from(service.create(request.owner()));
    }

    // Sửa bài tập 1 (Dễ) - mục 10 "Bài tập tự làm", Bài 03 Giai đoạn 0: GET /api/accounts
    @GetMapping
    public List<AccountResponse> list() {
        return service.findAll().stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return AccountResponse.from(service.get(id));
    }

    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable Long id, @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(service.deposit(id, request.amount()));
    }

    @PostMapping("/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable Long id, @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(service.withdraw(id, request.amount()));
    }
}
