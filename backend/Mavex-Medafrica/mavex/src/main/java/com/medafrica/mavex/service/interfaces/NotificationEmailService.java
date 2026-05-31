package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.email.SendEmailResponse;
import com.medafrica.mavex.dto.order.BulkEmailResult;

import java.util.List;
import java.util.Map;

public interface NotificationEmailService {

    SendEmailResponse sendPaymentEmail(Long orderId);

    Map<String, Object> sendAllPaymentEmails(Long shipmentId);

    /** Envoie les emails à une sélection précise d'orders */
    BulkEmailResult sendBulkEmails(List<Long> orderIds);
}
