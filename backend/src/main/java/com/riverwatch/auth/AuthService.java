package com.riverwatch.auth;

import com.riverwatch.common.ApiException;
import com.riverwatch.common.AuditService;
import com.riverwatch.config.AppProperties;
import com.riverwatch.user.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    /** Result of a successful login or refresh. The raw refresh token goes only into the HttpOnly cookie. */
    public record Session(TokenResponse tokens, String refreshToken) {}

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BAD_CREDENTIALS = "Invalid email or password.";

    private final UserAccountRepository users;
    private final BarangayRepository barangays;
    private final UserSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    private final AppProperties props;
    private final String dummyHash;

    public AuthService(UserAccountRepository users, BarangayRepository barangays, UserSessionRepository sessions,
                       PasswordEncoder encoder, JwtService jwt, AuditService audit, AppProperties props) {
        this.users = users;
        this.barangays = barangays;
        this.sessions = sessions;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        this.props = props;
        // Used so that unknown emails cost the same time as wrong passwords.
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    // ---------------------------------------------------------------- registration (FR-001, FR-002)

    public UserResponse register(RegisterRequest req, Role role) {
        if (role == Role.ADMIN) throw new IllegalArgumentException("Administrators cannot self-register.");
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw duplicateEmail();
        Barangay barangay = barangays.findById(req.barangayId()).orElseThrow(() ->
                new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Please correct the highlighted fields.",
                        Map.of("barangayId", "Selected barangay does not exist.")));

        UserAccount u = new UserAccount();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setPhoneNumber(req.phoneNumber().trim());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setBarangay(barangay);
        u.setRole(role);
        u.setAccountStatus(role == Role.OFFICER ? AccountStatus.PENDING : AccountStatus.ACTIVE);
        try {
            u = users.saveAndFlush(u);
        } catch (DataIntegrityViolationException e) {
            throw duplicateEmail(); // two sign-ups with the same email at the same moment
        }
        audit.record(u.getUserId(), role == Role.OFFICER ? "OFFICER_REGISTERED" : "RESIDENT_REGISTERED", email);
        return UserResponse.of(u);
    }

    private ApiException duplicateEmail() {
        return new ApiException(HttpStatus.CONFLICT, "Please correct the highlighted fields.",
                Map.of("email", "This email address is already registered."));
    }

    // ---------------------------------------------------------------- login (FR-004, NFR-S04)

    public Session login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        UserAccount user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            encoder.matches(req.password(), dummyHash);
            throw new ApiException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long mins = Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes() + 1);
            throw new ApiException(HttpStatus.LOCKED,
                    "Too many failed attempts. Try again in about " + mins + " minute(s).");
        }

        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            int failed = user.getFailedLoginCount() + 1;
            if (failed >= props.getMaxFailedLogins()) {
                user.setFailedLoginCount(0);
                user.setLockedUntil(now.plusMinutes(props.getLockMinutes()));
                users.save(user);
                audit.record(user.getUserId(), "ACCOUNT_LOCKED_FAILED_LOGINS", email);
            } else {
                user.setFailedLoginCount(failed);
                users.save(user);
            }
            throw new ApiException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }

        // Correct password: reset the counter, then check the account status.
        if (user.getFailedLoginCount() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
            users.save(user);
        }
        switch (user.getAccountStatus()) {
            case PENDING -> throw new ApiException(HttpStatus.FORBIDDEN,
                    "Your officer account is waiting for administrator approval.");
            case REJECTED -> throw new ApiException(HttpStatus.FORBIDDEN,
                    "Your officer registration was not approved. Please contact your administrator.");
            case LOCKED -> throw new ApiException(HttpStatus.FORBIDDEN,
                    "This account is locked. Please contact your administrator.");
            default -> { }
        }

        String refresh = newRefreshToken();
        UUID sid = UUID.randomUUID();
        sessions.save(new UserSession(sid, user.getUserId(), sha256(refresh), expiry()));
        audit.record(user.getUserId(), "LOGIN", email);
        return new Session(tokensFor(user, sid), refresh);
    }

    // ---------------------------------------------------------------- refresh and logout (NFR-S03)

    public Session refresh(String rawRefresh) {
        if (rawRefresh == null || rawRefresh.isBlank()) throw expired();
        UserSession session = sessions.findByRefreshTokenHash(sha256(rawRefresh)).orElseThrow(this::expired);
        if (!session.isActive()) throw expired();
        UserAccount user = users.findById(session.getUserId()).orElse(null);
        if (user == null || user.getAccountStatus() != AccountStatus.ACTIVE) {
            session.revoke();
            sessions.save(session);
            throw expired();
        }
        String next = newRefreshToken(); // rotation: the old refresh token stops working
        session.setRefreshTokenHash(sha256(next));
        session.setExpiresAt(expiry());
        sessions.save(session);
        return new Session(tokensFor(user, session.getSessionId()), next);
    }

    /** Ends the session identified by the refresh cookie and/or the access token's session id. */
    public void logout(String rawRefresh, UUID accessSessionId) {
        if (rawRefresh != null && !rawRefresh.isBlank()) {
            sessions.findByRefreshTokenHash(sha256(rawRefresh)).ifPresent(this::revoke);
        }
        if (accessSessionId != null) {
            sessions.findById(accessSessionId).ifPresent(this::revoke);
        }
    }

    private void revoke(UserSession s) {
        if (s.isActive()) {
            s.revoke();
            sessions.save(s);
            audit.record(s.getUserId(), "LOGOUT", null);
        }
    }

    public UserResponse currentUser(Long userId) {
        return UserResponse.of(users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expired.")));
    }

    // ---------------------------------------------------------------- helpers

    private TokenResponse tokensFor(UserAccount user, UUID sid) {
        return new TokenResponse(jwt.createAccessToken(user, sid), jwt.accessTokenSeconds(), UserResponse.of(user));
    }

    private LocalDateTime expiry() { return LocalDateTime.now().plusDays(props.getRefreshTokenDays()); }

    private ApiException expired() { return new ApiException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again."); }

    private static String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
