package org.gyula.onlineinvoiceapi.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Configures security settings for the application.
 *
 * This class implements the Spring Security configuration for the application,
 * enabling web security, defining the security filter chain, and configuring
 * CORS and password encoding.
 *
 * Annotations:
 * - @Configuration: Marks this class as a configuration class.
 * - @EnableWebSecurity: Enables Spring Security’s web security support and provides the Spring MVC integration.
 *
 * Beans:
 * 1. SecurityFilterChain:
 *    - Configures HTTP security settings, such as disabling CSRF protection,
 *      enabling CORS with a custom configuration source, and setting authorization
 *      policies for API endpoints.
 *
 * 2. CorsConfigurationSource:
 *    - Defines the CORS security policy, including allowed origins, methods, headers, and credentials.
 *    - Uses a property from the external configuration file for allowed origins.
 *
 * 3. BCryptPasswordEncoder:
 *    - Provides an implementation of password encoding using the BCrypt hashing algorithm.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${frontend.singletonlist}")
    private String singletonList;

    public static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("In securityFilterChain");

        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF if needed
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api/v1/login", "/api/v1/register", "/api/v1/user/**", "/api/v1/admin/**").permitAll() // Allow unrestricted access to these endpoints
                        .anyRequest().authenticated() // Secure all other endpoints
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(customAccessDeniedHandler())
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Collections.singletonList(singletonList)); // Use property from application.properties
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "API-KEY", "Origin", "Accept", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Component
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public static class RequestLoggingFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest req = (HttpServletRequest) request;
            log.info("[DEBUG_LOG] Incoming request: {} {} from {}", req.getMethod(), req.getRequestURI(), req.getRemoteAddr());

            // Log headers
            Enumeration<String> headerNames = req.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    log.debug("[DEBUG_LOG] Header: {} = {}", headerName, req.getHeader(headerName));
                }
            }

            chain.doFilter(request, response);
            
            HttpServletResponse res = (HttpServletResponse) response;
            log.info("[DEBUG_LOG] Response status: {} for {} {}", res.getStatus(), req.getMethod(), req.getRequestURI());
        }
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.error("Access Denied for request: {} {}, Reason: {}", request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Access Denied: " + accessDeniedException.getMessage() + "\"}");
            }
        };
    }

}
