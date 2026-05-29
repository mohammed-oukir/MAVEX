package com.medafrica.mavex.security.controller;

import com.medafrica.mavex.dto.ApiResponse;
import com.medafrica.mavex.security.dto.AuthResponseDTO;
import com.medafrica.mavex.security.dto.LoginRequestDTO;
import com.medafrica.mavex.security.dto.RefreshTokenRequestDTO;
import com.medafrica.mavex.security.dto.ForgotPasswordRequestDTO;
import com.medafrica.mavex.security.dto.ResetPasswordRequestDTO;
import com.medafrica.mavex.security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO dto) {
        AuthResponseDTO data = authService.login(dto);
        return ResponseEntity.ok(ApiResponse.<AuthResponseDTO>builder()
                .message("Connexion réussie")
                .data(data)
                .build());
    }

    /** POST /api/auth/refresh */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refresh(
            @Valid @RequestBody RefreshTokenRequestDTO dto) {
        AuthResponseDTO data = authService.refresh(dto);
        return ResponseEntity.ok(ApiResponse.<AuthResponseDTO>builder()
                .message("Token rafraîchi avec succès")
                .data(data)
                .build());
    }

    /** POST /api/auth/logout */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequestDTO dto) {
        authService.logout(dto);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Déconnexion réussie")
                .data(null)
                .build());
    }

    /** POST /api/auth/forgot-password */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO dto) {
        authService.forgotPassword(dto);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Email de réinitialisation envoyé avec succès")
                .data(null)
                .build());
    }

    /** POST /api/auth/reset-password */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO dto) {
        authService.resetPassword(dto);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Mot de passe réinitialisé avec succès")
                .data(null)
                .build());
    }
}