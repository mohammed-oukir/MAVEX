package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.payment.GatewayConfigRequest;
import com.medafrica.mavex.dto.payment.GatewayConfigResponse;
import com.medafrica.mavex.model.payment.PaymentGatewayConfig;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.PaymentGatewayConfigRepository;
import com.medafrica.mavex.service.interfaces.GatewayConfigService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatewayConfigServiceImpl implements GatewayConfigService {

    private final PaymentGatewayConfigRepository repo;

    @Override
    public List<GatewayConfigResponse> findAll() {
        return repo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public GatewayConfigResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public PaymentGatewayConfig findActiveConfig() {
        return repo.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun gateway de paiement actif. Activez-en un depuis l'interface admin."));
    }

    @Override
    @Transactional
    public GatewayConfigResponse create(GatewayConfigRequest req) {
        PaymentGatewayConfig config = PaymentGatewayConfig.builder()
                .type(req.getType())
                .name(req.getName())
                .apiKey(req.getApiKey())
                .secretKey(req.getSecretKey())
                .webhookSecret(req.getWebhookSecret())
                .mode(req.getMode())
                .active(false)
                .supportedCurrencies(req.getSupportedCurrencies() != null ? req.getSupportedCurrencies() : "USD")
                .description(req.getDescription())
                .createdBy(getCurrentUser())
                .build();

        return toResponse(repo.save(config));
    }

    @Override
    @Transactional
    public GatewayConfigResponse update(Long id, GatewayConfigRequest req) {
        PaymentGatewayConfig config = findOrThrow(id);

        config.setType(req.getType());
        config.setName(req.getName());
        config.setMode(req.getMode());
        if (req.getSupportedCurrencies() != null) config.setSupportedCurrencies(req.getSupportedCurrencies());
        if (req.getDescription()         != null) config.setDescription(req.getDescription());

        // Ne met à jour les clés que si elles sont fournies (non vides)
        if (req.getApiKey()        != null && !req.getApiKey().isBlank())        config.setApiKey(req.getApiKey());
        if (req.getSecretKey()     != null && !req.getSecretKey().isBlank())     config.setSecretKey(req.getSecretKey());
        if (req.getWebhookSecret() != null && !req.getWebhookSecret().isBlank()) config.setWebhookSecret(req.getWebhookSecret());

        return toResponse(repo.save(config));
    }

    @Override
    @Transactional
    public GatewayConfigResponse activate(Long id) {
        findOrThrow(id);
        repo.deactivateAll();
        PaymentGatewayConfig config = findOrThrow(id);
        config.setActive(true);
        return toResponse(repo.save(config));
    }

    @Override
    @Transactional
    public GatewayConfigResponse deactivate(Long id) {
        PaymentGatewayConfig config = findOrThrow(id);
        config.setActive(false);
        return toResponse(repo.save(config));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PaymentGatewayConfig config = findOrThrow(id);
        if (config.isActive()) {
            throw new IllegalStateException("Impossible de supprimer le gateway actif. Désactivez-le d'abord.");
        }
        repo.delete(config);
    }

    /* ── Helpers ────────────────────────────────────────── */

    private PaymentGatewayConfig findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Gateway introuvable : " + id));
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof User u) ? u : null;
    }

    private GatewayConfigResponse toResponse(PaymentGatewayConfig c) {
        return GatewayConfigResponse.builder()
                .id(c.getId())
                .type(c.getType())
                .name(c.getName())
                .apiKeyMasked(GatewayConfigResponse.mask(c.getApiKey()))
                .secretKeyMasked(GatewayConfigResponse.mask(c.getSecretKey()))
                .webhookSecretSet(c.getWebhookSecret() != null && !c.getWebhookSecret().isBlank())
                .mode(c.getMode())
                .active(c.isActive())
                .supportedCurrencies(c.getSupportedCurrencies())
                .description(c.getDescription())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
