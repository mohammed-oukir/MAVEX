package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.permission.PermissionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissionAuditLogRepository extends JpaRepository<PermissionAuditLog, Long> {

    List<PermissionAuditLog> findByAgent_IdOrderByPerformedAtDesc(Long agentId);
}
