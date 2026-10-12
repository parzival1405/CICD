package com.example.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet.exception.AccountNotFoundException;
import com.example.wallet.exception.InsufficientBalanceException;
import com.example.wallet.service.AccountService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Thêm code Bước 3 - mục 6 "Thực hành từng bước", Bài 09 Giai đoạn 1: slice test cho controller
@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService service;

    @Test
    void get_unknownAccount_returns404() throws Exception {
        given(service.get(42L)).willThrow(new AccountNotFoundException(42L));

        mockMvc.perform(get("/api/accounts/42"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void withdraw_insufficientBalance_returns409() throws Exception {
        given(service.withdraw(eq(1L), any(BigDecimal.class)))
            .willThrow(new InsufficientBalanceException(1L, BigDecimal.ZERO, BigDecimal.TEN));

        mockMvc.perform(post("/api/accounts/1/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10}"))
            .andExpect(status().isConflict());
    }

    @Test
    void create_blankOwner_returns400() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"owner\":\"\"}"))
            .andExpect(status().isBadRequest());
    }
}
