package com.riverwatch.config;

import com.riverwatch.common.AuditService;
import com.riverwatch.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first System Administrator from environment variables (ADMIN_EMAIL, ADMIN_PASSWORD)
 * when that email does not exist yet. The password is hashed with BCrypt; nothing is hardcoded.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppProperties props;
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AdminBootstrap(AppProperties props, UserAccountRepository users, PasswordEncoder encoder, AuditService audit) {
        this.props = props;
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Admin a = props.getAdmin();
        if (a.getEmail() == null || a.getEmail().isBlank() || a.getPassword() == null || a.getPassword().isBlank()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set: no administrator account created.");
            return;
        }
        if (a.getPassword().length() < 8) {
            log.warn("ADMIN_PASSWORD is shorter than 8 characters: administrator not created.");
            return;
        }
        String email = a.getEmail().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) return;
        UserAccount u = new UserAccount();
        u.setFullName(a.getName());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(a.getPassword()));
        u.setRole(Role.ADMIN);
        u.setAccountStatus(AccountStatus.ACTIVE);
        u = users.save(u);
        audit.record(u.getUserId(), "ADMIN_BOOTSTRAPPED", email);
        log.info("Administrator account created for {}", email);
    }
}
