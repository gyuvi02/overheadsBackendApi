package org.gyula.onlineinvoiceapi.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * The GasMeterValues class represents the recorded gas meter readings
 * associated with apartments. It is mapped to the "gas_meter_values" table
 * in the database and contains information such as the gas meter reading
 * value, the associated apartment, the date of recording, and whether the
 * record is the latest one.
 *
 * Each instance of this class corresponds to a single row in the "gas_meter_values" table.
 *
 * Attributes:
 * - id: Auto-generated unique identifier for the gas meter reading entry.
 * - apartmentReference: A reference to the associated apartment entity.
 * - gasValue: The recorded gas meter reading value.
 * - dateOfRecording: The timestamp of when the gas meter reading was recorded.
 * - latest: A flag indicating if this is the latest reading for the apartment.
 * - imageFile: Optional binary data for an image file associated with the record, stored in BYTEA format.
 */
@Entity
@Table(name = "gas_meter_values")
public class GasMeterValues {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generate the `id` value
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "apartment_reference", nullable = false)
    private Apartment apartmentReference;

    @Column(name = "gas_value", nullable = false)
    private Integer gasValue;


    @Column(name = "date_of_recording", nullable = false)
    private LocalDateTime dateOfRecording;

    @Column(name = "latest", nullable = false)
    private Boolean latest;

//    @Lob
    @Column(name = "image_file", columnDefinition = "BYTEA", nullable = true)
    private byte[] imageFile;

    @Column(name = "consumption", nullable = true)
    private Integer consumption;


    // Getters and Setters
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

    public Integer getGasValue() {
        return gasValue;
    }

    public void setGasValue(Integer gasValue) {
        this.gasValue = gasValue;
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
