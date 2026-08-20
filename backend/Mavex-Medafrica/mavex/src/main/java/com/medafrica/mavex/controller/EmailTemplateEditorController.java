package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.email.EmailTemplateDTO;
import com.medafrica.mavex.service.interfaces.EmailTemplateEditorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email-templates")
@RequiredArgsConstructor
public class EmailTemplateEditorController {

    private final EmailTemplateEditorService editorService;

    // ─────────────────────────────────────────────────────────────────
    // GET /api/email-templates
    // Liste tous les NotificationTypeEntity — ceux sans ligne en base ont id=null
    // ─────────────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("@permissionEvaluatorService.hasPermission('EMAIL_SETTINGS','VIEW_TEMPLATES')")
    public ResponseEntity<List<EmailTemplateDTO>> getAll() {
        return ResponseEntity.ok(editorService.listAll());
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/email-templates/{type}
    // Charge le template actif par type
    // Retourne : id + subject + bodyContent → Quill l'affiche
    // Exemple : GET /api/email-templates/PAYMENT_INVOICE_WITH_AMOUNT
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{type}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('EMAIL_SETTINGS','VIEW_TEMPLATES')")
    public ResponseEntity<EmailTemplateDTO> getByType(@PathVariable String type) {
        return ResponseEntity.ok(editorService.getByType(type));
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/email-templates/{id}
    // Reçoit subject + bodyContent modifiés par Quill
    // Reconstruit htmlContent et sauvegarde en base
    // ─────────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('EMAIL_SETTINGS','UPDATE_TEMPLATE')")
    public ResponseEntity<EmailTemplateDTO> update(
            @PathVariable Long id,
            @RequestBody EmailTemplateDTO dto) {
        return ResponseEntity.ok(editorService.update(id, dto));
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /api/email-templates
    // Crée un nouveau template pour un type sans ligne en base
    // ─────────────────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("@permissionEvaluatorService.hasPermission('EMAIL_SETTINGS','CREATE_TEMPLATE')")
    public ResponseEntity<EmailTemplateDTO> create(@RequestBody EmailTemplateDTO dto) {
        return ResponseEntity.ok(editorService.create(dto));
    }
}
