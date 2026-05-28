package com.medafrica.mavex.model.imports;

import com.medafrica.mavex.model.enums.ImportRowStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Detail d'une ligne lors d'un import Excel.
 * Une ligne par HAWB tente d'etre importee.
 * Table BDD : import_row_logs
 */
@Entity
@Table(name = "import_row_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportRowLog {

    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK -> import_logs.id */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_log_id", nullable = false)
    private ImportLog importLog;

    /** Numero de ligne dans le fichier Excel (commence a 2 - ligne 1 = header) */
    @Column(name = "row_number")
    private int rowNumber;

    /** Valeur du champ "Connote #" dans cette ligne */
    @Column(name = "hawb")
    private String hawb;

    /** Email trouve dans cette ligne */
    @Column(name = "receiver_email")
    private String receiverEmail;

    /** IMPORTED / SKIPPED / FAILED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportRowStatus status;

    /**
     * Raison du statut ex:
     * - "HAWB already exists in database"
     * - "Receiver email is missing"
     * - "Customs value is invalid"
     * - "File already imported (hash match)"
     */
    @Column(length = 500)
    private String reason;

    /** Corrections silencieuses appliquées sur cette ligne (séparées par " | ") */
    @Column(name = "warnings", columnDefinition = "TEXT")
    private String warnings;
}