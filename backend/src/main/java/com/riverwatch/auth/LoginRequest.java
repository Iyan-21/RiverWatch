package com.riverwatch.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email address is required.") String email,
        @NotBlank(message = "Password is required.") String password) {}
