package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.permission.AgentSummaryDTO;
import com.medafrica.mavex.dto.permission.SavePermissionsRequest;
import com.medafrica.mavex.dto.permission.UserPermissionsDTO;
import com.medafrica.mavex.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints réservés aux ADMIN pour gérer les permissions des agents.
 * Toutes les routes nécessitent le rôle ADMIN.
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * GET /api/permissions/agents
     * Liste tous les agents actifs avec un résumé de leurs permissions.
     * Utilisé pour afficher la liste d'agents dans l'interface de gestion.
     */
    @GetMapping("/agents")
    public ResponseEntity<List<AgentSummaryDTO>> listAgents() {
        return ResponseEntity.ok(permissionService.getAllAgentsSummary());
    }

    /**
     * GET /api/permissions/agents/{userId}
     * Retourne toutes les permissions (toutes combinaisons module×action)
     * d'un agent donné, avec leur état granted/false.
     */
    @GetMapping("/agents/{userId}")
    public ResponseEntity<UserPermissionsDTO> getAgentPermissions(@PathVariable Long userId) {
        return ResponseEntity.ok(permissionService.getPermissionsForUser(userId));
    }

    /**
     * PUT /api/permissions/agents/{userId}
     * Sauvegarde (remplace complètement) les permissions d'un agent.
     * Les règles de cascade sont revalidées côté backend avant sauvegarde.
     */
    @PutMapping("/agents/{userId}")
    public ResponseEntity<UserPermissionsDTO> saveAgentPermissions(
            @PathVariable Long userId,
            @Valid @RequestBody SavePermissionsRequest request) {
        return ResponseEntity.ok(
                permissionService.savePermissions(userId, request.getPermissions())
        );
    }
}
