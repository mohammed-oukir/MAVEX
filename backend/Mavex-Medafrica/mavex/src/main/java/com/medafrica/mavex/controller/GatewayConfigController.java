package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.ApiResponse;
import com.medafrica.mavex.dto.payment.GatewayConfigRequest;
import com.medafrica.mavex.dto.payment.GatewayConfigResponse;
import com.medafrica.mavex.service.interfaces.GatewayConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des gateways de paiement depuis l'UI admin.
 * Toutes les opérations sont réservées aux ADMIN.
 * GET /active est accessible aux AGENT aussi (pour la page de paiement).
 */
@RestController
@RequestMapping("/api/gateway-configs")
@RequiredArgsConstructor
public class GatewayConfigController {

    private final GatewayConfigService service;

    /** GET /api/gateway-configs — liste tous les gateways configurés */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GatewayConfigResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    /** GET /api/gateway-configs/{id} */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GatewayConfigResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /** GET /api/gateway-configs/active — gateway actif (pour la page paiement client) */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<GatewayConfigResponse> findActive() {
        return ResponseEntity.ok(service.findById(
                service.findActiveConfig().getId()
        ));
    }

    /** POST /api/gateway-configs — ajouter un nouveau gateway */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GatewayConfigResponse>> create(
            @Valid @RequestBody GatewayConfigRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<GatewayConfigResponse>builder()
                        .message("Gateway créé avec succès")
                        .data(service.create(req))
                        .build()
        );
    }

    /** PUT /api/gateway-configs/{id} — modifier la config */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GatewayConfigResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody GatewayConfigRequest req) {
        return ResponseEntity.ok(
                ApiResponse.<GatewayConfigResponse>builder()
                        .message("Gateway modifié avec succès")
                        .data(service.update(id, req))
                        .build()
        );
    }

    /** PATCH /api/gateway-configs/{id}/activate — activer ce gateway (désactive les autres) */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GatewayConfigResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<GatewayConfigResponse>builder()
                        .message("Gateway activé. Les autres gateways ont été désactivés.")
                        .data(service.activate(id))
                        .build()
        );
    }

    /** PATCH /api/gateway-configs/{id}/deactivate — désactiver */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GatewayConfigResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<GatewayConfigResponse>builder()
                        .message("Gateway désactivé.")
                        .data(service.deactivate(id))
                        .build()
        );
    }

    /** DELETE /api/gateway-configs/{id} — supprimer (impossible si actif) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Gateway supprimé.")
                        .data(null)
                        .build()
        );
    }
}
