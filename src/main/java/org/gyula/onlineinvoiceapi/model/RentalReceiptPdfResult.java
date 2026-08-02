package org.gyula.onlineinvoiceapi.model;

public class RentalReceiptPdfResult {

    private final RentalReceipt receipt;
    private final String pdfBase64;

    public RentalReceiptPdfResult(RentalReceipt receipt, String pdfBase64) {
        this.receipt = receipt;
        this.pdfBase64 = pdfBase64;
    }

    public RentalReceipt getReceipt() {
        return receipt;
    }

    public String getPdfBase64() {
        return pdfBase64;
    }
}
