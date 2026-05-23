package com.medafrica.mavex.dto.client;
import com.medafrica.mavex.model.country.Country;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 
 * DTO pour exposer les données d'un Client en réponse.
 * Utilisé dans les réponses GET /clients et POST /clients
 */
@Data
@Builder
public class ClientResponseDTO {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private Country country;
    private LocalDateTime createdAt;
     private boolean active;
}