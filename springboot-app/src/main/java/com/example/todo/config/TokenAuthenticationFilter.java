package com.example.todo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter that parses a simple Bearer token or request parameter to authenticate users.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * In FastAPI, dependency injection is used to handle security (e.g., using {@code Depends(HTTPBearer())}
 * or {@code Depends(oauth2_scheme)}). The security logic is executed as a dependency before the path
 * operation function runs.
 * In Spring Boot, Spring Security uses a servlet Filter Chain. This filter runs early in the HTTP request
 * lifecycle, extracting credentials and populating Spring's thread-local {@link SecurityContextHolder}.
 * Subscriptions to endpoints are then authorized based on roles configured in the security chain or
 * via annotation-based security rules ({@code @PreAuthorize}).
 * </p>
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;

        // 1. Try to extract token from the Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        }

        // 2. Fallback to query parameter "token" (useful for testing and Thymeleaf views)
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
        }

        if (token != null && !token.isEmpty()) {
            UsernamePasswordAuthenticationToken authentication = null;

            if ("admin-token".equals(token)) {
                List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_USER")
                );
                authentication = new UsernamePasswordAuthenticationToken("admin", null, authorities);
            } else if ("user-token".equals(token)) {
                List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_USER")
                );
                authentication = new UsernamePasswordAuthenticationToken("user", null, authorities);
            }

            if (authentication != null) {
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
