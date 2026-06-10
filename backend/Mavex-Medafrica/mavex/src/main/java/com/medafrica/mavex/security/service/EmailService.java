package com.medafrica.mavex.security.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Value("${sendgrid.api.key}")
    private String sendgridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    @Value("${sendgrid.from.name:MAVEX}")
    private String fromName;

    public void sendOtpEmail(String toEmail, String fullName, String otp) {
        try {
            Mail mail = new Mail();
            mail.setFrom(new Email(fromEmail, fromName));
            mail.setSubject("Mavex - Code de réinitialisation de mot de passe");

            Personalization personalization = new Personalization();
            personalization.addTo(new Email(toEmail));
            mail.addPersonalization(personalization);

            String body =
                "Bonjour " + fullName + ",\n\n" +
                "Votre code de réinitialisation est : " + otp + "\n\n" +
                "Ce code est valable 15 minutes.\n\n" +
                "Si vous n'avez pas demandé ce code, ignorez cet email.\n\n" +
                "L'équipe Mavex";

            mail.addContent(new Content("text/plain", body));

            SendGrid sg = new SendGrid(sendgridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 400) {
                log.error("SendGrid OTP error {} : {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Échec envoi OTP : " + response.getBody());
            }

            log.info("OTP email envoyé à {}", toEmail);

        } catch (Exception e) {
            log.error("Erreur envoi OTP à {} : {}", toEmail, e.getMessage());
            throw new RuntimeException("Échec envoi OTP", e);
        }
    }
}
