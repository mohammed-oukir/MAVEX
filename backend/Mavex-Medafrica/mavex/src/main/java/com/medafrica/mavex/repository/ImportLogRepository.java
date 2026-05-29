package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.imports.ImportLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImportLogRepository extends JpaRepository<ImportLog, Long> {

    /** Détecte si ce fichier a déjà été importé (hash MD5) */
    Optional<ImportLog> findByFileHash(String fileHash);

    boolean existsByFileHash(String fileHash);
}