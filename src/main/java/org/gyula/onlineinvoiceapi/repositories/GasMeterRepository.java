package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.model.GasMeterValues;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    @Modifying
    @Transactional
    @Query("UPDATE GasMeterValues g SET g.latest = false " +
            "WHERE g.apartmentReference = :apartment " +
            "AND g.id NOT IN (SELECT g2.id FROM GasMeterValues g2 " +
            "WHERE g2.apartmentReference = :apartment " +
            "ORDER BY g2.dateOfRecording DESC LIMIT 1)")
    void updateLatestFlagExceptMostRecent(@Param("apartment") Apartment apartment);

}
