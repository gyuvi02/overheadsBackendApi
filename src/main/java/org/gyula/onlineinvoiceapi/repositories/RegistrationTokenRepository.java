package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.RegistrationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for managing RegistrationToken entities.
 * Extends JpaRepository to provide CRUD operations and custom queries
 * for interacting with the registration token data.
 *
 * Primary functionalities:
 * - Persist and retrieve registration token data.
 * - Check for available and unused tokens associated with user registration or activation.
 *
 * Custom Queries:
 * - findByTokenAndIsUsedFalse: Finds the registration token by its token value
 *   and ensures that the token has not been used.
 *
 * This repository acts as a key component in managing the lifecycle
 * of registration tokens, including their generation, validation, and expiration.
 */
public interface RegistrationTokenRepository extends JpaRepository<RegistrationToken, Long> {
    Optional<RegistrationToken> findByTokenAndIsUsedFalse(String token);
}
