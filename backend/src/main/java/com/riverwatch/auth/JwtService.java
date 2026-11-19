package com.riverwatch.auth;

import com.riverwatch.config.AppProperties;
import com.riverwatch.user.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final AppProperties props;

    public JwtService(AppProperties props) {
        this.props = props;
        byte[] secret = props.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long.");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public String createAccessToken(UserAccount user, UUID sessionId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("role", user.getRole().name())
                .claim("sid", sessionId.toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + props.getAccessTokenMinutes() * 60_000L))
                .signWith(key)
                .compact();
    }

    /** Returns the claims if the token is correctly signed and not expired, otherwise empty. */
    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public int accessTokenSeconds() { return props.getAccessTokenMinutes() * 60; }
}
