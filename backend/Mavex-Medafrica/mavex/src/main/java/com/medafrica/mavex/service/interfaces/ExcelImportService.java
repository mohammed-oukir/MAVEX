package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.imports.ImportLogResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ExcelImportService {

    ImportLogResponse importManifest(MultipartFile file) throws Exception;
}
