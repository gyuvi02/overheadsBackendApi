package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.model.GasMeterValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GasMeterRepository extends JpaRepository<GasMeterValues, Long> {
    Optional<GasMeterValues> findByApartmentReferenceAndLatestTrue(Apartment apartment);
}
