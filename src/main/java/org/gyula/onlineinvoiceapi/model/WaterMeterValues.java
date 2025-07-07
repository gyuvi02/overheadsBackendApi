package org.gyula.onlineinvoiceapi.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents the water meter values recorded for a specific apartment.
 * Each instance of this class corresponds to a single record in the water meter readings table.
 *
 * The class is an entity mapped to the "water_meter_values" database table
 * and includes fields for attributes such as:
 * - The unique identifier for the record
 * - A reference to the associated apartment
 * - The water meter reading value
 * - The date when the value was recorded
 * - A flag indicating whether this is the latest record
 * - An optional image file representing a snapshot of the meter
 *
 * This class provides getter and setter methods for all fields, allowing easy
 * manipulation and retrieval of water meter data.
 */
@Entity
@Table(name = "water_meter_values")
public class WaterMeterValues {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generate the `id` value
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "apartment_reference", nullable = false)
    private Apartment apartmentReference;

    @Column(name = "water_value", nullable = false)
    private Integer waterValue;

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

    public Integer getWaterValue() {
        return waterValue;
    }

    public void setWaterValue(Integer waterValue) {
        this.waterValue = waterValue;
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


