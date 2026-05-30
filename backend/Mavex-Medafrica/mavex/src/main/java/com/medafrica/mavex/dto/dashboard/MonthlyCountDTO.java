package com.medafrica.mavex.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class MonthlyCountDTO {
    private String month;
    private long count;
}
