package com.example.implementacionparcial.auditlog.service;

import com.example.implementacionparcial.auditlog.entity.AuditLog;
import com.example.implementacionparcial.auditlog.repository.IAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Minimal placeholder service: this domain's read side (dto/mapper/controller) is owned by whoever
 * implements {@code feature/auditlog}. {@code record(...)} is already the final contract other
 * domains depend on.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final IAuditLogRepository auditLogRepository;

    @Transactional
    public void record(UUID userId, String action, String entityType, UUID entityId, String details) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }
}
