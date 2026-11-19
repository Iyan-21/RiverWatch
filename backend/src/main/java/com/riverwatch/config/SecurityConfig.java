package com.riverwatch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverwatch.auth.JwtAuthFilter;
import com.riverwatch.auth.JwtService;
import com.riverwatch.auth.UserSessionRepository;
import com.riverwatch.common.ApiError;
import com.riverwatch.user.UserAccountRepository;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** BCrypt with cost 10 (NFR-S01). The salt is generated and stored inside each hash. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, UserSessionRepository sessions,
                                           UserAccountRepository users, ObjectMapper mapper) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // stateless: bearer token header + SameSite=Strict refresh cookie
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/auth/register/**", "/api/v1/auth/login",
                                 "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/barangays").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/officer/**").hasRole("OFFICER")
                .requestMatchers("/api/v1/resident/**").hasRole("RESIDENT")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    mapper.writeValue(res.getOutputStream(), ApiError.of(401, "Please log in to continue."));
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    mapper.writeValue(res.getOutputStream(),
                            ApiError.of(403, "You do not have permission to access this resource."));
                }))
            .addFilterBefore(new JwtAuthFilter(jwt, sessions, users), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Only needed when the React app is served from a different origin than the API. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (props.getCorsOrigin() != null && !props.getCorsOrigin().isBlank()) {
            CorsConfiguration c = new CorsConfiguration();
            c.setAllowedOrigins(List.of(props.getCorsOrigin().split(",")));
            c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
            c.setAllowCredentials(true);
            source.registerCorsConfiguration("/api/**", c);
        }
        return source;
    }
}
