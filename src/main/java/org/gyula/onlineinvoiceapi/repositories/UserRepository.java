package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String email);
}

