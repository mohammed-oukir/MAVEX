package com.medafrica.mavex.dto.client;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
public class ClientPatchRequest {

    private String fullName;

    @Email(message = "Format d'email invalide")
    private String email;

    private String phone;
    private String address;
    private String city;

    @Size(max = 2, message = "Le code état doit faire 2 caractères max")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Le code état doit être 2 lettres majuscules")
    private String state;

    private String zipCode;

    @Size(min = 2, max = 2, message = "Le code pays doit faire 2 caractères")
    private String countryCode;

    private Boolean active;
}