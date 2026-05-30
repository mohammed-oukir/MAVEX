package com.medafrica.mavex.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class StatusCountDTO {
    private String status;
    private long count;
}
