package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.imports.ImportConfirmRequest;
import com.medafrica.mavex.dto.imports.ImportLogResponse;
import com.medafrica.mavex.dto.imports.ImportPreviewResponse;
import com.medafrica.mavex.dto.imports.ImportRevalidateRequest;
import com.medafrica.mavex.dto.imports.ImportStatsResponse;
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

    @PostMapping(value = "/manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionEvaluatorService.hasPermission('IMPORTS','CONFIRM')")
    public ResponseEntity<ImportLogResponse> importManifest(
            @RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) return ResponseEntity.badRequest().build();
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(excelImportService.importManifest(file));
    }

    @GetMapping
    @PreAuthorize("@permissionEvaluatorService.hasPermission('IMPORTS','VIEW')")
    public ResponseEntity<Page<ImportLogResponse>> list(
            @PageableDefault(size = 20, sort = "importedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(importLogService.list(pageable));
    }

    @GetMapping("/stats")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('IMPORTS','VIEW')")
    public ResponseEntity<ImportStatsResponse> getStats() {
        return ResponseEntity.ok(importLogService.getStats());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('IMPORTS','VIEW')")
    public ResponseEntity<ImportLogResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(importLogService.getById(id));
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionEvaluatorService.hasPermission('IMPORTS','PREPARE')")
    public ResponseEntity<ImportPreviewResponse> previewManifest(
            @RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(excelImportService.previewManifest(file));
    }

    @PostMapping("/revalidate")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('IMPORTS','PREPARE')")
    public ResponseEntity<ImportPreviewResponse> revalidate(
            @RequestBody ImportRevalidateRequest request) {
        return ResponseEntity.ok(excelImportService.revalidate(request.getRows()));
    }

    @PostMapping("/confirm")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('IMPORTS','CONFIRM')")
    public ResponseEntity<ImportLogResponse> confirmImport(
            @RequestBody ImportConfirmRequest request) throws Exception {
        return ResponseEntity.ok(excelImportService.confirmImport(request));
    }

    @GetMapping("/template")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('IMPORTS','VIEW')")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        byte[] bytes = excelImportService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"template_manifeste.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('IMPORTS','DELETE')")
    public ResponseEntity<Void> deleteImport(@PathVariable Long id) {
        importDeleteService.deleteImport(id);
        return ResponseEntity.noContent().build();
    }
}
