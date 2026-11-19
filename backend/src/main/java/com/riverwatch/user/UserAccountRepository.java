package com.riverwatch.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<UserAccount> findByRoleAndAccountStatusOrderByCreatedAtAsc(Role role, AccountStatus status);

    long countByRoleAndAccountStatus(Role role, AccountStatus status);

    long countByRole(Role role);

    Page<UserAccount> findByBarangay_BarangayIdAndRoleAndAccountStatus(
            Long barangayId, Role role, AccountStatus status, Pageable pageable);

    long countByBarangay_BarangayIdAndRoleAndAccountStatus(Long barangayId, Role role, AccountStatus status);
}
