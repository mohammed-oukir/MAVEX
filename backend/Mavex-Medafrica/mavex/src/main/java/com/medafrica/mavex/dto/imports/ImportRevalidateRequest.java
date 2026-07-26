package com.medafrica.mavex.dto.imports;

import lombok.Data;

import java.util.List;

@Data
public class ImportRevalidateRequest {
    private List<ImportPreviewResponse.PreviewRow> rows;
}
