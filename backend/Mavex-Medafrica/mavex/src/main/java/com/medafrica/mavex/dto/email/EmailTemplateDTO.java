package com.medafrica.mavex.dto.email;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailTemplateDTO {

    private Long id;

    private String subject;

    /** Corps éditable par Quill — sans le layout (header/footer/CSS) */
    private String bodyContent;

    /** HTML complet depuis la DB — lecture seule, pour l'aperçu */
    private String htmlContent;
}
