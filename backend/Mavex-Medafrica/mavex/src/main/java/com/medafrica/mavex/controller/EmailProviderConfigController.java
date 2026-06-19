package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.email.EmailProviderConfigRequest;
import com.medafrica.mavex.dto.email.EmailProviderConfigResponse;
import com.medafrica.mavex.service.email.EmailProviderConfigService;
import com.medafrica.mavex.service.email.EmailProviderResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings/email-provider")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmailProviderConfigController {

    private final EmailProviderConfigService configService;
    private final EmailProviderResolver      providerResolver;

    // GET /api/settings/email-provider
    @GetMapping
    public ResponseEntity<List<EmailProviderConfigResponse>> getAll() {
        return ResponseEntity.ok(configService.getAll());
    }

    // GET /api/settings/email-provider/active
    @GetMapping("/active")
    public ResponseEntity<EmailProviderConfigResponse> getActive() {
        return ResponseEntity.ok(configService.getActive());
    }

    // POST /api/settings/email-provider
    @PostMapping
    public ResponseEntity<EmailProviderConfigResponse> save(
            @Valid @RequestBody EmailProviderConfigRequest request) {
        return ResponseEntity.ok(configService.save(request));
    }

    // PUT /api/settings/email-provider/{id}/activate
    @PutMapping("/{id}/activate")
    public ResponseEntity<EmailProviderConfigResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(configService.activate(id));
    }

    // POST /api/settings/email-provider/{id}/test
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, String>> testConnection(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) throws Exception {

        String testEmail = body.get("testEmail");
        if (testEmail == null || testEmail.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "L'adresse email de test est obligatoire."));
        }

        configService.testConnection(id, testEmail, providerResolver.resolve());
        return ResponseEntity.ok(Map.of("message", "Email de test envoye avec succes a " + testEmail));
    }
}
