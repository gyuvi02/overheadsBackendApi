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

    @Column(name = "heating_meterid")
    private String heatingMeterID;

    @Column(name = "deadline")
    private Short deadline;

    @Column(name = "language")
    private char language;

    @Column(name = "rent")
    private Integer rent;

    @Column(name = "maintenance_fee")
    private Integer maintenanceFee;

    @Column(name = "gas_unit_price")
    private Integer gasUnitPrice;

    @Column(name = "electricity_unit_price")
    private Integer electricityUnitPrice;

    @Column(name = "water_unit_price")
    private Integer waterUnitPrice;

    @Column(name = "heating_unit_price")
    private Integer heatingUnitPrice;

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

    public Short getDeadline() {
        return deadline;
    }

    public void setDeadline(Short deadline) {
        this.deadline = deadline;
    }

    public char getLanguage() {
        return language;
    }

    public void setLanguage(char language) {
        this.language = language;
    }

    public Integer getRent() {
        return rent;
    }

    public void setRent(Integer rent) {
        this.rent = rent;
    }

    public Integer getMaintenanceFee() {
        return maintenanceFee;
    }

    public void setMaintenanceFee(Integer maintenanceFee) {
        this.maintenanceFee = maintenanceFee;
    }

    public Integer getGasUnitPrice() {
        return gasUnitPrice;
    }

    public void setGasUnitPrice(Integer gasUnitPrice) {
        this.gasUnitPrice = gasUnitPrice;
    }

    public Integer getElectricityUnitPrice() {
        return electricityUnitPrice;
    }

    public void setElectricityUnitPrice(Integer electricityUnitPrice) {
        this.electricityUnitPrice = electricityUnitPrice;
    }

    public Integer getWaterUnitPrice() {
        return waterUnitPrice;
    }

    public void setWaterUnitPrice(Integer waterUnitPrice) {
        this.waterUnitPrice = waterUnitPrice;
    }

    public Integer getHeatingUnitPrice() {
        return heatingUnitPrice;
    }

    public void setHeatingUnitPrice(Integer heatingUnitPrice) {
        this.heatingUnitPrice = heatingUnitPrice;
    }

    public String getHeatingMeterID() {
        return heatingMeterID;
    }

    public void setHeatingMeterID(String heatingMeterID) {
        this.heatingMeterID = heatingMeterID;
    }
}
