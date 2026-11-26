package com.riverwatch.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    /** Ends every other login of this user (used after a password change). */
    @Modifying
    @Query("update UserSession s set s.revokedAt = :now where s.userId = :uid and s.revokedAt is null and s.sessionId <> :keep")
    int revokeOthers(@Param("uid") Long uid, @Param("keep") UUID keep, @Param("now") LocalDateTime now);

    /** Ends every login of this user (used after a password reset). */
    @Modifying
    @Query("update UserSession s set s.revokedAt = :now where s.userId = :uid and s.revokedAt is null")
    int revokeAll(@Param("uid") Long uid, @Param("now") LocalDateTime now);
}
