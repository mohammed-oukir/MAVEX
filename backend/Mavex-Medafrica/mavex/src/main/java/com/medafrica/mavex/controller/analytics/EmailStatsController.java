package com.medafrica.mavex.controller.analytics;

import com.medafrica.mavex.dto.analytics.EmailStatsHistoryResponse;
import com.medafrica.mavex.service.analytics.EmailStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/stats")
@RequiredArgsConstructor
public class EmailStatsController {

    private final EmailStatsService emailStatsService;

    // POST /api/dashboard/stats/recompute?date=2026-07-27
    @PostMapping("/recompute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> recompute(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        emailStatsService.computeAndSaveStatsForDate(targetDate);
        return ResponseEntity.ok(Map.of(
            "date", targetDate.toString(),
            "message", "Stats recalculées"
        ));
    }

    // GET /api/dashboard/stats?days=30
    @GetMapping
    @PreAuthorize("@permissionEvaluatorService.hasPermission('DASHBOARD_ANALYTICS','VIEW')")
    public ResponseEntity<EmailStatsHistoryResponse> getHistory(
            @RequestParam(required = false, defaultValue = "30") int days) {
        return ResponseEntity.ok(emailStatsService.getStatsHistory(days));
    }
}
