package com.example.wallet.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAccountRequest(@NotBlank String owner) {
}
