package com.medafrica.mavex.service.payment;

import com.medafrica.mavex.dto.payment.PaymentTransactionResponse;
import com.medafrica.mavex.model.enums.EmailStatus;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.enums.PaymentStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.payment.PaymentTransaction;
import com.medafrica.mavex.repository.EmailLogRepository;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.repository.PaymentTransactionRepository;
import com.medafrica.mavex.repository.specification.PaymentTransactionSpecification;
import com.medafrica.mavex.service.ShipmentStatusService;
import com.medafrica.mavex.service.interfaces.NotificationEmailService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentTransactionRepository transactionRepository;
    private final OrderRepository              orderRepository;
    private final NotificationEmailService     notificationEmailService;
    private final EmailLogRepository           emailLogRepository;
    private final ShipmentStatusService        shipmentStatusService;

    // ─────────── Marque une transaction/commande comme payée (idempotent) ───────────
    @Transactional
    public void markSuccess(PaymentTransaction transaction) {
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Transaction id={} déjà marquée SUCCESS (idempotent, ignoré)", transaction.getId());
            return;
        }
        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setPaidAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        Order order = transaction.getOrder();
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        if (order.getShipment() != null) {
            shipmentStatusService.recalculate(order.getShipment().getId());
        }
        log.info("Transaction id={} marquée SUCCESS pour la première fois, order id={} passé à PAID",
                transaction.getId(), order.getId());

        // Confirmation email + reçu PDF — uniquement pour PayPal (paiement en ligne,
        // statut fiable). Jamais pour MANUEL : le passage à PAID y est une action
        // humaine, potentiellement corrigible/erronée, donc pas de confirmation auto.
        if (transaction.getGateway() == PaymentGatewayType.PAYPAL) {
            // Valeur de retour ignorée : comportement best-effort inchangé, l'échec
            // éventuel est déjà loggé en interne par sendPaymentConfirmationEmail().
            notificationEmailService.sendPaymentConfirmationEmail(order, transaction);
        }
    }

    // ─────────── Recherche paginée avec filtres ───────────
    @Transactional(readOnly = true)
    public Page<PaymentTransactionResponse> search(String hawb, String client,
            PaymentGatewayType gateway, PaymentStatus status,
            BigDecimal amountMin, BigDecimal amountMax,
            LocalDate from, LocalDate to, Pageable pageable) {

        Specification<PaymentTransaction> spec = PaymentTransactionSpecification.build(
                hawb, client, gateway, status, amountMin, amountMax, from, to);

        return transactionRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransaction> searchAll(String hawb, String client,
            PaymentGatewayType gateway, PaymentStatus status,
            BigDecimal amountMin, BigDecimal amountMax,
            LocalDate from, LocalDate to) {

        Specification<PaymentTransaction> spec = PaymentTransactionSpecification.build(
                hawb, client, gateway, status, amountMin, amountMax, from, to);

        return transactionRepository.findAll(spec, Pageable.unpaged()).getContent();
    }

    @Transactional(readOnly = true)
    public List<PaymentTransaction> findAllByIds(List<Long> ids) {
        return transactionRepository.findAllById(ids);
    }

    private PaymentTransactionResponse toResponse(PaymentTransaction t) {
        Order order = t.getOrder();
        return PaymentTransactionResponse.builder()
                .id(t.getId())
                .gatewayRef(t.getGatewayRef())
                .gateway(t.getGateway())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .status(t.getStatus())
                .ipAddress(t.getIpAddress())
                .paidAt(t.getPaidAt())
                .createdAt(t.getCreatedAt())
                .orderId(order != null ? order.getId() : null)
                .hawb(order != null ? order.getHawb() : null)
                .clientFullName(order != null && order.getClient() != null
                        ? order.getClient().getFullName() : null)
                .receiptSent(isReceiptSent(t, order))
                .build();
    }

    private boolean isReceiptSent(PaymentTransaction t, Order order) {
        if (t.getGateway() != PaymentGatewayType.MANUEL || order == null) {
            return false;
        }
        return emailLogRepository.existsByOrder_IdAndEmailTemplate_Type_NameAndStatus(
                order.getId(), "PAYMENT_CONFIRMED", EmailStatus.SENT);
    }

    // ─────────── Total encaissé (toutes transactions SUCCESS) ───────────
    @Transactional(readOnly = true)
    public BigDecimal getTotalCollected() {
        return transactionRepository.sumSuccessAmount();
    }

    // ─────────── Envoi manuel du reçu de confirmation (gateway MANUEL uniquement) ───────────
    @Transactional
    public void sendManualReceipt(Long transactionId) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transaction introuvable id=" + transactionId));

        if (transaction.getGateway() != PaymentGatewayType.MANUEL) {
            throw new IllegalStateException(
                    "L'envoi manuel du reçu n'est disponible que pour les paiements MANUEL (gateway actuel : "
                    + transaction.getGateway() + ").");
        }
        if (transaction.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException(
                    "Impossible d'envoyer le reçu : la transaction id=" + transactionId
                    + " n'est pas au statut SUCCESS (statut actuel : " + transaction.getStatus() + ").");
        }

        boolean sent = notificationEmailService.sendPaymentConfirmationEmail(
                transaction.getOrder(), transaction);

        if (!sent) {
            throw new IllegalStateException("L'envoi de l'email a échoué, vérifiez les logs.");
        }

        log.info("Reçu de confirmation envoyé manuellement pour transaction id={} (order id={})",
                transactionId, transaction.getOrder() != null ? transaction.getOrder().getId() : null);
    }
}
