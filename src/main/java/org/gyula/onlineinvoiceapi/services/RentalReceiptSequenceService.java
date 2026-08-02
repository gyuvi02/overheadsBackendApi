package org.gyula.onlineinvoiceapi.services;

import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.model.RentalReceiptSequence;
import org.gyula.onlineinvoiceapi.repositories.ApartmentRepository;
import org.gyula.onlineinvoiceapi.repositories.RentalReceiptSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RentalReceiptSequenceService {

    private final RentalReceiptSequenceRepository sequenceRepository;
    private final ApartmentRepository apartmentRepository;

    public RentalReceiptSequenceService(
            RentalReceiptSequenceRepository sequenceRepository,
            ApartmentRepository apartmentRepository) {
        this.sequenceRepository = sequenceRepository;
        this.apartmentRepository = apartmentRepository;
    }

    @Transactional
    public String nextReceiptNumber(Long apartmentId, int year) {
        RentalReceiptSequence sequence = sequenceRepository
                .findByApartmentIdAndReceiptYear(apartmentId, year)
                .orElseGet(() -> createSequence(apartmentId, year));

        int nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        sequenceRepository.save(sequence);

        return String.format("BIZ-%d-%d-%06d", apartmentId, year, nextNumber);
    }

    private RentalReceiptSequence createSequence(Long apartmentId, int year) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("Apartment not found with id: " + apartmentId));

        RentalReceiptSequence sequence = new RentalReceiptSequence();
        sequence.setApartment(apartment);
        sequence.setReceiptYear(year);
        sequence.setLastNumber(0);
        return sequenceRepository.saveAndFlush(sequence);
    }
}
