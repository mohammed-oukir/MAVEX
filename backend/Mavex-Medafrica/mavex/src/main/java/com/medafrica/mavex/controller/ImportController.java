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
@PreAuthorize("hasRole('ADMIN')")
public class ImportController {

    private final ExcelImportService  excelImportService;
    private final ImportLogService    importLogService;
    private final ImportDeleteService importDeleteService;

    @PostMapping(value = "/manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    public ResponseEntity<Page<ImportLogResponse>> list(
            @PageableDefault(size = 20, sort = "importedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(importLogService.list(pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<ImportStatsResponse> getStats() {
        return ResponseEntity.ok(importLogService.getStats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportLogResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(importLogService.getById(id));
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportPreviewResponse> previewManifest(
            @RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(excelImportService.previewManifest(file));
    }

    @PostMapping("/revalidate")
    public ResponseEntity<ImportPreviewResponse> revalidate(
            @RequestBody ImportRevalidateRequest request) {
        return ResponseEntity.ok(excelImportService.revalidate(request.getRows()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ImportLogResponse> confirmImport(
            @RequestBody ImportConfirmRequest request) throws Exception {
        return ResponseEntity.ok(excelImportService.confirmImport(request));
    }

    @GetMapping("/template")
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteImport(@PathVariable Long id) {
        importDeleteService.deleteImport(id);
        return ResponseEntity.noContent().build();
    }
}
