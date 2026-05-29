package com.medafrica.mavex.dto.email;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendEmailResponse {
    private boolean success;
    private String  message;
    private String  toEmail;
    private String  hawb;
    private String  orderStatus;
}