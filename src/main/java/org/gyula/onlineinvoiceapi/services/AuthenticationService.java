package org.gyula.onlineinvoiceapi.services;

import io.jsonwebtoken.Jwts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gyula.onlineinvoiceapi.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AuthenticationService {

    private static final Logger log = LogManager.getLogger(AuthenticationService.class);

    @Autowired
    private UserService userService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Value("${spring.validation.key}")
    private String validationKey;

    @Value("${spring.api.key}")
    private String apiKey;

    public boolean validateJwtToken(String token) {
        try {
            var claims = Jwts.parserBuilder()
                    .setSigningKey(validationKey) // Secret key for validating the JWT signature
                    .build()
                    .parseClaimsJws(token)     // Throws exception if invalid 
                    .getBody();
            log.info("JWT token expiration date: {}", claims.getExpiration());
            return true; // Token is valid
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean checkApiKey(String providedApiKey) {
        if (!Objects.equals(providedApiKey, apiKey)) {
            log.error("Invalid credentials or API key.");
            return true;
        }
        return false;
    }

    public String validateRequest(String apiKey, String authorizationHeader) throws Exception {

        log.info("validateRequest called");

        //limiting connections per second
        if (!userService.isAllowed()) {
            throw new IllegalArgumentException("Rate limit exceeded. Try again later.");
        }
        if (checkApiKey(apiKey)) {
            log.error("Invalid API key.");
            throw new Exception("Invalid credentials or API key.");
        }

        String jwtToken = authorizationHeader.replace("Bearer ", "");
        log.info("JWT Token: {}", jwtToken);
        if (!validateJwtToken(jwtToken)) {
//        if (false) {
            log.error("Invalid or expired JWT token.");
            throw new Exception("Invalid or expired JWT token.");
        }

        String username = JwtUtil.validateTokenAndExtractUsername(jwtToken);

        return username;
    }

    /**
     * Checks if a given username belongs to a user with administrative privileges.
     * The method verifies if the user has the "ROLE_ADMIN" authority assigned.
     * Logs an error message if the user does not have admin authority.
     *
     * @param username the username of the user to check for administrative privileges
     * @return true if the user has administrative privileges; false otherwise
     */
    public boolean checkAdminAuthority(String username) {
        if (customUserDetailsService.loadUserByUsername(username).getAuthorities().stream()
                .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            log.error("Only administrator can send email, user {} is not authorized.", customUserDetailsService.loadUserByUsername(username).getUsername());
            return false;
        }
        log.info("User {} is authorized.", customUserDetailsService.loadUserByUsername(username).getUsername());
        return true;
    }
}
