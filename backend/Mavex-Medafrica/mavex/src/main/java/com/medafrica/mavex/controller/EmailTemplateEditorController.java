package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.email.EmailTemplateDTO;
import com.medafrica.mavex.model.enums.NotificationType;
import com.medafrica.mavex.service.interfaces.EmailTemplateEditorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email-templates")
@RequiredArgsConstructor
public class EmailTemplateEditorController {

    private final EmailTemplateEditorService editorService;

    // ─────────────────────────────────────────────────────────────────
    // GET /api/email-templates/{type}
    // Charge le template actif par type
    // Retourne : id + subject + bodyContent → Quill l'affiche
    // Exemple : GET /api/email-templates/PAYMENT_INVOICE_WITH_AMOUNT
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmailTemplateDTO> getByType(@PathVariable NotificationType type) {
        return ResponseEntity.ok(editorService.getByType(type));
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/email-templates/{id}
    // Reçoit subject + bodyContent modifiés par Quill
    // Reconstruit htmlContent et sauvegarde en base
    // ─────────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmailTemplateDTO> update(
            @PathVariable Long id,
            @RequestBody EmailTemplateDTO dto) {
        return ResponseEntity.ok(editorService.update(id, dto));
    }
}
