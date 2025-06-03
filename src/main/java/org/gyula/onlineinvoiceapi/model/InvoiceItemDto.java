package org.gyula.onlineinvoiceapi.model;

public class InvoiceItemDto {
    private String apartmentAddress;
    private String email;
    private String rent;
    private String previousGas;
    private String previousGasDate;
    private String actualGas;
    private String actualGasDate;
    private String gasCost;
    private String previousElectricity;
    private String previousElectricityDate;
    private String actualElectricity;
    private String actualElectricityDate;
    private String electricityCost;
    private String previousWater;
    private String previousWaterDate;
    private String actualWater;
    private String actualWaterDate;
    private String waterCost;
    private String cleaning;
    private String commonCost;
    private String otherText;
    private String otherSum;
    private String totalSum;
    private String language;

    public InvoiceItemDto(String rent, String previousGas, String actualGas, String gasCost, String previousElectricity, String actualElectricity, String electricityCost, String previousWater, String actualWater, String waterCost, String cleaning, String commonCost, String totalSum) {
        this.rent = rent;
        this.previousGas = previousGas;
        this.actualGas = actualGas;
        this.gasCost = gasCost;
        this.previousElectricity = previousElectricity;
        this.actualElectricity = actualElectricity;
        this.electricityCost = electricityCost;
        this.previousWater = previousWater;
        this.actualWater = actualWater;
        this.waterCost = waterCost;
        this.cleaning = cleaning;
        this.commonCost = commonCost;
        this.totalSum = totalSum;
    }

    public String getRent() {
        return rent;
    }

    public void setRent(String rent) {
        this.rent = rent;
    }

    public String getPreviousGas() {
        return previousGas;
    }

    public void setPreviousGas(String previousGas) {
        this.previousGas = previousGas;
    }

    public String getActualGas() {
        return actualGas;
    }

    public void setActualGas(String actualGas) {
        this.actualGas = actualGas;
    }

    public String getGasCost() {
        return gasCost;
    }

    public void setGasCost(String gasCost) {
        this.gasCost = gasCost;
    }

    public String getPreviousElectricity() {
        return previousElectricity;
    }

    public void setPreviousElectricity(String previousElectricity) {
        this.previousElectricity = previousElectricity;
    }

    public String getActualElectricity() {
        return actualElectricity;
    }

    public void setActualElectricity(String actualElectricity) {
        this.actualElectricity = actualElectricity;
    }

    public String getElectricityCost() {
        return electricityCost;
    }

    public void setElectricityCost(String electricityCost) {
        this.electricityCost = electricityCost;
    }

    public String getPreviousWater() {
        return previousWater;
    }

    public void setPreviousWater(String previousWater) {
        this.previousWater = previousWater;
    }

    public String getActualWater() {
        return actualWater;
    }

    public void setActualWater(String actualWater) {
        this.actualWater = actualWater;
    }

    public String getWaterCost() {
        return waterCost;
    }

    public void setWaterCost(String waterCost) {
        this.waterCost = waterCost;
    }

    public String getCleaning() {
        return cleaning;
    }

    public void setCleaning(String cleaning) {
        this.cleaning = cleaning;
    }

    public String getCommonCost() {
        return commonCost;
    }

    public void setCommonCost(String commonCost) {
        this.commonCost = commonCost;
    }

    public String getTotalSum() {
        return totalSum;
    }

    public void setTotalSum(String totalSum) {
        this.totalSum = totalSum;
    }

    public String getApartmentAddress() {
        return apartmentAddress;
    }

    public void setApartmentAddress(String apartmentAddress) {
        this.apartmentAddress = apartmentAddress;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPreviousGasDate() {
        return previousGasDate;
    }

    public void setPreviousGasDate(String previousGasDate) {
        this.previousGasDate = previousGasDate;
    }

    public String getActualGasDate() {
        return actualGasDate;
    }

    public void setActualGasDate(String actualGasDate) {
        this.actualGasDate = actualGasDate;
    }

    public String getPreviousElectricityDate() {
        return previousElectricityDate;
    }

    public void setPreviousElectricityDate(String previousElectricityDate) {
        this.previousElectricityDate = previousElectricityDate;
    }

    public String getActualElectricityDate() {
        return actualElectricityDate;
    }

    public void setActualElectricityDate(String actualElectricityDate) {
        this.actualElectricityDate = actualElectricityDate;
    }

    public String getPreviousWaterDate() {
        return previousWaterDate;
    }

    public void setPreviousWaterDate(String previousWaterDate) {
        this.previousWaterDate = previousWaterDate;
    }

    public String getActualWaterDate() {
        return actualWaterDate;
    }

    public void setActualWaterDate(String actualWaterDate) {
        this.actualWaterDate = actualWaterDate;
    }

    public String getOtherText() {
        return otherText;
    }

    public void setOtherText(String otherText) {
        this.otherText = otherText;
    }

    public String getOtherSum() {
        return otherSum;
    }

    public void setOtherSum(String otherSum) {
        this.otherSum = otherSum;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
