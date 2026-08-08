package com.medafrica.mavex.service;

import com.medafrica.mavex.model.email.EmailLog;
import com.medafrica.mavex.model.email.EmailTemplate;
import com.medafrica.mavex.model.enums.EmailStatus;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.finance.ExchangeRate;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.EmailLogRepository;
import com.medafrica.mavex.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Toutes les écritures BDD liées à l'envoi d'email.
 * Bean séparé pour que @Transactional passe par le proxy Spring
 * et ne soit pas ignoré (problème de self-invocation).
 *
 * Chaque méthode recharge ses entités via ID — on ne passe jamais
 * d'entités JPA detached entre deux transactions.
 */
@Service
@RequiredArgsConstructor
public class EmailPersistenceService {

    private final OrderRepository    orderRepository;
    private final EmailLogRepository emailLogRepository;

    @Transactional
    public Order loadAndPrepareOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order introuvable id=" + orderId));

        if (order.getClient() == null || order.getClient().getEmail() == null) {
            throw new IllegalStateException("Le client n'a pas d'adresse email.");
        }

        if (!order.isTokenValid()) {
            order.generatePaymentToken();
            orderRepository.save(order);
        }

        return order;
    }

    @Transactional
    public Long createPendingLog(String toEmail, String subject,
                                 EmailTemplate template, Long orderId) {
        Order order = orderRepository.getReferenceById(orderId);
        EmailLog log = EmailLog.builder()
                .toEmail(toEmail)
                .subject(subject)
                .status(EmailStatus.PENDING)
                .emailTemplate(template)
                .order(order)
                .sentBy(currentUser())
                .build();
        return emailLogRepository.save(log).getId();
    }

    @Transactional
    public void markSuccess(Long emailLogId, Long orderId, String messageId,
                            Optional<ExchangeRate> activeRate) {
        EmailLog emailLog = emailLogRepository.findById(emailLogId)
                .orElseThrow(() -> new EntityNotFoundException("EmailLog introuvable id=" + emailLogId));
        emailLog.markSent();
        if (messageId != null) emailLog.setMessageId(messageId);
        emailLogRepository.save(emailLog);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order introuvable id=" + orderId));
        order.setEmailSentAt(LocalDateTime.now());
        order.setEmailSentCount(order.getEmailSentCount() + 1);
        order.setEmailSentToAddress(order.getClient().getEmail());
        order.setEmailOutdatedReason(null);
        if (order.getStatus() == OrderStatus.CREATED
                || order.getStatus() == OrderStatus.EMAIL_OUTDATED) {
            order.setStatus(OrderStatus.EMAIL_SENT);
        }
        activeRate.ifPresent(r -> {
            order.setLockedExchangeRate(r.getRate());
            order.setLockedToCurrency(r.getToCurrency());
        });
        orderRepository.save(order);
    }

    @Transactional
    public void markFailed(Long emailLogId, String errorMessage) {
        EmailLog emailLog = emailLogRepository.findById(emailLogId)
                .orElseThrow(() -> new EntityNotFoundException("EmailLog introuvable id=" + emailLogId));
        emailLog.markFailed(errorMessage);
        emailLogRepository.save(emailLog);
    }

    /**
     * Marque un EmailLog comme envoyé, sans toucher à l'Order (statut, taux figé...).
     * Utilisé pour les emails "annexes" (ex: confirmation de paiement) qui ne doivent
     * pas rejouer la logique métier de doSendPaymentEmail().
     */
    @Transactional
    public void markLogSuccess(Long emailLogId, String messageId) {
        EmailLog emailLog = emailLogRepository.findById(emailLogId)
                .orElseThrow(() -> new EntityNotFoundException("EmailLog introuvable id=" + emailLogId));
        emailLog.markSent();
        if (messageId != null) emailLog.setMessageId(messageId);
        emailLogRepository.save(emailLog);
    }

    /**
     * Marque un EmailLog comme échoué, sans toucher à l'Order.
     * Alias sémantique de markFailed(), pour la même raison que markLogSuccess().
     */
    @Transactional
    public void markLogFailed(Long emailLogId, String errorMessage) {
        markFailed(emailLogId, errorMessage);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
