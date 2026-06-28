package com.sairam.pharma.security;

import com.sairam.pharma.entity.AdminUser;
import com.sairam.pharma.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder     passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.email}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        if (adminUserRepository.count() > 0) {
            log.info("Admin account already exists");
            return;
        }

        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()
                || adminEmail == null || adminEmail.isBlank()) {
            log.error("ADMIN_USERNAME, ADMIN_PASSWORD, ADMIN_EMAIL env vars must all be set");
            return;
        }

        adminUserRepository.save(AdminUser.builder()
                .username(adminUsername.trim())
                .passwordHash(passwordEncoder.encode(adminPassword))
                .email(adminEmail.trim().toLowerCase())
                .build());

        log.info("Admin account created");
    }
}