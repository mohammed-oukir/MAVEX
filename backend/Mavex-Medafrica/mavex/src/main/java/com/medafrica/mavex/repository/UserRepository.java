package com.medafrica.mavex.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.medafrica.mavex.model.security.User;

public interface UserRepository extends JpaRepository<User, Long> {
 Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
     List<User> findAllByActiveTrue();
}
