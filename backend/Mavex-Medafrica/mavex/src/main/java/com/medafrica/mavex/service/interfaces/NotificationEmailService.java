package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.email.SendEmailResponse;
import com.medafrica.mavex.dto.order.BulkEmailResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface NotificationEmailService {

    SendEmailResponse sendPaymentEmail(Long orderId, MultipartFile[] attachments);

    BulkEmailResult sendAllPaymentEmails(Long shipmentId, MultipartFile[] attachments);

    BulkEmailResult sendBulkEmails(List<Long> orderIds, MultipartFile[] attachments);
}
