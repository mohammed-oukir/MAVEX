package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.email.SendEmailResponse;

import java.util.Map;

public interface NotificationEmailService {

    SendEmailResponse sendPaymentEmail(Long orderId);

    Map<String, Object> sendAllPaymentEmails(Long shipmentId);
}
