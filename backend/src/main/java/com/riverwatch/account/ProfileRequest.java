package com.riverwatch.account;

import jakarta.validation.constraints.*;

public record ProfileRequest(
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
        String phoneNumber) {}
