package com.medafrica.mavex.model.email;

import com.medafrica.mavex.model.enums.EmailStatus;
import com.medafrica.mavex.model.logistics.Order;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Trace complete de chaque tentative d'envoi email.
 * Gere les retries automatiques via scheduler @Scheduled.
 * Table BDD : email_logs
 */
@Entity
@Table(name = "email_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "to_email", nullable = false)
    private String toEmail;

    private String subject;

    /** PENDING -> SENT / FAILED -> RETRYING */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EmailStatus status = EmailStatus.PENDING;

    /** Message d'erreur SMTP si echec */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /** Nombre de tentatives effectuees */
    @Column(name = "retry_count")
    @Builder.Default
    private int retryCount = 0;

    /** Prochain retry planifie +30 min apres echec */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "smtp_config_id")
    private SmtpConfig smtpConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_template_id")
    private EmailTemplate emailTemplate;

    /** null si email non-transactionnel (activation compte...) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public void markSent() {
        this.status = EmailStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String error) {
        this.status       = EmailStatus.FAILED;
        this.errorMessage = error;
        this.retryCount++;
        this.nextRetryAt  = LocalDateTime.now().plusMinutes(30);
    }

    public void markRetrying() {
        this.status      = EmailStatus.RETRYING;
        this.nextRetryAt = null;
    }
}