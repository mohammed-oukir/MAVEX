package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.imports.ImportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ImportLogRepository extends JpaRepository<ImportLog, Long> {

    /** Détecte si ce fichier a déjà été importé (hash MD5) */
    Optional<ImportLog> findByFileHash(String fileHash);

    boolean existsByFileHash(String fileHash);

    /** Nombre d'imports dont importedAt est dans l'intervalle [start, end). */
    long countByImportedAtBetween(LocalDateTime start, LocalDateTime end);

    /** Nombre d'imports depuis une date donnée (fenêtre glissante). */
    long countByImportedAtAfter(LocalDateTime since);

    /** Sommes totalRows/successRows/failedRows depuis une date donnée, pour le taux de succès pondéré. */
    @Query("SELECT COALESCE(SUM(i.totalRows), 0), COALESCE(SUM(i.successRows), 0), COALESCE(SUM(i.failedRows), 0) " +
           "FROM ImportLog i WHERE i.importedAt >= :since")
    List<Object[]> sumRowsSince(@Param("since") LocalDateTime since);
}