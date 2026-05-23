package com.medafrica.mavex.dto.shipper;

import com.medafrica.mavex.model.country.Country;

import jakarta.validation.Valid;
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
@Valid
    
    private Country countryCode;
}