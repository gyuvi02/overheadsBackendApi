package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.RegistrationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistrationTokenRepository extends JpaRepository<RegistrationToken, Long> {
    Optional<RegistrationToken> findByTokenAndIsUsedFalse(String token);
}
