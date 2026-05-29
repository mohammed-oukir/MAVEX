package com.medafrica.mavex.model.imports;

import com.medafrica.mavex.model.enums.ImportStatus;
import com.medafrica.mavex.model.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Trace chaque import Excel du manifeste.
 * Detecte les doublons via fileHash (MD5 du fichier).
 * Detecte les lignes deja importees via HAWB existant.
 * Table BDD : import_logs
 */
@Entity
@Table(name = "import_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom du fichier uploade ex: manifeste_palais_d_art.xlsx */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /**
     * Hash MD5 du fichier.
     * Si fileHash existe deja en BDD -> fichier deja importe -> SKIPPED global.
     */
    @Column(name = "file_hash", unique = true)
    private String fileHash;

    /** MAWB trouve dans le fichier ex: 147-44480365 */
    @Column
    private String mawb;

    /** Nombre total de lignes dans le fichier Excel */
    @Column(name = "total_rows")
    private int totalRows;

    /** Lignes importees avec succes en BDD */
    @Column(name = "success_rows")
    private int successRows;

    /**
     * Lignes ignorees :
     * - HAWB deja existant en BDD
     * - Fichier deja importe (fileHash)
     */
    @Column(name = "skipped_rows")
    private int skippedRows;

    /**
     * Lignes en erreur :
     * - Email manquant
     * - Customs value invalide
     * - Donnees manquantes obligatoires
     */
    @Column(name = "failed_rows")
    private int failedRows;

    /** JSON detail des erreurs par ligne */
    @Lob
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    /** JSON detail des lignes ignorees + raison */
    @Lob
    @Column(name = "skipped_details", columnDefinition = "TEXT")
    private String skippedDetails;

    /** Agent qui a uploade le fichier */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by")
    private User importedBy;

    /** SUCCESS / PARTIAL / FAILED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ImportStatus status = ImportStatus.FAILED;

    /** Detail ligne par ligne */
    @OneToMany(mappedBy = "importLog", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ImportRowLog> rowLogs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "imported_at", updatable = false)
    private LocalDateTime importedAt;
}