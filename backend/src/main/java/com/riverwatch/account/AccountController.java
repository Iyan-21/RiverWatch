package com.riverwatch.account;

import com.riverwatch.auth.AuthUser;
import com.riverwatch.auth.UserResponse;
import com.riverwatch.common.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) { this.accounts = accounts; }

    // ---- profile: any logged-in user, only their own record (the id comes from the token) ----

    @GetMapping("/profile")
    public UserResponse profile(@AuthenticationPrincipal AuthUser me) {
        return accounts.getProfile(me.userId());
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ProfileRequest req) {
        return accounts.updateProfile(me.userId(), req);
    }

    // ---- change password: logged in ----

    @PostMapping("/auth/change-password")
    public MessageResponse changePassword(@AuthenticationPrincipal AuthUser me,
                                          @Valid @RequestBody ChangePasswordRequest req) {
        accounts.changePassword(me, req);
        return new MessageResponse("Password changed. Other devices have been logged out.");
    }

    // ---- forgot / reset password: public ----

    @PostMapping("/auth/forgot-password")
    public MessageResponse forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        accounts.forgotPassword(req.email());
        return new MessageResponse("If that email address is registered, a reset link has been sent. "
                + "The link is valid for 30 minutes.");
    }

    @GetMapping("/auth/reset-password/validate")
    public MessageResponse validate(@RequestParam String token) {
        accounts.validateResetToken(token);
        return new MessageResponse("Link is valid.");
    }

    @PostMapping("/auth/reset-password")
    public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest req) {
        accounts.resetPassword(req);
        return new MessageResponse("Your password has been reset. You can now log in.");
    }
}
