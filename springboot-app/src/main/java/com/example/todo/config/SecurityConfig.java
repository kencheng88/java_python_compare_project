package com.example.todo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration class for Spring Security.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * In FastAPI, there is no single global security configuration file. Security schemes
 * (like OAuth2, API key headers, etc.) are declared as dependencies and applied selectively
 * to individual route handlers.
 * In Spring Boot, Spring Security provides a centralized security filter chain configured via Java code.
 * The {@link SecurityFilterChain} bean enables customization of CSRF, CORS, session policies,
 * route matching permissions, and custom filter execution order. Enabling `@EnableMethodSecurity` allows us
 * to secure controller methods using standard annotations like {@code @PreAuthorize("hasRole('ADMIN')")}.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/style.css",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/**",
                    "/api/debug/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new TokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
