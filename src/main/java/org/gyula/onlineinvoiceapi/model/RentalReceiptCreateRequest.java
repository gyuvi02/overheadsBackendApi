package org.gyula.onlineinvoiceapi.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RentalReceiptCreateRequest {

    private Long apartmentId;
    private Integer periodYear;
    private Integer periodMonth;
    private LocalDate issueDate;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private BigDecimal rentAmount;
    private BigDecimal utilityAmount;
    private BigDecimal maintenanceFee;
    private BigDecimal cleaningAmount;
    private String otherText;
    private BigDecimal otherAmount;

    public Long getApartmentId() { return apartmentId; }
    public void setApartmentId(Long apartmentId) { this.apartmentId = apartmentId; }
    public Integer getPeriodYear() { return periodYear; }
    public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
    public Integer getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getRentAmount() { return rentAmount; }
    public void setRentAmount(BigDecimal rentAmount) { this.rentAmount = rentAmount; }
    public BigDecimal getUtilityAmount() { return utilityAmount; }
    public void setUtilityAmount(BigDecimal utilityAmount) { this.utilityAmount = utilityAmount; }
    public BigDecimal getMaintenanceFee() { return maintenanceFee; }
    public void setMaintenanceFee(BigDecimal maintenanceFee) { this.maintenanceFee = maintenanceFee; }
    public BigDecimal getCleaningAmount() { return cleaningAmount; }
    public void setCleaningAmount(BigDecimal cleaningAmount) { this.cleaningAmount = cleaningAmount; }
    public String getOtherText() { return otherText; }
    public void setOtherText(String otherText) { this.otherText = otherText; }
    public BigDecimal getOtherAmount() { return otherAmount; }
    public void setOtherAmount(BigDecimal otherAmount) { this.otherAmount = otherAmount; }
}
