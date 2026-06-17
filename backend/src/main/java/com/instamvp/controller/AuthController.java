package com.instamvp.controller;

import com.instamvp.dto.AuthRequest;
import com.instamvp.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Endpoint público: cria um novo usuário. Não loga automaticamente — o cliente faz login em seguida com as mesmas credenciais. */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        try {
            var user = authService.register(request.getUsername(), request.getPassword());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("username", user.getUsername()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint protegido (exige HTTP Basic válido) usado como "login": o
     * cliente manda usuário/senha no header Authorization e, se vier 200, as
     * credenciais são válidas — o Flutter usa isso pra validar o login antes
     * de salvá-las e usá-las nas próximas chamadas.
     */
    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        return Map.of("username", authentication.getName());
    }
}
