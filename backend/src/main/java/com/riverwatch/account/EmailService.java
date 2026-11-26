package com.riverwatch.account;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/**
 * Sends the password reset email through SMTP (settings come from the environment / .env:
 * MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM). Sending happens in the background so
 * the response time does not reveal whether an email address is registered.
 * If MAIL_HOST is empty (local development only) the link is written to the server log instead.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;

    public EmailService(@Value("${MAIL_HOST:}") String host,
                        @Value("${MAIL_PORT:587}") int port,
                        @Value("${MAIL_USERNAME:}") String username,
                        @Value("${MAIL_PASSWORD:}") String password,
                        @Value("${MAIL_FROM:}") String from) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.from = from;
    }

    public void sendPasswordReset(String to, String name, String link) {
        String body = "Hello " + name + ",\n\n"
                + "We received a request to reset your RiverWatch Cebu password.\n"
                + "Open this link within 30 minutes to choose a new password (it can be used only once):\n\n"
                + link + "\n\n"
                + "If you did not ask for this, you can ignore this email. Your password will not change.\n\n"
                + "RiverWatch Cebu";
        if (host == null || host.isBlank()) {
            log.warn("MAIL_HOST is not set, so no email was sent. DEVELOPMENT ONLY - reset link for {}: {}", to, link);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                JavaMailSenderImpl sender = new JavaMailSenderImpl();
                sender.setHost(host);
                sender.setPort(port);
                sender.setUsername(username);
                sender.setPassword(password);
                Properties p = sender.getJavaMailProperties();
                p.put("mail.smtp.auth", "true");
                p.put("mail.smtp.starttls.enable", "true");
                p.put("mail.smtp.connectiontimeout", "8000");
                p.put("mail.smtp.timeout", "8000");
                p.put("mail.smtp.writetimeout", "8000");
                SimpleMailMessage m = new SimpleMailMessage();
                m.setFrom(from == null || from.isBlank() ? username : from);
                m.setTo(to);
                m.setSubject("Reset your RiverWatch Cebu password");
                m.setText(body);
                sender.send(m);
                log.info("Password reset email sent to {}", to);
            } catch (Exception e) {
                log.error("Could not send password reset email to {}: {}", to, e.getMessage());
            }
        });
    }
}
