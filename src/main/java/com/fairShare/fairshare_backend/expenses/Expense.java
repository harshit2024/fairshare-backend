
package com.fairShare.fairshare_backend.expenses;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "paid_by", nullable = false)
    private UUID paidBy;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ExpenseCategory category = ExpenseCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    private SplitType splitType = SplitType.EQUAL;

    @Column(name = "expense_date", nullable = false)
    private LocalDateTime expenseDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (expenseDate == null) expenseDate = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getGroupId() { return groupId; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public UUID getPaidBy() { return paidBy; }
    public UUID getCreatedBy() { return createdBy; }
    public ExpenseCategory getCategory() { return category; }
    public SplitType getSplitType() { return splitType; }
    public LocalDateTime getExpenseDate() { return expenseDate; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setGroupId(UUID value) { groupId = value; }
    public void setDescription(String value) { description = value; }
    public void setAmount(BigDecimal value) { amount = value; }
    public void setCurrency(String value) { currency = value; }
    public void setPaidBy(UUID value) { paidBy = value; }
    public void setCreatedBy(UUID value) { createdBy = value; }
    public void setCategory(ExpenseCategory value) { category = value; }
    public void setSplitType(SplitType value) { splitType = value; }
    public void setExpenseDate(LocalDateTime value) { expenseDate = value; }
    public void setNotes(String value) { notes = value; }
}