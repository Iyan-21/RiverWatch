package com.riverwatch.common;

import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    public void record(Long userId, String action, String details) {
        repo.save(new AuditLog(userId, action, details));
    }
}
