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

    private static final Pattern BODY_PATTERN = Pattern.compile(
            "(<!-- BODY_START -->)(.*?)(<!-- BODY_END -->)",
            Pattern.DOTALL
    );

    private static final Pattern BODY_AFTER_PATTERN = Pattern.compile(
            "(<!-- BODY_AFTER_START -->)(.*?)(<!-- BODY_AFTER_END -->)",
            Pattern.DOTALL
    );

    // GET — retourne bodyContent + bodyAfterContent + htmlContent complet (pour aperçu)
    @Override
    public EmailTemplateDTO getByType(NotificationType type) {
        EmailTemplate template = templateRepository
                .findByTypeAndActiveTrue(type)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun template actif trouvé pour le type : " + type));

        String body = (template.getBodyContent() != null && !template.getBodyContent().isBlank())
                ? template.getBodyContent()
                : extraire(template.getHtmlContent(), BODY_PATTERN);

        String bodyAfter = (template.getBodyAfterContent() != null && !template.getBodyAfterContent().isBlank())
                ? template.getBodyAfterContent()
                : extraire(template.getHtmlContent(), BODY_AFTER_PATTERN);

        return EmailTemplateDTO.builder()
                .id(template.getId())
                .subject(template.getSubject())
                .bodyContent(body)
                .bodyAfterContent(bodyAfter)
                .htmlContent(template.getHtmlContent())
                .build();
    }

    // PUT — reçoit bodyContent + bodyAfterContent, les réinjecte dans htmlContent
    @Override
    @Transactional
    public EmailTemplateDTO update(Long id, EmailTemplateDTO dto) {
        EmailTemplate template = templateRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Template introuvable id=" + id));

        template.setSubject(dto.getSubject());
        template.setBodyContent(dto.getBodyContent());
        template.setBodyAfterContent(dto.getBodyAfterContent());

        String html = template.getHtmlContent();
        html = remplacer(html, BODY_PATTERN, dto.getBodyContent());
        if (dto.getBodyAfterContent() != null) {
            html = remplacer(html, BODY_AFTER_PATTERN, dto.getBodyAfterContent());
        }
        template.setHtmlContent(html);

        templateRepository.save(template);

        log.info("Template email mis à jour — id={} type={}", id, template.getType());

        return EmailTemplateDTO.builder()
                .id(template.getId())
                .subject(template.getSubject())
                .bodyContent(template.getBodyContent())
                .bodyAfterContent(template.getBodyAfterContent())
                .htmlContent(template.getHtmlContent())
                .build();
    }

    private String extraire(String html, Pattern pattern) {
        if (html == null) return "";
        Matcher m = pattern.matcher(html);
        return m.find() ? m.group(2).strip() : "";
    }

    private String remplacer(String html, Pattern pattern, String nouveauBody) {
        if (html == null) return nouveauBody != null ? nouveauBody : "";
        Matcher m = pattern.matcher(html);
        if (m.find()) {
            return m.replaceFirst(
                    Matcher.quoteReplacement(m.group(1) + "\n" + nouveauBody + "\n" + m.group(3))
            );
        }
        return html;
    }
}
