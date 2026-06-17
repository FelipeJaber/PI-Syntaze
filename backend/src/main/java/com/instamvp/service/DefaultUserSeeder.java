package com.instamvp.service;

import com.instamvp.model.AppUser;
import com.instamvp.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Garante que o usuário admin padrão (de application.yml) sempre exista, sem precisar cadastrar manualmente. */
@Component
@Order(0)
public class DefaultUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultUserSeeder.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultUsername;
    private final String defaultPassword;

    public DefaultUserSeeder(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                              @Value("${security.demo.username:admin}") String defaultUsername,
                              @Value("${security.demo.password:admin123}") String defaultPassword) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultUsername = defaultUsername;
        this.defaultPassword = defaultPassword;
    }

    @Override
    public void run(String... args) {
        if (appUserRepository.existsByUsername(defaultUsername)) {
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(defaultUsername);
        user.setPasswordHash(passwordEncoder.encode(defaultPassword));
        appUserRepository.save(user);
        log.info("Usuário padrão '{}' criado (credenciais em application.yml: security.demo.*)", defaultUsername);
    }
}
