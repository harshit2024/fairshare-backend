
package com.fairShare.fairshare_backend.expenses;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "expense_splits",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_expense_split_user",
                columnNames = {"expense_id", "user_id"}
        )
)
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "expense_id", nullable = false)
    private UUID expenseId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "share_amount", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal shareAmount;

    @Column(precision = 7, scale = 4)
    private BigDecimal percentage;

    @Column(precision = 12, scale = 4)
    private BigDecimal shares;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getExpenseId() { return expenseId; }
    public UUID getUserId() { return userId; }
    public BigDecimal getShareAmount() { return shareAmount; }
    public BigDecimal getPercentage() { return percentage; }
    public BigDecimal getShares() { return shares; }

    public void setExpenseId(UUID value) { expenseId = value; }
    public void setUserId(UUID value) { userId = value; }
    public void setShareAmount(BigDecimal value) { shareAmount = value; }
    public void setPercentage(BigDecimal value) { percentage = value; }
    public void setShares(BigDecimal value) { shares = value; }
}