package com.medafrica.mavex.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from.email}")
    private String fromEmail;

    @Value("${app.mail.from.name:MedAfrica}")
    private String fromName;

    public void sendOtpEmail(String toEmail, String fullName, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromEmail + ">");
            message.setTo(toEmail);
            message.setSubject("MedAfrica - Code de réinitialisation de mot de passe");
            message.setText(
                "Bonjour " + fullName + ",\n\n" +
                "Votre code de réinitialisation est : " + otp + "\n\n" +
                "Ce code est valable 15 minutes.\n\n" +
                "Si vous n'avez pas demandé ce code, ignorez cet email.\n\n" +
                "L'équipe MedAfrica"
            );

            mailSender.send(message);
            log.info("OTP email envoyé à {}", toEmail);

        } catch (Exception e) {
            log.error("Erreur envoi OTP à {} : {}", toEmail, e.getMessage());
            throw new RuntimeException("Échec envoi OTP", e);
        }
    }
}
