package com.riverwatch.account;

import jakarta.validation.constraints.*;

public record ResetPasswordRequest(
        @NotBlank(message = "The reset link is missing its token.") String token,

        @NotBlank(message = "Enter a new password.")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters.")
        String newPassword,

        @NotBlank(message = "Confirm your new password.") String confirmPassword) {}
