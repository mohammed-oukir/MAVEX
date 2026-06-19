package com.medafrica.mavex.service.email;

import com.medafrica.mavex.model.email.EmailProviderConfig;
import com.medafrica.mavex.model.enums.EmailProviderType;
import com.medafrica.mavex.repository.EmailProviderConfigRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Properties;

/**
 * Provider SMTP dynamique — construit JavaMailSenderImpl a la volee depuis EmailProviderConfig.
 * N'utilise pas le bean JavaMailSender autoconfigue par Spring Boot (fixe au demarrage).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailProviderService implements EmailProviderService {

    private final EmailProviderConfigRepository configRepository;
    private final EncryptionService             encryptionService;

    @Override
    public EmailProviderType getType() {
        return EmailProviderType.SMTP;
    }

    @Override
    public String send(String toEmail, String subject, String htmlBody,
                       List<MultipartFile> attachments) throws Exception {

        EmailProviderConfig config = configRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Aucun provider email actif en base."));

        if (config.getSmtpPasswordEncrypted() == null) {
            throw new IllegalStateException("Le mot de passe SMTP n'est pas configure.");
        }

        JavaMailSenderImpl sender = buildSender(config);

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(config.getFromName() + " <" + config.getFromEmail() + ">");
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        if (attachments != null) {
            for (MultipartFile file : attachments) {
                if (file != null && !file.isEmpty()) {
                    helper.addAttachment(
                        file.getOriginalFilename() != null
                            ? file.getOriginalFilename() : "attachment",
                        file
                    );
                }
            }
        }

        sender.send(message);
        log.debug("Email SMTP envoye a {} via {}:{}", toEmail, config.getSmtpHost(), config.getSmtpPort());
        return null; // SMTP n'expose pas de messageId
    }

    private JavaMailSenderImpl buildSender(EmailProviderConfig config) {
        String password = encryptionService.decrypt(config.getSmtpPasswordEncrypted());

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getSmtpHost());
        sender.setPort(config.getSmtpPort());
        sender.setUsername(config.getSmtpUsername());
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        if (config.isSmtpSslEnabled()) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust",  config.getSmtpHost());
        }

        if (config.isSmtpTlsEnabled()) {
            props.put("mail.smtp.starttls.enable",   "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        return sender;
    }
}
