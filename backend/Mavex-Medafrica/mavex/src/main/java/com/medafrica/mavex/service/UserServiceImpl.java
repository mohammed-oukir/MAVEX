package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.user.UserRequestDTO;
import com.medafrica.mavex.dto.user.UserResponseDTO;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.UserRepository;
import com.medafrica.mavex.service.interfaces.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable avec l'id : " + id));
        return toResponseDTO(user);
    }

    @Override
    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà : " + dto.getEmail());
        }

        User user = User.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .build();

        return toResponseDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponseDTO update(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable avec l'id : " + id));

        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà : " + dto.getEmail());
        }

        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());

        return toResponseDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponseDTO patch(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable avec l'id : " + id));

        if (dto.getFullName() != null) user.setFullName(dto.getFullName());

        if (dto.getEmail() != null) {
            if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà : " + dto.getEmail());
            }
            user.setEmail(dto.getEmail());
        }

        if (dto.getPassword() != null) user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        if (dto.getRole()     != null) user.setRole(dto.getRole());

        return toResponseDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable avec l'id : " + id));

        String connectedEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getEmail().equals(connectedEmail)) {
            throw new IllegalStateException("Vous ne pouvez pas désactiver votre propre compte");
        }

        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable avec l'id : " + id));
        user.setActive(true);
        userRepository.save(user);
    }

    private UserResponseDTO toResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
