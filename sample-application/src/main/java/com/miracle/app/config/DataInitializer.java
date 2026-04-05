package com.miracle.app.config;

import com.miracle.app.model.User;
import com.miracle.app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeds the database with default users on first startup.
 *
 * My design decision: using ApplicationRunner (not @PostConstruct) ensures JPA is fully
 * initialised before we attempt writes.
 *
 * IMPORTANT: These credentials will have to be changed before deploying to production,
 * It is best to bootstrap admins via a secrets-manager-backed migration script.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUser("admin",   "AdminPass1!", Set.of("ROLE_ADMIN", "ROLE_USER"));
        seedUser("tinubu",    "UserPass1!",  Set.of("ROLE_USER"));
    }

    private void seedUser(String username, String rawPassword, Set<String> roles) {
        if (userRepository.existsByUsername(username)) {
            log.debug("Seed user '{}' already exists – skipping.", username);
            return;
        }
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .roles(roles)
                .enabled(true)
                .build();
        userRepository.save(user);
        log.info("Seeded user '{}' with roles {}", username, roles);
    }
}
