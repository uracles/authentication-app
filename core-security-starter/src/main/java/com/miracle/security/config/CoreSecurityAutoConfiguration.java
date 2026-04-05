package com.miracle.security.config;

import com.miracle.security.properties.SecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;


/**
 * Spring Boot Auto-Configuration for the core-security-starter.
 *
 * Registered via META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
 *
 * My design decisions:
 * - {@code @ConditionalOnWebApplication} ensures this config only activates in servlet contexts.
 * - {@code @ConditionalOnMissingBean} on individual beans allows consuming applications to
 *   override any single component without disabling the entire starter.
 * - CSRF is disabled because this is a stateless JWT API; no session cookies are issued.
 * - Session policy is STATELESS so the SecurityContext is never persisted between requests.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(SecurityProperties.class)
@Import({
        SecurityBeansConfig.class,
        SecurityFilterConfig.class
})
public class CoreSecurityAutoConfiguration {
}