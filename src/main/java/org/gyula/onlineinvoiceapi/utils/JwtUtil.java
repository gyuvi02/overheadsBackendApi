package org.gyula.onlineinvoiceapi.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class JwtUtil {

    private static final String SECRET_KEY = "mhsJzZ9HpjbsAHFDi0KmIA40qe6XH2G/k+dRQ9AhN8HdPPWue5/3+mMGo4m54jderPzLziBdFf4tnGoPzAOg3w=="; // Use a strong, secure key

    public static String generateToken(String username, long expirationMillis) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();
    }

    public static String validateTokenAndExtractUsername(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
