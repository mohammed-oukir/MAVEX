package com.medafrica.mavex.security.service;

import com.medafrica.mavex.service.email.EmailProviderResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailProviderResolver providerResolver;

    public void sendOtpEmail(String toEmail, String fullName, String otp) {
        try {
            String html = "<p>Bonjour " + fullName + ",</p>" +
                          "<p>Votre code de réinitialisation est : <strong>" + otp + "</strong></p>" +
                          "<p>Ce code est valable 15 minutes.</p>" +
                          "<p>Si vous n'avez pas demandé ce code, ignorez cet email.</p>" +
                          "<p>L'équipe MedAfrica</p>";

            providerResolver.resolve().send(
                toEmail,
                "MedAfrica - Code de réinitialisation de mot de passe",
                html,
                List.of()
            );
            log.info("OTP email envoyé à {}", toEmail);

        } catch (Exception e) {
            log.error("Erreur envoi OTP à {} : {}", toEmail, e.getMessage());
            throw new RuntimeException("Échec envoi OTP", e);
        }
    }
}
