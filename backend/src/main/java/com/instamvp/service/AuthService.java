package com.instamvp.service;

import com.instamvp.model.AppUser;
import com.instamvp.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** @throws IllegalArgumentException se os dados forem inválidos ou o username já existir. */
    public AppUser register(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username é obrigatório");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("senha precisa ter pelo menos 4 caracteres");
        }
        String trimmed = username.trim();
        if (appUserRepository.existsByUsername(trimmed)) {
            throw new IllegalArgumentException("username já cadastrado");
        }

        AppUser user = new AppUser();
        user.setUsername(trimmed);
        user.setPasswordHash(passwordEncoder.encode(password));
        return appUserRepository.save(user);
    }
}
