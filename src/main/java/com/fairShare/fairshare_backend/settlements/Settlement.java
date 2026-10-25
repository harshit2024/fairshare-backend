package com.fairShare.fairshare_backend.settlements;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlements")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "paid_by", nullable = false)
    private UUID paidBy;

    @Column(name = "paid_to", nullable = false)
    private UUID paidTo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "transaction_reference")
    private String transactionReference;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getGroupId() { return groupId; }
    public UUID getPaidBy() { return paidBy; }
    public UUID getPaidTo() { return paidTo; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public SettlementStatus getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getTransactionReference() { return transactionReference; }
    public String getNotes() { return notes; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setGroupId(UUID groupId) { this.groupId = groupId; }
    public void setPaidBy(UUID paidBy) { this.paidBy = paidBy; }
    public void setPaidTo(UUID paidTo) { this.paidTo = paidTo; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setStatus(SettlementStatus status) { this.status = status; }
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }
    public void setNotes(String notes) { this.notes = notes; }
    public void setSettledAt(LocalDateTime settledAt) {
        this.settledAt = settledAt;
    }
}
