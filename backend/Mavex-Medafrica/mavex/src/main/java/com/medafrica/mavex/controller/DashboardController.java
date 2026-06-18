package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.dashboard.*;
import com.medafrica.mavex.model.enums.PermissionAction;
import com.medafrica.mavex.model.enums.PermissionModule;
import com.medafrica.mavex.security.annotation.RequiresPermission;
import com.medafrica.mavex.service.DashboardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
public class DashboardController {

    private final DashboardServiceImpl dashboardService;

    @GetMapping("/kpis")
    @RequiresPermission(module = PermissionModule.DASHBOARD, action = PermissionAction.VIEW)
    public ResponseEntity<DashboardKpiDTO> getKpis() {
        return ResponseEntity.ok(dashboardService.getKpis());
    }

    @GetMapping("/orders-by-status")
    @RequiresPermission(module = PermissionModule.DASHBOARD, action = PermissionAction.VIEW)
    public ResponseEntity<List<StatusCountDTO>> getOrdersByStatus() {
        return ResponseEntity.ok(dashboardService.getOrdersByStatus());
    }

    @GetMapping("/shipments-by-month")
    @RequiresPermission(module = PermissionModule.DASHBOARD, action = PermissionAction.VIEW)
    public ResponseEntity<List<MonthlyCountDTO>> getShipmentsByMonth() {
        return ResponseEntity.ok(dashboardService.getShipmentsByMonth());
    }

    @GetMapping("/revenue-by-month")
    @RequiresPermission(module = PermissionModule.DASHBOARD, action = PermissionAction.VIEW)
    public ResponseEntity<List<MonthlyRevenueDTO>> getRevenueByMonth() {
        return ResponseEntity.ok(dashboardService.getRevenueByMonth());
    }

    @GetMapping("/alerts")
    @RequiresPermission(module = PermissionModule.DASHBOARD, action = PermissionAction.VIEW)
    public ResponseEntity<AlertsDTO> getAlerts() {
        return ResponseEntity.ok(dashboardService.getAlerts());
    }
}
