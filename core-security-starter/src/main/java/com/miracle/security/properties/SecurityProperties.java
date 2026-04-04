package com.miracle.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    @NotBlank(message = "app.security.jwt-secret must not be blank")
    @Size(min = 32, message = "app.security.jwt-secret must be at least 32 characters for HS256")
    private String jwtSecret = "default-insecure-secret-change-me-in-production!";

    @Min(value = 60_000, message = "Token expiry must be at least 60 seconds")
    private long jwtExpirationMs = 86_400_000L;

    @NotBlank
    private String jwtIssuer = "core-security-starter";

    private String[] publicPaths = {
            "/api/public/**",
            "/actuator/health"
    };

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

    public long getJwtExpirationMs() { return jwtExpirationMs; }
    public void setJwtExpirationMs(long jwtExpirationMs) { this.jwtExpirationMs = jwtExpirationMs; }

    public String getJwtIssuer() { return jwtIssuer; }
    public void setJwtIssuer(String jwtIssuer) { this.jwtIssuer = jwtIssuer; }

    public String[] getPublicPaths() { return publicPaths; }
    public void setPublicPaths(String[] publicPaths) { this.publicPaths = publicPaths; }
}
