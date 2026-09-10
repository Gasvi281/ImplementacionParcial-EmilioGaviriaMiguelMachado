package com.example.implementacionparcial.auditlog.repository;

import com.example.implementacionparcial.auditlog.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Minimal placeholder: see {@link com.example.implementacionparcial.auditlog.entity.AuditLog}.
 */
public interface IAuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
