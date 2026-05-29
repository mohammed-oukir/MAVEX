package com.medafrica.mavex.security.repository;

import com.medafrica.mavex.model.security.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
 
import java.util.Optional;
 
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
 
    Optional<RefreshToken> findByToken(String token);
 
    /** Révoque tous les tokens actifs d'un utilisateur (ex: reset password) */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId AND r.revoked = false")
    void revokeAllByUserId(Long userId);
}