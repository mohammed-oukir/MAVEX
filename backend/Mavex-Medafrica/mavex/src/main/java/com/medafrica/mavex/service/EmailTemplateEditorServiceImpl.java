package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.email.EmailTemplateDTO;
import com.medafrica.mavex.model.email.EmailTemplate;
import com.medafrica.mavex.model.enums.NotificationType;
import com.medafrica.mavex.repository.EmailTemplateRepository;
import com.medafrica.mavex.service.interfaces.EmailTemplateEditorService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateEditorServiceImpl implements EmailTemplateEditorService {

    private final EmailTemplateRepository templateRepository;

    private static final Pattern CONTENT_PATTERN = Pattern.compile(
            "(<!-- BODY_START -->)(.*?)(<!-- BODY_END -->)",
            Pattern.DOTALL
    );

    // GET — retourne bodyContent (la partie texte) + htmlContent complet (pour aperçu)
    @Override
    public EmailTemplateDTO getByType(NotificationType type) {
        EmailTemplate template = templateRepository
                .findByTypeAndActiveTrue(type)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun template actif trouvé pour le type : " + type));

        String body = (template.getBodyContent() != null && !template.getBodyContent().isBlank())
                ? template.getBodyContent()
                : extraireBodyContent(template.getHtmlContent());

        return EmailTemplateDTO.builder()
                .id(template.getId())
                .subject(template.getSubject())
                .bodyContent(body)
                .htmlContent(template.getHtmlContent())
                .build();
    }

    // PUT — reçoit bodyContent modifié, le réinjecte dans htmlContent, sauvegarde les deux
    @Override
    @Transactional
    public EmailTemplateDTO update(Long id, EmailTemplateDTO dto) {
        EmailTemplate template = templateRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Template introuvable id=" + id));

        template.setSubject(dto.getSubject());
        template.setBodyContent(dto.getBodyContent());

        String nouveauHtml = remplacerBodyContent(template.getHtmlContent(), dto.getBodyContent());
        template.setHtmlContent(nouveauHtml);

        templateRepository.save(template);

        log.info("Template email mis à jour — id={} type={}", id, template.getType());

        return EmailTemplateDTO.builder()
                .id(template.getId())
                .subject(template.getSubject())
                .bodyContent(template.getBodyContent())
                .htmlContent(template.getHtmlContent())
                .build();
    }

    private String extraireBodyContent(String htmlContent) {
        if (htmlContent == null) return "";
        Matcher m = CONTENT_PATTERN.matcher(htmlContent);
        return m.find() ? m.group(2).strip() : "";
    }

    private String remplacerBodyContent(String htmlContent, String nouveauBody) {
        if (htmlContent == null) return nouveauBody;
        Matcher m = CONTENT_PATTERN.matcher(htmlContent);
        if (m.find()) {
            return m.replaceFirst(
                    Matcher.quoteReplacement(m.group(1) + "\n" + nouveauBody + "\n" + m.group(3))
            );
        }
        log.warn("Pattern <div class=\"content\"> introuvable dans htmlContent");
        return htmlContent;
    }
}
