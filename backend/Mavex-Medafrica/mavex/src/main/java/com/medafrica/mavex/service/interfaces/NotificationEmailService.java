package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.email.SendEmailResponse;
import com.medafrica.mavex.dto.order.BulkEmailResult;

import java.util.List;

public interface NotificationEmailService {

    SendEmailResponse sendPaymentEmail(Long orderId);

    BulkEmailResult sendAllPaymentEmails(Long shipmentId);

    BulkEmailResult sendBulkEmails(List<Long> orderIds);
}
