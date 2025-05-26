package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * UserRepository interface provides methods for CRUD operations and custom query methods
 * associated with the User entity.
 *
 * This interface extends the JpaRepository interface, which provides JPA related methods
 * for standard data access operations.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String email);

    User findByApartmentId(Long id);
}

