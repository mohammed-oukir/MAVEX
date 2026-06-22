package com.medafrica.mavex.dto.notification;

import com.medafrica.mavex.model.enums.NotificationLevel;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResponse {

    private Long              id;
    private String            title;
    private String            message;
    private NotificationLevel level;
    private String            link;
    private boolean           read;
    private LocalDateTime     createdAt;
}
