package com.riverwatch.account;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    /** Marks a token as used only if it is still unused and unexpired. Returns 1 for the one request that wins. */
    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now where t.tokenHash = :hash and t.usedAt is null and t.expiresAt > :now")
    int consume(@Param("hash") String hash, @Param("now") LocalDateTime now);

    /** Cancels older unused links when a new one is issued. */
    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now where t.userId = :uid and t.usedAt is null")
    int invalidateOpen(@Param("uid") Long uid, @Param("now") LocalDateTime now);
}
