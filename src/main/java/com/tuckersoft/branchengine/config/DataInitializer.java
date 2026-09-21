package com.tuckersoft.branchengine.config;

import com.tuckersoft.branchengine.user.Role;
import com.tuckersoft.branchengine.user.User;
import com.tuckersoft.branchengine.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Crea al administrador al arrancar, con las credenciales del .env. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.display-name}")
    private String adminName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setDisplayName(adminName);
        admin.setRole(Role.ROLE_ADMIN);
        admin.setCreatedAt(Instant.now());
        userRepository.save(admin);
        log.info("Administrador {} creado", adminEmail);
    }
}
