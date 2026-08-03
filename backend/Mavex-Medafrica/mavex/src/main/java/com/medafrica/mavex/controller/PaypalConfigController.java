package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.payment.PaypalConfigRequest;
import com.medafrica.mavex.dto.payment.PaypalConfigResponse;
import com.medafrica.mavex.service.payment.PaypalConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paypal-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PaypalConfigController {

    private final PaypalConfigService paypalConfigService;

    // POST /api/paypal-config
    @PostMapping
    public ResponseEntity<PaypalConfigResponse> save(
            @Valid @RequestBody PaypalConfigRequest request) {
        return ResponseEntity.ok(paypalConfigService.saveConfig(
                request.getClientId(),
                request.getClientSecret(),
                request.getWebhookId(),
                request.getMode()));
    }

    // GET /api/paypal-config
    @GetMapping
    public ResponseEntity<PaypalConfigResponse> getCurrentConfig() {
        return paypalConfigService.getCurrentConfig()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
