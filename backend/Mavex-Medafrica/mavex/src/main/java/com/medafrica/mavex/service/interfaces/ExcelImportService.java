package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.imports.ImportConfirmRequest;
import com.medafrica.mavex.dto.imports.ImportLogResponse;
import com.medafrica.mavex.dto.imports.ImportPreviewResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ExcelImportService {

    ImportLogResponse importManifest(MultipartFile file) throws Exception;

    ImportPreviewResponse previewManifest(MultipartFile file) throws Exception;

    ImportLogResponse confirmImport(ImportConfirmRequest request) throws Exception;

    byte[] generateTemplate() throws Exception;
}
