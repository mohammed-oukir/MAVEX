package com.medafrica.mavex.dto.airline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AirlineDTO {

    @NotBlank
    @Pattern(regexp = "\\d{3}", message = "Le préfixe doit être 3 chiffres")
    private String prefix;

    @NotBlank
    private String name;

    @Size(max = 2)
    private String iataCode;

    @Size(max = 3)
    private String icaoCode;

    private String country;

    private String mode;
}
