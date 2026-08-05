package com.medafrica.mavex.service.payment;

import com.medafrica.mavex.dto.payment.PaymentTransactionResponse;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.enums.PaymentGatewayType;
import com.medafrica.mavex.model.enums.PaymentStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.payment.PaymentTransaction;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.repository.PaymentTransactionRepository;
import com.medafrica.mavex.repository.specification.PaymentTransactionSpecification;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentTransactionRepository transactionRepository;
    private final OrderRepository              orderRepository;

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
        log.info("Transaction id={} marquée SUCCESS pour la première fois, order id={} passé à PAID",
                transaction.getId(), order.getId());
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
                .build();
    }

    // ─────────── Total encaissé (toutes transactions SUCCESS) ───────────
    @Transactional(readOnly = true)
    public BigDecimal getTotalCollected() {
        return transactionRepository.sumSuccessAmount();
    }
}
