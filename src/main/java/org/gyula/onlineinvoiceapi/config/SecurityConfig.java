package org.gyula.onlineinvoiceapi.config;

import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("In securityFilterChain");

        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF if needed
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/login", "/api/v1/register", "/api/v1/user/*", "/api/v1/admin/*").permitAll() // Allow unrestricted access to login and register
//                        .requestMatchers("/api/v1/sendEmail").hasRole("ADMIN") // Restrict access to ADMIN role
                        .anyRequest().authenticated() // Secure all other endpoints
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:4200")); //accepts requests from port 4200 while testing
//        configuration.setAllowedOriginPatterns(Collections.singletonList("http://localhost:*""
        configuration.setAllowedOriginPatterns(Collections.singletonList("http://*")); //these version accepts wildcards, this accepts everything as I couldn't add another value next to localhostt
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "API-KEY"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
