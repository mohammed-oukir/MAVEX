package com.medafrica.mavex.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** POST /api/auth/refresh */
@Data
public class RefreshTokenRequestDTO {

    @NotBlank(message = "Le refresh token est obligatoire")
    private String refreshToken;
}