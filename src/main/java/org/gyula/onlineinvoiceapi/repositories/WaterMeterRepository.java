package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.model.WaterMeterValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * WaterMeterRepository interface provides methods for CRUD operations and a custom query method
 * for accessing WaterMeterValues data associated with an apartment entity.
 *
 * This interface extends the JpaRepository interface, which provides JPA-related methods
 * for standard data access operations such as saving, deleting, and finding entities.
 *
 * Custom Methods:
 * - findByApartmentReferenceAndLatestTrue: Retrieves the latest water meter value
 *   associated with the specified apartment entity, as determined by the 'latest' flag.
 */
public interface WaterMeterRepository extends JpaRepository<WaterMeterValues, Long> {
    Optional<WaterMeterValues> findByApartmentReferenceAndLatestTrue(Apartment apartment);
}
