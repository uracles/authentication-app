package com.miracle.app.service;

import com.miracle.app.model.*;
import com.miracle.app.repository.UserRepository;
import com.miracle.security.token.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application-level authentication service.
 *
 * My design decisions:
 * - Delegates credential verification to Spring Security's AuthenticationManager so
 *   the logic benefits from security events, BadCredentialsException, etc.
 * - JWT generation is delegated to JwtTokenProvider (from the starter), keeping the
 *   application free of JWT plumbing.
 * - Registration intentionally assigns only ROLE_USER by default; admins must be
 *   bootstrapped via DataInitializer or a privileged admin endpoint.
 */
@Service
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider         = tokenProvider;
        this.userRepository        = userRepository;
        this.passwordEncoder       = passwordEncoder;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        UserDetails principal = (UserDetails) auth.getPrincipal();

        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB"));

        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String token = tokenProvider.generateToken(user.getId(), principal);

        return new LoginResponse(token, user.getId(), user.getUsername(), roles);
    }

    public RegisterResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + req.getUsername());
        }

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .roles(Set.of("ROLE_USER"))
                .enabled(true)
                .build();

        userRepository.save(user);

        return new RegisterResponse(
                user.getId(),
                user.getUsername(),
                List.copyOf(user.getRoles()),
                "Registration successful. Please log in."
        );
    }
}
