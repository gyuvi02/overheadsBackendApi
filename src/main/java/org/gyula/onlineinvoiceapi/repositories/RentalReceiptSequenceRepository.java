package org.gyula.onlineinvoiceapi.repositories;

import jakarta.persistence.LockModeType;
import org.gyula.onlineinvoiceapi.model.RentalReceiptSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface RentalReceiptSequenceRepository extends JpaRepository<RentalReceiptSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RentalReceiptSequence> findByApartmentIdAndReceiptYear(Long apartmentId, Integer receiptYear);
}
