package com.medafrica.mavex.model.finance;

import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Taux de change entre deux devises, figé à la date d'import.
 * Table BDD : exchange_rates
 */
@Entity
@Table(
    name = "exchange_rates",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_exchange_rate_pair_date",
        columnNames = {"from_currency", "to_currency", "effective_date"}
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Code ISO 4217 de la devise source ex: USD */
    @Column(name = "from_currency", length = 3, nullable = false)
    private String fromCurrency;

    /** Code ISO 4217 de la devise cible ex: MAD */
    @Column(name = "to_currency", length = 3, nullable = false)
    private String toCurrency;

    /** Taux de conversion ex: 10.05 pour 1 USD = 10.05 MAD */
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal rate;

    /** Date à partir de laquelle ce taux est applicable */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    /** Source du taux ex: Bank Al-Maghrib, Manuel, API */
    @Column(length = 100)
    private String source;

    /** false = taux remplacé par une version plus récente */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Agent qui a saisi ou importé ce taux */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by")
    private User importedBy;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
