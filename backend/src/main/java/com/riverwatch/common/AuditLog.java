package com.riverwatch.common;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String action;

    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected AuditLog() {}

    public AuditLog(Long userId, String action, String details) {
        this.userId = userId;
        this.action = action;
        this.details = details;
    }
}
