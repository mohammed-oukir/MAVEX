package com.medafrica.mavex.dto.permission;

import com.medafrica.mavex.model.enums.PermissionModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Permissions complètes d'un agent, sous deux formes :
 * - entries : liste plate (pour sauvegarde)
 * - byModule : map module → set d'actions accordées (pour affichage frontend)
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserPermissionsDTO {
    private Long userId;
    private String fullName;
    private String email;
    private List<PermissionEntryDTO> entries;
    private Map<PermissionModule, Set<String>> byModule;
}
