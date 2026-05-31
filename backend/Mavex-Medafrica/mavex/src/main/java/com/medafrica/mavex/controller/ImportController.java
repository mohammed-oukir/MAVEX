package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.imports.ImportConfirmRequest;
import com.medafrica.mavex.dto.imports.ImportLogResponse;
import com.medafrica.mavex.dto.imports.ImportPreviewResponse;
import com.medafrica.mavex.service.interfaces.ExcelImportService;
import com.medafrica.mavex.service.interfaces.ImportDeleteService;
import com.medafrica.mavex.service.interfaces.ImportLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ExcelImportService  excelImportService;
    private final ImportLogService    importLogService;
    private final ImportDeleteService importDeleteService;

    // ── POST /api/v1/imports/manifest ──────────────────────────────
    // Upload et traitement du fichier Excel
    @PostMapping(value = "/manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ImportLogResponse> importManifest(
            @RequestParam("file") MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(excelImportService.importManifest(file));
    }

    // ── GET /api/v1/imports ────────────────────────────────────────
    // Historique paginé de tous les imports
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Page<ImportLogResponse>> list(
            @PageableDefault(size = 20, sort = "importedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(importLogService.list(pageable));
    }

    // ── GET /api/v1/imports/{id} ───────────────────────────────────
    // Détail complet d'un import avec toutes les lignes
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ImportLogResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(importLogService.getById(id));
    }

    // ── POST /api/v1/imports/preview ──────────────────────────────
    // Lit et valide le fichier Excel SANS créer quoi que ce soit en base.
    // Retourne le statut de chaque ligne : VALID / INVALID / SKIPPED
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ImportPreviewResponse> previewManifest(
            @RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(excelImportService.previewManifest(file));
    }

    // ── POST /api/v1/imports/confirm ──────────────────────────────
    // Reçoit les lignes (corrigées ou non) et crée les entités en base.
    // C'est la seule étape qui écrit en base dans le nouveau flow.
    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ImportLogResponse> confirmImport(
            @RequestBody ImportConfirmRequest request) throws Exception {
        return ResponseEntity.ok(excelImportService.confirmImport(request));
    }

    // ── GET /api/v1/imports/template ──────────────────────────────
    // Téléchargement du template Excel vierge avec 2 lignes d'exemple
    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        byte[] bytes = excelImportService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"template_manifeste.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // ── DELETE /api/v1/imports/{id} ────────────────────────────────
    // Suppression complète : log + orders + shipments + clients liés
    // Réservé ADMIN uniquement
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteImport(@PathVariable Long id) {
        importDeleteService.deleteImport(id);
        return ResponseEntity.noContent().build();
    }
}