package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.dto.user.UserRequestDTO;
import com.medafrica.mavex.dto.user.UserResponseDTO;
import com.medafrica.mavex.dto.user.UserSearchCriteria;
import com.medafrica.mavex.dto.user.UserStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    List<UserResponseDTO> findAll();

    Page<UserResponseDTO> search(UserSearchCriteria criteria, Pageable pageable);

    UserStatsResponse getStats();

    UserResponseDTO findById(Long id);

    UserResponseDTO create(UserRequestDTO dto);

    UserResponseDTO update(Long id, UserRequestDTO dto);

    UserResponseDTO patch(Long id, UserRequestDTO dto);

    void delete(Long id);

    void activate(Long id);
}
