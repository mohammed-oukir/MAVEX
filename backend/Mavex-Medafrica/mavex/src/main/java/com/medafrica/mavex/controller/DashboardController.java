package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.dashboard.*;
import com.medafrica.mavex.service.DashboardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardServiceImpl dashboardService;

    @GetMapping("/kpis")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('DASHBOARD','VIEW')")
    public ResponseEntity<DashboardKpiDTO> getKpis() {
        return ResponseEntity.ok(dashboardService.getKpis());
    }

    @GetMapping("/orders-by-status")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('DASHBOARD','VIEW')")
    public ResponseEntity<List<StatusCountDTO>> getOrdersByStatus() {
        return ResponseEntity.ok(dashboardService.getOrdersByStatus());
    }

    @GetMapping("/shipments-by-month")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('DASHBOARD','VIEW')")
    public ResponseEntity<List<MonthlyCountDTO>> getShipmentsByMonth() {
        return ResponseEntity.ok(dashboardService.getShipmentsByMonth());
    }

    @GetMapping("/revenue-by-month")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('DASHBOARD','VIEW')")
    public ResponseEntity<List<MonthlyRevenueDTO>> getRevenueByMonth() {
        return ResponseEntity.ok(dashboardService.getRevenueByMonth());
    }

    @GetMapping("/alerts")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('DASHBOARD','VIEW')")
    public ResponseEntity<AlertsDTO> getAlerts() {
        return ResponseEntity.ok(dashboardService.getAlerts());
    }
}
