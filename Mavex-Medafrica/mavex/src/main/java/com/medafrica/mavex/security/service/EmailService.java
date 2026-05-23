package com.medafrica.mavex.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
 
@Service
@RequiredArgsConstructor
public class EmailService {
 
    private final JavaMailSender mailSender;
 
    @Value("${spring.mail.username}")
    private String fromEmail;
 
    public void sendOtpEmail(String toEmail, String fullName, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Mavex - Code de réinitialisation de mot de passe");
        message.setText(
            "Bonjour " + fullName + ",\n\n" +
            "Votre code de réinitialisation est : " + otp + "\n\n" +
            "Ce code est valable 15 minutes.\n\n" +
            "Si vous n'avez pas demandé ce code, ignorez cet email.\n\n" +
            "L'équipe Mavex"
        );
        mailSender.send(message);
    }
}
