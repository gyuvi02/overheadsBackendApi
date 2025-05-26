package org.gyula.onlineinvoiceapi.model;

import jakarta.persistence.*;

import java.sql.Timestamp;

/**
 * Represents a registration token that is utilized for user registration
 * or activation processes. This class is mapped to a database entity
 * using JPA annotations.
 *
 * The RegistrationToken entity includes the following fields:
 * - A unique identifier for the token.
 * - The token value (a unique string).
 * - A flag indicating whether the token has been used.
 * - An expiration timestamp that specifies the validity period of the token.
 * - An optional user email address to associate the token with a specific user.
 *
 * The class provides getter and setter methods for all its attributes.
 * It is typically used to generate, manage, and validate registration
 * tokens throughout the user registration lifecycle.
 */
@Entity
public class RegistrationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;
    private boolean isUsed;
    private Timestamp expiration;

    // Optional: Store any metadata (e.g., user ID or email) for traceability
    private String userEmail;

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public Timestamp getExpiration() {
        return expiration;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setIsUsed(boolean used) {
        isUsed = used;
    }

    public void setExpiration(Timestamp expiration) {
        this.expiration = expiration;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}

