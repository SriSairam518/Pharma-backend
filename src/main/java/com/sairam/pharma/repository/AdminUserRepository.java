package com.sairam.pharma.repository;

import com.sairam.pharma.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByUsername(String username);

    Optional<AdminUser> findByEmail(String email);

    // Used to find the row when verifying a reset link
    Optional<AdminUser> findByResetToken(String resetToken);

    boolean existsByUsername(String username);
}