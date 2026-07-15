package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.email.NotificationTypeDTO;

import java.util.List;

public interface NotificationTypeService {

    List<NotificationTypeDTO> getAll();

    NotificationTypeDTO create(String name);

    NotificationTypeDTO update(Long id, String name);

    void delete(Long id);
}
