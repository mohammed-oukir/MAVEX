package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.imports.ImportLog;
import com.medafrica.mavex.model.imports.ImportRowLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportRowLogRepository extends JpaRepository<ImportRowLog, Long> {

    List<ImportRowLog> findByImportLog(ImportLog importLog);
}