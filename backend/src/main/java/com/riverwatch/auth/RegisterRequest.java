package com.riverwatch.auth;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 120, message = "Full name must be 120 characters or fewer.")
        String fullName,

        @NotBlank(message = "Email address is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 160, message = "Email must be 160 characters or fewer.")
        String email,

        @NotBlank(message = "Mobile number is required.")
        @Pattern(regexp = "^(09\\d{9}|\\+639\\d{9})$",
                 message = "Use a Philippine mobile number like 09171234567 or +639171234567.")
        String phoneNumber,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters.")
        String password,

        @NotNull(message = "Please select your barangay.")
        Long barangayId) {}
