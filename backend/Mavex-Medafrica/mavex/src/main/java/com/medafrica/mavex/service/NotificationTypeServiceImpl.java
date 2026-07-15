package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.email.NotificationTypeDTO;
import com.medafrica.mavex.model.email.NotificationTypeEntity;
import com.medafrica.mavex.repository.NotificationTypeRepository;
import com.medafrica.mavex.service.interfaces.NotificationTypeService;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationTypeServiceImpl implements NotificationTypeService {

    private final NotificationTypeRepository repository;

    @Override
    public List<NotificationTypeDTO> getAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public NotificationTypeDTO create(String name) {
        if (repository.existsByName(name)) {
            throw new EntityExistsException("NotificationType existe déjà : " + name);
        }
        NotificationTypeEntity entity = NotificationTypeEntity.builder()
                .name(name)
                .build();
        return toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public NotificationTypeDTO update(Long id, String name) {
        repository.findByName(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new IllegalStateException("Un type avec ce nom existe déjà."); });

        NotificationTypeEntity entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("NotificationType introuvable id=" + id));
        entity.setName(name);
        return toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        NotificationTypeEntity entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("NotificationType introuvable id=" + id));
        try {
            repository.delete(entity);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Ce type est utilisé par un template email et ne peut pas être supprimé.");
        }
    }

    private NotificationTypeDTO toDto(NotificationTypeEntity entity) {
        return NotificationTypeDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
