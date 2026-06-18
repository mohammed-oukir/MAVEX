package com.medafrica.mavex.security.dto;

import com.medafrica.mavex.model.enums.PermissionModule;
import com.medafrica.mavex.model.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;

/** Réponse renvoyée après login ou refresh */
@Data
@Builder
public class AuthResponseDTO {

    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String fullName;
    private String email;
    private UserRole role;

    /**
     * Permissions effectives pour l'affichage frontend uniquement.
     * Le backend ne se base JAMAIS sur ce champ pour autoriser une action —
     * chaque requête est vérifiée en base via PermissionService.
     */
    private Map<PermissionModule, Set<String>> permissions;
}