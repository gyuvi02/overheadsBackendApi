package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.model.ElectricityMeterValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for performing CRUD operations and custom queries
 * on the ElectricityMeterValues entity.
 *
 * This repository provides methods to access and manipulate electricity meter data
 * associated with apartments. It extends JpaRepository, thereby inheriting several
 * methods for database interactions, such as saving, deleting, and querying records.
 *
 * Custom Queries:
 * - findByApartmentReferenceAndLatestTrue: Retrieves the latest electricity meter values
 *   for a specific apartment, identified by its reference and marked as the latest.
 *
 * Primary functionalities:
 * - Manage electricity meter value records for apartments.
 * - Fetch the most recent electricity meter reading for a given apartment.
 */
public interface ElectricityMeterRepository extends JpaRepository<ElectricityMeterValues, Long> {
    Optional<ElectricityMeterValues> findByApartmentReferenceAndLatestTrue(Apartment apartment);
}
