package org.gyula.onlineinvoiceapi.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "rental_receipt_sequences",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rental_receipt_sequence_apartment_year",
                columnNames = {"apartment_id", "receipt_year"}
        )
)
public class RentalReceiptSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(name = "receipt_year", nullable = false)
    private Integer receiptYear;

    @Column(name = "last_number", nullable = false)
    private Integer lastNumber;

    public Long getId() {
        return id;
    }

    public Apartment getApartment() {
        return apartment;
    }

    public void setApartment(Apartment apartment) {
        this.apartment = apartment;
    }

    public Integer getReceiptYear() {
        return receiptYear;
    }

    public void setReceiptYear(Integer receiptYear) {
        this.receiptYear = receiptYear;
    }

    public Integer getLastNumber() {
        return lastNumber;
    }

    public void setLastNumber(Integer lastNumber) {
        this.lastNumber = lastNumber;
    }
}
