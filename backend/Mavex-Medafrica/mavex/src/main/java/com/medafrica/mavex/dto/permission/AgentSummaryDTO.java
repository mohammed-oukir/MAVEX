package com.medafrica.mavex.dto.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Résumé d'un agent pour la liste de l'écran de gestion des permissions. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AgentSummaryDTO {
    private Long userId;
    private String fullName;
    private String email;
    private int totalGranted;
}
