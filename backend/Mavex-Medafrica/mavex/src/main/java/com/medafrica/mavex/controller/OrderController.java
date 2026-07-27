package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.email.EmailLogDTO;
import com.medafrica.mavex.dto.order.*;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.enums.PermissionAction;
import com.medafrica.mavex.model.enums.PermissionModule;
import com.medafrica.mavex.repository.EmailLogRepository;
import com.medafrica.mavex.security.annotation.RequiresPermission;
import com.medafrica.mavex.service.interfaces.NotificationEmailService;
import com.medafrica.mavex.service.interfaces.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService            orderService;
    private final NotificationEmailService emailService;
    private final EmailLogRepository      emailLogRepository;

    // ─────────── POST /api/orders ───────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.CREATE)
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    // ─────────── GET /api/orders — recherche paginée avec filtres ───────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.VIEW)
    public ResponseEntity<Page<OrderResponse>> search(
            @RequestParam(required = false) String hawb,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String clientEmail,
            @RequestParam(required = false) String shipmentSearch,
            @RequestParam(required = false) Double shipmentWeight,
            @RequestParam(required = false) Double customsValue,
            @RequestParam(required = false) Double totalAmount,
            @RequestParam(required = false) Double dutyRate,
            @RequestParam(required = false) String customsCurrency,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long shipmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(orderService.search(
                hawb, client, clientEmail, shipmentSearch, shipmentWeight, customsValue,
                totalAmount, dutyRate, customsCurrency, status, shipmentId,
                from, to, pageable));
    }

    // ─────────── GET /api/orders/{id} ───────────
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.VIEW)
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    // ─────────── GET /api/orders/hawb/{hawb} ───────────
    @GetMapping("/hawb/{hawb}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.VIEW)
    public ResponseEntity<OrderResponse> getByHawb(@PathVariable String hawb) {
        return ResponseEntity.ok(orderService.getByHawb(hawb));
    }

    // ─────────── GET /api/orders/shipment/{shipmentId} ───────────
    @GetMapping("/shipment/{shipmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.VIEW)
    public ResponseEntity<List<OrderResponse>> getByShipment(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(orderService.getByShipment(shipmentId));
    }

    // ─────────── GET /api/orders/client/{clientId} ───────────
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.VIEW)
    public ResponseEntity<List<OrderResponse>> getByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(orderService.getByClient(clientId));
    }

    // ─────────── PUT /api/orders/{id} ───────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.EDIT)
    public ResponseEntity<OrderResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.update(id, request));
    }

    // ─────────── PATCH /api/orders/{id} ───────────
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.EDIT)
    public ResponseEntity<OrderResponse> patch(@PathVariable Long id,
                                               @Valid @RequestBody OrderPatchRequest request) {
        return ResponseEntity.ok(orderService.patch(id, request));
    }

    // ─────────── PATCH /api/orders/{id}/status ───────────
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.EDIT)
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }

    // ─────────── POST /api/orders/bulk/email ───────────
    @PostMapping(value = "/bulk/email", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.EDIT)
    public ResponseEntity<BulkEmailResult> bulkEmail(
            @RequestPart("request") BulkEmailRequest request,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        return ResponseEntity.ok(emailService.sendBulkEmails(request.getIds()));
    }

    // ─────────── POST /api/orders/bulk/status ───────────
    @PostMapping("/bulk/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.EDIT)
    public ResponseEntity<Void> bulkStatus(@RequestBody BulkStatusRequest request) {
        orderService.bulkUpdateStatus(request.getIds(), request.getNewStatus(), request.getNote());
        return ResponseEntity.noContent().build();
    }

    // ─────────── POST /api/orders/{id}/payment-token ───────────
    @PostMapping("/{id}/payment-token")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.EDIT)
    public ResponseEntity<OrderResponse> generatePaymentToken(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.generatePaymentToken(id));
    }

    // ─────────── GET /api/orders/pay/{token} (PUBLIC — pas d'auth) ───────────
    @GetMapping("/pay/{token}")
    public ResponseEntity<OrderResponse> getByPaymentToken(@PathVariable String token) {
        return ResponseEntity.ok(orderService.getByPaymentToken(token));
    }

    // ─────────── GET /api/orders/{id}/email-logs ───────────
    @GetMapping("/{id}/email-logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.VIEW)
    public ResponseEntity<List<EmailLogDTO>> getEmailLogs(@PathVariable Long id) {
        List<EmailLogDTO> logs = emailLogRepository.findByOrderId(id)
                .stream()
                .map(EmailLogDTO::from)
                .toList();
        return ResponseEntity.ok(logs);
    }

    // ─────────── DELETE /api/orders/{id} ───────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.DELETE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}