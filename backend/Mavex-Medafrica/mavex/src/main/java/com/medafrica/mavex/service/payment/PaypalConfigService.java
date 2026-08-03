package com.medafrica.mavex.service.payment;

import com.medafrica.mavex.dto.payment.PaypalConfigResponse;
import com.medafrica.mavex.model.enums.PaymentGatewayMode;
import com.medafrica.mavex.model.payment.PaypalConfig;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.PaypalConfigRepository;
import com.medafrica.mavex.service.email.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaypalConfigService {

    private final PaypalConfigRepository configRepository;
    private final EncryptionService      encryptionService;

    @Transactional
    public PaypalConfigResponse saveConfig(String clientId, String clientSecret,
                                            String webhookId, PaymentGatewayMode mode) {
        PaypalConfig config = findExisting()
                .orElseGet(PaypalConfig::new);

        config.setClientId(clientId);
        if (hasValue(clientSecret)) {
            config.setClientSecretEncrypted(encryptionService.encrypt(clientSecret));
        }
        config.setWebhookId(webhookId);
        config.setMode(mode);

        if (config.getCreatedBy() == null) {
            config.setCreatedBy(currentUser());
        }

        return toResponse(configRepository.save(config));
    }

    public Optional<PaypalConfigResponse> getCurrentConfig() {
        return findExisting().map(this::toResponse);
    }

    // ---------------------------------------------------------------
    // USAGE INTERNE — jamais expose via un controller
    // ---------------------------------------------------------------

    String getDecryptedSecret() {
        PaypalConfig config = findExisting()
                .orElseThrow(() -> new IllegalStateException("Aucune configuration PayPal enregistree."));
        return encryptionService.decrypt(config.getClientSecretEncrypted());
    }

    // ---------------------------------------------------------------
    // HELPERS PRIVES
    // ---------------------------------------------------------------

    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    private Optional<PaypalConfig> findExisting() {
        List<PaypalConfig> all = configRepository.findAll();
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    private PaypalConfigResponse toResponse(PaypalConfig config) {
        return PaypalConfigResponse.builder()
                .clientId(config.getClientId())
                .webhookId(config.getWebhookId())
                .mode(config.getMode())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
