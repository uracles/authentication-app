package com.miracle.security.token;

import com.miracle.security.properties.SecurityProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLES   = "roles";

    private final SecretKey signingKey;
    private final SecurityProperties props;

    public JwtTokenProvider(SecurityProperties props) {
        this.props = props;
        this.signingKey = Keys.hmacShaKeyFor(
                props.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String userId, UserDetails userDetails) {
        Instant now    = Instant.now();
        Instant expiry = now.plusMillis(props.getJwtExpirationMs());

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(props.getJwtIssuer())
                .subject(userDetails.getUsername())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(props.getJwtIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parseAndValidate(token).getSubject();
    }

    public String extractUserId(String token) {
        return parseAndValidate(token).get(CLAIM_USER_ID, String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return (List<String>) parseAndValidate(token).get(CLAIM_ROLES, List.class);
    }

    public boolean isValid(String token) {
        try {
            parseAndValidate(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("JWT malformed: {}", ex.getMessage());
        } catch (JwtException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
        }
        return false;
    }
}
