package com.riverwatch.account;

import com.riverwatch.auth.AuthUser;
import com.riverwatch.auth.UserResponse;
import com.riverwatch.auth.UserSessionRepository;
import com.riverwatch.common.ApiException;
import com.riverwatch.common.AuditService;
import com.riverwatch.common.TokenUtil;
import com.riverwatch.user.AccountStatus;
import com.riverwatch.user.UserAccount;
import com.riverwatch.user.UserAccountRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Increment 2: profile management, change password, forgot password and reset password. */
@Service
public class AccountService {
    private static final int RESET_MINUTES = 30;
    private static final int RESEND_SECONDS = 60;

    private final UserAccountRepository users;
    private final UserSessionRepository sessions;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final EmailService email;
    private final String appBaseUrl;

    public AccountService(UserAccountRepository users, UserSessionRepository sessions,
                          PasswordResetTokenRepository resetTokens, PasswordEncoder encoder,
                          AuditService audit, EmailService email,
                          @Value("${APP_BASE_URL:http://localhost:5173}") String appBaseUrl) {
        this.users = users;
        this.sessions = sessions;
        this.resetTokens = resetTokens;
        this.encoder = encoder;
        this.audit = audit;
        this.email = email;
        this.appBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
    }

    // ------------------------------------------------------------ profile

    /** Always works on the caller's own id taken from the verified token, never from the request. */
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.of(load(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, ProfileRequest req) {
        UserAccount u = load(userId);
        String newEmail = req.email().trim().toLowerCase();
        if (!newEmail.equalsIgnoreCase(u.getEmail())) {
            if (users.existsByEmailIgnoreCase(newEmail)) throw fieldError(HttpStatus.CONFLICT, "email",
                    "This email address is already registered.");
            u.setEmail(newEmail);
        }
        u.setFullName(req.fullName().trim());
        u.setPhoneNumber(req.phoneNumber().trim());
        try {
            u = users.saveAndFlush(u);
        } catch (DataIntegrityViolationException e) {
            throw fieldError(HttpStatus.CONFLICT, "email", "This email address is already registered.");
        }
        audit.record(userId, "PROFILE_UPDATED", null);
        return UserResponse.of(u);
    }

    // ------------------------------------------------------------ change password (logged in)

    @Transactional
    public void changePassword(AuthUser me, ChangePasswordRequest req) {
        UserAccount u = load(me.userId());
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw fieldError(HttpStatus.UNPROCESSABLE_ENTITY, "confirmPassword", "Passwords do not match.");
        }
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw fieldError(HttpStatus.UNPROCESSABLE_ENTITY, "currentPassword", "Current password is incorrect.");
        }
        if (encoder.matches(req.newPassword(), u.getPasswordHash())) {
            throw fieldError(HttpStatus.UNPROCESSABLE_ENTITY, "newPassword",
                    "New password must be different from the current password.");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
        users.save(u);
        sessions.revokeOthers(u.getUserId(), me.sessionId(), LocalDateTime.now()); // other devices must log in again
        audit.record(u.getUserId(), "PASSWORD_CHANGED", null);
    }

    // ------------------------------------------------------------ forgot / reset password

    /** Always looks the same to the caller, whether or not the email is registered. */
    @Transactional
    public void forgotPassword(String rawEmail) {
        String addr = rawEmail.trim().toLowerCase();
        Optional<UserAccount> found = users.findByEmailIgnoreCase(addr);
        if (found.isEmpty() || found.get().getAccountStatus() != AccountStatus.ACTIVE) return;
        UserAccount u = found.get();
        LocalDateTime now = LocalDateTime.now();

        Optional<PasswordResetToken> last = resetTokens.findTopByUserIdOrderByCreatedAtDesc(u.getUserId());
        if (last.isPresent() && last.get().getCreatedAt().isAfter(now.minusSeconds(RESEND_SECONDS))) return;

        resetTokens.invalidateOpen(u.getUserId(), now);
        String raw = TokenUtil.newToken();
        resetTokens.save(new PasswordResetToken(u.getUserId(), TokenUtil.sha256(raw), now.plusMinutes(RESET_MINUTES)));
        audit.record(u.getUserId(), "PASSWORD_RESET_REQUESTED", null);
        email.sendPasswordReset(u.getEmail(), u.getFullName(), appBaseUrl + "/reset-password?token=" + raw);
    }

    @Transactional(readOnly = true)
    public void validateResetToken(String rawToken) {
        usableToken(rawToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw fieldError(HttpStatus.UNPROCESSABLE_ENTITY, "confirmPassword", "Passwords do not match.");
        }
        String hash = TokenUtil.sha256(req.token());
        PasswordResetToken token = usableToken(req.token());
        UserAccount u = load(token.getUserId());
        // Atomic "use once": only one request can flip used_at from null.
        if (resetTokens.consume(hash, LocalDateTime.now()) != 1) throw invalidLink();

        u.setPasswordHash(encoder.encode(req.newPassword()));
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
        users.save(u);
        sessions.revokeAll(u.getUserId(), LocalDateTime.now()); // every old login stops working
        audit.record(u.getUserId(), "PASSWORD_RESET_COMPLETED", null);
    }

    // ------------------------------------------------------------ helpers

    private PasswordResetToken usableToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw invalidLink();
        PasswordResetToken t = resetTokens.findByTokenHash(TokenUtil.sha256(rawToken)).orElseThrow(this::invalidLink);
        if (!t.isUsable()) throw invalidLink();
        UserAccount u = users.findById(t.getUserId()).orElseThrow(this::invalidLink);
        if (u.getAccountStatus() != AccountStatus.ACTIVE) throw invalidLink();
        return t;
    }

    private UserAccount load(Long userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expired."));
    }

    private ApiException invalidLink() {
        return new ApiException(HttpStatus.GONE, "This reset link is invalid or has expired. Please request a new one.");
    }

    private ApiException fieldError(HttpStatus status, String field, String message) {
        return new ApiException(status, "Please correct the highlighted fields.", Map.of(field, message));
    }
}
