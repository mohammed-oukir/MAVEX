package com.medafrica.mavex.controller;
 
import com.medafrica.mavex.dto.email.SendEmailResponse;
import com.medafrica.mavex.service.NotificationEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
 
import java.util.Map;
 
@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor

public class NotificationEmailController {

 private final NotificationEmailService emailService;
 
    // ─────────────────────────────────────────────
    // POST /api/emails/orders/{id}/send
    // Envoie l'email de paiement pour un seul order
    // ─────────────────────────────────────────────
    @PostMapping("/orders/{id}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<SendEmailResponse> sendPaymentEmail(@PathVariable Long id) {
        return ResponseEntity.ok(emailService.sendPaymentEmail(id));
    }
 
    // ─────────────────────────────────────────────
    // POST /api/emails/shipments/{id}/send-all
    // Envoie les emails à tous les clients d'un shipment
    // ─────────────────────────────────────────────
    @PostMapping("/shipments/{id}/send-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Map<String, Object>> sendAllPaymentEmails(@PathVariable Long id) {
        return ResponseEntity.ok(emailService.sendAllPaymentEmails(id));
    }
}
