package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.payment.ManualPaymentConfigResponse;
import com.medafrica.mavex.service.payment.ManualPaymentConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/manual-payment")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ManualPaymentConfigController {

    private final ManualPaymentConfigService manualPaymentConfigService;

    // POST /api/manual-payment/rib
    @PostMapping("/rib")
    public ResponseEntity<ManualPaymentConfigResponse> uploadRib(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(manualPaymentConfigService.uploadRib(file));
    }

    // GET /api/manual-payment/rib
    @GetMapping("/rib")
    public ResponseEntity<ManualPaymentConfigResponse> getCurrentRib() {
        return manualPaymentConfigService.getCurrentRib()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
