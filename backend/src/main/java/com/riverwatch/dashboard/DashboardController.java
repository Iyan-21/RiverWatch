package com.riverwatch.dashboard;

import com.riverwatch.auth.AuthUser;
import com.riverwatch.auth.UserResponse;
import com.riverwatch.common.ApiException;
import com.riverwatch.user.*;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Role-specific dashboard data. URL-level role rules are in SecurityConfig; the data below is
 * additionally scoped by the caller's own barangay (FR-005, NFR-S05, NFR-D03).
 */
@RestController
@RequestMapping("/api/v1")
public class DashboardController {
    public record Dashboard(UserResponse user, Map<String, Long> stats) {}
    public record ResidentRow(Long userId, String fullName, String email, String phoneNumber) {}
    public record ResidentPage(List<ResidentRow> items, int page, int size, long total) {}

    private final UserAccountRepository users;

    public DashboardController(UserAccountRepository users) { this.users = users; }

    @GetMapping("/resident/dashboard")
    public Dashboard resident(@AuthenticationPrincipal AuthUser me) {
        UserAccount u = load(me);
        long officers = users.countByBarangay_BarangayIdAndRoleAndAccountStatus(
                me.barangayId(), Role.OFFICER, AccountStatus.ACTIVE);
        return new Dashboard(UserResponse.of(u), Map.of("activeOfficersInMyBarangay", officers));
    }

    @GetMapping("/officer/dashboard")
    public Dashboard officer(@AuthenticationPrincipal AuthUser me) {
        UserAccount u = load(me);
        long residents = users.countByBarangay_BarangayIdAndRoleAndAccountStatus(
                me.barangayId(), Role.RESIDENT, AccountStatus.ACTIVE);
        return new Dashboard(UserResponse.of(u), Map.of("registeredResidents", residents));
    }

    /** An officer can list only the residents of the officer's own barangay, 25 per page (FR-012 pattern). */
    @GetMapping("/officer/residents")
    public ResidentPage residents(@AuthenticationPrincipal AuthUser me,
                                  @RequestParam(defaultValue = "0") int page) {
        Page<UserAccount> p = users.findByBarangay_BarangayIdAndRoleAndAccountStatus(
                me.barangayId(), Role.RESIDENT, AccountStatus.ACTIVE,
                PageRequest.of(Math.max(page, 0), 25, org.springframework.data.domain.Sort.by("fullName")));
        List<ResidentRow> rows = p.getContent().stream()
                .map(r -> new ResidentRow(r.getUserId(), r.getFullName(), r.getEmail(), r.getPhoneNumber()))
                .toList();
        return new ResidentPage(rows, p.getNumber(), p.getSize(), p.getTotalElements());
    }

    @GetMapping("/admin/dashboard")
    public Dashboard admin(@AuthenticationPrincipal AuthUser me) {
        UserAccount u = load(me);
        return new Dashboard(UserResponse.of(u), Map.of(
                "residents", users.countByRole(Role.RESIDENT),
                "activeOfficers", users.countByRoleAndAccountStatus(Role.OFFICER, AccountStatus.ACTIVE),
                "pendingOfficers", users.countByRoleAndAccountStatus(Role.OFFICER, AccountStatus.PENDING)));
    }

    private UserAccount load(AuthUser me) {
        return users.findById(me.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expired."));
    }
}
