package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.model.ElectricityMeterValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ElectricityMeterRepository extends JpaRepository<ElectricityMeterValues, Long> {
    Optional<ElectricityMeterValues> findByApartmentReferenceAndLatestTrue(Apartment apartment);
}
