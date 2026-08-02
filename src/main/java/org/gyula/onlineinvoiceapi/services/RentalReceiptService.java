package org.gyula.onlineinvoiceapi.services;

import org.gyula.onlineinvoiceapi.model.*;
import org.gyula.onlineinvoiceapi.repositories.ApartmentRepository;
import org.gyula.onlineinvoiceapi.repositories.RentalReceiptRepository;
import org.gyula.onlineinvoiceapi.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class RentalReceiptService {

    private final RentalReceiptRepository rentalReceiptRepository;
    private final RentalReceiptSequenceService sequenceService;
    private final RentalReceiptPdfService rentalReceiptPdfService;
    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;

    public RentalReceiptService(
            RentalReceiptRepository rentalReceiptRepository,
            RentalReceiptSequenceService sequenceService,
            RentalReceiptPdfService rentalReceiptPdfService,
            ApartmentRepository apartmentRepository,
            UserRepository userRepository) {
        this.rentalReceiptRepository = rentalReceiptRepository;
        this.sequenceService = sequenceService;
        this.rentalReceiptPdfService = rentalReceiptPdfService;
        this.apartmentRepository = apartmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RentalReceipt createReceipt(RentalReceiptCreateRequest request) {
        validateRequest(request);

        Apartment apartment = apartmentRepository.findById(request.getApartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Apartment not found with id: " + request.getApartmentId()));
        User tenant = userRepository.findByApartmentId(apartment.getId());
        if (tenant == null) {
            throw new IllegalArgumentException("No tenant is connected to apartment id: " + apartment.getId());
        }
        validateTenantData(tenant);

        String receiptNumber = sequenceService.nextReceiptNumber(apartment.getId(), request.getPeriodYear());

        RentalReceipt receipt = new RentalReceipt();
        receipt.setReceiptNumber(receiptNumber);
        receipt.setApartment(apartment);
        receipt.setTenantUser(tenant);
        receipt.setTenantName(tenant.getFullName().trim());
        receipt.setTenantAddress(tenant.getPermanentAddress().trim());
        receipt.setPropertyAddress(formatPropertyAddress(apartment));
        receipt.setPeriodYear(request.getPeriodYear());
        receipt.setPeriodMonth(request.getPeriodMonth());
        receipt.setIssueDate(defaultDate(request.getIssueDate()));
        receipt.setPaymentDate(defaultDate(request.getPaymentDate()));
        receipt.setPaymentMethod(request.getPaymentMethod());
        receipt.setRentAmount(amountOrZero(request.getRentAmount()));
        receipt.setUtilityAmount(amountOrZero(request.getUtilityAmount()));
        receipt.setMaintenanceFee(amountOrZero(request.getMaintenanceFee()));
        receipt.setCleaningAmount(amountOrZero(request.getCleaningAmount()));
        receipt.setOtherText(trimToNull(request.getOtherText()));
        receipt.setOtherAmount(amountOrZero(request.getOtherAmount()));
        receipt.setTotalAmount(calculateTotal(receipt));
        receipt.setStatus(RentalReceiptStatus.ISSUED);

        return rentalReceiptRepository.save(receipt);
    }

    @Transactional
    public RentalReceiptPdfResult createReceiptWithPdf(RentalReceiptCreateRequest request) {
        RentalReceipt receipt = createReceipt(request);
        String pdfBase64 = rentalReceiptPdfService.createPdf(receipt);
        rentalReceiptRepository.save(receipt);
        return new RentalReceiptPdfResult(receipt, pdfBase64);
    }

    private void validateRequest(RentalReceiptCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Rental receipt request is required");
        }
        if (request.getApartmentId() == null) {
            throw new IllegalArgumentException("Apartment id is required");
        }
        if (request.getPeriodYear() == null || request.getPeriodYear() < 2000) {
            throw new IllegalArgumentException("Valid period year is required");
        }
        if (request.getPeriodMonth() == null || request.getPeriodMonth() < 1 || request.getPeriodMonth() > 12) {
            throw new IllegalArgumentException("Valid period month is required");
        }
        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
    }

    private void validateTenantData(User tenant) {
        if (tenant.getFullName() == null || tenant.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant full legal name is missing");
        }
        if (tenant.getPermanentAddress() == null || tenant.getPermanentAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant permanent address is missing");
        }
    }

    private String formatPropertyAddress(Apartment apartment) {
        return apartment.getZip() + " " + apartment.getCity() + ", " + apartment.getStreet();
    }

    private LocalDate defaultDate(LocalDate date) {
        return date != null ? date : LocalDate.now();
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal calculateTotal(RentalReceipt receipt) {
        return receipt.getRentAmount()
                .add(receipt.getUtilityAmount())
                .add(receipt.getMaintenanceFee())
                .add(receipt.getCleaningAmount())
                .add(receipt.getOtherAmount());
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
