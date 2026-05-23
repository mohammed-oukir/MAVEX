package com.medafrica.mavex.security.repository;
import com.medafrica.mavex.model.security.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    /** Récupère le dernier OTP valide (non utilisé, non expiré) pour cet email */
    @Query("SELECT o FROM PasswordResetOtp o WHERE o.email = :email AND o.used = false ORDER BY o.createdAt DESC")
    Optional<PasswordResetOtp> findLatestValidByEmail(String email);

    /** Invalide tous les anciens OTPs de cet email avant d'en créer un nouveau */
    @Modifying
    @Query("UPDATE PasswordResetOtp o SET o.used = true WHERE o.email = :email AND o.used = false")
    void invalidateAllByEmail(String email);
}