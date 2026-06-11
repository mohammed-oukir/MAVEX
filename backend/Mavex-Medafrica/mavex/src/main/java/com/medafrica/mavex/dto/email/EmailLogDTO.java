package com.medafrica.mavex.dto.email;

import com.medafrica.mavex.model.email.EmailLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EmailLogDTO {

    private Long          id;
    private String        toEmail;
    private String        subject;
    private String        status;
    private String        errorMessage;
    private int           retryCount;
    private LocalDateTime sentAt;

    public static EmailLogDTO from(EmailLog log) {
        return EmailLogDTO.builder()
                .id(log.getId())
                .toEmail(log.getToEmail())
                .subject(log.getSubject())
                .status(log.getStatus().name())
                .errorMessage(log.getErrorMessage())
                .retryCount(log.getRetryCount())
                .sentAt(log.getSentAt())
                .build();
    }
}
