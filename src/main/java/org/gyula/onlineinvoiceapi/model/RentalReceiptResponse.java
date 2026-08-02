package org.gyula.onlineinvoiceapi.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RentalReceiptResponse {

    private Long id;
    private String receiptNumber;
    private Long apartmentId;
    private String tenantName;
    private String propertyAddress;
    private Integer periodYear;
    private Integer periodMonth;
    private LocalDate issueDate;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private RentalReceiptStatus status;
    private String pdfBase64;

    public RentalReceiptResponse(RentalReceipt receipt) {
        this(receipt, null);
    }

    public RentalReceiptResponse(RentalReceipt receipt, String pdfBase64) {
        this.id = receipt.getId();
        this.receiptNumber = receipt.getReceiptNumber();
        this.apartmentId = receipt.getApartment().getId();
        this.tenantName = receipt.getTenantName();
        this.propertyAddress = receipt.getPropertyAddress();
        this.periodYear = receipt.getPeriodYear();
        this.periodMonth = receipt.getPeriodMonth();
        this.issueDate = receipt.getIssueDate();
        this.paymentDate = receipt.getPaymentDate();
        this.paymentMethod = receipt.getPaymentMethod();
        this.totalAmount = receipt.getTotalAmount();
        this.status = receipt.getStatus();
        this.pdfBase64 = pdfBase64;
    }

    public Long getId() { return id; }
    public String getReceiptNumber() { return receiptNumber; }
    public Long getApartmentId() { return apartmentId; }
    public String getTenantName() { return tenantName; }
    public String getPropertyAddress() { return propertyAddress; }
    public Integer getPeriodYear() { return periodYear; }
    public Integer getPeriodMonth() { return periodMonth; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public RentalReceiptStatus getStatus() { return status; }
    public String getPdfBase64() { return pdfBase64; }
}
