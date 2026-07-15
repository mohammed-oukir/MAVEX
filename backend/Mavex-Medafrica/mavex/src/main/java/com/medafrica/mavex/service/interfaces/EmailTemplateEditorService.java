package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.email.EmailTemplateDTO;

import java.util.List;

public interface EmailTemplateEditorService {

    /** Charge le template actif par type — retourne id + subject + bodyContent */
    EmailTemplateDTO getByType(String type);

    /** Sauvegarde subject + bodyContent, reconstruit htmlContent complet */
    EmailTemplateDTO update(Long id, EmailTemplateDTO dto);

    /** Crée un nouveau template pour un type sans ligne en base */
    EmailTemplateDTO create(EmailTemplateDTO dto);

    /** Liste tous les NotificationTypeEntity — ceux sans ligne en base ont id=null */
    List<EmailTemplateDTO> listAll();
}
