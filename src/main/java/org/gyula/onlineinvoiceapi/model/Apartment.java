package org.gyula.onlineinvoiceapi.model;

import jakarta.persistence.*;

/**
 * The Apartment class represents an apartment entity in the system.
 * It is mapped to the "apartments" table in the database and includes
 * attributes such as city, zip code, street address, and identifiers
 * for gas, electricity, and water meters.
 *
 * This class provides getter and setter methods for all its fields,
 * allowing for manipulation and retrieval of apartment-related data.
 *
 * An instance of this class represents a single row in the database table.
 */
@Entity
@Table(name = "apartments")  // Name of the table in the database
public class Apartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "zip", nullable = false)
    private String zip;

    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "gas_meterid")
    private String gasMeterID;

    @Column(name = "electricity_meterid")
    private String electricityMeterID;

    @Column(name = "water_meterid")
    private String waterMeterID;

    @Column(name = "deadline")
    private String deadline;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getGasMeterID() {
        return gasMeterID;
    }

    public void setGasMeterID(String gasMeterID) {
        this.gasMeterID = gasMeterID;
    }

    public String getElectricityMeterID() {
        return electricityMeterID;
    }

    public void setElectricityMeterID(String electricityMeterID) {
        this.electricityMeterID = electricityMeterID;
    }

    public String getWaterMeterID() {
        return waterMeterID;
    }

    public void setWaterMeterID(String waterMeterID) {
        this.waterMeterID = waterMeterID;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }
}
