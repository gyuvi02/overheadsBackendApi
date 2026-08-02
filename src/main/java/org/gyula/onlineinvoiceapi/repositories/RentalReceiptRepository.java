package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.RentalReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RentalReceiptRepository extends JpaRepository<RentalReceipt, Long> {

    Optional<RentalReceipt> findByReceiptNumber(String receiptNumber);

    List<RentalReceipt> findByApartmentIdAndPeriodYearOrderByPeriodMonthDesc(Long apartmentId, Integer periodYear);
}
