package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.model.ElectricityMeterValues;
import org.gyula.onlineinvoiceapi.model.GasMeterValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ElectricityMeterRepository extends JpaRepository<ElectricityMeterValues, Long> {
    Optional<ElectricityMeterValues> findByApartmentReferenceAndLatestTrue(Apartment apartment);

}
