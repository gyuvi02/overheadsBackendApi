package org.gyula.onlineinvoiceapi.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents the electricity meter values associated with a specific apartment.
 * This entity is mapped to the "electricity_meter_values" table in the database.
 *
 * Each instance corresponds to a record of electricity usage for a given apartment,
 * including details like the recorded electricity value, the date of the recording,
 * and whether it is the latest record.
 *
 * Fields:
 * - id: Unique identifier for the record, automatically generated.
 * - apartmentReference: The apartment entity to which this electricity meter value belongs.
 * - electricityValue: The electricity meter reading value.
 * - dateOfRecording: The date and time when the value was recorded.
 * - latest: Indicates whether this record represents the latest reading for the apartment.
 * - imageFile: An optional image file associated with the reading, stored as a byte array.
 *
 * This class provides getter and setter methods for all fields, enabling
 * manipulation and retrieval of electricity meter data.
 */
@Entity
@Table(name = "electricity_meter_values")
public class ElectricityMeterValues {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generate the `id` value
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "apartment_reference", nullable = false)
    private Apartment apartmentReference;

    @Column(name = "electricity_value", nullable = false)
    private Integer electricityValue;

    @Column(name = "date_of_recording", nullable = false)
    private LocalDateTime dateOfRecording;

    @Column(name = "latest", nullable = false)
    private Boolean latest;

//    @Lob
    @Column(name = "image_file", columnDefinition = "BYTEA", nullable = true)
    private byte[] imageFile;

    @Column(name = "consumption", nullable = true)
    private Integer consumption;

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

    public Integer getElectricityValue() {
        return electricityValue;
    }

    public void setElectricityValue(Integer gasValue) {
        this.electricityValue = gasValue;
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
}

