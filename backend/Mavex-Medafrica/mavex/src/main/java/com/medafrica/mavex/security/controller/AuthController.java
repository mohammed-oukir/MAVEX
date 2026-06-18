package com.medafrica.mavex.security.controller;

import com.medafrica.mavex.dto.ApiResponse;
import com.medafrica.mavex.model.enums.PermissionModule;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.security.dto.AuthResponseDTO;
import com.medafrica.mavex.security.dto.LoginRequestDTO;
import com.medafrica.mavex.security.dto.RefreshTokenRequestDTO;
import com.medafrica.mavex.security.dto.ForgotPasswordRequestDTO;
import com.medafrica.mavex.security.dto.ResetPasswordRequestDTO;
import com.medafrica.mavex.security.service.AuthService;
import com.medafrica.mavex.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService       authService;
    private final PermissionService permissionService;

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

    /**
     * GET /api/auth/me/permissions
     * Retourne les permissions effectives de l'utilisateur connecté.
     * Utilisé par le frontend pour rafraîchir les droits affichés
     * sans avoir à se reconnecter (ex : un admin vient de modifier les droits).
     * Ne sert PAS à la sécurité réelle — chaque endpoint vérifie en base.
     */
    @GetMapping("/me/permissions")
    public ResponseEntity<ApiResponse<Map<PermissionModule, Set<String>>>> getMyPermissions() {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(401).build();
        }
        Map<PermissionModule, Set<String>> perms = permissionService.getEffectivePermissionsMap(user);
        return ResponseEntity.ok(ApiResponse.<Map<PermissionModule, Set<String>>>builder()
                .message("Permissions récupérées")
                .data(perms)
                .build());
    }
}