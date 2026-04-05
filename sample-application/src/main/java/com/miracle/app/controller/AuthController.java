package com.miracle.app.controller;

import com.miracle.app.model.LoginRequest;
import com.miracle.app.model.LoginResponse;
import com.miracle.app.model.RegisterRequest;
import com.miracle.app.model.RegisterResponse;
import com.miracle.app.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public authentication endpoints (no JWT required).
 * These paths are covered by the /api/public/** permit-all rule in the starter.
 */
@RestController
@RequestMapping("/api/public")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UP");
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.register(registerRequest));
    }
}
