package org.gyula.onlineinvoiceapi.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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

}
