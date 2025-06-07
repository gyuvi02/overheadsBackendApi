package org.gyula.onlineinvoiceapi.model;

public class emailPdfDTO {
    private String email;
    private String pdfBase64;
    private String language;
    private String apartmentAddress;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPdfBase64() {
        return pdfBase64;
    }

    public void setPdfBase64(String pdfBase64) {
        this.pdfBase64 = pdfBase64;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getApartmentAddress() {
        return apartmentAddress;
    }

    public void setApartmentAddress(String apartmentAddress) {
        this.apartmentAddress = apartmentAddress;
    }
}
