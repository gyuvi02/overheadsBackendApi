package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.model.GasMeterValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * The GasMeterRepository interface provides the mechanism for interacting with
 * the database to manage operations for the `GasMeterValues` entity. It
 * extends the `JpaRepository` interface, benefiting from predefined CRUD
 * operations, and also defines custom queries specific to the application
 * logic.
 *
 * Methods:
 * - findByApartmentReferenceAndLatestTrue: Retrieves the latest `GasMeterValues`
 *   entry associated with a specific `Apartment` where the `latest` flag is set
 *   to true. This is used to fetch the most recent gas meter reading for the apartment.
 */
public interface GasMeterRepository extends JpaRepository<GasMeterValues, Long> {
    Optional<GasMeterValues> findByApartmentReferenceAndLatestTrue(Apartment apartment);
}
