package org.gyula.onlineinvoiceapi.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "rental_receipts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rental_receipts_receipt_number",
                columnNames = "receipt_number"
        )
)
public class RentalReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_number", nullable = false, length = 50)
    private String receiptNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_user_id", nullable = false)
    private User tenantUser;

    @Column(name = "tenant_name", nullable = false)
    private String tenantName;

    @Column(name = "tenant_address", nullable = false)
    private String tenantAddress;

    @Column(name = "property_address", nullable = false)
    private String propertyAddress;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "rent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal rentAmount;

    @Column(name = "utility_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal utilityAmount;

    @Column(name = "maintenance_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal maintenanceFee;

    @Column(name = "cleaning_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal cleaningAmount;

    @Column(name = "other_text")
    private String otherText;

    @Column(name = "other_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RentalReceiptStatus status;

    @Column(name = "pdf_path")
    private String pdfPath;

    @Column(name = "pdf_sha256", length = 64)
    private String pdfSha256;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = RentalReceiptStatus.ISSUED;
        }
    }

    public Long getId() { return id; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public Apartment getApartment() { return apartment; }
    public void setApartment(Apartment apartment) { this.apartment = apartment; }
    public User getTenantUser() { return tenantUser; }
    public void setTenantUser(User tenantUser) { this.tenantUser = tenantUser; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public String getTenantAddress() { return tenantAddress; }
    public void setTenantAddress(String tenantAddress) { this.tenantAddress = tenantAddress; }
    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }
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
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public RentalReceiptStatus getStatus() { return status; }
    public void setStatus(RentalReceiptStatus status) { this.status = status; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public String getPdfSha256() { return pdfSha256; }
    public void setPdfSha256(String pdfSha256) { this.pdfSha256 = pdfSha256; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
