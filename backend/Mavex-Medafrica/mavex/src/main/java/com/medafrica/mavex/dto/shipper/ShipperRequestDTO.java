package com.medafrica.mavex.dto.shipper;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipperRequestDTO {

    @NotBlank(message = "Le nom de la société est obligatoire")
    private String companyName;

    private String contactName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    private String phone;
    private String address;
    private String city;

    /** Code ISO pays ex: MA */
    @Size(min = 2, max = 2, message = "Le code pays doit faire 2 caractères")
    private String countryCode;
}