package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.payment.GatewayConfigRequest;
import com.medafrica.mavex.dto.payment.GatewayConfigResponse;
import com.medafrica.mavex.service.payment.PaymentGatewayConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gateway-configs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PaymentGatewayConfigController {

    private final PaymentGatewayConfigService configService;

    // GET /api/gateway-configs
    @GetMapping
    public ResponseEntity<List<GatewayConfigResponse>> getAll() {
        return ResponseEntity.ok(configService.getAll());
    }

    // POST /api/gateway-configs
    @PostMapping
    public ResponseEntity<GatewayConfigResponse> create(
            @Valid @RequestBody GatewayConfigRequest request) {
        return ResponseEntity.ok(configService.create(request));
    }

    // PUT /api/gateway-configs/{id}
    @PutMapping("/{id}")
    public ResponseEntity<GatewayConfigResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody GatewayConfigRequest request) {
        return ResponseEntity.ok(configService.update(id, request));
    }

    // PATCH /api/gateway-configs/{id}/activate
    @PatchMapping("/{id}/activate")
    public ResponseEntity<GatewayConfigResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(configService.activate(id));
    }

    // PATCH /api/gateway-configs/{id}/deactivate
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<GatewayConfigResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(configService.deactivate(id));
    }

    // DELETE /api/gateway-configs/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
