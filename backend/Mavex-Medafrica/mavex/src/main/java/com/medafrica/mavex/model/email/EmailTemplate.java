package com.medafrica.mavex.model.email;

import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Template HTML cree dans GrapesJS builder.
 *
 * Variables supportees dans htmlContent et subject :
 *   {{hawb}}            Order.hawb
 *   {{receiverName}}    Client.fullName
 *   {{deliveryAddress}} Client.address + city + state + zipCode
 *   {{goodsDescription}} Order.goodsDescription
 *   {{dutyAmount}}      Order.dutyAmount (present si Version 2)
 *   {{paymentLink}}     /pay/{token}
 *   {{shipperName}}     Shipper.companyName
 *
 * type (NotificationTypeEntity) :
 *   PAYMENT_INVOICE_NO_AMOUNT   = Version 1 (sans montant - mail standard Med Africa)
 *   PAYMENT_INVOICE_WITH_AMOUNT = Version 2 (avec montant USD xxx)
 *
 * Table BDD : email_templates
 */
@Entity
@Table(name = "email_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private NotificationTypeEntity type;

    /** Label affiche a l'agent dans la liste */
    @Column(nullable = false)
    private String name;

    /** Sujet email - peut contenir des {{variables}} */
    @Column(nullable = false)
    private String subject;

    /**
     * HTML final genere par GrapesJS avec {{variables}}.
     * Stocke en TEXT (pas de limite 255 chars).
     */
    @Lob
    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent;

    /**
     * JSON editor.getProjectData() de GrapesJS.
     * Recharge avec editor.loadProjectData(json) pour modification.
     */
    @Lob
    @Column(name = "builder_json", columnDefinition = "TEXT")
    private String builderJson;

    /**
     * Paragraphes intro (avant total-box) — modifiables par Quill.
     * Injectes entre BODY_START et BODY_END dans htmlContent.
     */
    @Lob
    @Column(name = "body_content", columnDefinition = "TEXT")
    private String bodyContent;

    /**
     * Paragraphes apres total-box — modifiables par Quill.
     * Injectes entre BODY_AFTER_START et BODY_AFTER_END dans htmlContent.
     */
    @Lob
    @Column(name = "body_after_content", columnDefinition = "TEXT")
    private String bodyAfterContent;

    /** Seul le template actif est utilise par type */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Resout les {{variables}} dans le HTML avant envoi */
    public String resolveHtml(Map<String, String> variables) {
        String html = this.htmlContent != null ? this.htmlContent : "";
        for (Map.Entry<String, String> e : variables.entrySet()) {
            html = html.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        return html;
    }

    /** Resout les {{variables}} dans le sujet */
    public String resolveSubject(Map<String, String> variables) {
        String sub = this.subject != null ? this.subject : "";
        for (Map.Entry<String, String> e : variables.entrySet()) {
            sub = sub.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        return sub;
    }
}