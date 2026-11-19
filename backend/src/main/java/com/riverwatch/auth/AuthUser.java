package com.riverwatch.auth;

import com.riverwatch.user.Role;
import java.util.UUID;

/** The authenticated principal placed in the security context for each request. */
public record AuthUser(Long userId, String email, Role role, Long barangayId, UUID sessionId) {}
