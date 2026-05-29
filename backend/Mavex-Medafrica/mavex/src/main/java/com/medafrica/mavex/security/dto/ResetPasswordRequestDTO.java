package com.medafrica.mavex.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** POST /api/auth/reset-password */
@Data
public class ResetPasswordRequestDTO {

    @NotBlank(message = "L'email est obligatoire")
    private String email;

    @NotBlank(message = "L'OTP est obligatoire")
    private String otp;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String newPassword;
}
