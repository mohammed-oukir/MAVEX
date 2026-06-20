    package com.medafrica.mavex.service.email;

    import com.medafrica.mavex.dto.email.EmailProviderConfigRequest;
    import com.medafrica.mavex.dto.email.EmailProviderConfigResponse;
    import com.medafrica.mavex.model.email.EmailProviderConfig;
    import com.medafrica.mavex.model.enums.EmailProviderType;
    import com.medafrica.mavex.model.security.User;
    import com.medafrica.mavex.repository.EmailProviderConfigRepository;
    import jakarta.persistence.EntityNotFoundException;
    import lombok.RequiredArgsConstructor;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.List;

    @Service
    @RequiredArgsConstructor
    public class EmailProviderConfigService {

        private final EmailProviderConfigRepository configRepository;
        private final EncryptionService             encryptionService;

        // ---------------------------------------------------------------
        // LECTURE
        // ---------------------------------------------------------------

        public List<EmailProviderConfigResponse> getAll() {
            return configRepository.findAll().stream()
                    .map(EmailProviderConfigResponse::from)
                    .toList();
        }

        public EmailProviderConfigResponse getActive() {
            return configRepository.findByActiveTrue()
                    .map(EmailProviderConfigResponse::from)
                    .orElseThrow(() -> new IllegalStateException("Aucun provider email actif."));
        }

        // ---------------------------------------------------------------
        // SAUVEGARDE (upsert par type)
        // ---------------------------------------------------------------

        @Transactional
        public EmailProviderConfigResponse save(EmailProviderConfigRequest request) {
            EmailProviderType type = request.getProviderType();

            // Upsert : met a jour la config existante pour ce type, ou en cree une nouvelle
            EmailProviderConfig config = configRepository.findByProviderType(type)
                    .orElseGet(() -> EmailProviderConfig.builder()
                            .providerType(type)
                            .active(false)
                            .build());

            config.setFromEmail(request.getFromEmail());
            config.setFromName(request.getFromName());

            if (type == EmailProviderType.BREVO) {
                applyBrevoSecrets(config, request);
            } else {
                applySmtpFields(config, request);
            }

            config.setUpdatedBy(currentUser());
            if (config.getCreatedBy() == null) {
                config.setCreatedBy(currentUser());
            }

            return EmailProviderConfigResponse.from(configRepository.save(config));
        }

        // ---------------------------------------------------------------
        // ACTIVATION
        // ---------------------------------------------------------------

        @Transactional
        public EmailProviderConfigResponse activate(Long id) {
            // Verifie que la config existe avant de tout desactiver
            EmailProviderConfig target = configRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "EmailProviderConfig introuvable id=" + id));

            configRepository.deactivateAll();
            target.setActive(true);
            target.setUpdatedBy(currentUser());

            return EmailProviderConfigResponse.from(configRepository.save(target));
        }

        // ---------------------------------------------------------------
        // TEST DE CONNEXION
        // ---------------------------------------------------------------

        public void testConnection(Long id, String testEmail, EmailProviderService provider) throws Exception {
            EmailProviderConfig config = configRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "EmailProviderConfig introuvable id=" + id));

            if (config.getProviderType() != provider.getType()) {
                throw new IllegalArgumentException(
                        "Le provider fourni ne correspond pas a la config id=" + id);
            }

            provider.send(
                    testEmail,
                    "Test de connexion MAVEX",
                    "<p>La connexion au provider <strong>" + config.getProviderType()
                            + "</strong> fonctionne.</p>",
                    List.of()
            );
        }

        // ---------------------------------------------------------------
        // HELPERS PRIVES
        // ---------------------------------------------------------------

        private void applyBrevoSecrets(EmailProviderConfig config, EmailProviderConfigRequest request) {
            if (hasValue(request.getBrevoApiKey())) {
                config.setBrevoApiKeyEncrypted(encryptionService.encrypt(request.getBrevoApiKey()));
            }
            if (hasValue(request.getBrevoWebhookToken())) {
                config.setBrevoWebhookTokenEncrypted(encryptionService.encrypt(request.getBrevoWebhookToken()));
            }
        }

        private void applySmtpFields(EmailProviderConfig config, EmailProviderConfigRequest request) {
            config.setSmtpHost(request.getSmtpHost());
            config.setSmtpPort(request.getSmtpPort());
            config.setSmtpUsername(request.getSmtpUsername());
            config.setSmtpSslEnabled(Boolean.TRUE.equals(request.getSmtpSslEnabled()));
            config.setSmtpTlsEnabled(Boolean.TRUE.equals(request.getSmtpTlsEnabled()));

            if (hasValue(request.getSmtpPassword())) {
                config.setSmtpPasswordEncrypted(encryptionService.encrypt(request.getSmtpPassword()));
            }
        }

        private boolean hasValue(String s) {
            return s != null && !s.isBlank();
        }

        private User currentUser() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User user) {
                return user;
            }
            return null;
        }
    }
