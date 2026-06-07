package com.medafrica.mavex.controller;
 
import com.medafrica.mavex.dto.email.SendEmailResponse;
import com.medafrica.mavex.service.interfaces.NotificationEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class NotificationEmailController {

    private final NotificationEmailService emailService;

    @PostMapping(value = "/orders/{id}/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<SendEmailResponse> sendPaymentEmail(
            @PathVariable Long id,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        return ResponseEntity.ok(emailService.sendPaymentEmail(id, files));
    }

    @PostMapping(value = "/shipments/{id}/send-all", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Map<String, Object>> sendAllPaymentEmails(
            @PathVariable Long id,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        return ResponseEntity.ok(emailService.sendAllPaymentEmails(id, files));
    }
}
