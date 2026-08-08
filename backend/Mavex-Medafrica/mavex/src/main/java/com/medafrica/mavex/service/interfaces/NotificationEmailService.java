package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.email.SendEmailResponse;
import com.medafrica.mavex.dto.order.BulkEmailResult;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.model.payment.PaymentTransaction;

import java.util.List;

public interface NotificationEmailService {

    SendEmailResponse sendPaymentEmail(Long orderId);

    BulkEmailResult sendAllPaymentEmails(Long shipmentId);

    BulkEmailResult sendBulkEmails(List<Long> orderIds);

    /**
     * Envoie l'email de confirmation de paiement avec le reçu PDF en pièce jointe.
     * Best-effort : ne doit jamais lever d'exception vers l'appelant (voir impl.).
     */
    void sendPaymentConfirmationEmail(Order order, PaymentTransaction transaction);
}
