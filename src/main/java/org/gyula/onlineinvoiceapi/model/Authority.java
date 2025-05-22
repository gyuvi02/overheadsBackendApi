package org.gyula.onlineinvoiceapi.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;

/**
 * Represents an authority granted to a user in the system.
 * This entity is mapped to the "authorities" table in the database
 * and implements the {@link GrantedAuthority} interface, which is commonly
 * used in Spring Security to represent an authority (or role) assigned to a user.
 *
 * Each Authority is associated with a specific {@link User}, indicating
 * which user the authority belongs to. The authority itself is represented
 * as a string value.
 *
 * The class includes fields for authority name and references the user it is
 * associated with. The authority field represents the granted role or permission
 * while the user field maps this authority to the corresponding user entity.
 *
 * This class follows JPA annotations to define the entity structure,
 * relationships, and table mappings.
 */
@Entity
@Table(name = "authorities")
public class Authority implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String authority;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Getters and Setters
    @Override
    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}

