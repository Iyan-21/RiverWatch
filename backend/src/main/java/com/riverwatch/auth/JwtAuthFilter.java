package com.riverwatch.auth;

import com.riverwatch.user.AccountStatus;
import com.riverwatch.user.UserAccount;
import com.riverwatch.user.UserAccountRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads "Authorization: Bearer ...", verifies the signature and expiry, then checks that the session
 * has not been revoked (logout) and that the account is still ACTIVE. If anything fails the request
 * simply stays unauthenticated and is answered with 401 by the entry point.
 */
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserSessionRepository sessions;
    private final UserAccountRepository users;

    public JwtAuthFilter(JwtService jwtService, UserSessionRepository sessions, UserAccountRepository users) {
        this.jwtService = jwtService;
        this.sessions = sessions;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authenticate(header.substring(7).trim());
        }
        chain.doFilter(req, res);
    }

    private void authenticate(String token) {
        Optional<Claims> claims = jwtService.parse(token);
        if (claims.isEmpty()) return;
        try {
            Long userId = Long.valueOf(claims.get().getSubject());
            UUID sid = UUID.fromString(claims.get().get("sid", String.class));
            Optional<UserSession> session = sessions.findById(sid);
            if (session.isEmpty() || !session.get().isActive() || !session.get().getUserId().equals(userId)) return;
            Optional<UserAccount> user = users.findById(userId);
            if (user.isEmpty() || user.get().getAccountStatus() != AccountStatus.ACTIVE) return;
            UserAccount u = user.get();
            AuthUser principal = new AuthUser(u.getUserId(), u.getEmail(), u.getRole(), u.barangayId(), sid);
            var auth = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (RuntimeException e) {
            // malformed claims: treat as unauthenticated
        }
    }
}
