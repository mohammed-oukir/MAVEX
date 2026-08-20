package com.medafrica.mavex.controller;

import com.medafrica.mavex.dto.email.EmailLogDTO;
import com.medafrica.mavex.dto.order.*;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.repository.EmailLogRepository;
import com.medafrica.mavex.service.export.TableExportService;
import com.medafrica.mavex.service.interfaces.NotificationEmailService;
import com.medafrica.mavex.service.interfaces.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService            orderService;
    private final NotificationEmailService emailService;
    private final EmailLogRepository      emailLogRepository;
    private final TableExportService      exportService;

    // ─────────── POST /api/orders ───────────
    @PostMapping
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','CREATE')")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    // ─────────── GET /api/orders — recherche paginée avec filtres ───────────
    @GetMapping
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','VIEW')")
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

    // ─────────── GET /api/orders/export/pdf — export PDF des orders filtrés ───────────
    @GetMapping("/export/pdf")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','EXPORT')")
    public ResponseEntity<byte[]> exportPdf(
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<List<String>> rows = buildExportRows(
                hawb, client, clientEmail, shipmentSearch, shipmentWeight, customsValue,
                totalAmount, dutyRate, customsCurrency, status, shipmentId, from, to);

        byte[] bytes = exportService.generatePdf("Liste des commandes", EXPORT_HEADERS, rows);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"commandes.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // ─────────── GET /api/orders/export/excel — export Excel des orders filtrés ───────────
    @GetMapping("/export/excel")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','EXPORT')")
    public ResponseEntity<byte[]> exportExcel(
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<List<String>> rows = buildExportRows(
                hawb, client, clientEmail, shipmentSearch, shipmentWeight, customsValue,
                totalAmount, dutyRate, customsCurrency, status, shipmentId, from, to);

        byte[] bytes = exportService.generateExcel("Orders", EXPORT_HEADERS, rows);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"commandes.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // ─────────── POST /api/orders/export/excel/selection — export Excel d'une sélection d'ids ───────────
    @PostMapping("/export/excel/selection")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','EXPORT')")
    public ResponseEntity<byte[]> exportExcelSelection(@RequestBody ExportSelectionRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new IllegalArgumentException("Veuillez sélectionner au moins une commande.");
        }

        List<Order> orders = orderService.findAllByIds(request.getIds());
        List<List<String>> rows = rowsFromOrders(orders);

        byte[] bytes = exportService.generateExcel("Orders (sélection)", EXPORT_HEADERS, rows);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"commandes_selection.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // ─────────── POST /api/orders/export/detail/pdf — export PDF détaillé d'une sélection d'ids ───────────
    @PostMapping("/export/detail/pdf")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','EXPORT')")
    public ResponseEntity<byte[]> exportDetailPdf(@RequestBody ExportSelectionRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new IllegalArgumentException("Veuillez sélectionner au moins une commande.");
        }

        List<Order> orders = orderService.findAllByIds(request.getIds());
        List<List<String>> rows = rowsFromOrdersDetail(orders);

        byte[] bytes = exportService.generatePdf("Commandes du shipment", EXPORT_DETAIL_HEADERS, rows);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"commandes_shipment.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // ─────────── POST /api/orders/export/detail/excel — export Excel détaillé d'une sélection d'ids ───────────
    @PostMapping("/export/detail/excel")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','EXPORT')")
    public ResponseEntity<byte[]> exportDetailExcel(@RequestBody ExportSelectionRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new IllegalArgumentException("Veuillez sélectionner au moins une commande.");
        }

        List<Order> orders = orderService.findAllByIds(request.getIds());
        List<List<String>> rows = rowsFromOrdersDetail(orders);

        byte[] bytes = exportService.generateExcel("Orders", EXPORT_DETAIL_HEADERS, rows);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"commandes_shipment.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    private static final List<String> EXPORT_DETAIL_HEADERS =
            List.of("HAWB", "Client", "Shipper", "Description", "Poids", "Items",
                    "Customs", "Devise", "Duty%", "Total", "Statut");

    private List<List<String>> rowsFromOrdersDetail(List<Order> orders) {
        List<List<String>> rows = new ArrayList<>();
        for (Order o : orders) {
            String clientName = o.getClient() != null ? o.getClient().getFullName() : "";
            String shipperName = (o.getShipment() != null && o.getShipment().getShipper() != null)
                    ? o.getShipment().getShipper().getCompanyName()
                    : "";
            String weight = o.getShipmentWeight() != null ? o.getShipmentWeight().toString() : "";
            String items  = o.getNumberOfItems() != null ? o.getNumberOfItems().toString() : "";
            String currency = o.getCustomsCurrency() != null ? o.getCustomsCurrency() : "";
            String customsVal = o.getCustomsValue() != null ? o.getCustomsValue().toString() : "";
            String dutyVal = o.getDutyRate() != null
                    ? o.getDutyRate().multiply(BigDecimal.valueOf(100)) + "%"
                    : "";
            String totalVal = o.getTotalAmount() != null
                    ? o.getTotalAmount().toString() + " " + currency
                    : "";
            String statusName = o.getStatus() != null ? o.getStatus().name() : "";

            rows.add(List.of(
                    o.getHawb() != null ? o.getHawb() : "",
                    clientName != null ? clientName : "",
                    shipperName != null ? shipperName : "",
                    o.getGoodsDescription() != null ? o.getGoodsDescription() : "",
                    weight,
                    items,
                    customsVal,
                    currency,
                    dutyVal,
                    totalVal,
                    statusName
            ));
        }
        return rows;
    }

    private static final List<String> EXPORT_HEADERS =
            List.of("HAWB", "Client", "Shipment", "Poids", "Valeur", "Total dû", "Statut");

    private List<List<String>> buildExportRows(
            String hawb, String client, String clientEmail, String shipmentSearch,
            Double shipmentWeight, Double customsValue, Double totalAmount, Double dutyRate,
            String customsCurrency, OrderStatus status, Long shipmentId,
            LocalDate from, LocalDate to) {

        List<Order> orders = orderService.searchAll(
                hawb, client, clientEmail, shipmentSearch, shipmentWeight, customsValue,
                totalAmount, dutyRate, customsCurrency, status, shipmentId, from, to);

        return rowsFromOrders(orders);
    }

    private List<List<String>> rowsFromOrders(List<Order> orders) {
        List<List<String>> rows = new ArrayList<>();
        for (Order o : orders) {
            String clientName = o.getClient() != null ? o.getClient().getFullName() : "";
            String mawb       = o.getShipment() != null ? o.getShipment().getMawb() : "";
            String weight      = o.getShipmentWeight() != null ? o.getShipmentWeight().toString() : "";
            String currency    = o.getCustomsCurrency() != null ? o.getCustomsCurrency() : "";
            String customsVal  = o.getCustomsValue() != null
                    ? o.getCustomsValue().toString() + " " + currency
                    : "";
            String totalVal    = o.getTotalAmount() != null
                    ? o.getTotalAmount().toString() + " " + currency
                    : "";
            String statusName  = o.getStatus() != null ? o.getStatus().name() : "";

            rows.add(List.of(
                    o.getHawb() != null ? o.getHawb() : "",
                    clientName != null ? clientName : "",
                    mawb != null ? mawb : "",
                    weight,
                    customsVal,
                    totalVal,
                    statusName
            ));
        }
        return rows;
    }

    // ─────────── GET /api/orders/{id} ───────────
    @GetMapping("/{id}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','VIEW')")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    // ─────────── GET /api/orders/hawb/{hawb} ───────────
    @GetMapping("/hawb/{hawb}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','VIEW')")
    public ResponseEntity<OrderResponse> getByHawb(@PathVariable String hawb) {
        return ResponseEntity.ok(orderService.getByHawb(hawb));
    }

    // ─────────── GET /api/orders/shipment/{shipmentId} ───────────
    @GetMapping("/shipment/{shipmentId}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','VIEW')")
    public ResponseEntity<List<OrderResponse>> getByShipment(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(orderService.getByShipment(shipmentId));
    }

    // ─────────── GET /api/orders/client/{clientId} ───────────
    @GetMapping("/client/{clientId}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','VIEW')")
    public ResponseEntity<List<OrderResponse>> getByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(orderService.getByClient(clientId));
    }

    // ─────────── PUT /api/orders/{id} ───────────
    @PutMapping("/{id}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','UPDATE')")
    public ResponseEntity<OrderResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.update(id, request));
    }

    // ─────────── PATCH /api/orders/{id} ───────────
    @PatchMapping("/{id}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','UPDATE')")
    public ResponseEntity<OrderResponse> patch(@PathVariable Long id,
                                               @Valid @RequestBody OrderPatchRequest request) {
        return ResponseEntity.ok(orderService.patch(id, request));
    }

    // ─────────── PATCH /api/orders/{id}/status ───────────
    @PatchMapping("/{id}/status")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','CHANGE_STATUS')")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }

    // ─────────── POST /api/orders/bulk/email ───────────
    @PostMapping(value = "/bulk/email", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','BULK_SEND_EMAIL')")
    public ResponseEntity<BulkEmailResult> bulkEmail(
            @RequestPart("request") BulkEmailRequest request,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        return ResponseEntity.ok(emailService.sendBulkEmails(request.getIds()));
    }

    // ─────────── POST /api/orders/bulk/status ───────────
    @PostMapping("/bulk/status")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','BULK_CHANGE_STATUS')")
    public ResponseEntity<BulkStatusResult> bulkStatus(@RequestBody BulkStatusRequest request) {
        return ResponseEntity.ok(orderService.bulkUpdateStatus(request.getIds(), request.getNewStatus(), request.getNote()));
    }

    // ─────────── POST /api/orders/{id}/payment-token ───────────
    @PostMapping("/{id}/payment-token")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','GENERATE_PAYMENT_LINK')")
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
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','VIEW_EMAIL_LOGS')")
    public ResponseEntity<List<EmailLogDTO>> getEmailLogs(@PathVariable Long id) {
        List<EmailLogDTO> logs = emailLogRepository.findByOrderId(id)
                .stream()
                .map(EmailLogDTO::from)
                .toList();
        return ResponseEntity.ok(logs);
    }

    // ─────────── DELETE /api/orders/{id} ───────────
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluatorService.hasPermission('ORDERS','DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}