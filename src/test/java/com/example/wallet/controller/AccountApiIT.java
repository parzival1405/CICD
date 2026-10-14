package com.example.wallet.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Thêm code Bước 4 - mục 6 "Thực hành từng bước", Bài 09 Giai đoạn 1: integration test (chạy bởi failsafe)
// Sửa code Bước 4 - mục 6 "Thực hành từng bước", Bài 03 Giai đoạn 3: chạy với PostgreSQL thật bằng Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AccountApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createDepositWithdraw_fullFlow() throws Exception {
        String created = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"owner\":\"An\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.owner").value("An"))
            .andExpect(jsonPath("$.balance").value(0))
            .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        mockMvc.perform(post("/api/accounts/{id}/deposit", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":100000}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(100000));

        mockMvc.perform(post("/api/accounts/{id}/withdraw", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":500000}"))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/accounts/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(100000));
    }

    @Test
    void actuatorHealth_isUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
}
