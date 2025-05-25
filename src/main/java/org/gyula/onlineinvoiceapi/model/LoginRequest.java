package org.gyula.onlineinvoiceapi.model;

/**
 * The LoginRequest class represents a data model for handling login
 * requests in the application. It encapsulates the user's credentials,
 * which include a username and a password.
 *
 * This class is typically used to transfer login details from the client
 * to the server for authentication purposes.
 *
 * Methods in this class provide access to the username and password fields,
 * allowing retrieval of these credentials as needed. It is important to
 * handle instances of this class securely to protect sensitive user
 * information.
 */
public class LoginRequest {
    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

