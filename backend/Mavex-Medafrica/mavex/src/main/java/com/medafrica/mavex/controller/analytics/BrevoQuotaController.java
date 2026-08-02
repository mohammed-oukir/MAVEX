package com.medafrica.mavex.controller.analytics;

import com.medafrica.mavex.dto.analytics.BrevoQuotaResponse;
import com.medafrica.mavex.service.analytics.BrevoAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/quota")
@RequiredArgsConstructor
public class BrevoQuotaController {

    private final BrevoAccountService brevoAccountService;

    // GET /api/dashboard/quota
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrevoQuotaResponse> getQuota() {
        return ResponseEntity.ok(brevoAccountService.getEmailQuota());
    }
}
