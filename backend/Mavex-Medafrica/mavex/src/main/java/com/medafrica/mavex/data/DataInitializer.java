package com.medafrica.mavex.data;

import com.medafrica.mavex.model.enums.UserRole;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@mavex.com")) {
            User admin = User.builder()
                    .fullName("Admin Mavex")
                    .email("admin@mavex.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(UserRole.ADMIN)
                    .build();

            userRepository.save(admin);
            log.info("✅ Admin créé → email: admin@mavex.com | password: admin123");
        } else {
            log.info("ℹ️ Admin déjà existant, rien à faire.");
        }
    }
}