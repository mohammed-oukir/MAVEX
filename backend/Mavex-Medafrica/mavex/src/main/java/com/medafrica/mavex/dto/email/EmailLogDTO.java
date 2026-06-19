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
    private LocalDateTime deliveredAt;
    private LocalDateTime openedAt;
    private LocalDateTime clickedAt;
    private LocalDateTime bouncedAt;
    private String        bounceReason;

    public static EmailLogDTO from(EmailLog log) {
        return EmailLogDTO.builder()
                .id(log.getId())
                .toEmail(log.getToEmail())
                .subject(log.getSubject())
                .status(log.getStatus().name())
                .errorMessage(log.getErrorMessage())
                .retryCount(log.getRetryCount())
                .sentAt(log.getSentAt())
                .deliveredAt(log.getDeliveredAt())
                .openedAt(log.getOpenedAt())
                .clickedAt(log.getClickedAt())
                .bouncedAt(log.getBouncedAt())
                .bounceReason(log.getBounceReason())
                .build();
    }
}
