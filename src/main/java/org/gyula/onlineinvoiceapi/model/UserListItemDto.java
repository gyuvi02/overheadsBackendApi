package org.gyula.onlineinvoiceapi.model;

/**
 * The UserListItemDto class is a Data Transfer Object (DTO) used to represent a summarized
 * view of a user, typically for listing purposes.
 *
 * This class encapsulates the following user-related information:
 * - The unique identifier of the user (id)
 * - The username of the user
 * - The email address of the user
 * - The identifier of the apartment associated with the user, if any
 *
 * It serves as a lightweight object for transferring user data between different
 * layers of the application, such as between the service and the presentation layer.
 *
 * Appropriate getter and setter methods are provided for all fields to ensure
 * encapsulation and allow controlled access to the attributes.
 */
public class UserListItemDto {
    private Long id;
    private String username;
    private String email;
    private Long apartmentId;

    public UserListItemDto(Long id, String username, String email, Long apartmentId) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.apartmentId = apartmentId;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public Long getApartmentId() { return apartmentId; }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setApartmentId(Long apartmentId) {
        this.apartmentId = apartmentId;
    }
}

