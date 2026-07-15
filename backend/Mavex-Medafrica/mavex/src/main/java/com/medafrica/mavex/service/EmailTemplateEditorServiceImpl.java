package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.email.EmailTemplateDTO;
import com.medafrica.mavex.model.email.EmailTemplate;
import com.medafrica.mavex.model.email.NotificationTypeEntity;
import com.medafrica.mavex.repository.EmailTemplateRepository;
import com.medafrica.mavex.repository.NotificationTypeRepository;
import com.medafrica.mavex.service.interfaces.EmailTemplateEditorService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service        
@RequiredArgsConstructor
public class EmailTemplateEditorServiceImpl implements EmailTemplateEditorService {

    private final EmailTemplateRepository templateRepository;
    private final NotificationTypeRepository notificationTypeRepository;

    // GET — retourne bodyContent + bodyAfterContent + htmlContent complet (pour aperçu)
    @Override
    public EmailTemplateDTO getByType(String type) {
        EmailTemplate template = templateRepository
                .findByType_NameAndActiveTrue(type)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun template actif trouvé pour le type : " + type));

        return EmailTemplateDTO.builder()
                .id(template.getId())
                .type(template.getType().getName())
                .name(template.getName())
                .subject(template.getSubject())
                .htmlContent(template.getHtmlContent())
                .builderJson(template.getBuilderJson())
                .build();
    }

    // PUT — reçoit subject + htmlContent + builderJson, sauvegarde directement (plus de regex bodyContent)
    @Override
    @Transactional
    public EmailTemplateDTO update(Long id, EmailTemplateDTO dto) {
        EmailTemplate template = templateRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Template introuvable id=" + id));

        if (dto.getSubject() != null) template.setSubject(dto.getSubject());
        if (dto.getHtmlContent() != null) template.setHtmlContent(dto.getHtmlContent());
        if (dto.getBuilderJson() != null) template.setBuilderJson(dto.getBuilderJson());

        templateRepository.save(template);

        log.info("Template email mis à jour — id={} type={}", id, template.getType().getName());

        return EmailTemplateDTO.builder()
                .id(template.getId())
                .type(template.getType().getName())
                .name(template.getName())
                .subject(template.getSubject())
                .htmlContent(template.getHtmlContent())
                .builderJson(template.getBuilderJson())
                .build();
    }

    // POST — crée un nouveau template pour un type qui n'existe pas encore en base
    @Override
    @Transactional
    public EmailTemplateDTO create(EmailTemplateDTO dto) {
        NotificationTypeEntity type = notificationTypeRepository.findByName(dto.getType())
                .orElseThrow(() -> new EntityNotFoundException(
                        "NotificationType introuvable : " + dto.getType()));

        if (templateRepository.findByType_NameAndActiveTrue(type.getName()).isPresent()) {
            throw new IllegalStateException("Un template actif existe déjà pour ce type : " + type.getName());
        }

        EmailTemplate template = EmailTemplate.builder()
                .type(type)
                .name(dto.getName() != null && !dto.getName().isBlank()
                        ? dto.getName() : dto.getType())
                .subject(dto.getSubject())
                .htmlContent(dto.getHtmlContent())
                .builderJson(dto.getBuilderJson())
                .active(true)
                .build();

        templateRepository.save(template);

        log.info("Template email créé — id={} type={}", template.getId(), template.getType().getName());

        return EmailTemplateDTO.builder()
                .id(template.getId())
                .type(template.getType().getName())
                .name(template.getName())
                .subject(template.getSubject())
                .htmlContent(template.getHtmlContent())
                .builderJson(template.getBuilderJson())
                .build();
    }

    // GET ALL — un DTO par NotificationTypeEntity ; id=null si pas de ligne active en base
    @Override
    public List<EmailTemplateDTO> listAll() {
        return notificationTypeRepository.findAll().stream()
                .map(type -> templateRepository.findByType_NameAndActiveTrue(type.getName())
                        .map(t -> EmailTemplateDTO.builder()
                                .id(t.getId())
                                .type(t.getType().getName())
                                .name(t.getName())
                                .subject(t.getSubject())
                                .htmlContent(t.getHtmlContent())
                                .builderJson(t.getBuilderJson())
                                .build())
                        .orElseGet(() -> EmailTemplateDTO.builder().type(type.getName()).build()))
                .toList();
    }
}
