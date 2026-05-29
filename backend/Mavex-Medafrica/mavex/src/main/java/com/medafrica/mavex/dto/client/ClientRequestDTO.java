package com.medafrica.mavex.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.medafrica.mavex.model.country.Country;
import lombok.Builder;
import lombok.Data;

/**
 * DTO pour la création d'un nouveau Client.
 * Utilisé dans les requêtes POST /clients
 */
@Data
@Builder
public class ClientRequestDTO {

    @NotBlank(message = "Le nom complet est obligatoire")
    private String fullName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    private String phone;

    private String address;

    private String city;

    /** Code état US sur 2 lettres (NY, CA, CO...) */
    @Size(max = 2, message = "Le code état doit faire 2 caractères max")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Le code état doit être 2 lettres majuscules")
    private String state;

    /** Code postal US — String pour conserver les zéros de gauche */
    private String zipCode;

    /** Par défaut "US" si non fourni */
   // ✅ Après
@NotBlank(message = "Le code pays est obligatoire")
@Size(min = 2, max = 2, message = "Le code pays doit faire 2 caractères")
private String countryCode;
   /*@Builder.Default
    private String country = "US"; */ 

}