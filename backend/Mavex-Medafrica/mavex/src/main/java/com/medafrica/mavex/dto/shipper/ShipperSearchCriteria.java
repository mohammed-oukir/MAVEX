package com.medafrica.mavex.dto.shipper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Critères de recherche pour GET /api/shippers/search.
 * Tous les champs sont optionnels : un champ null/vide n'ajoute aucune
 * restriction à la requête (voir ShipperSpecification).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipperSearchCriteria {

    /** Contains, insensible à la casse — matche companyName */
    private String company;

    /** Contains, insensible à la casse — matche contactName */
    private String contact;

    /** Contains, insensible à la casse — matche email */
    private String email;

    /** Contains, insensible à la casse — matche phone */
    private String phone;

    /** Contains, insensible à la casse — matche city */
    private String city;

    /** Contains, insensible à la casse — matche country.code uniquement */
    private String country;

    /** "all" | "active" | "inactive" — "all" ou null/vide = pas de restriction */
    private String status;
}
