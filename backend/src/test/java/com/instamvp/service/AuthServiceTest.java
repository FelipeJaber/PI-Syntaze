package com.instamvp.service;

import com.instamvp.model.AppUser;
import com.instamvp.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Testa as regras de validação de cadastro (AuthService.register). */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(appUserRepository, passwordEncoder);
    }

    @Test
    void registersUserWithHashedPassword() {
        when(appUserRepository.existsByUsername("nike_fan")).thenReturn(false);
        when(appUserRepository.save(org.mockito.ArgumentMatchers.any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppUser user = authService.register("nike_fan", "senha123");

        assertEquals("nike_fan", user.getUsername());
        assertNotEquals("senha123", user.getPasswordHash()); // nunca guarda senha em texto puro
        assertTrue(passwordEncoder.matches("senha123", user.getPasswordHash()));
    }

    @Test
    void rejectsDuplicateUsername() {
        when(appUserRepository.existsByUsername("admin")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register("admin", "senha123"));
        assertTrue(ex.getMessage().contains("já cadastrado"));
    }

    @Test
    void rejectsPasswordShorterThanFourChars() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register("novo_usuario", "123"));
        assertTrue(ex.getMessage().contains("4 caracteres"));
    }

    @Test
    void rejectsBlankUsername() {
        assertThrows(IllegalArgumentException.class, () -> authService.register("  ", "senha123"));
        assertThrows(IllegalArgumentException.class, () -> authService.register(null, "senha123"));
    }
}
