package com.medafrica.mavex.security.dto;

import com.medafrica.mavex.model.enums.UserRole;
import lombok.Builder;
import lombok.Data;

/** Réponse renvoyée après login ou refresh */
@Data
@Builder
public class AuthResponseDTO {

    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String fullName;
    private String email;
    private UserRole role;
}