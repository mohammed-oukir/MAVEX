package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.permission.AgentPermissionsResponse;
import com.medafrica.mavex.dto.permission.PermissionAuditLogDTO;
import com.medafrica.mavex.dto.permission.PermissionCatalogDTO;
import com.medafrica.mavex.dto.permission.UpdateAgentPermissionsRequest;
import com.medafrica.mavex.model.enums.UserRole;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.service.AgentPermissionManagementService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * Gestion des permissions AGENT — strictement réservée à ADMIN,
 * jamais déléguée même avec permission explicite.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AgentPermissionController {

    private final AgentPermissionManagementService permissionManagementService;

    @GetMapping("/api/permission-catalog")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PermissionCatalogDTO>> getFullCatalog() {
        return ResponseEntity.ok(permissionManagementService.getFullCatalog());
    }

    @GetMapping("/api/agent-permissions/{agentId}")
    public ResponseEntity<AgentPermissionsResponse> getAgentPermissions(@PathVariable Long agentId) {
        return ResponseEntity.ok(permissionManagementService.getAgentPermissions(agentId));
    }

    @PutMapping("/api/agent-permissions/{agentId}")
    public ResponseEntity<AgentPermissionsResponse> updateAgentPermissions(
            @PathVariable Long agentId,
            @RequestBody UpdateAgentPermissionsRequest request) {
        Long adminId = currentUserId();
        return ResponseEntity.ok(
                permissionManagementService.updateAgentPermissions(agentId, request, adminId));
    }

    @GetMapping("/api/agent-permissions/{agentId}/history")
    public ResponseEntity<List<PermissionAuditLogDTO>> getAgentPermissionHistory(@PathVariable Long agentId) {
        return ResponseEntity.ok(permissionManagementService.getAgentPermissionHistory(agentId));
    }

    /**
     * Consultation de SES PROPRES permissions par l'utilisateur connecté
     * (ADMIN, AGENT ou COMPTABLE). Déroge à @PreAuthorize("hasRole('ADMIN')")
     * de la classe : accessible à tout utilisateur authentifié.
     * Ne retourne jamais les permissions d'un autre utilisateur.
     */
    @GetMapping("/api/agent-permissions/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgentPermissionsResponse> getMyPermissions() {
        User currentUser = currentUser();

        if (currentUser.getRole() != UserRole.AGENT) {
            return ResponseEntity.ok(AgentPermissionsResponse.builder()
                    .agentId(currentUser.getId())
                    .agentName(currentUser.getFullName())
                    .permissionIds(Collections.emptyList())
                    .build());
        }

        return ResponseEntity.ok(permissionManagementService.getAgentPermissions(currentUser.getId()));
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new EntityNotFoundException("Aucun administrateur authentifié dans le contexte de sécurité.");
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        throw new EntityNotFoundException("Aucun utilisateur authentifié dans le contexte de sécurité.");
    }
}
