package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.order.OrderStatusHistoryResponse;
import com.medafrica.mavex.service.interfaces.OrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-history")
@RequiredArgsConstructor
public class OrderStatusHistoryController {

    private final OrderStatusHistoryService historyService;

    // ─────────── GET /api/order-history/order/{orderId} ───────────
    /** Retourne tout l'historique des statuts d'un order donné */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','VIEW_STATUS_HISTORY')")
    public ResponseEntity<List<OrderStatusHistoryResponse>> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(historyService.getHistoryByOrder(orderId));
    }

    // ─────────── GET /api/order-history/user/{userId} ───────────
    /** Retourne toutes les actions d'un utilisateur (audit ADMIN) — STRICTEMENT ADMIN, jamais délégable via AgentPermission */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderStatusHistoryResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(historyService.getHistoryByUser(userId));
    }
}