package org.gyula.onlineinvoiceapi.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "heating_meter_values")
public class HeatingMeterValues {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generate the `id` value
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "apartment_reference", nullable = false)
    private Apartment apartmentReference;

    @Column(name = "heating_value", nullable = false)
    private Integer heatingValue;

    @Column(name = "date_of_recording", nullable = false)
    private LocalDateTime dateOfRecording;

    @Column(name = "latest", nullable = false)
    private Boolean latest;

    @Column(name = "consumption", nullable = true)
    private Integer consumption;

    //    @Lob
    @Column(name = "image_file", columnDefinition = "BYTEA", nullable = true)
    private byte[] imageFile;

    @Column(name = "invoice", columnDefinition = "BYTEA", nullable = true)
    private byte[] invoice;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Apartment getApartmentReference() {
        return apartmentReference;
    }

    public void setApartmentReference(Apartment apartmentReference) {
        this.apartmentReference = apartmentReference;
    }

    public Integer getHeatingValue() {
        return heatingValue;
    }

    public void setHeatingValue(Integer heatingValue) {
        this.heatingValue = heatingValue;
    }

    public LocalDateTime getDateOfRecording() {
        return dateOfRecording;
    }

    public void setDateOfRecording(LocalDateTime dateOfRecording) {
        this.dateOfRecording = dateOfRecording;
    }

    public Boolean getLatest() {
        return latest;
    }

    public void setLatest(Boolean latest) {
        this.latest = latest;
    }

    public byte[] getImageFile() {
        return imageFile;
    }

    public void setImageFile(byte[] imageFile) {
        this.imageFile = imageFile;
    }

    public Integer getConsumption() {
        return consumption;
    }

    public void setConsumption(Integer consumption) {
        this.consumption = consumption;
    }

    public byte[] getInvoice() {
        return invoice;
    }

    public void setInvoice(byte[] invoice) {
        this.invoice = invoice;
    }
}


