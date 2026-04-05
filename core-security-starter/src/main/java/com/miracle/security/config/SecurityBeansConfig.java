package com.miracle.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miracle.security.exception.SecurityExceptionHandler;
import com.miracle.security.filter.JwtAuthenticationFilter;
import com.miracle.security.properties.SecurityProperties;
import com.miracle.security.token.JwtTokenProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Application-level exception handler.
 *
 * This complements the starter's SecurityExceptionHandler (which handles 401/403).
 * Reuses ApiErrorResponse from the starter so all error envelopes are consistent.
 */
@Configuration
public class SecurityBeansConfig {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider(SecurityProperties properties) {
        return new JwtTokenProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityExceptionHandler securityExceptionHandler(ObjectMapper objectMapper) {
        return new SecurityExceptionHandler(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenProvider tokenProvider,
            UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(tokenProvider, userDetailsService);
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}