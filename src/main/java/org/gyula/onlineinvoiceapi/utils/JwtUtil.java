package org.gyula.onlineinvoiceapi.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Utility class for handling JSON Web Tokens (JWTs).
 * Provides methods to generate a new JWT and validate an existing one.
 */
@Service
public class JwtUtil {

    private static String sKey = null;

    //To inject a static value from the properties file
    @Autowired
    public JwtUtil(@Value("${spring.validation.key}") String sKey) {
        this.sKey = sKey;
    }

    public static String generateToken(String username, long expirationMillis) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(SignatureAlgorithm.HS512, sKey)
                .compact();
    }

    public static String validateTokenAndExtractUsername(String token) {
        return Jwts.parser()
                .setSigningKey(sKey)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

}
