package org.gyula.onlineinvoiceapi.model;

import jakarta.persistence.*;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;

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

