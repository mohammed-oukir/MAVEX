package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.user.UserRequestDTO;
import com.medafrica.mavex.dto.user.UserResponseDTO;

import java.util.List;

public interface UserService {

    List<UserResponseDTO> findAll();

    UserResponseDTO findById(Long id);

    UserResponseDTO create(UserRequestDTO dto);

    UserResponseDTO update(Long id, UserRequestDTO dto);

    UserResponseDTO patch(Long id, UserRequestDTO dto);

    void delete(Long id);

    void activate(Long id);
}
