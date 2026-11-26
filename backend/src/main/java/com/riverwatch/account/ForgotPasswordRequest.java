package com.riverwatch.account;

import jakarta.validation.constraints.*;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email address is required.")
        @Email(message = "Enter a valid email address.") String email) {}
