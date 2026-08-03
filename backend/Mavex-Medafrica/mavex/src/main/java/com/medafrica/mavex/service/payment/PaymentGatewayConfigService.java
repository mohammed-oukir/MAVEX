package com.medafrica.mavex.service.payment;

import com.medafrica.mavex.dto.payment.GatewayConfigRequest;
import com.medafrica.mavex.dto.payment.GatewayConfigResponse;
import com.medafrica.mavex.model.payment.PaymentGatewayConfig;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.PaymentGatewayConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentGatewayConfigService {

    private final PaymentGatewayConfigRepository configRepository;

    // ---------------------------------------------------------------
    // LECTURE
    // ---------------------------------------------------------------

    public List<GatewayConfigResponse> getAll() {
        return configRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // ---------------------------------------------------------------
    // CREATION / MODIFICATION
    // ---------------------------------------------------------------

    @Transactional
    public GatewayConfigResponse create(GatewayConfigRequest request) {
        PaymentGatewayConfig config = PaymentGatewayConfig.builder()
                .type(request.getType())
                .name(request.getName())
                .mode(request.getMode())
                .active(false)
                .description(request.getDescription())
                .createdBy(currentUser())
                .build();

        return toResponse(configRepository.save(config));
    }

    @Transactional
    public GatewayConfigResponse update(Long id, GatewayConfigRequest request) {
        PaymentGatewayConfig config = configRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PaymentGatewayConfig introuvable id=" + id));

        config.setType(request.getType());
        config.setName(request.getName());
        config.setMode(request.getMode());
        config.setDescription(request.getDescription());

        return toResponse(configRepository.save(config));
    }

    // ---------------------------------------------------------------
    // ACTIVATION
    // ---------------------------------------------------------------

    @Transactional
    public GatewayConfigResponse activate(Long id) {
        PaymentGatewayConfig target = configRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PaymentGatewayConfig introuvable id=" + id));

        configRepository.deactivateAll();
        target.setActive(true);
        return toResponse(configRepository.save(target));
    }

    @Transactional
    public GatewayConfigResponse deactivate(Long id) {
        PaymentGatewayConfig target = configRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PaymentGatewayConfig introuvable id=" + id));

        target.setActive(false);
        return toResponse(configRepository.save(target));
    }

    // ---------------------------------------------------------------
    // SUPPRESSION
    // ---------------------------------------------------------------

    @Transactional
    public void delete(Long id) {
        if (!configRepository.existsById(id)) {
            throw new EntityNotFoundException("PaymentGatewayConfig introuvable id=" + id);
        }
        configRepository.deleteById(id);
    }

    // ---------------------------------------------------------------
    // HELPERS PRIVES
    // ---------------------------------------------------------------

    private GatewayConfigResponse toResponse(PaymentGatewayConfig config) {
        return GatewayConfigResponse.builder()
                .id(config.getId())
                .type(config.getType())
                .name(config.getName())
                .mode(config.getMode())
                .active(config.isActive())
                .description(config.getDescription())
                .createdAt(config.getCreatedAt())
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
