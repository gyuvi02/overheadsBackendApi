package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.model.HeatingMeterValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HeatingMeterRepository extends JpaRepository<HeatingMeterValues, Long> {

        Optional<HeatingMeterValues> findByApartmentReferenceAndLatestTrue(Apartment apartment);
}
