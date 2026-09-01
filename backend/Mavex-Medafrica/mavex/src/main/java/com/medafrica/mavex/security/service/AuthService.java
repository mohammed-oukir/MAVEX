package com.medafrica.mavex.security.service;


import com.medafrica.mavex.dto.user.*;
import com.medafrica.mavex.model.security.PasswordResetOtp;
import com.medafrica.mavex.model.security.RefreshToken;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.UserRepository;
import com.medafrica.mavex.security.repository.PasswordResetOtpRepository;
import com.medafrica.mavex.security.repository.RefreshTokenRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.medafrica.mavex.security.dto.AuthResponseDTO;
import com.medafrica.mavex.security.dto.LoginRequestDTO;
import com.medafrica.mavex.security.dto.RefreshTokenRequestDTO;
import com.medafrica.mavex.security.dto.ForgotPasswordRequestDTO;
import com.medafrica.mavex.security.dto.ResetPasswordRequestDTO;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository             userRepository;
    private final RefreshTokenRepository     refreshTokenRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final JwtService                 jwtService;
    private final PasswordEncoder            passwordEncoder;
    private final EmailService               emailService;

    // ─────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────
    public AuthResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Compte désactivé");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ─────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────
    public AuthResponseDTO refresh(RefreshTokenRequestDTO dto) {
        RefreshToken stored = refreshTokenRepository.findByToken(dto.getRefreshToken())
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalide"));

        if (!stored.isValid()) {
            throw new BadCredentialsException("Refresh token expiré ou révoqué");
        }

        // Rotation : on révoque l'ancien et on en crée un nouveau
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user           = stored.getUser();
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ─────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────
    public void logout(RefreshTokenRequestDTO dto) {
        refreshTokenRepository.findByToken(dto.getRefreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    // ─────────────────────────────────────────────
    // FORGOT PASSWORD - envoie OTP par email
    // ─────────────────────────────────────────────
    public void forgotPassword(ForgotPasswordRequestDTO dto) {
        // On ne révèle pas si l'email existe ou non (sécurité)
        userRepository.findByEmail(dto.getEmail()).ifPresent(user -> {
            // Invalider les anciens OTPs
            otpRepository.invalidateAllByEmail(dto.getEmail());

            // Générer OTP 6 chiffres
            String otp = String.format("%06d", new Random().nextInt(999999));

            PasswordResetOtp resetOtp = PasswordResetOtp.builder()
                    .email(dto.getEmail())
                    .otp(otp)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();

            otpRepository.save(resetOtp);
            emailService.sendOtpEmail(dto.getEmail(), user.getFullName(), otp);
        });
    }

    // ─────────────────────────────────────────────
    // RESET PASSWORD - vérifie OTP et change le mdp
    // ─────────────────────────────────────────────
    public void resetPassword(ResetPasswordRequestDTO dto) {
        PasswordResetOtp resetOtp = otpRepository.findLatestValidByEmail(dto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("OTP invalide ou expiré"));

        if (!resetOtp.isValid()) {
            throw new BadCredentialsException("OTP invalide ou expiré");
        }

        if (!resetOtp.getOtp().equals(dto.getOtp())) {
            throw new BadCredentialsException("OTP incorrect");
        }

        // Marquer l'OTP comme utilisé
        resetOtp.setUsed(true);
        otpRepository.save(resetOtp);

        // Mettre à jour le mot de passe
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // Révoquer tous les refresh tokens actifs (sécurité)
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    // ─────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────
    private String createRefreshToken(User user) {
        String token = java.util.UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private AuthResponseDTO buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}