package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.model.WaterMeterValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WaterMeterRepository extends JpaRepository<WaterMeterValues, Long> {
    Optional<WaterMeterValues> findByApartmentReferenceAndLatestTrue(Apartment apartment);
}
