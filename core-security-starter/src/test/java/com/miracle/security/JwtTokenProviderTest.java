package com.miracle.security;

import com.miracle.security.properties.SecurityProperties;
import com.miracle.security.token.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private SecurityProperties props;

    @BeforeEach
    void setUp() {
        props = new SecurityProperties();
        props.setJwtSecret("test-secret-key-must-be-at-least-32-chars!!");
        props.setJwtExpirationMs(60_000L);
        props.setJwtIssuer("test-issuer");
        tokenProvider = new JwtTokenProvider(props);
    }

    private UserDetails buildUser(String username, String... roles) {
        return User.withUsername(username)
                   .password("irrelevant")
                   .authorities(java.util.Arrays.stream(roles)
                           .map(SimpleGrantedAuthority::new)
                           .toList())
                   .build();
    }

    @Test
    @DisplayName("generateToken produces a non-blank JWT")
    void generateToken_isNotBlank() {
        String token = tokenProvider.generateToken("user-1", buildUser("alice", "ROLE_USER"));
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("isValid returns true for a freshly generated token")
    void isValid_freshToken() {
        String token = tokenProvider.generateToken("user-1", buildUser("alice", "ROLE_USER"));
        assertThat(tokenProvider.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("isValid returns false for a garbage string")
    void isValid_garbage() {
        assertThat(tokenProvider.isValid("not.a.jwt.at.all")).isFalse();
    }

    @Test
    @DisplayName("extractUsername returns the correct subject")
    void extractUsername() {
        String token = tokenProvider.generateToken("user-1", buildUser("alice", "ROLE_USER"));
        assertThat(tokenProvider.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    @DisplayName("extractRoles returns all granted roles")
    void extractRoles() {
        String token = tokenProvider.generateToken("user-1",
                buildUser("bob", "ROLE_USER", "ROLE_ADMIN"));
        List<String> roles = tokenProvider.extractRoles(token);
        assertThat(roles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("token signed with different secret fails validation")
    void differentSecret_failsValidation() {
        SecurityProperties other = new SecurityProperties();
        other.setJwtSecret("completely-different-secret-key-32chars!!");
        other.setJwtIssuer("test-issuer");
        JwtTokenProvider otherProvider = new JwtTokenProvider(other);

        String token = tokenProvider.generateToken("user-1", buildUser("alice", "ROLE_USER"));
        assertThat(otherProvider.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("expired token fails validation")
    void expiredToken_failsValidation() throws InterruptedException {
        props.setJwtExpirationMs(1L);
        JwtTokenProvider shortLived = new JwtTokenProvider(props);

        String token = shortLived.generateToken("user-1", buildUser("alice", "ROLE_USER"));
        Thread.sleep(10);

        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("parseAndValidate returns Claims with correct userId")
    void parseAndValidate_userId() {
        String token = tokenProvider.generateToken("uuid-42", buildUser("carol", "ROLE_USER"));
        Claims claims = tokenProvider.parseAndValidate(token);
        assertThat(claims.get("userId", String.class)).isEqualTo("uuid-42");
    }
}
