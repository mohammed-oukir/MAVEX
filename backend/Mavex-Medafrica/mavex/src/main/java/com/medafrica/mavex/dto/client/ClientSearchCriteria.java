package com.medafrica.mavex.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Critères de recherche pour GET /api/clients/search.
 * Tous les champs sont optionnels : un champ null/vide n'ajoute aucune
 * restriction à la requête (voir ClientSpecification).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSearchCriteria {

    /** Contains, insensible à la casse — matche fullName */
    private String name;

    /** Contains, insensible à la casse — matche email */
    private String email;

    /** Contains, insensible à la casse — matche phone */
    private String phone;

    /** Contains, insensible à la casse — matche city */
    private String city;

    /** Contains, insensible à la casse — matche state */
    private String state;

    /** Contains, insensible à la casse — matche country.code OU country.name (OR interne) */
    private String country;

    /** "all" | "active" | "inactive" — "all" ou null/vide = pas de restriction */
    private String status;

    /** Borne basse sur createdAt (inclusive) */
    private LocalDate dateFrom;

    /** Borne haute sur createdAt (inclusive) */
    private LocalDate dateTo;
}
