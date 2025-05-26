package org.gyula.onlineinvoiceapi.model;

/**
 * The RegisterRequest class represents a data model for handling user registration
 * requests in the application. It encapsulates the necessary information required
 * for registering a new user, such as email, username, password, an optional token,
 * and an associated apartment ID.
 *
 * This class is typically used to transfer registration details from the client
 * to the server during the registration process. The provided data is later
 * processed and validated to create a new user account in the system.
 *
 * Methods in this class allow for retrieving and updating the email, username,
 * password, token, and apartment ID fields as needed.
 *
 * It is essential to handle instances of this class securely to protect
 * sensitive information, particularly the user's password.
 */
public class RegisterRequest {
    private String email;
    private String username;
    private String password;
    private String token;
    private String apartmentId;

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getApartmentId() {
        return apartmentId;
    }

    public void setApartmentId(String apartmentId) {
        this.apartmentId = apartmentId;
    }
}

