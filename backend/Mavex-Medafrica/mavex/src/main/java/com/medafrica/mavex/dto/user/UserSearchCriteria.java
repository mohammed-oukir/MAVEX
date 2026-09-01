package com.medafrica.mavex.dto.user;

import com.medafrica.mavex.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Critères de recherche pour GET /api/users/search.
 * Tous les champs sont optionnels : un champ null/vide n'ajoute aucune
 * restriction à la requête (voir UserSpecification).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchCriteria {

    /** Contains, insensible à la casse — matche fullName */
    private String fullName;

    /** Contains, insensible à la casse — matche email */
    private String email;

    /** ADMIN | AGENT | COMPTABLE — null = pas de restriction */
    private UserRole role;

    /** "all" | "active" | "inactive" — "all" ou null/vide = pas de restriction (même convention que ClientSearchCriteria.status) */
    private String status;
}
