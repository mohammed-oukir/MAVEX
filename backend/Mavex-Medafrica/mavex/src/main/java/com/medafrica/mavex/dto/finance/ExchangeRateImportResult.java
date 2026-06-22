package com.medafrica.mavex.dto.finance;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExchangeRateImportResult {

    private int imported;
    private int skipped;
    private int errors;

    @Builder.Default
    private List<String> errorDetails = new ArrayList<>();
}
