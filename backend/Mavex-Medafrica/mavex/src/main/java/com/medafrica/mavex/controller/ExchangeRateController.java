package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.finance.ExchangeRateImportResult;
import com.medafrica.mavex.dto.finance.ExchangeRateRequest;
import com.medafrica.mavex.dto.finance.ExchangeRateResponse;
import com.medafrica.mavex.service.finance.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    // POST /api/exchange-rates/import
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPTABLE')")
    public ResponseEntity<ExchangeRateImportResult> importFile(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(exchangeRateService.importFile(file));
    }

    // GET /api/exchange-rates?fromCurrency=USD&toCurrency=MAD&isActive=true
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'COMPTABLE')")
    public ResponseEntity<List<ExchangeRateResponse>> list(
            @RequestParam(required = false) String fromCurrency,
            @RequestParam(required = false) String toCurrency,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(exchangeRateService.findAll(fromCurrency, toCurrency, isActive));
    }

    // GET /api/exchange-rates/active?from=USD&to=MAD
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'COMPTABLE')")
    public ResponseEntity<ExchangeRateResponse> getActive(
            @RequestParam String from,
            @RequestParam String to) {
        return ResponseEntity.ok(exchangeRateService.findActive(from, to));
    }

    // GET /api/exchange-rates/lookup?from=USD&to=MAD&date=2026-06-19
    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'COMPTABLE')")
    public ResponseEntity<ExchangeRateResponse> lookup(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(exchangeRateService.findByPairAndDate(from, to, date));
    }

    // DELETE /api/exchange-rates/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPTABLE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        exchangeRateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/exchange-rates
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPTABLE')")
    public ResponseEntity<ExchangeRateResponse> create(
            @RequestBody ExchangeRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(exchangeRateService.createManual(request));
    }

    // PUT /api/exchange-rates/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPTABLE')")
    public ResponseEntity<ExchangeRateResponse> update(
            @PathVariable Long id,
            @RequestBody ExchangeRateRequest request) {
        return ResponseEntity.ok(exchangeRateService.update(id, request));
    }
}
