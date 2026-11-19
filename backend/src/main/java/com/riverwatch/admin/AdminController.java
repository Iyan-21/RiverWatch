package com.riverwatch.admin;

import com.riverwatch.auth.AuthUser;
import com.riverwatch.auth.UserResponse;
import com.riverwatch.common.ApiException;
import com.riverwatch.common.AuditService;
import com.riverwatch.user.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** FR-003: the System Administrator approves or rejects officer accounts. */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final UserAccountRepository users;
    private final AuditService audit;

    public AdminController(UserAccountRepository users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    @GetMapping("/officers/pending")
    public List<UserResponse> pendingOfficers() {
        return users.findByRoleAndAccountStatusOrderByCreatedAtAsc(Role.OFFICER, AccountStatus.PENDING)
                .stream().map(UserResponse::of).toList();
    }

    @PostMapping("/officers/{id}/approve")
    public UserResponse approve(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        return decide(id, admin, AccountStatus.ACTIVE, "OFFICER_APPROVED");
    }

    @PostMapping("/officers/{id}/reject")
    public UserResponse reject(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        return decide(id, admin, AccountStatus.REJECTED, "OFFICER_REJECTED");
    }

    private UserResponse decide(Long id, AuthUser admin, AccountStatus newStatus, String action) {
        UserAccount officer = users.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Officer account not found."));
        if (officer.getRole() != Role.OFFICER || officer.getAccountStatus() != AccountStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "This account is not waiting for approval.");
        }
        officer.setAccountStatus(newStatus);
        users.save(officer);
        audit.record(admin.userId(), action, "officer=" + officer.getUserId());
        return UserResponse.of(officer);
    }
}
