package com.medafrica.mavex.dto.email;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailTemplateDTO {

    private Long id;

    /** Nom du NotificationTypeEntity (ex: PAYMENT_INVOICE_WITH_AMOUNT) */
    private String type;

    /** Label affiché à l'agent dans la liste */
    private String name;

    private String subject;

    /** Paragraphes intro (avant le total-box) — éditable par Quill */
    private String bodyContent;

    /** Paragraphes après le total-box — éditable par Quill */
    private String bodyAfterContent;

    /** HTML complet depuis la DB — lecture seule, pour l'aperçu */
    private String htmlContent;

    /** JSON editor.getProjectData() de GrapesJS */
    private String builderJson;
}
