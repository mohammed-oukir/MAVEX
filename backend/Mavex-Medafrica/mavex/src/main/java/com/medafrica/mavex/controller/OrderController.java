package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.order.OrderPatchRequest;
import com.medafrica.mavex.dto.order.OrderRequest;
import com.medafrica.mavex.dto.order.OrderResponse;
import com.medafrica.mavex.dto.order.OrderStatusUpdateRequest;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.service.interfaces.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ─────────── POST /api/orders ───────────
    @PostMapping
   // @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    // ─────────── GET /api/orders ───────────
    @GetMapping
   // @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<List<OrderResponse>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
    }

    // ─────────── GET /api/orders/{id} ───────────
    @GetMapping("/{id}")
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    // ─────────── GET /api/orders/hawb/{hawb} ───────────
    @GetMapping("/hawb/{hawb}")
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<OrderResponse> getByHawb(@PathVariable String hawb) {
        return ResponseEntity.ok(orderService.getByHawb(hawb));
    }

    // ─────────── GET /api/orders/shipment/{shipmentId} ───────────
    @GetMapping("/shipment/{shipmentId}")
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<List<OrderResponse>> getByShipment(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(orderService.getByShipment(shipmentId));
    }

    // ─────────── GET /api/orders/client/{clientId} ───────────
    @GetMapping("/client/{clientId}")
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<List<OrderResponse>> getByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(orderService.getByClient(clientId));
    }

    // ─────────── GET /api/orders/status/{status} ───────────
    @GetMapping("/status/{status}")
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<List<OrderResponse>> getByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getByStatus(status));
    }

    // ─────────── PUT /api/orders/{id} ───────────
    @PutMapping("/{id}")
   // @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<OrderResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.update(id, request));
    }

    // ─────────── PATCH /api/orders/{id} ───────────
    @PatchMapping("/{id}")
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<OrderResponse> patch(@PathVariable Long id,
                                               @Valid @RequestBody OrderPatchRequest request) {
        return ResponseEntity.ok(orderService.patch(id, request));
    }

    // ─────────── PATCH /api/orders/{id}/status ───────────
    @PatchMapping("/{id}/status")
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }

    // ─────────── POST /api/orders/{id}/payment-token ───────────
    @PostMapping("/{id}/payment-token")
    //@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<OrderResponse> generatePaymentToken(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.generatePaymentToken(id));
    }

    // ─────────── GET /api/orders/pay/{token} (PUBLIC - pas d'auth) ───────────
    @GetMapping("/pay/{token}")
    public ResponseEntity<OrderResponse> getByPaymentToken(@PathVariable String token) {
        return ResponseEntity.ok(orderService.getByPaymentToken(token));
    }

    // ─────────── DELETE /api/orders/{id} ───────────
    @DeleteMapping("/{id}")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}