package com.medafrica.mavex.model.payment;

import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Configuration du paiement manuel (virement bancaire) : RIB à afficher au client.
 * Table BDD : manual_payment_configs
 */
@Entity
@Table(name = "manual_payment_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ManualPaymentConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Chemin du fichier PDF (RIB) sur le serveur */
    @Column(name = "rib_file_path")
    private String ribFilePath;

    /** Nom original du fichier pour affichage */
    @Column(name = "rib_file_name")
    private String ribFileName;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
