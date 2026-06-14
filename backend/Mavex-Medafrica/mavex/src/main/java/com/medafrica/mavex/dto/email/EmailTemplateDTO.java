package com.medafrica.mavex.dto.email;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailTemplateDTO {

    private Long id;

    private String subject;

    /** Paragraphes intro (avant le total-box) — éditable par Quill */
    private String bodyContent;

    /** Paragraphes après le total-box — éditable par Quill */
    private String bodyAfterContent;

    /** HTML complet depuis la DB — lecture seule, pour l'aperçu */
    private String htmlContent;
}
