package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.email.EmailTemplateDTO;
import com.medafrica.mavex.model.enums.NotificationType;

public interface EmailTemplateEditorService {

    /** Charge le template actif par type — retourne id + subject + bodyContent */
    EmailTemplateDTO getByType(NotificationType type);

    /** Sauvegarde subject + bodyContent, reconstruit htmlContent complet */
    EmailTemplateDTO update(Long id, EmailTemplateDTO dto);
}
