package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.imports.ImportLogResponse;
import com.medafrica.mavex.service.ExcelImportService;
import com.medafrica.mavex.service.ImportDeleteService;
import com.medafrica.mavex.service.ImportLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
            @PageableDefault(size = 20, sort = "importedAt") Pageable pageable) {
        return ResponseEntity.ok(importLogService.list(pageable));
    }

    // ── GET /api/v1/imports/{id} ───────────────────────────────────
    // Détail complet d'un import avec toutes les lignes
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ImportLogResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(importLogService.getById(id));
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