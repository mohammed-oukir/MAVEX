package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.permission.AgentPermissionsResponse;
import com.medafrica.mavex.dto.permission.PermissionAuditLogDTO;
import com.medafrica.mavex.dto.permission.PermissionCatalogDTO;
import com.medafrica.mavex.dto.permission.UpdateAgentPermissionsRequest;
import com.medafrica.mavex.model.enums.PermissionActionType;
import com.medafrica.mavex.model.enums.UserRole;
import com.medafrica.mavex.model.permission.AgentPermission;
import com.medafrica.mavex.model.permission.PermissionAuditLog;
import com.medafrica.mavex.model.permission.PermissionCatalog;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.AgentPermissionRepository;
import com.medafrica.mavex.repository.PermissionAuditLogRepository;
import com.medafrica.mavex.repository.PermissionCatalogRepository;
import com.medafrica.mavex.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gestion CRUD des permissions accordées aux AGENT (catalogue, lecture,
 * mise à jour par diff, historique d'audit). Ne fait pas d'évaluation
 * de permission — voir PermissionEvaluatorService pour ça.
 */
@Service
@RequiredArgsConstructor
public class AgentPermissionManagementService {

    private final PermissionCatalogRepository   permissionCatalogRepository;
    private final AgentPermissionRepository      agentPermissionRepository;
    private final PermissionAuditLogRepository   permissionAuditLogRepository;
    private final UserRepository                 userRepository;

    public List<PermissionCatalogDTO> getFullCatalog() {
        return permissionCatalogRepository.findAll().stream()
                .map(this::toCatalogDTO)
                .toList();
    }

    public AgentPermissionsResponse getAgentPermissions(Long agentId) {
        User agent = loadAgent(agentId);

        List<Long> permissionIds = agentPermissionRepository.findByAgent_Id(agentId).stream()
                .map(ap -> ap.getPermission().getId())
                .toList();

        return AgentPermissionsResponse.builder()
                .agentId(agent.getId())
                .agentName(agent.getFullName())
                .permissionIds(permissionIds)
                .build();
    }

    @Transactional
    public AgentPermissionsResponse updateAgentPermissions(
            Long agentId, UpdateAgentPermissionsRequest request, Long performedByAdminId) {

        User agent = loadAgent(agentId);
        User admin = userRepository.findById(performedByAdminId)
                .orElseThrow(() -> new EntityNotFoundException("Administrateur introuvable avec l'id : " + performedByAdminId));

        List<AgentPermission> existing = agentPermissionRepository.findByAgent_Id(agentId);
        Set<Long> current = existing.stream()
                .map(ap -> ap.getPermission().getId())
                .collect(Collectors.toSet());

        Set<Long> desired = request.getPermissionIds() != null
                ? new HashSet<>(request.getPermissionIds())
                : new HashSet<>();

        Set<Long> toAdd = new HashSet<>(desired);
        toAdd.removeAll(current);

        Set<Long> toRemove = new HashSet<>(current);
        toRemove.removeAll(desired);

        for (Long permissionId : toAdd) {
            PermissionCatalog permission = permissionCatalogRepository.findById(permissionId)
                    .orElseThrow(() -> new EntityNotFoundException("Permission introuvable avec l'id : " + permissionId));

            agentPermissionRepository.save(AgentPermission.builder()
                    .agent(agent)
                    .permission(permission)
                    .grantedBy(admin)
                    .build());

            permissionAuditLogRepository.save(PermissionAuditLog.builder()
                    .agent(agent)
                    .permission(permission)
                    .actionType(PermissionActionType.GRANTED)
                    .performedBy(admin)
                    .build());
        }

        if (!toRemove.isEmpty()) {
            for (AgentPermission ap : existing) {
                Long permissionId = ap.getPermission().getId();
                if (toRemove.contains(permissionId)) {
                    permissionAuditLogRepository.save(PermissionAuditLog.builder()
                            .agent(agent)
                            .permission(ap.getPermission())
                            .actionType(PermissionActionType.REVOKED)
                            .performedBy(admin)
                            .build());

                    agentPermissionRepository.delete(ap);
                }
            }
        }

        return getAgentPermissions(agentId);
    }

    public List<PermissionAuditLogDTO> getAgentPermissionHistory(Long agentId) {
        loadAgent(agentId);

        return permissionAuditLogRepository.findByAgent_IdOrderByPerformedAtDesc(agentId).stream()
                .map(this::toAuditLogDTO)
                .toList();
    }

    private User loadAgent(Long agentId) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("Agent introuvable avec l'id : " + agentId));

        if (agent.getRole() != UserRole.AGENT) {
            throw new IllegalArgumentException(
                    "L'utilisateur id=" + agentId + " n'a pas le rôle AGENT (rôle actuel : " + agent.getRole() + ").");
        }

        return agent;
    }

    private PermissionCatalogDTO toCatalogDTO(PermissionCatalog p) {
        return PermissionCatalogDTO.builder()
                .id(p.getId())
                .module(p.getModule())
                .action(p.getAction())
                .label(p.getLabel())
                .contextNote(p.getContextNote())
                .build();
    }

    private PermissionAuditLogDTO toAuditLogDTO(PermissionAuditLog log) {
        return PermissionAuditLogDTO.builder()
                .id(log.getId())
                .permissionModule(log.getPermission().getModule())
                .permissionAction(log.getPermission().getAction())
                .permissionLabel(log.getPermission().getLabel())
                .actionType(log.getActionType())
                .performedByName(log.getPerformedBy() != null ? log.getPerformedBy().getFullName() : null)
                .performedAt(log.getPerformedAt())
                .build();
    }
}
